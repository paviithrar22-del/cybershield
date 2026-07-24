import threading
import time
import urllib.request
import urllib.error
import json
import statistics

# Real target: the project's actual deployed Supabase Edge Function, which calls the real
# Gemini API on every request. This used to spin up a local mock HTTP server and load-test
# that instead - meaning the "API load test" never touched the real backend at all.
#
# The connected Google Cloud project's Gemini free tier caps generate_content calls for
# this model at 20 requests/day, project-wide (confirmed via a real 429 RESOURCE_EXHAUSTED
# response: "GenerateRequestsPerDayPerProjectPerModel-FreeTier", limit 20). Defaults here
# are kept well under that so a single test run doesn't consume the whole day's quota by
# itself - raise them only once billing is enabled on that Google Cloud project.
SUPABASE_URL = "https://rkzrhiwxbypqfttoczzj.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_Gk6mjuBLJAwNejBarnDzSw_zT2ITHy5"
CLASSIFY_ENDPOINT = f"{SUPABASE_URL}/functions/v1/classify-message"

SAMPLE_MESSAGES = [
    "Please stop bullying me.",
    "You are worthless and should just go away.",
    "Hey, want to hang out after school?",
    "I hate you, everyone hates you too.",
    "Great job on the project today!",
]


def run_load_test(url=CLASSIFY_ENDPOINT, concurrency=2, total_requests=6):
    """Send real concurrent HTTPS requests to the live classify-message Edge Function."""
    print(f"Starting real load test to {url} with concurrency={concurrency}, total_requests={total_requests}...")

    latencies = []
    errors = 0
    success = 0
    lock = threading.Lock()

    def run_thread_requests(count, start_index):
        nonlocal errors, success
        for i in range(count):
            t0 = time.time()
            try:
                message = SAMPLE_MESSAGES[(start_index + i) % len(SAMPLE_MESSAGES)]
                data = json.dumps({"text": message}).encode("utf-8")
                req = urllib.request.Request(
                    url,
                    data=data,
                    headers={
                        "Content-Type": "application/json",
                        "apikey": SUPABASE_ANON_KEY,
                        "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
                    },
                    method="POST",
                )
                with urllib.request.urlopen(req, timeout=15.0) as response:
                    body = json.loads(response.read().decode("utf-8"))
                    with lock:
                        if response.status == 200 and "severity" in body:
                            success += 1
                        else:
                            errors += 1
            except Exception:
                with lock:
                    errors += 1
            finally:
                with lock:
                    latencies.append((time.time() - t0) * 1000)  # ms

    reqs_per_thread = max(total_requests // concurrency, 1)
    threads = []
    start_time = time.time()

    for i in range(concurrency):
        t = threading.Thread(target=run_thread_requests, args=(reqs_per_thread, i * reqs_per_thread))
        threads.append(t)
        t.start()

    for t in threads:
        t.join()

    total_duration = time.time() - start_time

    if not latencies:
        latencies = [0]

    mean_latency = statistics.mean(latencies)
    p95_latency = sorted(latencies)[int(len(latencies) * 0.95)] if len(latencies) > 1 else latencies[0]
    throughput = len(latencies) / total_duration if total_duration > 0 else 0

    stats = {
        "concurrency": concurrency,
        "total_requests": len(latencies),
        "passed": success,
        "failed": errors,
        "duration_s": round(total_duration, 2),
        "mean_latency_ms": round(mean_latency, 2),
        "p95_latency_ms": round(p95_latency, 2),
        "throughput_req_sec": round(throughput, 2),
    }

    return stats


def execute():
    return run_load_test()


if __name__ == "__main__":
    results = execute()
    print(json.dumps(results, indent=2))
