#!/usr/bin/env python3
"""Reproduce article arithmetic. No model, benchmark, or third-party packages."""
import json
import math
import struct


def fp16(value):
    value = float(value)
    try:
        return struct.unpack(">e", struct.pack(">e", value))[0]
    except OverflowError:
        return math.copysign(math.inf, value)


def bf16(value):
    """Round a finite FP32 value to BF16, nearest with ties to even."""
    bits = struct.unpack(">I", struct.pack(">f", value))[0]
    rounded = (bits + 0x7FFF + ((bits >> 16) & 1)) & 0xFFFF0000
    return struct.unpack(">f", struct.pack(">I", rounded))[0]


def endpoint_grid(value, bits, low=-1.0, high=1.0):
    """Teaching grid, not an implementation of GGUF/NF4/signed INT4."""
    maximum_code = (1 << bits) - 1
    step = (high - low) / maximum_code
    code = min(max(round((value - low) / step), 0), maximum_code)
    restored = low + code * step
    return {
        "bits": bits,
        "levels": maximum_code + 1,
        "step": step,
        "code": code,
        "restored": restored,
        "absolute_error": abs(value - restored),
    }


def main():
    value = 0.1234
    report = {
        "scope": "teaching arithmetic only; not a model quality/speed benchmark",
        "parameters": 8_000_000_000,
        "weight_only_budget": [
            {"bits": bits, "GB": 8_000_000_000 * bits / 8 / 1e9,
             "GiB": 8_000_000_000 * bits / 8 / 2**30}
            for bits in (32, 16, 8, 4)
        ],
        "floating_point": [
            {"original": v, "FP16": fp16(v) if math.isfinite(fp16(v)) else "+inf",
             "BF16": bf16(v)}
            for v in (1.001, 100000.0)
        ],
        "scale_example": {
            "original": value, "scale": 0.02, "zero_point": 0,
            "code": round(value / 0.02), "restored": round(value / 0.02) * 0.02,
            "absolute_error": abs(value - round(value / 0.02) * 0.02),
        },
        "endpoint_grids": [endpoint_grid(value, bits) for bits in (8, 4)],
        "group_range_example": {
            "wide_step": 20 / 15, "narrow_step": 0.2 / 15,
            "note": "same 16-level teaching grid, different ranges",
        },
        "q4_0_block": {
            "weights": 32, "code_bytes": 16, "scale_bytes": 2,
            "total_bytes": 18, "bits_per_weight": 18 * 8 / 32,
            "all_8B_block_data_GB": 8_000_000_000 * (18 / 32) / 1e9,
        },
    }
    assert fp16(1.001) == 1.0009765625
    assert bf16(1.001) == 1.0 and bf16(100000) == 99840
    assert math.isinf(fp16(100000))
    assert endpoint_grid(value, 8)["code"] == 143
    assert endpoint_grid(value, 4)["code"] == 8
    assert report["q4_0_block"]["bits_per_weight"] == 4.5
    print(json.dumps(report, ensure_ascii=False, indent=2, allow_nan=False))


if __name__ == "__main__":
    main()
