# Railway relay deployment trigger — HazRob 0.10.2

This documentation-only commit intentionally refreshes the HEAD of `release/hazrob-0.10.2` so Railway GitHub Auto Deploy fetches the configured branch instead of reusing an older deployment snapshot.

Target service contract:
- source branch: `release/hazrob-0.10.2`
- root directory: `remote/video-relay`
- start command: `npm start`
- health check: `/health`
- service identity: HazRob `0.10.2`
- protocol: `2`

No application, firmware, analytical-library, MQTT-topic, ACL, or runtime behavior is changed by this commit.
