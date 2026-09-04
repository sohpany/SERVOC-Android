package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.EditText;

import com.servoc.app.communication.RemoteAccessPackage;
import com.servoc.app.communication.RemoteConnection;
import com.servoc.app.communication.RemoteSessionManager;

/** Developer UI actions for issuing and withdrawing the ordinary-user remote authorization. */
final class GuestAuthorizationUi {
    private GuestAuthorizationUi(){}

    static void issue(Context uiContext,HazRobHost host){
        Context app=uiContext.getApplicationContext();String token=RemoteAccessPackage.newGuestToken();
        host.io().execute(()->{
            try{
                withDeveloperInternet(app,host,r->r.guestAccessSet(token));
                RemoteAccessPackage.saveGuestToken(host.preferences(),token);
                String code=RemoteAccessPackage.exportCode(app,host.preferences());
                new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{host.refreshAllTabs();show(uiContext,host,code);});
            }catch(Exception e){host.showToast("تعذر إنشاء التصريح عبر الإنترنت: تحقق من اتصال الهاتف والروبوت");}
        });
    }

    static void withdraw(Context uiContext,HazRobHost host){
        Context app=uiContext.getApplicationContext();
        new AlertDialog.Builder(uiContext).setTitle("إلغاء تصريح المستخدم").setMessage("سيتم إيقاف التحكم البعيد للمستخدم الذي يحمل الرمز الحالي.").setNegativeButton("رجوع",null).setPositiveButton("إلغاء التصريح",(d,w)->host.io().execute(()->{
            try{
                withDeveloperInternet(app,host,RemoteConnection::guestAccessRevoke);
                RemoteAccessPackage.clearGuestToken(host.preferences());
                new android.os.Handler(android.os.Looper.getMainLooper()).post(host::refreshAllTabs);
                host.showToast("تم إلغاء تصريح المستخدم");
            }catch(Exception e){host.showToast("تعذر إلغاء التصريح عبر الإنترنت: تحقق من اتصال الهاتف والروبوت");}
        })).show();
    }

    /** Authorization management is cloud-only. It never requires the developer phone to join the robot AP. */
    private static void withDeveloperInternet(Context app,HazRobHost host,RemoteAction action)throws Exception{
        SharedPreferences prefs=host.preferences();boolean keepRemote="remote".equalsIgnoreCase(prefs.getString("connection_mode","direct"));
        RemoteSessionManager manager=new RemoteSessionManager(app,prefs);RemoteConnection live=RemoteConnection.active();
        if(live==null||!"developer".equalsIgnoreCase(live.operatorRole())){
            manager.close();prefs.edit().putString("remote_operator_role","developer").apply();
        }
        try{
            RemoteConnection r=manager.ensureConnected();
            if(!"developer".equalsIgnoreCase(r.operatorRole()))throw new SecurityException("developer session required");
            action.run(r);
        }finally{
            if(!keepRemote)manager.close();
        }
    }

    private interface RemoteAction{void run(RemoteConnection remote)throws Exception;}

    private static void show(Context c,HazRobHost host,String code){
        EditText box=UiKit.input(c,code);box.setSingleLine(false);box.setMinLines(3);box.setTextIsSelectable(true);box.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        new AlertDialog.Builder(c).setTitle("رمز تصريح HazRob").setView(box).setNegativeButton("إغلاق",null).setPositiveButton("نسخ",(d,w)->{ClipboardManager cb=(ClipboardManager)c.getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);if(cb!=null){cb.setPrimaryClip(ClipData.newPlainText("HazRob access",code));host.showToast("تم نسخ رمز التصريح");}}).show();
    }
}
