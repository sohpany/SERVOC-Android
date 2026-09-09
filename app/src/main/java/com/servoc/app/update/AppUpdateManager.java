package com.servoc.app.update;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import com.servoc.app.BuildConfig;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * End-user APK updater. Scientific ChemCore library administration remains developer-only;
 * this class updates only the signed HazRob application package.
 *
 * Security model:
 *  - HTTPS manifest and APK URLs only;
 *  - monotonic versionCode (no downgrade/reinstall loop);
 *  - SHA-256 payload verification;
 *  - package-name verification;
 *  - APK signing-certificate SHA-256 pinned in the application;
 *  - Android package installer performs the final user-authorized installation.
 */
public final class AppUpdateManager {
    public static final String MANIFEST_URL = BuildConfig.APP_UPDATE_MANIFEST_URL;
    public static final String EXPECTED_PACKAGE = BuildConfig.APPLICATION_ID;
    public static final String EXPECTED_SIGNING_CERT_SHA256 =
            "d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0";
    private static final String SCHEMA = "hazrob-app-update/1";
    private static final String PREFS = "servoc";
    private static final String PENDING_CODE = "app_update_pending_version_code";
    private static final String PENDING_NAME = "app_update_pending_version_name";
    private static final String LAST_INSTALLED_NAME = "app_update_last_installed_version_name";
    private static final String LAST_INSTALLED_CODE = "app_update_last_installed_version_code";
    private static final String LAST_COMPLETED_AT = "app_update_last_completed_at";

    public interface Progress {
        void onState(State state);
    }

    public enum Phase {
        CHECKING,
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        DOWNLOADING,
        READY_TO_INSTALL,
        INSTALL_PERMISSION_REQUIRED,
        INSTALLER_OPENED,
        COMPLETED,
        ERROR
    }

    public static final class State {
        public final Phase phase;
        public final String message;
        public final int percent;
        public final UpdateInfo update;
        public final File apk;

        State(Phase phase, String message, int percent, UpdateInfo update, File apk) {
            this.phase = phase;
            this.message = message == null ? "" : message;
            this.percent = percent;
            this.update = update;
            this.apk = apk;
        }
    }

    public static final class UpdateInfo {
        public final int versionCode;
        public final String versionName;
        public final String apkUrl;
        public final String sha256;
        public final long sizeBytes;
        public final String packageName;
        public final String signingCertSha256;
        public final String notes;
        public final String publishedAtUtc;

        UpdateInfo(int versionCode, String versionName, String apkUrl, String sha256,
                   long sizeBytes, String packageName, String signingCertSha256,
                   String notes, String publishedAtUtc) {
            this.versionCode = versionCode;
            this.versionName = versionName == null ? "" : versionName;
            this.apkUrl = apkUrl == null ? "" : apkUrl;
            this.sha256 = normalizeDigest(sha256);
            this.sizeBytes = sizeBytes;
            this.packageName = packageName == null ? "" : packageName;
            this.signingCertSha256 = normalizeDigest(signingCertSha256);
            this.notes = notes == null ? "" : notes;
            this.publishedAtUtc = publishedAtUtc == null ? "" : publishedAtUtc;
        }
    }

    private final Context context;
    private final SharedPreferences prefs;

    public AppUpdateManager(Context context) {
        if (context == null) throw new IllegalArgumentException("context required");
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        reconcileInstalledVersion();
    }

    /** Detects successful installation after the new APK launches. */
    public void reconcileInstalledVersion() {
        int pending = prefs.getInt(PENDING_CODE, 0);
        if (pending > 0 && BuildConfig.VERSION_CODE >= pending) {
            String pendingName = prefs.getString(PENDING_NAME, BuildConfig.VERSION_NAME);
            prefs.edit()
                    .putInt(LAST_INSTALLED_CODE, BuildConfig.VERSION_CODE)
                    .putString(LAST_INSTALLED_NAME, pendingName == null || pendingName.isEmpty()
                            ? BuildConfig.VERSION_NAME : pendingName)
                    .putLong(LAST_COMPLETED_AT, System.currentTimeMillis())
                    .remove(PENDING_CODE)
                    .remove(PENDING_NAME)
                    .apply();
        }
    }

    public String recentCompletionLabel() {
        long at = prefs.getLong(LAST_COMPLETED_AT, 0);
        if (at <= 0 || System.currentTimeMillis() - at > 24L * 60L * 60L * 1000L) return "";
        int code = prefs.getInt(LAST_INSTALLED_CODE, 0);
        String name = prefs.getString(LAST_INSTALLED_NAME, "");
        if (code != BuildConfig.VERSION_CODE) return "";
        return name == null || name.isEmpty() ? "تم التحديث" : "تم التحديث • " + name;
    }

    /** Check only. Caller decides whether to proceed with download. */
    public UpdateInfo check() throws Exception {
        URL manifest = checkedHttps(MANIFEST_URL, "manifest");
        HttpURLConnection c = open(manifest);
        c.setRequestProperty("Accept", "application/json");
        int code = c.getResponseCode();
        if (code < 200 || code >= 300) throw new IllegalStateException("update manifest HTTP " + code);
        String text;
        try (InputStream in = new BufferedInputStream(c.getInputStream())) {
            text = readUtf8(in, 512 * 1024);
        } finally { c.disconnect(); }
        JSONObject q = new JSONObject(text);
        if (!SCHEMA.equals(q.optString("schema"))) throw new IllegalStateException("unsupported update manifest schema");
        UpdateInfo info = new UpdateInfo(
                q.optInt("version_code", 0),
                q.optString("version_name", ""),
                q.optString("apk_url", ""),
                q.optString("sha256", ""),
                q.optLong("size_bytes", -1),
                q.optString("package_name", ""),
                q.optString("signing_cert_sha256", ""),
                q.optString("notes", ""),
                q.optString("published_at_utc", ""));
        validateManifest(info);
        return info;
    }

    /** One user action: check, download when newer, verify, then open installer. */
    public void updateNow(Progress progress) {
        emit(progress, new State(Phase.CHECKING, "جار البحث عن تحديث...", 0, null, null));
        try {
            UpdateInfo info = check();
            if (info.versionCode <= BuildConfig.VERSION_CODE) {
                emit(progress, new State(Phase.UP_TO_DATE, "لا يوجد تحديث جديد", 100, info, null));
                return;
            }
            emit(progress, new State(Phase.UPDATE_AVAILABLE, "يتوفر " + info.versionName, 0, info, null));
            File apk = downloadAndVerify(info, progress);
            emit(progress, new State(Phase.READY_TO_INSTALL, "تم التنزيل والتحقق • جاهز للتثبيت", 100, info, apk));
            requestInstall(info, apk, progress);
        } catch (Exception e) {
            emit(progress, new State(Phase.ERROR, friendlyError(e), 0, null, null));
        }
    }

    private File downloadAndVerify(UpdateInfo info, Progress progress) throws Exception {
        URL apkUrl = checkedHttps(info.apkUrl, "APK");
        File dir = new File(context.getCacheDir(), "updates");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("cannot create update cache");
        File tmp = new File(dir, "HazRob-" + safe(info.versionName) + ".apk.part");
        File apk = new File(dir, "HazRob-" + safe(info.versionName) + ".apk");
        if (tmp.exists()) tmp.delete();
        if (apk.exists()) apk.delete();

        HttpURLConnection c = open(apkUrl);
        c.setRequestProperty("Accept", "application/vnd.android.package-archive,application/octet-stream,*/*");
        int status = c.getResponseCode();
        if (status < 200 || status >= 300) throw new IllegalStateException("APK HTTP " + status);
        long declared = c.getContentLengthLong();
        long total = info.sizeBytes > 0 ? info.sizeBytes : declared;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        long done = 0;
        int lastPercent = -1;
        try (InputStream in = new BufferedInputStream(c.getInputStream());
             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
            byte[] b = new byte[64 * 1024];
            for (int n; (n = in.read(b)) >= 0;) {
                if (n == 0) continue;
                out.write(b, 0, n);
                md.update(b, 0, n);
                done += n;
                int pct = total > 0 ? (int)Math.min(99, (done * 100L) / total) : -1;
                if (pct != lastPercent && (pct < 0 || pct == 0 || pct >= lastPercent + 2)) {
                    lastPercent = pct;
                    emit(progress, new State(Phase.DOWNLOADING,
                            pct >= 0 ? "جار تنزيل التحديث... " + pct + "%" : "جار تنزيل التحديث...",
                            Math.max(0, pct), info, null));
                }
            }
        } finally { c.disconnect(); }
        if (info.sizeBytes > 0 && done != info.sizeBytes)
            throw new IllegalStateException("APK size mismatch");
        String actual = hex(md.digest());
        if (!actual.equals(info.sha256)) throw new SecurityException("APK SHA-256 mismatch");
        if (!tmp.renameTo(apk)) {
            copy(tmp, apk);
            if (!tmp.delete()) tmp.deleteOnExit();
        }
        verifyArchive(info, apk);
        return apk;
    }

    private void requestInstall(UpdateInfo info, File apk, Progress progress) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.getPackageManager().canRequestPackageInstalls()) {
            prefs.edit().putInt(PENDING_CODE, info.versionCode).putString(PENDING_NAME, info.versionName).apply();
            Intent permission = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName()));
            permission.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(permission);
            emit(progress, new State(Phase.INSTALL_PERMISSION_REQUIRED,
                    "اسمح بتثبيت التحديث ثم اضغط الزر مرة أخرى", 100, info, apk));
            return;
        }
        prefs.edit().putInt(PENDING_CODE, info.versionCode).putString(PENDING_NAME, info.versionName).apply();
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", apk);
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(install);
        emit(progress, new State(Phase.INSTALLER_OPENED,
                "جار التحديث... أكمل التثبيت", 100, info, apk));
    }

    private void validateManifest(UpdateInfo i) throws Exception {
        if (i.versionCode <= 0 || i.versionName.trim().isEmpty()) throw new IllegalStateException("invalid update version");
        checkedHttps(i.apkUrl, "APK");
        if (!EXPECTED_PACKAGE.equals(i.packageName)) throw new SecurityException("update package mismatch");
        if (!EXPECTED_SIGNING_CERT_SHA256.equals(i.signingCertSha256))
            throw new SecurityException("update signer is not trusted");
        if (i.sha256.length() != 64 || !i.sha256.matches("[0-9a-f]{64}"))
            throw new SecurityException("invalid APK SHA-256");
        if (i.sizeBytes == 0) throw new IllegalStateException("invalid APK size");
    }

    private void verifyArchive(UpdateInfo info, File apk) throws Exception {
        PackageManager pm = context.getPackageManager();
        PackageInfo pi;
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pi = pm.getPackageArchiveInfo(apk.getAbsolutePath(), PackageManager.GET_SIGNING_CERTIFICATES);
            if (pi == null || pi.signingInfo == null) throw new SecurityException("cannot inspect APK signer");
            signatures = pi.signingInfo.getApkContentsSigners();
        } else {
            //noinspection deprecation
            pi = pm.getPackageArchiveInfo(apk.getAbsolutePath(), PackageManager.GET_SIGNATURES);
            //noinspection deprecation
            signatures = pi == null ? null : pi.signatures;
        }
        if (pi == null) throw new SecurityException("invalid APK archive");
        if (!EXPECTED_PACKAGE.equals(pi.packageName)) throw new SecurityException("APK package mismatch");
        if (pi.versionCode != info.versionCode) throw new SecurityException("APK versionCode mismatch");
        if (signatures == null || signatures.length == 0) throw new SecurityException("APK has no signer");
        boolean trusted = false;
        for (Signature s : signatures) {
            String digest = hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray()));
            if (EXPECTED_SIGNING_CERT_SHA256.equals(digest)) { trusted = true; break; }
        }
        if (!trusted) throw new SecurityException("APK certificate mismatch");
    }

    private static HttpURLConnection open(URL u) throws Exception {
        HttpURLConnection c = (HttpURLConnection)u.openConnection();
        c.setConnectTimeout(10_000);
        c.setReadTimeout(30_000);
        c.setInstanceFollowRedirects(true);
        c.setUseCaches(false);
        return c;
    }

    private static URL checkedHttps(String raw, String label) throws Exception {
        if (raw == null || raw.trim().isEmpty()) throw new IllegalStateException(label + " URL not configured");
        URL u = new URL(raw.trim());
        if (!"https".equalsIgnoreCase(u.getProtocol()) || u.getUserInfo() != null)
            throw new SecurityException(label + " URL must use HTTPS without credentials");
        return u;
    }

    private static String readUtf8(InputStream in, int max) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int total = 0;
        for (int n; (n = in.read(b)) >= 0;) {
            if (n == 0) continue;
            total += n;
            if (total > max) throw new IllegalStateException("update manifest too large");
            out.write(b, 0, n);
        }
        return out.toString("UTF-8");
    }

    private static void copy(File from, File to) throws Exception {
        try (InputStream in = new BufferedInputStream(new FileInputStream(from));
             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(to))) {
            byte[] b = new byte[64 * 1024];
            for (int n; (n = in.read(b)) >= 0;) if (n > 0) out.write(b, 0, n);
        }
    }

    private static String normalizeDigest(String s) {
        return s == null ? "" : s.replace(":", "").replace(" ", "").trim().toLowerCase(Locale.US);
    }
    private static String hex(byte[] x) {
        StringBuilder b = new StringBuilder(x.length * 2);
        for (byte q : x) b.append(String.format(Locale.US, "%02x", q & 255));
        return b.toString();
    }
    private static String safe(String s) {
        String x = s == null ? "update" : s.replaceAll("[^A-Za-z0-9._-]", "_");
        return x.isEmpty() ? "update" : x;
    }
    private static String friendlyError(Exception e) {
        if (e instanceof SecurityException) return "تعذر التحديث: فشل التحقق الأمني";
        String m = e.getMessage();
        if (m == null || m.trim().isEmpty()) m = e.getClass().getSimpleName();
        return "تعذر التحديث: " + m;
    }
    private static void emit(Progress p, State s) { if (p != null) p.onState(s); }
}
