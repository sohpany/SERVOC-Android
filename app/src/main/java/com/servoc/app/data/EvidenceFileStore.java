package com.servoc.app.data;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.Locale;

/** Atomic-ish app-private evidence file writer with content hashing. */
public final class EvidenceFileStore {
    private EvidenceFileStore(){}

    public static final class SavedFile {
        public final File file;public final String uri,sha256;public final long bytes;
        SavedFile(File file,String sha256,long bytes){this.file=file;this.uri=Uri.fromFile(file).toString();this.sha256=sha256;this.bytes=bytes;}
    }

    public static SavedFile saveJpeg(Context context,String baseName,byte[] jpeg)throws Exception{
        if(jpeg==null||jpeg.length<4||jpeg[0]!=(byte)0xFF||jpeg[1]!=(byte)0xD8)throw new IllegalArgumentException("invalid JPEG evidence");
        File root=context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);if(root==null)root=new File(context.getFilesDir(),"pictures");File dir=new File(root,"HazRob");if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("cannot create evidence directory");
        String safe=(baseName==null?"HazRob_evidence":baseName).replaceAll("[^A-Za-z0-9_.-]","_");File target=new File(dir,safe+".jpg"),tmp=new File(dir,safe+".tmp");
        try(FileOutputStream out=new FileOutputStream(tmp)){out.write(jpeg);out.getFD().sync();}
        if(target.exists()&&!target.delete())throw new IllegalStateException("cannot replace evidence file");
        if(!tmp.renameTo(target)){try(FileOutputStream out=new FileOutputStream(target)){out.write(jpeg);out.getFD().sync();}if(!tmp.delete())tmp.deleteOnExit();}
        return new SavedFile(target,sha256(jpeg),jpeg.length);
    }

    private static String sha256(byte[] data)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(data);StringBuilder s=new StringBuilder(64);for(byte b:h)s.append(String.format(Locale.US,"%02x",b&255));return s.toString();}
}
