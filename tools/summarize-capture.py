#!/usr/bin/env python3
"""Summarize LinAntiAim JSONL captures for REA card §4."""
from __future__ import annotations

import argparse
import json
import math
import re
import sys
from pathlib import Path


def fix_line(line: str) -> str:
    # legacy bug: protocol value written without quotes
    return re.sub(r'"protocol":([A-Za-z0-9_-]+)', r'"protocol":"\1"', line)


def percentile(sorted_vals: list[float], p: float) -> float:
    if not sorted_vals:
        return float("nan")
    if len(sorted_vals) == 1:
        return sorted_vals[0]
    k = (len(sorted_vals) - 1) * p
    f = math.floor(k)
    c = math.ceil(k)
    if f == c:
        return sorted_vals[int(k)]
    return sorted_vals[f] * (c - k) + sorted_vals[c] * (k - f)


def analyze(path: Path) -> None:
    looks: list[dict] = []
    attacks: list[dict] = []
    swings = 0
    meta: dict = {}
    end: dict = {}
    last_rot_dy = None
    last_rot_t = None
    swing_dy: list[float] = []

    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        row = json.loads(fix_line(line))
        t = row.get("type")
        if t == "session":
            meta = row
        elif t == "session_end":
            end = row
        elif t == "look":
            looks.append(row)
            if row.get("hasRot") and "dYaw" in row:
                last_rot_dy = abs(float(row["dYaw"]))
                last_rot_t = int(row["t"])
        elif t == "attack":
            attacks.append(row)
        elif t == "swing":
            swings += 1
            if last_rot_t is not None and last_rot_dy is not None and int(row["t"]) - last_rot_t <= 150:
                swing_dy.append(last_rot_dy)

    dyaws = [abs(float(r["dYaw"])) for r in looks if "dYaw" in r and r.get("hasRot")]
    dpitchs = [abs(float(r["dPitch"])) for r in looks if "dPitch" in r and r.get("hasRot")]
    aligns = [float(r["alignDeg"]) for r in attacks if "alignDeg" in r and float(r["alignDeg"]) >= 0]

    dyaws.sort()
    dpitchs.sort()
    aligns.sort()
    swing_dy.sort()

    print(f"=== {path.name} ===")
    if meta:
        print(f"label={meta.get('label')} player={meta.get('player')}")
        if meta.get("label_corrected"):
            print(f"label_note={meta.get('label_note')}")
    if end:
        print(f"durationMs={end.get('durationMs')}")
    print(f"looks={len(looks)} attacks={len(attacks)} swings={swings}")
    if dyaws:
        ge20 = sum(1 for v in dyaws if v >= 20) / len(dyaws)
        print(
            " |dYaw|@hasRot p50={:.3f} p90={:.3f} p99={:.3f} max={:.3f} ge20°={:.1%}".format(
                percentile(dyaws, 0.5), percentile(dyaws, 0.9), percentile(dyaws, 0.99), dyaws[-1], ge20
            )
        )
    if dpitchs:
        print(
            " |dPitch|@hasRot p50={:.3f} p90={:.3f} p99={:.3f} max={:.3f}".format(
                percentile(dpitchs, 0.5), percentile(dpitchs, 0.9), percentile(dpitchs, 0.99), dpitchs[-1]
            )
        )
    if swing_dy:
        print(
            " swing-adj |dYaw| (≤150ms) n={} p50={:.3f} p90={:.3f}".format(
                len(swing_dy), percentile(swing_dy, 0.5), percentile(swing_dy, 0.9)
            )
        )
    if aligns:
        print(
            " alignDeg@attack p50={:.3f} p90={:.3f} p99={:.3f} min={:.3f}".format(
                percentile(aligns, 0.5), percentile(aligns, 0.9), percentile(aligns, 0.99), aligns[0]
            )
        )
    elif attacks == 0:
        print(" alignDeg: N/A (no attack events in this capture)")
    print()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("files", nargs="+", type=Path)
    args = ap.parse_args()
    for f in args.files:
        if not f.is_file():
            print(f"missing: {f}", file=sys.stderr)
            continue
        analyze(f)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
