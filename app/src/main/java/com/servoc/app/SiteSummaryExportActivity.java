package com.servoc.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.report.SiteSummaryReportExporter;
import com.servoc.app.report.SmartSiteXlsxWriter;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** SAF bridge for full-site intelligent PDF/XLSX; never export on Android UI thread. */
public final class SiteSummaryExportActivity extends Activity {
    private static final String EXTRA_SITE_ID="site_id",EXTRA_FORMAT="format";
    private static final int CREATE_REPORT=9201;
    private String siteId="",format="pdf";
    private boolean pickerOpened=false;

    public static void start(Context context,String siteId){start(context,siteId,"pdf");}
    public static void start(Context context,String siteId,String format){
        if(context==null)return;
        Intent i=new Intent(context,SiteSummaryExportActivity.class);
        i.putExtra(EXTRA_SITE_ID,siteId==null?"":siteId.trim());
        i.putExtra(EXTRA_FORMAT,"xlsx".equalsIgnoreCase(format)?"xlsx":"pdf");
        context.startActivity(i);
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        siteId=state!=null?state.getString(EXTRA_SITE_ID,""):getIntent()==null?"":getIntent().getStringExtra(EXTRA_SITE_ID);
        siteId=siteId==null?"":siteId.trim();
        format=state!=null?state.getString(EXTRA_FORMAT,"pdf"):getIntent()==null?"pdf":getIntent().getStringExtra(EXTRA_FORMAT);
        format="xlsx".equalsIgnoreCase(format)?"xlsx":"pdf";
        if(siteId.isEmpty()){Toast.makeText(this,"الموقع غير صالح",Toast.LENGTH_SHORT).show();finish();return;}
        if(state!=null&&state.getBoolean("picker_opened",false)){pickerOpened=true;return;}
        // Do not build a full-site bundle on the main thread before SAF opens.
        boolean xlsx="xlsx".equals(format);
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(xlsx?"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":"application/pdf");
        i.putExtra(Intent.EXTRA_TITLE,"HazRob_Site_"+safe(siteId)+"_"+stamp()+(xlsx?".xlsx":".pdf"));
        pickerOpened=true;
        startActivityForResult(i,CREATE_REPORT);
    }

    @Override protected void onSaveInstanceState(Bundle out){
        super.onSaveInstanceState(out);
        out.putString(EXTRA_SITE_ID,siteId);out.putString(EXTRA_FORMAT,format);
        out.putBoolean("picker_opened",pickerOpened);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=CREATE_REPORT||resultCode!=RESULT_OK||data==null||data.getData()==null){finish();return;}
        final Uri uri=data.getData();
        final String reportSite=siteId,reportFormat=format;
        new Thread(()->{
            String result;
            try(ExperimentsDb db=new ExperimentsDb(getApplicationContext());
                OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
                if(out==null)throw new IllegalStateException("output stream");
                if("xlsx".equals(reportFormat))SmartSiteXlsxWriter.write(out,SiteSummaryReportExporter.resolve(db,reportSite));
                else SiteSummaryReportExporter.writePdf(out,db,reportSite);
                result="xlsx".equals(reportFormat)?"تم تصدير Excel الذكي للموقع":"تم تصدير تقرير الموقع PDF";
            }catch(Exception e){result="فشل تصدير تقرير الموقع: "+e.getClass().getSimpleName();}
            final String message=result;
            runOnUiThread(()->{Toast.makeText(this,message,Toast.LENGTH_LONG).show();finish();});
        },"HazRob-SiteReport").start();
    }

    private static String safe(String s){return (s==null?"site":s).replaceAll("[^A-Za-z0-9._-]","_");}
    private static String stamp(){return new SimpleDateFormat("yyyyMMdd-HHmm",Locale.US).format(new Date());}
}
