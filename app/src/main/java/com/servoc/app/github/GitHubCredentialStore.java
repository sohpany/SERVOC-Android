package com.servoc.app.github;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Device-bound encrypted storage for the developer GitHub upload credential.
 *
 * The token is never embedded in the APK and is never stored as plaintext.
 * Android Keystore owns the AES key; SharedPreferences stores ciphertext + IV only.
 */
public final class GitHubCredentialStore {
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "hazrob_github_upload_token_v1";
    private static final String PREFS = "hazrob_secure_credentials";
    private static final String CT = "github_upload_ciphertext";
    private static final String IV = "github_upload_iv";

    private GitHubCredentialStore() {}

    public static void save(Context context, String rawToken) throws Exception {
        if (context == null) throw new IllegalArgumentException("context required");
        String token = rawToken == null ? "" : rawToken.trim();
        if (token.length() < 20) throw new IllegalArgumentException("GitHub token required");

        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));

        prefs(context).edit()
                .putString(CT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .apply();
        GitHubSessionAuth.set(token);
    }

    public static String load(Context context) {
        if (context == null) return "";
        if (GitHubSessionAuth.available()) return GitHubSessionAuth.token();
        SharedPreferences p = prefs(context);
        String ct = p.getString(CT, "");
        String iv = p.getString(IV, "");
        if (ct == null || iv == null || ct.isEmpty() || iv.isEmpty()) return "";
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            java.security.Key key = ks.getKey(KEY_ALIAS, null);
            if (!(key instanceof SecretKey)) return "";
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, (SecretKey) key,
                    new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
            String token = new String(cipher.doFinal(Base64.decode(ct, Base64.NO_WRAP)), StandardCharsets.UTF_8).trim();
            if (token.length() < 20) return "";
            GitHubSessionAuth.set(token);
            return token;
        } catch (Exception e) {
            clear(context);
            return "";
        }
    }

    public static boolean available(Context context) {
        return load(context).length() >= 20;
    }

    public static void clear(Context context) {
        if (context != null) prefs(context).edit().remove(CT).remove(IV).apply();
        GitHubSessionAuth.clear();
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS);
        } catch (Exception ignored) {}
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        java.security.Key existing = ks.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;

        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build();
        generator.init(spec);
        return generator.generateKey();
    }
}
