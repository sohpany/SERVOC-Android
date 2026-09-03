package com.servoc.app.communication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Keeps the selected HazRob transport alive while the app is backgrounded or a system picker is open. */
public final class RobotLinkService extends Service {
    private static final String CHANNEL="hazrob_link";
    private static final int NOTIFICATION_ID=9001;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
    private volatile RobotApi api;private SharedPreferences prefs;private RemoteSessionManager remoteSessions;
    private int reconnectAttempt=0;private long reconnectAt=0;

    public static void start(Context c){Intent i=new Intent(c,RobotLinkService.class);if(android.os.Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}

    @Override public void onCreate(){
        super.onCreate();prefs=getSharedPreferences("servoc",MODE_PRIVATE);api=new RobotApi(prefs.getString("robot","192.168.4.1"));remoteSessions=new RemoteSessionManager(this,prefs);
        ensureChannel();startForeground(NOTIFICATION_ID,notification("HazRob link active"));
        worker.scheduleAtFixedRate(this::linkTick,0,2,TimeUnit.SECONDS);
    }

    private void linkTick(){
        try{
            boolean remote="remote".equalsIgnoreCase(prefs.getString("connection_mode","direct"));
            if(!remote){RemoteConnection stale=RemoteConnection.current();if(stale!=null)remoteSessions.close();resetBackoff();RobotApi a=api;if(a!=null)a.heartbeat();return;}
            RemoteConnection live=RemoteConnection.active();if(live!=null){live.heartbeat();resetBackoff();return;}
            long now=SystemClock.elapsedRealtime();if(now<reconnectAt)return;
            try{RemoteConnection r=remoteSessions.ensureConnected();r.heartbeat();resetBackoff();updateNotification("HazRob Remote connected");}
            catch(Exception e){scheduleReconnect(now);updateNotification("HazRob Remote reconnecting");}
        }catch(Exception ignored){}
    }

    private void scheduleReconnect(long now){int shift=Math.min(reconnectAttempt,4);long delay=Math.min(30000L,2000L<<shift);reconnectAttempt=Math.min(reconnectAttempt+1,5);reconnectAt=now+delay;}
    private void resetBackoff(){reconnectAttempt=0;reconnectAt=0;}

    @Override public int onStartCommand(Intent intent,int flags,int startId){RobotApi a=api;if(a!=null)a.setRobotHost(prefs.getString("robot","192.168.4.1"));return START_STICKY;}
    @Override public void onDestroy(){worker.shutdownNow();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}

    private void ensureChannel(){if(android.os.Build.VERSION.SDK_INT<26)return;NotificationManager n=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(n==null)return;NotificationChannel c=new NotificationChannel(CHANNEL,"HazRob Link",NotificationManager.IMPORTANCE_LOW);c.setDescription("اتصال التحكم والقياس بالمركبة");n.createNotificationChannel(c);}
    private void updateNotification(String text){NotificationManager n=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(n!=null)n.notify(NOTIFICATION_ID,notification(text));}
    private Notification notification(String text){Notification.Builder b=android.os.Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);return b.setContentTitle("HazRob 0.9").setContentText(text).setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setOngoing(true).build();}
}
