package com.servoc.app.chemcore.library;

import com.servoc.app.github.GitHubSessionAuth;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** HTTPS transport for verified ChemCore updates, including the private HazRob organization library. */
public final class ChemCoreLibraryUpdateClient {
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 7000;
    public static final int DEFAULT_READ_TIMEOUT_MS = 20000;
    public static final long MAX_MANIFEST_BYTES = 512 * 1024L;
    public static final long MAX_PAYLOAD_BYTES = 256L * 1024L * 1024L;
    public static final String CANONICAL_REPOSITORY="HazRob/build-harness";
    private static final String CANONICAL_API_PREFIX="/repos/HazRob/build-harness/contents/library/";
    private static final String API_VERSION="2022-11-28";

    public static final class CheckResult {
        public final ChemCoreLibraryManifest manifest;
        public final URL manifestUrl;
        public final URL payloadUrl;
        public CheckResult(ChemCoreLibraryManifest manifest, URL manifestUrl, URL payloadUrl) {
            this.manifest = manifest; this.manifestUrl = manifestUrl; this.payloadUrl = payloadUrl;
        }
    }

    public CheckResult check(URL manifestUrl) throws Exception {
        requireHttps(manifestUrl);
        String json = getText(manifestUrl, MAX_MANIFEST_BYTES);
        ChemCoreLibraryManifest m = ChemCoreLibraryManifest.fromJson(new JSONObject(json));
        if (!m.validate().isEmpty()) throw new IllegalStateException("remote manifest invalid: " + String.join("; ", m.validate()));
        URL payload = resolveSibling(manifestUrl, m.payloadFile);
        requireHttps(payload);
        return new CheckResult(m, manifestUrl, payload);
    }

    /** Downloads only into caller-provided staging storage; this method never activates a library. */
    public File downloadPayload(CheckResult check, File stagingDir) throws Exception {
        if (check == null || check.manifest == null || check.payloadUrl == null) throw new IllegalArgumentException("check result required");
        if (check.manifest.payloadSizeBytes <= 0 || check.manifest.payloadSizeBytes > MAX_PAYLOAD_BYTES)
            throw new IllegalArgumentException("payload size outside updater policy");
        if (stagingDir == null) throw new IllegalArgumentException("staging directory required");
        if (!stagingDir.exists() && !stagingDir.mkdirs()) throw new IllegalStateException("cannot create staging directory");
        File out = new File(stagingDir, ".download-" + System.nanoTime() + ".part");

        HttpURLConnection c = open(check.payloadUrl);
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) throw new IllegalStateException("payload HTTP " + code);
            long declared = c.getContentLengthLong();
            if (declared > 0 && declared != check.manifest.payloadSizeBytes)
                throw new IllegalStateException("remote Content-Length does not match signed manifest");
            if (declared > MAX_PAYLOAD_BYTES) throw new IllegalStateException("payload too large");
            long total = 0;
            byte[] b = new byte[32768];
            try (BufferedInputStream in = new BufferedInputStream(c.getInputStream()); FileOutputStream dst = new FileOutputStream(out)) {
                int n;
                while ((n = in.read(b)) >= 0) {
                    if (n == 0) continue;
                    total += n;
                    if (total > check.manifest.payloadSizeBytes || total > MAX_PAYLOAD_BYTES) throw new IllegalStateException("payload exceeded expected size");
                    dst.write(b, 0, n);
                }
                dst.flush(); dst.getFD().sync();
            }
            if (total != check.manifest.payloadSizeBytes) throw new IllegalStateException("downloaded payload size mismatch");
            return out;
        } catch (Exception e) {
            //noinspection ResultOfMethodCallIgnored
            out.delete();
            throw e;
        } finally { c.disconnect(); }
    }

    private static String getText(URL url, long maxBytes) throws Exception {
        HttpURLConnection c = open(url);
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) throw new IllegalStateException("manifest HTTP " + code);
            long declared = c.getContentLengthLong();
            if (declared > maxBytes) throw new IllegalStateException("manifest too large");
            StringBuilder s = new StringBuilder();
            long total = 0;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buf = new char[4096]; int n;
                while ((n = r.read(buf)) >= 0) {
                    total += n * 2L;
                    if (total > maxBytes * 2L) throw new IllegalStateException("manifest exceeded size limit");
                    s.append(buf, 0, n);
                }
            }
            return s.toString();
        } finally { c.disconnect(); }
    }

    private static HttpURLConnection open(URL url) throws Exception {
        requireHttps(url);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MS); c.setReadTimeout(DEFAULT_READ_TIMEOUT_MS);
        c.setUseCaches(false); c.setInstanceFollowRedirects(false);
        c.setRequestProperty("Cache-Control", "no-cache");
        if (isCanonicalPrivateGitHub(url)) {
            String token=GitHubSessionAuth.token();
            if(token==null||token.length()<20)throw new IllegalStateException("GitHub developer session token required for private HazRob library");
            c.setRequestProperty("Authorization","Bearer "+token);
            c.setRequestProperty("Accept","application/vnd.github.raw+json");
            c.setRequestProperty("X-GitHub-Api-Version",API_VERSION);
            c.setRequestProperty("User-Agent","HazRob-Android/0.10.2");
        } else {
            c.setRequestProperty("Accept", "application/json, application/octet-stream;q=0.9");
        }
        return c;
    }

    private static boolean isCanonicalPrivateGitHub(URL url){
        return url!=null&&"api.github.com".equalsIgnoreCase(url.getHost())&&url.getPath()!=null&&url.getPath().startsWith(CANONICAL_API_PREFIX);
    }

    private static URL resolveSibling(URL manifestUrl, String payloadFile) throws Exception {
        URI base = manifestUrl.toURI();
        String path = base.getPath();
        int slash = path.lastIndexOf('/');
        String parent = slash >= 0 ? path.substring(0, slash + 1) : "/";
        URI resolved = new URI(base.getScheme(), base.getAuthority(), parent + payloadFile, base.getQuery(), null);
        return resolved.toURL();
    }

    private static void requireHttps(URL url) {
        if (url == null || !"https".equalsIgnoreCase(url.getProtocol())) throw new IllegalArgumentException("ChemCore updates require HTTPS");
        if (url.getUserInfo() != null) throw new IllegalArgumentException("credentials in update URL are not permitted");
    }
}
