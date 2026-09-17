#!/usr/bin/env python3
"""Small dependency-free HTTP load gate with machine-readable latency results."""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import math
import statistics
import sys
import time
import urllib.error
import urllib.request
from dataclasses import asdict, dataclass
from pathlib import Path


@dataclass(frozen=True)
class Sample:
    ok: bool
    status: int
    latency_ms: float
    error: str | None = None


def percentile(values: list[float], probability: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    rank = max(0, min(len(ordered) - 1, math.ceil(probability * len(ordered)) - 1))
    return ordered[rank]


def request_once(url: str, method: str, headers: dict[str, str], body: bytes | None, timeout: float) -> Sample:
    started = time.perf_counter()
    request = urllib.request.Request(url=url, method=method, headers=headers, data=body)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            response.read(64)
            status = int(response.status)
            ok = 200 <= status < 300
            return Sample(ok, status, (time.perf_counter() - started) * 1000, None if ok else f"HTTP {status}")
    except urllib.error.HTTPError as exc:
        return Sample(False, int(exc.code), (time.perf_counter() - started) * 1000, str(exc))
    except Exception as exc:
        return Sample(False, 0, (time.perf_counter() - started) * 1000, str(exc))


def parse_headers(raw_headers: list[str]) -> dict[str, str]:
    headers: dict[str, str] = {}
    for raw in raw_headers:
        name, separator, value = raw.partition(":")
        if not separator or not name.strip() or not value.strip():
            raise ValueError(f"invalid header {raw!r}; expected 'Name: value'")
        headers[name.strip()] = value.strip()
    return headers


def main() -> int:
    parser = argparse.ArgumentParser(description="Run a bounded concurrent HTTP load test and enforce SLO thresholds.")
    parser.add_argument("--url", default="http://127.0.0.1:8080/actuator/health")
    parser.add_argument("--method", default="GET", choices=["GET", "POST", "PUT", "DELETE"])
    parser.add_argument("--requests", type=int, default=500)
    parser.add_argument("--concurrency", type=int, default=25)
    parser.add_argument("--timeout", type=float, default=10.0)
    parser.add_argument("--warmup", type=int, default=10)
    parser.add_argument("--header", action="append", default=[])
    parser.add_argument("--json-body", default=None)
    parser.add_argument("--max-error-rate", type=float, default=0.01)
    parser.add_argument("--max-p95-ms", type=float, default=500.0)
    parser.add_argument("--min-rps", type=float, default=1.0)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    if args.requests < 1 or args.concurrency < 1 or args.concurrency > args.requests:
        parser.error("requests must be >= 1 and concurrency must be between 1 and requests")
    headers = parse_headers(args.header)
    body = args.json_body.encode("utf-8") if args.json_body is not None else None
    if body is not None:
        headers.setdefault("Content-Type", "application/json")

    for _ in range(max(0, args.warmup)):
        request_once(args.url, args.method, headers, body, args.timeout)

    started = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as executor:
        futures = [
            executor.submit(request_once, args.url, args.method, headers, body, args.timeout)
            for _ in range(args.requests)
        ]
        samples = [future.result() for future in concurrent.futures.as_completed(futures)]
    elapsed = time.perf_counter() - started

    latencies = [sample.latency_ms for sample in samples]
    failures = [sample for sample in samples if not sample.ok]
    error_rate = len(failures) / len(samples)
    result = {
        "url": args.url,
        "requests": len(samples),
        "concurrency": args.concurrency,
        "elapsedSeconds": round(elapsed, 3),
        "requestsPerSecond": round(len(samples) / elapsed, 3) if elapsed else 0.0,
        "successCount": len(samples) - len(failures),
        "errorCount": len(failures),
        "errorRate": round(error_rate, 6),
        "latencyMs": {
            "min": round(min(latencies), 3),
            "mean": round(statistics.fmean(latencies), 3),
            "p50": round(percentile(latencies, 0.50), 3),
            "p95": round(percentile(latencies, 0.95), 3),
            "p99": round(percentile(latencies, 0.99), 3),
            "max": round(max(latencies), 3),
        },
        "statusCounts": {
            str(status): sum(1 for sample in samples if sample.status == status)
            for status in sorted({sample.status for sample in samples})
        },
        "sampleErrors": [asdict(sample) for sample in failures[:5]],
        "thresholds": {
            "maxErrorRate": args.max_error_rate,
            "maxP95Ms": args.max_p95_ms,
            "minRequestsPerSecond": args.min_rps,
        },
    }
    violations = []
    if error_rate > args.max_error_rate:
        violations.append(f"error rate {error_rate:.4f} exceeds {args.max_error_rate:.4f}")
    if percentile(latencies, 0.95) > args.max_p95_ms:
        violations.append(f"p95 {percentile(latencies, 0.95):.2f}ms exceeds {args.max_p95_ms:.2f}ms")
    if result["requestsPerSecond"] < args.min_rps:
        violations.append(f"throughput {result['requestsPerSecond']:.2f}rps is below {args.min_rps:.2f}rps")
    result["passed"] = not violations
    result["violations"] = violations
    serialized = json.dumps(result, ensure_ascii=False, indent=2)
    print(serialized)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(serialized + "\n", encoding="utf-8")
    return 0 if not violations else 1


if __name__ == "__main__":
    sys.exit(main())
