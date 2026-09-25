package com.servoc.app.field;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Creates HazRob's safe bootstrap workspace on first launch.
 *
 * On Android 10+ the app cannot silently create an arbitrary persistent public
 * root that survives uninstall. Therefore this bootstrap is app-scoped and safe;
 * the durable user-owned library remains the SAF-selected HazRob folder.
 */
public final class HazRobStorageBootstrap {
    private HazRobStorageBootstrap(){}

    public static File ensure(Context context){
        if(context==null)throw new IllegalArgumentException("context required");
        File base=context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if(base==null)base=context.getFilesDir();
        File root=new File(base,"HazRob");
        mkdir(root);
        mkdir(new File(root,"Staging"));
        mkdir(new File(root,"Cache"));
        mkdir(new File(root,"Recovery"));
        return root;
    }

    private static void mkdir(File f){
        if(!f.exists()&&!f.mkdirs())throw new IllegalStateException("cannot create HazRob storage: "+f.getAbsolutePath());
        if(!f.isDirectory())throw new IllegalStateException("HazRob storage path is not a directory: "+f.getAbsolutePath());
    }
}
