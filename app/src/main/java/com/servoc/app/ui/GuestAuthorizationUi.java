package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;

import com.servoc.app.camera.RemoteVideoCredentialStore;
import com.servoc.app.communication.RemoteAccessPackage;
import com.servoc.app.communication.RemoteCredentialStore;

/** Developer UI actions for issuing and withdrawing the ordinary-user remote authorization. */
final class GuestAuthorizationUi {
    private GuestAuthorizationUi(){}

    static void issue(Context uiContext,HazRobHost host){
        Context app=uiContext.getApplicationContext();RemoteCredentialStore.Credentials mqtt=new RemoteCredentialStore(app,host.preferences()).load();String video=new RemoteVideoCredentialStore(host.preferences()).load();if(!mqtt.present()||video.trim().isEmpty()){host.showToast("احفظ بيانات MQTT وVideo أولًا");return;}
        String token=RemoteAccessPackage.newGuestToken();host.io().execute(()->{try{host.robotApi().setGuestAccess(token);RemoteAccessPackage.saveGuestToken(host.preferences(),token);String code=RemoteAccessPackage.exportCode(app,host.preferences());new android.os.Handler(android.os.Looper.getMainLooper()).post(()->show(uiContext,host,code));}catch(Exception e){host.showToast("تعذر إنشاء التصريح: يجب أن يكون الروبوت متصلًا");}});
    }

    static void withdraw(Context uiContext,HazRobHost host){
        new AlertDialog.Builder(uiContext).setTitle("إلغاء تصريح المستخدم").setMessage("سيتم إيقاف التحكم البعيد للمستخدم الذي يحمل الرمز الحالي.").setNegativeButton("رجوع",null).setPositiveButton("إلغاء التصريح",(d,w)->host.io().execute(()->{try{host.robotApi().revokeGuestAccess();RemoteAccessPackage.clearGuestToken(host.preferences());host.showToast("تم إلغاء تصريح المستخدم");}catch(Exception e){host.showToast("تعذر إلغاء التصريح: يجب أن يكون الروبوت متصلًا");}})).show();
    }

    private static void show(Context c,HazRobHost host,String code){EditText box=UiKit.input(c,code);box.setSingleLine(false);box.setMinLines(3);box.setTextIsSelectable(true);box.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);new AlertDialog.Builder(c).setTitle("رمز تصريح HazRob").setView(box).setNegativeButton("إغلاق",null).setPositiveButton("نسخ",(d,w)->{ClipboardManager cb=(ClipboardManager)c.getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);if(cb!=null){cb.setPrimaryClip(ClipData.newPlainText("HazRob access",code));host.showToast("تم نسخ رمز التصريح");}}).show();}
}
