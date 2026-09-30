#!/usr/bin/env python3
"""HazRob 0.10.2 / HR069 P3 unified measurement/time-series contract."""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def txt(p): return (ROOT/p).read_text(encoding='utf-8')
def req(s,n,label):
    if n not in s: raise AssertionError(f'{label}: missing {n!r}')
def forbid(s,n,label):
    if n in s: raise AssertionError(f'{label}: forbidden {n!r}')

def main():
    cfg=txt('firmware/HazRob/Config.h')
    hw=txt('firmware/HazRob/Hardware.cpp')
    profile=txt('app/src/main/java/com/servoc/app/hardware/HazRobHardwareProfile.java')
    protocol=txt('app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java')
    api=txt('app/src/main/java/com/servoc/app/communication/RobotApi.java')
    canonical=txt('app/src/main/java/com/servoc/app/measurement/CanonicalMeasurementNormalizer.java')
    r0=txt('app/src/main/java/com/servoc/app/calibration/ManualR0Calibration.java')
    health=txt('app/src/main/java/com/servoc/app/measurement/SensorHealthEngine.java')
    recorder=txt('app/src/main/java/com/servoc/app/measurement/TelemetryRecorder.java')
    series=txt('app/src/main/java/com/servoc/app/measurement/SessionSeries.java')

    # Exact HR069 acquisition topology: 8 channels / 2 ADS1115.
    for n in [
        '{"mq2",0,0','{"mq4",0,1','{"mq5",0,2','{"mq8",0,3',
        '{"mq3",1,0','{"mq7",1,1','{"mq9",1,2','{"mq135",1,3']:
        req(cfg,n,'firmware MQ mapping')
    for n in [
        'new Channel(GasEngine.MQ2,  "mq2",  0,0x48,0)',
        'new Channel(GasEngine.MQ4,  "mq4",  0,0x48,1)',
        'new Channel(GasEngine.MQ5,  "mq5",  0,0x48,2)',
        'new Channel(GasEngine.MQ8,  "mq8",  0,0x48,3)',
        'new Channel(GasEngine.MQ3,  "mq3",  1,0x49,0)',
        'new Channel(GasEngine.MQ7,  "mq7",  1,0x49,1)',
        'new Channel(GasEngine.MQ9,  "mq9",  1,0x49,2)',
        'new Channel(GasEngine.MQ135,"mq135",1,0x49,3)']:
        req(profile,n,'Android MQ mapping')
    req(cfg,'ADS1_ADDR=0x48,ADS2_ADDR=0x49','ADS addresses')
    req(hw,'r.rs=Cfg::RL*(Cfg::MQ_VCC-r.ao)/r.ao','firmware Rs equation')
    req(hw,'r.ratio=r.rs/c.r0','firmware source ratio')
    req(hw,'\\"scan_cycle\\"','firmware scan-cycle provenance')
    req(hw,'\\"channel_offset_us\\"','firmware per-channel timing')

    # ProtocolV4 is transport-neutral pre-normalization; Android owns active R0 normalization.
    req(protocol,'ManualR0Calibration.apply(hw.key,x)','active Android R0 application')
    req(protocol,'out.put("_data_api",data)','raw firmware data retained')
    req(canonical,'SCHEMA="hazrob-measurement/1"','measurement schema')
    req(canonical,'NORMALIZATION_OWNER="ANDROID_ACTIVE_R0"','normalization authority')
    req(canonical,'n.put("ratio",canonical)','canonical Rs/R0 write')
    req(canonical,'copyNumber(r,n,"r0","firmware_r0")','firmware R0 audit preservation')
    req(canonical,'copyNumber(r,n,"ratio","firmware_ratio")','firmware ratio audit preservation')
    req(canonical,'"OPEN_OR_LOW_OUTPUT"','open/low quality state')
    req(canonical,'"RAIL_HIGH_OR_SATURATED"','rail/saturation quality state')
    req(canonical,'"ADC_UNAVAILABLE"','ADC missing quality state')
    req(canonical,'"STALE"','stale quality state')
    req(canonical,'quality.put("manufacturer_heater_protocol_valid"','heater qualification separation')

    # R0 identity, time, age and validation provenance.
    for n in ['manual_r0_session_','manual_r0_utc_','r0_age_known','r0_age_ms','r0_provenance_complete']:
        req(r0,n,'R0 provenance')
    req(r0,'q.put("r0_reference_validated",false)','R0 must not manufacture quantitative validation')

    # Chamber environment remains physically and semantically separate from ADS acquisition.
    req(cfg,'ENV_SDA=13,ENV_SCL=14','ENV bus pins')
    req(hw,'TwoWire EnvWire(1)','dedicated ENV controller')
    req(canonical,'env.put("source","HR069_ENV_I2C")','environment provenance')
    req(canonical,'env.put("bus"','environment bus provenance')

    # Direct and Remote converge on the exact same canonical application layer.
    req(api,'return RobotProtocolV4.normalize(diagnosticsRaw(),dataRaw(),commandedLeft,commandedRight)','Direct ProtocolV4 stage')
    req(api,'CanonicalMeasurementNormalizer.enrich(s,"REMOTE")','Remote canonical stage')
    req(api,'CanonicalMeasurementNormalizer.enrich(directNormalizedSnapshot(commandedLeft,commandedRight),"DIRECT")','Direct canonical stage')
    req(canonical,'transport.put("offline_capable","DIRECT".equals(mode))','Direct offline capability declaration')
    forbid(canonical,'http://','canonical layer must be offline/local computation')
    forbid(canonical,'https://','canonical layer must be offline/local computation')

    # One temporal quality object feeds Live and persisted Session.
    req(health,'annotate(snapshot)','temporal quality annotation')
    req(health,'q.put("noise_evaluable",noiseReady)','noise evaluability')
    req(health,'q.put("noise_cv"','temporal noise CV')
    req(health,'q.put("noise_flag"','temporal noise flag')
    req(health,'s.cv>0.12','existing unstable threshold')

    # Canonical session order/time/transport provenance is portable for deterministic replay.
    for n in ['state.put("sample_seq"','state.put("sample_utc_ms"','state.put("sample_monotonic_ms"','state.put("session_t_ms"','state.put("transport"']:
        req(recorder,n,'telemetry time/transport provenance')
    req(recorder,'CanonicalMeasurementNormalizer.SCHEMA','recorder schema lineage')
    req(series,'ORDER BY t_ms,id','deterministic replay ordering')
    for n in ['q.put("seq"','q.put("utc_ms"','q.put("monotonic_ms"','q.put("transport_mode"']:
        req(series,n,'portable session provenance')

    print('HazRob 0.10.2 / HR069 P3 measurement foundation contract: PASS')
    return 0

if __name__=='__main__':
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f'HazRob 0.10.2 / HR069 P3 measurement foundation contract: FAIL: {exc}')
        raise SystemExit(1)
