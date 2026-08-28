package com.servoc.app.exposure;

/**
 * Port of the current ESP32 ExposureLimits.h screening database.
 * Important: screening colours are NOT compliance alarms. OEL/LEL alarms
 * remain disabled until the complete measurement chain is calibrated and validated.
 */
public final class ExposureLimits {
    private ExposureLimits() {}

    public static final int VERY_SAFE=1, SAFE=2, ALERT=3, DANGER=4, CRITICAL=5;
    private static final int NIOSH_TWA=0, NIOSH_STEL=1, NIOSH_CEILING=2,
            NIOSH_CEILING_MIN=3, OSHA_TWA=4, OSHA_STEL=5, OSHA_CEILING=6,
            PEAK_PPM=7, PEAK_MIN=8, IDLH=9, LEL=10;

    private static final class E {
        final int chem, kind; final double v;
        E(int c,int k,double v){this.chem=c;this.kind=k;this.v=v;}
    }

    private static final E[] DB = {
        new E(0,NIOSH_TWA,.1), new E(0,NIOSH_CEILING,.2), new E(0,NIOSH_CEILING_MIN,15), new E(0,OSHA_TWA,.1), new E(0,IDLH,2),
        new E(1,NIOSH_STEL,4.7), new E(1,OSHA_TWA,10), new E(1,IDLH,50), new E(1,LEL,5.6),
        new E(4,NIOSH_CEILING,10), new E(4,NIOSH_CEILING_MIN,10), new E(4,OSHA_CEILING,20), new E(4,PEAK_PPM,50), new E(4,PEAK_MIN,10), new E(4,IDLH,100), new E(4,LEL,4),
        new E(20,NIOSH_TWA,35), new E(20,NIOSH_CEILING,200), new E(20,OSHA_TWA,50), new E(20,IDLH,1200), new E(20,LEL,12.5),
        new E(21,NIOSH_TWA,25), new E(21,NIOSH_STEL,35), new E(21,OSHA_TWA,50), new E(21,IDLH,300), new E(21,LEL,15),
        new E(23,NIOSH_TWA,5000), new E(23,NIOSH_STEL,30000), new E(23,OSHA_TWA,5000), new E(23,IDLH,40000),
        new E(34,NIOSH_TWA,.1), new E(34,NIOSH_STEL,1), new E(34,OSHA_TWA,1), new E(34,OSHA_STEL,5), new E(34,IDLH,500), new E(34,LEL,1.2),
        new E(75,LEL,4),
        new E(76,LEL,5),
        new E(78,NIOSH_TWA,1000), new E(78,OSHA_TWA,1000), new E(78,IDLH,2100), new E(78,LEL,2.1),
        new E(84,NIOSH_TWA,50), new E(84,OSHA_TWA,500), new E(84,IDLH,1100), new E(84,LEL,1.1),
        new E(95,NIOSH_TWA,100), new E(95,NIOSH_STEL,150), new E(95,OSHA_TWA,200), new E(95,OSHA_CEILING,300), new E(95,PEAK_PPM,500), new E(95,PEAK_MIN,10), new E(95,IDLH,500), new E(95,LEL,1.1),
        new E(103,NIOSH_TWA,1000), new E(103,OSHA_TWA,1000), new E(103,IDLH,3300), new E(103,LEL,3.3),
        new E(110,NIOSH_TWA,250), new E(110,OSHA_TWA,1000), new E(110,IDLH,2500), new E(110,LEL,2.5),
        new E(135,NIOSH_TWA,1000), new E(135,OSHA_TWA,1000), new E(135,IDLH,2000), new E(135,LEL,1.9)
    };

    public static int entryCount(){ return DB.length; }
    public static int coveredChemicalCount(){ int n=0,last=-1; for(E e:DB){if(e.chem!=last){n++;last=e.chem;}} return n; }
    public static boolean hasData(int chem){ for(E e:DB) if(e.chem==chem) return true; return false; }
    public static double screeningReference(int chem){ return screeningRef(chem); }

    private static double value(int chem,int kind){ for(E e:DB) if(e.chem==chem&&e.kind==kind) return e.v; return -1; }
    private static double lower(double a,double b){ if(a<0)return b; if(b<0)return a; return Math.min(a,b); }
    public static double twa(int c){return lower(value(c,NIOSH_TWA),value(c,OSHA_TWA));}
    public static double stel(int c){return lower(value(c,NIOSH_STEL),value(c,OSHA_STEL));}
    public static double ceiling(int c){return lower(value(c,NIOSH_CEILING),value(c,OSHA_CEILING));}
    public static double idlh(int c){return value(c,IDLH);}
    public static double lelPercent(int c){return value(c,LEL);}
    private static double screeningRef(int c){double x=ceiling(c);if(x>=0)return x;x=stel(c);if(x>=0)return x;return twa(c);}
    private static int maxRisk(int a,int b){return Math.max(a,b);}

    public static int screeningRisk(int chem,double ppm){
        if(!Double.isFinite(ppm)||ppm<0)return 0;
        int out=VERY_SAFE;boolean defined=false;
        double ref=screeningRef(chem);
        if(ref>0){defined=true;double q=ppm/ref;out=q<.25?VERY_SAFE:q<.50?SAFE:q<1?ALERT:q<10?DANGER:CRITICAL;}
        double peak=value(chem,PEAK_PPM);if(peak>0){defined=true;if(ppm>=peak)out=maxRisk(out,DANGER);}
        double id=idlh(chem);if(id>0){defined=true;if(ppm>=id)out=maxRisk(out,CRITICAL);}
        double lel=lelPercent(chem);
        if(lel>0){defined=true;double pctLEL=ppm/(lel*10000.0)*100.0;int r=pctLEL<2.5?VERY_SAFE:pctLEL<5?SAFE:pctLEL<10?ALERT:pctLEL<20?DANGER:CRITICAL;out=maxRisk(out,r);}
        return defined?out:0;
    }
}
