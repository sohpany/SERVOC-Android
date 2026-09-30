package com.servoc.app.chemcore;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ChemCoreSessionDynamicsTest {
    @Test public void recoveryAndCrossSensorDynamicsAreDerivedWithoutChangingFeatureV1()throws Exception{
        int n=40,m=30,s=8;double[] pt=time(n,.5),rt=time(m,.5);double[][] p=new double[n][s],r=new double[m][s];double[][] pe=env(n,25,50,1010),re=env(m,26,54,1009);
        for(int i=0;i<n;i++)for(int j=0;j<s;j++)p[i][j]=i<8?1.0:(1.0+(.50+.03*j)*Math.min(1,(i-7)/20.0));
        for(int i=0;i<m;i++)for(int j=0;j<s;j++){double start=1.50+.03*j;r[i][j]=1.0+(start-1.0)*Math.exp(-i/6.0);}
        ChemCoreSessionDynamics.Result x=ChemCoreSessionDynamics.compute("P","R",pt,p,pe,rt,r,re);
        assertEquals(ChemCoreSessionDynamics.SCHEMA,x.toJson().getString("schema"));assertTrue(x.hasRecovery());assertEquals(8,x.primaryValidSensors);assertEquals(8,x.recoveryValidSensors);
        JSONObject mq2=x.toJson().getJSONObject("sensors").getJSONObject("mq2");
        assertTrue(mq2.getDouble("recovery_fraction_to_baseline")>.8);assertTrue(mq2.getDouble("recovery_t90_s")>=mq2.getDouble("recovery_t50_s"));
        JSONObject cross=x.toJson().getJSONObject("inter_sensor");assertTrue(cross.getJSONObject("primary_pairwise_log10_correlation").length()>0);
        JSONObject env=x.toJson().getJSONObject("environment").getJSONObject("recovery_minus_primary");assertEquals(1.0,env.getDouble("temperature_c"),1e-9);
        assertEquals("chemcore-features/1",x.toJson().getString("feature_schema_compatibility"));
        assertEquals("dynamics_audit_only_not_an_automatic_confidence_contributor",x.toJson().getString("confidence_semantics"));
    }

    @Test public void missingChannelsRemainExplicitlyIncomplete()throws Exception{
        double[] t=time(12,1);double[][] p=new double[12][8],r=new double[12][8];for(int i=0;i<12;i++)for(int j=0;j<8;j++){p[i][j]=j<3?1+.02*i:Double.NaN;r[i][j]=j<2?1.2-.015*i:Double.NaN;}
        ChemCoreSessionDynamics.Result x=ChemCoreSessionDynamics.compute("P","R",t,p,env(12,25,50,1010),t,r,env(12,25,50,1010));
        assertEquals(3,x.primaryValidSensors);assertEquals(2,x.recoveryValidSensors);JSONObject c=x.toJson().getJSONObject("coverage");assertFalse(c.getBoolean("primary_complete"));assertFalse(c.getBoolean("recovery_complete"));
    }

    @Test public void repeatComputationIsDeterministic()throws Exception{
        double[] t=time(20,.25);double[][] p=new double[20][8],r=new double[20][8];for(int i=0;i<20;i++)for(int j=0;j<8;j++){p[i][j]=1+.01*j+.02*i;r[i][j]=1.4+.01*j-.015*i;}
        String a=ChemCoreSessionDynamics.compute("P","R",t,p,env(20,24,45,1008),t,r,env(20,24,45,1008)).toJson().toString();
        String b=ChemCoreSessionDynamics.compute("P","R",t,p,env(20,24,45,1008),t,r,env(20,24,45,1008)).toJson().toString();assertEquals(a,b);
    }

    @Test public void heavyDynamicsComputationStaysUnderTenSecondBudget(){
        int n=20000,s=8;double[] t=time(n,.05);double[][] p=new double[n][s],r=new double[n][s];for(int i=0;i<n;i++)for(int j=0;j<s;j++){p[i][j]=1+.001*j+.00003*i;r[i][j]=1.6+.001*j-.00002*i;}
        long a=System.nanoTime();ChemCoreSessionDynamics.Result x=ChemCoreSessionDynamics.compute("P","R",t,p,null,t,r,null);long ms=(System.nanoTime()-a)/1_000_000L;
        assertEquals(n,x.primarySamples);assertTrue("deep-session dynamics exceeded 10 s: "+ms+" ms",ms<10000);
    }

    private static double[] time(int n,double dt){double[] t=new double[n];for(int i=0;i<n;i++)t[i]=i*dt;return t;}
    private static double[][] env(int n,double temp,double rh,double p){double[][] e=new double[n][3];for(int i=0;i<n;i++){e[i][0]=temp;e[i][1]=rh;e[i][2]=p;}return e;}
}
