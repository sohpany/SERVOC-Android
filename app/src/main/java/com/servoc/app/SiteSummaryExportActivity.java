package com.servoc.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.report.SiteSummaryReportExporter;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Small SAF bridge dedicated to exporting one site-level summary PDF. */
public final class SiteSummaryExportActivity extends Activity {
    private static final String EXTRA_SITE_ID="site_id";
    private static final int CREATE_PDF=9201;
    private String siteId="";

    public static void start(Context context,String siteId){
        if(context==null)return;Intent i=new Intent(context,SiteSummaryExportActivity.class);i.putExtra(EXTRA_SITE_ID,siteId==null?"":siteId.trim());context.startActivity(i);
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);siteId=getIntent()==null?"":getIntent().getStringExtra(EXTRA_SITE_ID);siteId=siteId==null?"":siteId.trim();
        if(siteId.isEmpty()){Toast.makeText(this,"الموقع غير صالح",Toast.LENGTH_SHORT).show();finish();return;}
        try(ExperimentsDb db=new ExperimentsDb(this)){SiteSummaryReportExporter.resolve(db,siteId);}catch(Exception e){Toast.makeText(this,"تعذر قراءة الموقع",Toast.LENGTH_SHORT).show();finish();return;}
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,"HazRob_Site_"+safe(siteId)+"_"+stamp()+".pdf");startActivityForResult(i,CREATE_PDF);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);if(requestCode!=CREATE_PDF){finish();return;}if(resultCode!=RESULT_OK||data==null||data.getData()==null){finish();return;}Uri uri=data.getData();
        try(ExperimentsDb db=new ExperimentsDb(this);OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
            if(out==null)throw new IllegalStateException("output stream");SiteSummaryReportExporter.writePdf(out,db,siteId);Toast.makeText(this,"تم تصدير تقرير الموقع",Toast.LENGTH_SHORT).show();
        }catch(Exception e){Toast.makeText(this,"فشل تصدير تقرير الموقع: "+e.getClass().getSimpleName(),Toast.LENGTH_SHORT).show();}
        finish();
    }

    private static String safe(String s){return (s==null?"site":s).replaceAll("[^A-Za-z0-9._-]","_");}
    private static String stamp(){return new SimpleDateFormat("yyyyMMdd-HHmm",Locale.US).format(new Date());}
}
