#!/usr/bin/env python3
"""Temporary HR050 recovery utility.

Recovers the exact controlled flammability asset from the last accepted 0.10.1 APK,
verifies its pinned SHA-256, and exposes it through the release-gate public folder.
When HAZROB_HR050_DUMP_SEED=1 it also emits deterministic gzip+base64 chunks so the
verified snapshot can be transferred into controlled source. Removed after pinning.
"""
from pathlib import Path
from urllib.request import Request, urlopen
from zipfile import ZipFile
import base64
import gzip
import hashlib
import io
import os

APK_URL = "https://hazrob-0101-build.onrender.com/HazRob-0.10.1-TEST.apk"
EXPECTED = "457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434"
OUT = Path("public/recovered-flammability/niosh_npg_flammability.json")


def main() -> int:
    req = Request(APK_URL, headers={"User-Agent": "HazRob-HR050-controlled-recovery/1"})
    with urlopen(req, timeout=60) as r:
        apk = r.read()
    with ZipFile(io.BytesIO(apk), "r") as z:
        candidates = [n for n in z.namelist() if n.endswith("niosh_npg_flammability.json")]
        if len(candidates) != 1:
            raise RuntimeError(f"expected one flammability asset, found {candidates}")
        data = z.read(candidates[0])
    actual = hashlib.sha256(data).hexdigest()
    if actual != EXPECTED:
        raise RuntimeError(f"controlled seed SHA mismatch: expected={EXPECTED} actual={actual}")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(data)
    (OUT.parent / "sha256.txt").write_text(f"{actual}  {OUT.name}\n", encoding="utf-8")
    (OUT.parent / "recovery.txt").write_text(
        f"source_apk={APK_URL}\nasset={candidates[0]}\nsha256={actual}\nstatus=RECOVERED_AND_SHA_VERIFIED\n",
        encoding="utf-8",
    )
    packed = gzip.compress(data, compresslevel=9, mtime=0)
    encoded = base64.b64encode(packed).decode("ascii")
    print(f"HR050 controlled seed recovery: PASS asset={candidates[0]} sha256={actual} bytes={len(data)} gzip_bytes={len(packed)} b64_chars={len(encoded)}")
    if os.environ.get("HAZROB_HR050_DUMP_SEED") == "1":
        width = 6000
        chunks = [encoded[i:i+width] for i in range(0, len(encoded), width)]
        for i, chunk in enumerate(chunks, 1):
            print(f"HR050SEED:{i:03d}/{len(chunks):03d}:{chunk}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
