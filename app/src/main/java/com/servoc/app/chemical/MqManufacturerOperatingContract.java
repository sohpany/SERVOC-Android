package com.servoc.app.chemical;

/**
 * Manufacturer operating/test-condition metadata for the eight MQ sensors installed in HazRob.
 *
 * These values are not calibration coefficients. They define the reference/test envelope needed
 * to interpret manufacturer curves and to prevent non-standard heater operation from silently
 * becoming quantitative evidence.
 */
public final class MqManufacturerOperatingContract {
    private MqManufacturerOperatingContract(){}

    public static final class Spec {
        public final int sensor;
        public final String model;
        public final double minTargetPpm;
        public final double maxTargetPpm;
        public final double standardTemperatureC;
        public final double standardTemperatureToleranceC;
        public final double standardHumidityPct;
        public final double standardHumidityTolerancePct;
        public final double minimumOxygenPct;
        public final double nominalHeaterV;
        public final boolean heaterCycleRequired;
        public final double highHeaterV;
        public final int highHeaterSeconds;
        public final double lowHeaterV;
        public final int lowHeaterSeconds;
        public final int minimumInitialPreheatHours;
        public final String source;

        Spec(int sensor,String model,double minTargetPpm,double maxTargetPpm,
             double standardTemperatureC,double standardTemperatureToleranceC,
             double standardHumidityPct,double standardHumidityTolerancePct,
             double minimumOxygenPct,double nominalHeaterV,boolean heaterCycleRequired,
             double highHeaterV,int highHeaterSeconds,double lowHeaterV,int lowHeaterSeconds,
             int minimumInitialPreheatHours,String source){
            this.sensor=sensor;this.model=model;this.minTargetPpm=minTargetPpm;this.maxTargetPpm=maxTargetPpm;
            this.standardTemperatureC=standardTemperatureC;this.standardTemperatureToleranceC=standardTemperatureToleranceC;
            this.standardHumidityPct=standardHumidityPct;this.standardHumidityTolerancePct=standardHumidityTolerancePct;
            this.minimumOxygenPct=minimumOxygenPct;this.nominalHeaterV=nominalHeaterV;
            this.heaterCycleRequired=heaterCycleRequired;this.highHeaterV=highHeaterV;this.highHeaterSeconds=highHeaterSeconds;
            this.lowHeaterV=lowHeaterV;this.lowHeaterSeconds=lowHeaterSeconds;
            this.minimumInitialPreheatHours=minimumInitialPreheatHours;this.source=source;
        }

        /** True only for the manufacturer's standard reference test environment, not the full operating envelope. */
        public boolean atReferenceEnvironment(double temperatureC,double humidityPct){
            return Double.isFinite(temperatureC)&&Double.isFinite(humidityPct)
                    &&Math.abs(temperatureC-standardTemperatureC)<=standardTemperatureToleranceC
                    &&Math.abs(humidityPct-standardHumidityPct)<=standardHumidityTolerancePct;
        }
    }

    public static Spec get(int sensor){
        switch(sensor){
            case GasEngine.MQ2:return normal(sensor,"MQ-2",300,10000,"https://www.winsen-sensor.com/product/mq-2.html");
            case GasEngine.MQ3:return normal(sensor,"MQ-3B",25,500,"https://www.winsen-sensor.com/product/mq-3b.html");
            case GasEngine.MQ4:return normal(sensor,"MQ-4",300,10000,"https://www.winsen-sensor.com/product/mq-4.html");
            case GasEngine.MQ5:return normal(sensor,"MQ-5",300,10000,"https://www.winsen-sensor.com/product/mq-5.html");
            case GasEngine.MQ8:return normal(sensor,"MQ-8",100,1000,"https://www.winsen-sensor.com/product/mq-8.html");
            case GasEngine.MQ135:return normal(sensor,"MQ135",10,1000,"https://www.winsen-sensor.com/product/mq135.html");
            case GasEngine.MQ7:return cycled(sensor,"MQ-7B",10,500,"https://www.winsen-sensor.com/product/mq-7b.html");
            case GasEngine.MQ9:return cycled(sensor,"MQ-9B",10,10000,"https://www.winsen-sensor.com/product/mq-9b.html");
            default:return null;
        }
    }

    public static boolean requiresHeaterCycle(int sensor){Spec s=get(sensor);return s!=null&&s.heaterCycleRequired;}

    private static Spec normal(int sensor,String model,double min,double max,String source){
        return new Spec(sensor,model,min,max,20,2,55,5,18,5,false,
                Double.NaN,0,Double.NaN,0,48,source);
    }

    private static Spec cycled(int sensor,String model,double min,double max,String source){
        return new Spec(sensor,model,min,max,20,2,55,5,18,Double.NaN,true,
                5.0,60,1.5,90,48,source);
    }
}
