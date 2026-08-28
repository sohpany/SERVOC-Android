# HazRob Prototype Signing

The checked-in base64 keystore is intentionally a **prototype-only** signing key used to keep iterative test APKs update-compatible from HazRob v0.3.0 onward.

It must not be reused for public, production, Play Store, regulated, or commercial distribution. Before production, replace it with a production key stored outside Git and supplied to CI through protected secrets.

Prototype certificate SHA-256:
`D6:B5:B6:9B:E3:82:F2:AF:A6:DF:4B:46:BA:DE:5B:6F:86:4F:40:60:62:2E:94:90:75:29:3A:C6:20:03:D3:F0`
