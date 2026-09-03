package com.servoc.app.communication;

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

/** Stores remote broker credentials encrypted with a non-exportable Android Keystore AES key. */
public final class RemoteCredentialStore {
    private static final String ALIAS="HazRob.Remote.MQTT.v1",U="remote_mqtt_user_enc",P="remote_mqtt_pass_enc";
    private final SharedPreferences prefs;
    public static final class Credentials{public final String username,password;Credentials(String u,String p){username=u;password=p;}public boolean present(){return !username.isEmpty()&&!password.isEmpty();}}

    public RemoteCredentialStore(Context context,SharedPreferences prefs){if(context==null||prefs==null)throw new IllegalArgumentException("context/preferences required");this.prefs=prefs;}

    public void save(String username,String password)throws Exception{SecretKey k=key();prefs.edit().putString(U,encrypt(k,username==null?"":username)).putString(P,encrypt(k,password==null?"":password)).remove("remote_broker_user").remove("remote_broker_password").apply();}
    public Credentials load(){try{SecretKey k=key();return new Credentials(decrypt(k,prefs.getString(U,"")),decrypt(k,prefs.getString(P,"")));}catch(Exception e){return new Credentials("","");}}
    public void clear(){prefs.edit().remove(U).remove(P).remove("remote_broker_user").remove("remote_broker_password").apply();}

    private static SecretKey key()throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);KeyStore.Entry e=ks.getEntry(ALIAS,null);if(e instanceof KeyStore.SecretKeyEntry)return ((KeyStore.SecretKeyEntry)e).getSecretKey();
        KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey();
    }
    private static String encrypt(SecretKey k,String s)throws Exception{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,k);byte[] iv=c.getIV(),ct=c.doFinal(s.getBytes(StandardCharsets.UTF_8));return Base64.encodeToString(iv,Base64.NO_WRAP)+":"+Base64.encodeToString(ct,Base64.NO_WRAP);}
    private static String decrypt(SecretKey k,String value)throws Exception{if(value==null||value.isEmpty())return"";int p=value.indexOf(':');if(p<=0)return"";byte[] iv=Base64.decode(value.substring(0,p),Base64.NO_WRAP),ct=Base64.decode(value.substring(p+1),Base64.NO_WRAP);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,k,new GCMParameterSpec(128,iv));return new String(c.doFinal(ct),StandardCharsets.UTF_8);}
}
