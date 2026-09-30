package com.servoc.app.github;

/**
 * Process-memory-only GitHub credential used by developer library operations.
 * Never persisted, never written to SharedPreferences, never embedded in the APK.
 */
public final class GitHubSessionAuth {
    private static volatile String token="";
    private GitHubSessionAuth(){}

    public static void set(String raw){
        String v=raw==null?"":raw.trim();
        if(v.isEmpty()){clear();return;}
        if(v.length()<20)throw new IllegalArgumentException("GitHub token required");
        token=v;
    }

    public static String token(){return token;}
    public static boolean available(){String v=token;return v!=null&&v.length()>=20;}
    public static void clear(){token="";}
}
