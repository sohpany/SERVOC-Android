package com.servoc.app;

import android.app.Activity;
import android.app.Application;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.servoc.app.ui.EchoDistanceBadge;
import com.servoc.app.ui.GasHazardBadge;
import com.servoc.app.ui.HazRobHost;

/**
 * Application-level lifecycle guard for the operator workspace.
 *
 * Landscape is a dedicated driving presentation: the normal navigation tabs, footer and generic
 * refresh row are suppressed while the global safety/status header and the existing ControlTab
 * camera surface, drive controls, E-STOP and safety overlays remain active. Portrait keeps the
 * normal tabbed shell.
 *
 * Pausing MainActivity always neutralizes the drive command so rotation/background transitions
 * cannot preserve a non-zero operator command. Firmware watchdog/interlocks remain authoritative.
 */
public final class HazRobApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final String ECHO_BADGE_TAG="hazrob_echo_distance_badge";
    private static final String GAS_BADGE_TAG="hazrob_gas_hazard_badge";

    @Override public void onCreate(){
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    static boolean isLandscape(Activity activity){
        return activity!=null && activity.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
    }

    private static int dp(Activity activity,int value){
        return Math.round(value*activity.getResources().getDisplayMetrics().density);
    }

    private static FrameLayout drivingCameraBox(Activity activity,ViewGroup pageGroup){
        if(!(activity instanceof HazRobHost)||pageGroup.getChildCount()<1)return null;
        View wrapped=pageGroup.getChildAt(0);
        if(!(wrapped instanceof LinearLayout))return null;
        LinearLayout box=(LinearLayout)wrapped;
        if(box.getChildCount()<2)return null;
        View control=box.getChildAt(1);
        if(!(control instanceof LinearLayout))return null;
        LinearLayout controlRoot=(LinearLayout)control;
        if(controlRoot.getChildCount()<1)return null;
        View camera=controlRoot.getChildAt(0);
        return camera instanceof FrameLayout?(FrameLayout)camera:null;
    }

    private static void installEchoDistanceBadge(Activity activity,ViewGroup pageGroup){
        FrameLayout cameraBox=drivingCameraBox(activity,pageGroup);
        if(cameraBox==null||cameraBox.findViewWithTag(ECHO_BADGE_TAG)!=null)return;
        EchoDistanceBadge badge=new EchoDistanceBadge(activity,(HazRobHost)activity);
        badge.setTag(ECHO_BADGE_TAG);
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER_HORIZONTAL|Gravity.TOP);
        p.setMargins(0,dp(activity,48),0,0);
        cameraBox.addView(badge,p);
    }

    private static void installGasHazardBadge(Activity activity,ViewGroup pageGroup){
        FrameLayout cameraBox=drivingCameraBox(activity,pageGroup);
        if(cameraBox==null||cameraBox.findViewWithTag(GAS_BADGE_TAG)!=null)return;
        GasHazardBadge badge=new GasHazardBadge(activity,(HazRobHost)activity);
        badge.setTag(GAS_BADGE_TAG);
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER_HORIZONTAL|Gravity.TOP);
        p.setMargins(0,dp(activity,88),0,0);
        cameraBox.addView(badge,p);
    }

    static void applyLandscapeDrivingShell(Activity activity){
        if(!(activity instanceof MainActivity) || !isLandscape(activity))return;
        View content=activity.findViewById(android.R.id.content);
        if(!(content instanceof ViewGroup))return;
        ViewGroup contentRoot=(ViewGroup)content;
        if(contentRoot.getChildCount()<1)return;
        View shell=contentRoot.getChildAt(0);
        if(!(shell instanceof LinearLayout))return;
        LinearLayout root=(LinearLayout)shell;

        // MainActivity shell contract: header, tabs, page, footer.
        if(root.getChildCount()>=4){
            root.getChildAt(0).setVisibility(View.VISIBLE); // keep global safety/connection/gas status
            root.getChildAt(1).setVisibility(View.GONE);    // hide application navigation tabs
            root.getChildAt(3).setVisibility(View.GONE);    // hide footer

            // The driving page is wrapped by withRefresh(content). Hide only the generic refresh row;
            // ControlTab itself retains camera, driving controls, E-STOP and safety overlays.
            View page=root.getChildAt(2);
            if(page instanceof ViewGroup){
                ViewGroup pageGroup=(ViewGroup)page;
                if(pageGroup.getChildCount()>0){
                    View wrapped=pageGroup.getChildAt(0);
                    if(wrapped instanceof LinearLayout){
                        LinearLayout box=(LinearLayout)wrapped;
                        if(box.getChildCount()>=2)box.getChildAt(0).setVisibility(View.GONE);
                    }
                    installEchoDistanceBadge(activity,pageGroup);
                    installGasHazardBadge(activity,pageGroup);
                }
            }
        }
    }

    static void neutralizeDrive(Activity activity){
        if(activity instanceof MainActivity && activity instanceof HazRobHost){
            ((HazRobHost)activity).setDriveCommand(0,0);
        }
    }

    @Override public void onActivityResumed(Activity activity){applyLandscapeDrivingShell(activity);}
    @Override public void onActivityPaused(Activity activity){neutralizeDrive(activity);}
    @Override public void onActivityCreated(Activity activity,Bundle state){}
    @Override public void onActivityStarted(Activity activity){}
    @Override public void onActivityStopped(Activity activity){}
    @Override public void onActivitySaveInstanceState(Activity activity,Bundle outState){}
    @Override public void onActivityDestroyed(Activity activity){}
}
