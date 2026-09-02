package com.servoc.app.models;

/** Data-domain requirements for the adopted MQDataScience v6.0.0 catalogue. */
public final class MqDataScienceModelProfile {
    private MqDataScienceModelProfile(){}

    public enum Domain {
        MULTIVARIATE_ENV,
        CURVE_1D,
        TIME_SERIES,
        MACHINE_LEARNING
    }

    public static Domain domain(int index){
        check(index);
        if(index<=10)return Domain.MULTIVARIATE_ENV;
        if(index<=50)return Domain.CURVE_1D;
        if(index<=62)return Domain.TIME_SERIES;
        return Domain.MACHINE_LEARNING;
    }

    public static boolean needsTemperature(int index){
        check(index);
        return index==0||index==1||index==2||index==4||index==5||index==6||index==7||index==9;
    }

    public static boolean needsHumidity(int index){
        check(index);
        return index==0||index==1||index==3||index==4||index==5||index==6||index==8||index==10;
    }

    public static boolean needsOrderedTime(int index){
        check(index);
        return index==0||index==2||index==3||index>=51&&index<=62;
    }

    /** Conservative laboratory guidance, not an upstream hard minimum. */
    public static int recommendedMinimumSamples(int index){
        check(index);
        Domain d=domain(index);
        if(d==Domain.MULTIVARIATE_ENV)return 12;
        if(d==Domain.TIME_SERIES)return 24;
        if(d==Domain.MACHINE_LEARNING)return 30;
        if(index==24)return 5;
        if(index==25)return 6;
        if(index==26)return 7;
        if(index>=43&&index<=49)return 10;
        return 6;
    }

    public static String requirementLabel(int index){
        Domain d=domain(index);
        if(d==Domain.MULTIVARIATE_ENV){
            StringBuilder s=new StringBuilder("multivariable");
            if(needsOrderedTime(index))s.append(" + time");
            if(needsTemperature(index))s.append(" + T");
            if(needsHumidity(index))s.append(" + RH");
            return s.toString();
        }
        if(d==Domain.TIME_SERIES)return "ordered time-series";
        if(d==Domain.MACHINE_LEARNING)return "training dataset / ML";
        return "1-D calibration curve";
    }

    public static int count(Domain d){
        int n=0;
        for(int i=0;i<MqDataScience76Catalog.COUNT;i++)if(domain(i)==d)n++;
        return n;
    }

    private static void check(int index){
        if(index<0||index>=MqDataScience76Catalog.COUNT)throw new IllegalArgumentException("model index");
    }
}
