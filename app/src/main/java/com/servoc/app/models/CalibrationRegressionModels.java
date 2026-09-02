package com.servoc.app.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Dependency-free regression candidates for device-specific MQ calibration research.
 * These fits are laboratory tools only; selecting a good mathematical fit does not
 * by itself establish analyte identity, reference-gas traceability or safety validity.
 */
public final class CalibrationRegressionModels {
    private CalibrationRegressionModels(){}

    // Zero-based indexes in MqDataScience76Catalog.
    public static final int LINEAR=11;
    public static final int WEIGHTED_LINEAR=12;
    public static final int HUBER=13;
    public static final int THEIL_SEN=14;
    public static final int RIDGE=16;
    public static final int POLY2=24;
    public static final int POLY3=25;
    public static final int EXPONENTIAL=27;
    public static final int LOGARITHMIC=28;
    public static final int POWER=29;

    public static final int[] NATIVE={LINEAR,WEIGHTED_LINEAR,HUBER,THEIL_SEN,RIDGE,POLY2,POLY3,EXPONENTIAL,LOGARITHMIC,POWER};

    public static final class Fit {
        public final int catalogIndex;
        public final String name;
        public final double[] coefficients;
        public final int minPoints;
        private final int kind;
        Fit(int catalogIndex,double[] coefficients,int minPoints,int kind){
            this.catalogIndex=catalogIndex;this.name=MqDataScience76Catalog.NAMES[catalogIndex];this.coefficients=coefficients;this.minPoints=minPoints;this.kind=kind;
        }
        public double predict(double x){
            if(!Double.isFinite(x))return Double.NaN;
            switch(kind){
                case 0:return coefficients[0]+coefficients[1]*x;
                case 1:{double y=0,p=1;for(double c:coefficients){y+=c*p;p*=x;}return y;}
                case 2:return Math.exp(coefficients[0]+coefficients[1]*x);
                case 3:return x>0?coefficients[0]+coefficients[1]*Math.log(x):Double.NaN;
                case 4:return x>0?Math.exp(coefficients[0]+coefficients[1]*Math.log(x)):Double.NaN;
                default:return Double.NaN;
            }
        }
        public String equation(){
            if(kind==0)return "ppm = "+f(coefficients[0])+" + "+f(coefficients[1])+"·x";
            if(kind==1){StringBuilder s=new StringBuilder("ppm = ");for(int i=0;i<coefficients.length;i++){if(i>0)s.append(" + ");s.append(f(coefficients[i]));if(i>0)s.append("·x^").append(i);}return s.toString();}
            if(kind==2)return "ppm = exp("+f(coefficients[0])+" + "+f(coefficients[1])+"·x)";
            if(kind==3)return "ppm = "+f(coefficients[0])+" + "+f(coefficients[1])+"·ln(x)";
            return "ppm = "+f(Math.exp(coefficients[0]))+"·x^"+f(coefficients[1]);
        }
    }

    public static final class Score {
        public final Fit fit;
        public final double r2;
        public final double logRmse;
        public final boolean crossValidated;
        public final int samples;
        Score(Fit fit,double r2,double logRmse,boolean crossValidated,int samples){this.fit=fit;this.r2=r2;this.logRmse=logRmse;this.crossValidated=crossValidated;this.samples=samples;}
    }

    public static List<Score> rank(double[] ratio,double[] ppm){
        double[][] clean=clean(ratio,ppm);double[] x=clean[0],y=clean[1];List<Score> out=new ArrayList<>();
        for(int idx:NATIVE){Fit fit=fit(idx,x,y);if(fit==null)continue;double r2=r2Log(fit,x,y);boolean cv=x.length>=fit.minPoints+2;double err=cv?looLogRmse(idx,x,y):logRmse(fit,x,y);if(Double.isFinite(err))out.add(new Score(fit,r2,err,cv,x.length));}
        out.sort(Comparator.comparingDouble(s->s.logRmse));return out;
    }

    public static Fit fit(int catalogIndex,double[] ratio,double[] ppm){
        double[][] clean=clean(ratio,ppm);double[] x=clean[0],y=clean[1];
        if(catalogIndex==LINEAR)return linear(catalogIndex,x,y,null,2,0);
        if(catalogIndex==WEIGHTED_LINEAR){if(x.length<2)return null;double[] w=new double[x.length];for(int i=0;i<w.length;i++)w[i]=1.0/Math.max(1.0,y[i]);return linear(catalogIndex,x,y,w,2,0);}
        if(catalogIndex==HUBER)return huber(x,y);
        if(catalogIndex==THEIL_SEN)return theilSen(x,y);
        if(catalogIndex==RIDGE)return ridge(x,y);
        if(catalogIndex==POLY2)return polynomial(catalogIndex,x,y,2);
        if(catalogIndex==POLY3)return polynomial(catalogIndex,x,y,3);
        if(catalogIndex==EXPONENTIAL)return transformed(catalogIndex,x,y,2);
        if(catalogIndex==LOGARITHMIC)return transformed(catalogIndex,x,y,3);
        if(catalogIndex==POWER)return transformed(catalogIndex,x,y,4);
        return null;
    }

    private static Fit linear(int idx,double[] x,double[] y,double[] w,int minPoints,int kind){
        if(x.length<minPoints)return null;double sw=0,sx=0,sy=0,sxx=0,sxy=0;
        for(int i=0;i<x.length;i++){double wi=w==null?1:w[i];sw+=wi;sx+=wi*x[i];sy+=wi*y[i];sxx+=wi*x[i]*x[i];sxy+=wi*x[i]*y[i];}
        double den=sw*sxx-sx*sx;if(Math.abs(den)<1e-15)return null;double b=(sw*sxy-sx*sy)/den,a=(sy-b*sx)/sw;return finite(a,b)?new Fit(idx,new double[]{a,b},minPoints,kind):null;
    }

    private static Fit ridge(double[] x,double[] y){
        if(x.length<2)return null;double mx=mean(x),my=mean(y),sxx=0,sxy=0;for(int i=0;i<x.length;i++){double dx=x[i]-mx;sxx+=dx*dx;sxy+=dx*(y[i]-my);}double lambda=Math.max(1e-12,sxx*1e-3);double b=sxy/(sxx+lambda),a=my-b*mx;return finite(a,b)?new Fit(RIDGE,new double[]{a,b},2,0):null;
    }

    private static Fit huber(double[] x,double[] y){
        Fit f=linear(HUBER,x,y,null,3,0);if(f==null)return null;double a=f.coefficients[0],b=f.coefficients[1];
        for(int it=0;it<20;it++){double[] r=new double[x.length],abs=new double[x.length];for(int i=0;i<x.length;i++){r[i]=y[i]-(a+b*x[i]);abs[i]=Math.abs(r[i]);}double scale=median(abs)/0.67448975;if(!Double.isFinite(scale)||scale<1e-12)break;double delta=1.345*scale;double[] w=new double[x.length];for(int i=0;i<w.length;i++)w[i]=abs[i]<=delta?1.0:delta/abs[i];Fit n=linear(HUBER,x,y,w,3,0);if(n==null)break;double da=Math.abs(n.coefficients[0]-a),db=Math.abs(n.coefficients[1]-b);a=n.coefficients[0];b=n.coefficients[1];if(da+db<1e-10)break;}
        return finite(a,b)?new Fit(HUBER,new double[]{a,b},3,0):null;
    }

    private static Fit theilSen(double[] x,double[] y){
        if(x.length<3)return null;List<Double> slopes=new ArrayList<>();for(int i=0;i<x.length;i++)for(int j=i+1;j<x.length;j++){double dx=x[j]-x[i];if(Math.abs(dx)>1e-15)slopes.add((y[j]-y[i])/dx);}if(slopes.isEmpty())return null;double[] ss=new double[slopes.size()];for(int i=0;i<ss.length;i++)ss[i]=slopes.get(i);double b=median(ss);double[] ints=new double[x.length];for(int i=0;i<x.length;i++)ints[i]=y[i]-b*x[i];double a=median(ints);return finite(a,b)?new Fit(THEIL_SEN,new double[]{a,b},3,0):null;
    }

    private static Fit polynomial(int idx,double[] x,double[] y,int degree){
        int p=degree+1;if(x.length<p)return null;double[][] a=new double[p][p+1];
        for(int r=0;r<p;r++){for(int c=0;c<p;c++){double sum=0;for(double v:x)sum+=Math.pow(v,r+c);a[r][c]=sum;}double sum=0;for(int i=0;i<x.length;i++)sum+=y[i]*Math.pow(x[i],r);a[r][p]=sum;}
        double[] coef=solve(a);if(coef==null)return null;for(double c:coef)if(!Double.isFinite(c))return null;return new Fit(idx,coef,p,1);
    }

    /** mode: 2 exponential, 3 logarithmic, 4 power. */
    private static Fit transformed(int idx,double[] x,double[] y,int mode){
        if(x.length<2)return null;double[] tx=new double[x.length],ty=new double[y.length];
        for(int i=0;i<x.length;i++){
            if(mode==2){if(y[i]<=0)return null;tx[i]=x[i];ty[i]=Math.log(y[i]);}
            else if(mode==3){if(x[i]<=0)return null;tx[i]=Math.log(x[i]);ty[i]=y[i];}
            else{if(x[i]<=0||y[i]<=0)return null;tx[i]=Math.log(x[i]);ty[i]=Math.log(y[i]);}
        }
        Fit f=linear(idx,tx,ty,null,2,0);return f==null?null:new Fit(idx,f.coefficients,2,mode);
    }

    private static double looLogRmse(int idx,double[] x,double[] y){
        double ss=0;int n=0;for(int hold=0;hold<x.length;hold++){double[] tx=new double[x.length-1],ty=new double[y.length-1];for(int i=0,k=0;i<x.length;i++)if(i!=hold){tx[k]=x[i];ty[k]=y[i];k++;}Fit f=fit(idx,tx,ty);if(f==null)continue;double pred=f.predict(x[hold]);if(pred>0&&Double.isFinite(pred)){double d=Math.log10(pred)-Math.log10(y[hold]);ss+=d*d;n++;}}return n==x.length?Math.sqrt(ss/n):Double.NaN;
    }
    private static double logRmse(Fit f,double[] x,double[] y){double ss=0;int n=0;for(int i=0;i<x.length;i++){double p=f.predict(x[i]);if(p>0&&Double.isFinite(p)){double d=Math.log10(p)-Math.log10(y[i]);ss+=d*d;n++;}}return n==x.length&&n>0?Math.sqrt(ss/n):Double.NaN;}
    private static double r2Log(Fit f,double[] x,double[] y){double mean=0;for(double v:y)mean+=Math.log10(v);mean/=y.length;double tot=0,res=0;for(int i=0;i<x.length;i++){double pred=f.predict(x[i]);if(!(pred>0&&Double.isFinite(pred)))return Double.NaN;double yy=Math.log10(y[i]),pp=Math.log10(pred);tot+=(yy-mean)*(yy-mean);res+=(yy-pp)*(yy-pp);}return tot<=1e-15?1.0:1.0-res/tot;}

    private static double[][] clean(double[] x,double[] y){if(x==null||y==null)return new double[][]{new double[0],new double[0]};int n=Math.min(x.length,y.length),m=0;for(int i=0;i<n;i++)if(Double.isFinite(x[i])&&x[i]>0&&Double.isFinite(y[i])&&y[i]>0)m++;double[] a=new double[m],b=new double[m];for(int i=0,k=0;i<n;i++)if(Double.isFinite(x[i])&&x[i]>0&&Double.isFinite(y[i])&&y[i]>0){a[k]=x[i];b[k]=y[i];k++;}return new double[][]{a,b};}
    private static double[] solve(double[][] a){int n=a.length;for(int col=0;col<n;col++){int pivot=col;for(int r=col+1;r<n;r++)if(Math.abs(a[r][col])>Math.abs(a[pivot][col]))pivot=r;if(Math.abs(a[pivot][col])<1e-15)return null;double[] tmp=a[col];a[col]=a[pivot];a[pivot]=tmp;double q=a[col][col];for(int c=col;c<=n;c++)a[col][c]/=q;for(int r=0;r<n;r++)if(r!=col){double z=a[r][col];for(int c=col;c<=n;c++)a[r][c]-=z*a[col][c];}}double[] x=new double[n];for(int i=0;i<n;i++)x[i]=a[i][n];return x;}
    private static double mean(double[] a){double s=0;for(double v:a)s+=v;return s/a.length;}
    private static double median(double[] a){if(a.length==0)return Double.NaN;double[] b=a.clone();Arrays.sort(b);int n=b.length;return(n&1)==1?b[n/2]:(b[n/2-1]+b[n/2])*0.5;}
    private static boolean finite(double...v){for(double x:v)if(!Double.isFinite(x))return false;return true;}
    private static String f(double v){return String.format(java.util.Locale.US,"%.6g",v);}
}
