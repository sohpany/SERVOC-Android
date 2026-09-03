package com.servoc.app.communication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Keeps the robot communication heartbeat alive while HazRob is backgrounded or a system picker is open. */
public final class RobotLinkService extends Service {
    private static final String CHANNEL="hazrob_link";
    private static final int NOTIFICATION_ID=9001;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
    private volatile RobotApi api;

    public static void start(Context c){
        Intent i=new Intent(c,RobotLinkService.class);
        if(android.os.Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);
    }

    @Override public void onCreate(){
        super.onCreate();
        SharedPreferences p=getSharedPreferences("servoc",MODE_PRIVATE);
        api=new RobotApi(p.getString("robot","192.168.4.1"));
        ensureChannel();startForeground(NOTIFICATION_ID,notification("HazRob متصل بالمركبة"));
        worker.scheduleAtFixedRate(()->{try{RobotApi a=api;if(a!=null)a.heartbeat();}catch(Exception ignored){}},0,2,TimeUnit.SECONDS);
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        SharedPreferences p=getSharedPreferences("servoc",MODE_PRIVATE);RobotApi a=api;if(a!=null)a.setRobotHost(p.getString("robot","192.168.4.1"));
        return START_STICKY;
    }

    @Override public void onDestroy(){worker.shutdownNow();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}

    private void ensureChannel(){
        if(android.os.Build.VERSION.SDK_INT<26)return;NotificationManager n=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(n==null)return;
        NotificationChannel c=new NotificationChannel(CHANNEL,"HazRob Link",NotificationManager.IMPORTANCE_LOW);c.setDescription("اتصال التحكم والقياس بالمركبة");n.createNotificationChannel(c);
    }
    private Notification notification(String text){
        Notification.Builder b=android.os.Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        return b.setContentTitle("HazRob 0.9").setContentText(text).setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setOngoing(true).build();
    }
}
