# HazRob 0.10.1 — Render Deployment Contract

Status: **AUTHORITATIVE DEPLOYMENT CONTRACT / LIVE ALIGNMENT PENDING**  
Change control: `HR-0101-014`  
Source branch: `dev/hazrob-0.10.1`

## Production service identity

- Service name: `hazrob-video-relay`
- Service ID: `srv-dad3p8gn74is73d52aj0`
- Public host: `hazrob-video-relay.onrender.com`
- Repository: `sohpany/SERVOC-Android`
- Required branch: `dev/hazrob-0.10.1` during integration; release deployment shall ultimately reference the controlled 0.10.1 release baseline.
- Root directory: `remote/video-relay`
- Runtime: Node.js
- Build command: `npm install`
- Start command: `npm start`
- Health path: `/health`
- Auto deploy: enabled only after the service branch is aligned with the controlled 0.10.1 source.

## Required environment-variable contract

Secret values are intentionally not stored in GitHub. Render shall provide these names at runtime:

| Variable | Role | Required |
|---|---|---|
| `BOOTSTRAP_TOKEN` | authenticates the controlled bootstrap request | yes |
| `PUBLISH_TOKEN` | ESP32-CAM WSS publisher credential | yes |
| `VIEW_TOKEN` | Android WSS viewer/status credential | yes |
| `MQTT_APP_USER` | Android/operator HiveMQ identity; default source value is `hazrob_app` when omitted | recommended explicit |
| `MQTT_APP_PASSWORD` | Android/operator HiveMQ credential | yes |
| `MQTT_ROBOT_USER` | master ESP32 HiveMQ identity; default source value is `hazrob_robot` when omitted | recommended explicit |
| `MQTT_ROBOT_PASSWORD` | master ESP32 HiveMQ credential | yes for 0.10.1 |
| `MQTT_ROBOT_PASSWORD_TEST` | legacy diagnostic fallback only; must not be the authoritative 0.10.1 robot secret | migration-only |
| `HAZROB_SKIP_MQTT_DIAGNOSTICS` | optional startup-diagnostic suppression flag | optional |
| `PORT` | supplied by Render | platform-managed |

The 0.10.1 service must not expose credential values in logs, health responses, source control, update manifests, or operator UI.

## Role-scoped bootstrap response contract

Authorized `GET /hazrob/bootstrap` requests identify the client with `X-HazRob-Client`:

- `android` -> `mqtt_user`, `mqtt_password`, `video_view_token`.
- `esp32` -> robot `mqtt_user`, robot `mqtt_password` only.
- `esp32-cam` -> `video_publish_token` only.
- `legacy` -> temporary migration bridge for already-deployed 0.10.0 clients.

The legacy response is transitional and must not become the long-term 0.10.1 client contract.

## Video endpoints

- Publisher WSS: `/hazrob/video/publish/hazrob-001`
- Viewer WSS: `/hazrob/video/view/hazrob-001`
- Authenticated status: `/hazrob/video/status/hazrob-001`
- Public health: `/health`

Publisher and viewer credentials must be distinct. JPEG frames are binary WebSocket payloads and are validated for JPEG SOI/EOI markers and bounded payload size before forwarding.

## HiveMQ contract used by the relay

- Broker: `e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud`
- TLS port: `8883`
- Operator and robot identities are distinct.
- Startup diagnostics may verify TLS + MQTT CONNACK without logging passwords.

## Live audit finding — 2026-09-10

The current production service is **not yet aligned** with this contract:

- Render service branch is `dev/hazrob-0.9`.
- Current live source commit is `873bbaa23756b45c77c1eff66963048147c5bab5`.
- Current live relay reports version `0.10.0`.
- Current live bootstrap is not role-scoped and does not provide the 0.10.1 ESP32-CAM `video_publish_token` response.
- Render startup logs from the current deployment show successful TLS/MQTT authentication for both the application identity and robot identity (`CONNACK=0`), proving those stored broker credentials were accepted at that deployment time.

## Deployment rule

Do **not** trigger a production redeploy while the service still points at `dev/hazrob-0.9`; doing so only republishes the retired source line. Do not copy 0.10.1 code into the old branch as a workaround.

The production branch must first be changed to the controlled 0.10.1 branch through an authorized Render service-setting operation. After that change:

1. deploy the exact controlled commit;
2. verify build and startup logs show relay version 0.10.1;
3. verify `/health` reports the 0.10.1 contract and all required configuration booleans are true;
4. verify role-scoped bootstrap for `android`, `esp32`, and `esp32-cam` without exposing returned secret values;
5. verify HiveMQ application and robot diagnostics both return `CONNACK=0`;
6. verify ESP32-CAM publisher WSS, Android viewer WSS and status freshness;
7. record the live Render deploy ID + source SHA in the final 0.10.1 release manifest.

The currently available Render integration does not expose a safe operation for changing the branch of an existing service. Therefore branch alignment remains an explicit release blocker rather than being bypassed through a new service or retired branch.
