#!/usr/bin/env python3
"""Summarize LinAntiAim JSONL captures for REA card §4."""
from __future__ import annotations

import argparse
import json
import math
import sys
from pathlib import Path


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
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        row = json.loads(line)
        t = row.get("type")
        if t == "session":
            meta = row
        elif t == "look":
            looks.append(row)
        elif t == "attack":
            attacks.append(row)
        elif t == "swing":
            swings += 1

    dyaws = [abs(float(r["dYaw"])) for r in looks if "dYaw" in r]
    dpitchs = [abs(float(r["dPitch"])) for r in looks if "dPitch" in r]
    aligns = [float(r["alignDeg"]) for r in attacks if "alignDeg" in r and float(r["alignDeg"]) >= 0]

    dyaws.sort()
    dpitchs.sort()
    aligns.sort()

    print(f"=== {path.name} ===")
    if meta:
        print(f"label={meta.get('label')} player={meta.get('player')}")
    print(f"looks={len(looks)} attacks={len(attacks)} swings={swings}")
    if dyaws:
        print(
            " |dYaw| p50={:.3f} p90={:.3f} p99={:.3f} max={:.3f}".format(
                percentile(dyaws, 0.5), percentile(dyaws, 0.9), percentile(dyaws, 0.99), dyaws[-1]
            )
        )
    if dpitchs:
        print(
            " |dPitch| p50={:.3f} p90={:.3f} p99={:.3f} max={:.3f}".format(
                percentile(dpitchs, 0.5), percentile(dpitchs, 0.9), percentile(dpitchs, 0.99), dpitchs[-1]
            )
        )
    if aligns:
        print(
            " alignDeg@attack p50={:.3f} p90={:.3f} p99={:.3f} min={:.3f}".format(
                percentile(aligns, 0.5), percentile(aligns, 0.9), percentile(aligns, 0.99), aligns[0]
            )
        )
    # crude GCD hint: fraction of dYaw that are "too clean" vs 0.15° grid (provisional)
    if dyaws:
        grid = 0.15
        near = sum(1 for v in dyaws if v > 1e-6 and abs(v / grid - round(v / grid)) < 0.05)
        print(f" provisional near-grid(|dYaw|,0.15°) ratio={near / len(dyaws):.3f} (not a real sens GCD)")
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
