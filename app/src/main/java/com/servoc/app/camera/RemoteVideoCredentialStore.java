package com.servoc.app.camera;

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

/** Stores the remote video bearer token encrypted with Android Keystore. */
public final class RemoteVideoCredentialStore {
    private static final String ALIAS="HazRob.Remote.Video.v1",KEY="remote_video_token_enc";
    private final SharedPreferences prefs;
    public RemoteVideoCredentialStore(SharedPreferences prefs){if(prefs==null)throw new IllegalArgumentException("preferences required");this.prefs=prefs;}
    public void save(String token)throws Exception{prefs.edit().putString(KEY,encrypt(key(),token==null?"":token)).remove("remote_video_token").apply();}
    public String load(){try{return decrypt(key(),prefs.getString(KEY,""));}catch(Exception e){return"";}}
    public void clear(){prefs.edit().remove(KEY).remove("remote_video_token").apply();}
    private static SecretKey key()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);KeyStore.Entry e=ks.getEntry(ALIAS,null);if(e instanceof KeyStore.SecretKeyEntry)return((KeyStore.SecretKeyEntry)e).getSecretKey();KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey();}
    private static String encrypt(SecretKey k,String s)throws Exception{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,k);return Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(s.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);}
    private static String decrypt(SecretKey k,String v)throws Exception{if(v==null||v.isEmpty())return"";int p=v.indexOf(':');if(p<=0)return"";byte[] iv=Base64.decode(v.substring(0,p),Base64.NO_WRAP),ct=Base64.decode(v.substring(p+1),Base64.NO_WRAP);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,k,new GCMParameterSpec(128,iv));return new String(c.doFinal(ct),StandardCharsets.UTF_8);}
}
