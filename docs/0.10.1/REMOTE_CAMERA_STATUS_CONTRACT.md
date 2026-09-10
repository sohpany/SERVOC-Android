# HazRob 0.10.1 — Remote Camera Status Contract

Status: **AUDIT FINDING / CORRECTION PENDING**  
Change control: `HR-0101-016`

## Finding

The actual remote-video path and the Status-screen camera indicator currently use different authorities.

The working remote-video path uses Render directly:

1. authenticated HTTPS status at `/hazrob/video/status/hazrob-001` using the VIEW token;
2. `publisher_online` freshness from the Render relay;
3. authenticated WSS viewer at `/hazrob/video/view/hazrob-001`;
4. first-frame JPEG validation and Android bitmap decode.

By contrast, `RemoteConnection` subscribes to MQTT topic `hazrob/hazrob-001/camera/status`, and `StatusTab` reads `RobotApi.remoteCameraStatus()` from that MQTT state. The 0.10.1 ESP32-CAM publishes video through WSS and does not publish this MQTT topic; the 0.10.1 Render relay also does not publish it to HiveMQ.

## Impact

- Remote command/telemetry readiness is not affected.
- Remote WSS video itself is not affected.
- The Status tab may report the camera status as unknown even while video is functioning.
- Camera readiness must not become a prerequisite for remote robot control.

## Authoritative 0.10.1 rule

Render `/hazrob/video/status/hazrob-001` is the authoritative lightweight status source for remote camera publisher state.

The full `RemoteVideoReadiness` end-to-end test remains appropriate for explicit diagnostics because it waits for the service/publisher and validates a decodable WSS JPEG. It must not be run synchronously on every normal UI refresh.

## Required correction

1. add a lightweight asynchronous Render video-status client using the already provisioned VIEW token;
2. use that result in `StatusTab` for remote camera state/freshness;
3. retain WSS first-frame/decode testing for explicit readiness diagnostics;
4. remove or deprecate the unowned MQTT `camera/status` subscription only after confirming no other 0.10.1 component publishes it;
5. add a cross-component test preventing StatusTab from regressing to an unowned camera-status transport.

## Verification gates

- source contract confirms one remote camera status authority;
- Android build/tests pass;
- live Render 0.10.1 status endpoint reports publisher state;
- with ESP32-CAM publisher online, Status tab shows online;
- with publisher offline, Status tab shows offline/unknown without blocking Remote robot control.
