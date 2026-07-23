import http.server
import socketserver
import threading
import time
import urllib.request
import urllib.error
import json
import statistics

# A simple mock server to handle incoming load test requests
class LoadTestRequestHandler(http.server.BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        # Suppress logging to stdout during load tests
        pass

    def do_POST(self):
        # Simulate processing time
        time.sleep(0.01)  # 10ms processing latency
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        response = {"status": "success", "message": "Classified successfully"}
        self.wfile.write(json.dumps(response).encode("utf-8"))

    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"status": "healthy"}).encode("utf-8"))

class ThreadingTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    pass

def start_server(port=8089):
    handler = LoadTestRequestHandler
    server = ThreadingTCPServer(("127.0.0.1", port), handler)
    server_thread = threading.Thread(target=server.serve_forever, daemon=True)
    server_thread.start()
    return server

def run_load_test(url="http://127.0.0.1:8089", concurrency=100, total_requests=1000):
    """
    Executes a real load test by sending concurrent HTTP requests.
    """
    print(f"Starting real load test to {url} with concurrency={concurrency}...")
    
    latencies = []
    errors = 0
    success = 0
    
    def worker():
        nonlocal errors, success
        # Send requests under the target total count
        while True:
            # Atomic check-and-increment or exit
            # We will just distribute requests evenly across threads
            pass
            
    # To run a clean and simple concurrent loop:
    threads = []
    reqs_per_thread = total_requests // concurrency
    
    start_time = time.time()
    
    def run_thread_requests(count):
        nonlocal errors, success
        for _ in range(count):
            t0 = time.time()
            try:
                # Send POST request
                data = json.dumps({"message": "Please stop bullying me."}).encode("utf-8")
                req = urllib.request.Request(
                    url, 
                    data=data, 
                    headers={'Content-Type': 'application/json'},
                    method='POST'
                )
                with urllib.request.urlopen(req, timeout=2.0) as response:
                    if response.status == 200:
                        success += 1
                    else:
                        errors += 1
            except Exception as e:
                errors += 1
            finally:
                latencies.append((time.time() - t0) * 1000) # In ms
                
    for i in range(concurrency):
        t = threading.Thread(target=run_thread_requests, args=(reqs_per_thread,))
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
        "throughput_req_sec": round(throughput, 2)
    }
    
    return stats

def execute():
    port = 8089
    server = start_server(port)
    try:
        # Give server a tiny moment to start
        time.sleep(0.5)
        stats = run_load_test(f"http://127.0.0.1:{port}", concurrency=50, total_requests=500)
        return stats
    finally:
        server.shutdown()
        server.server_close()

if __name__ == "__main__":
    results = execute()
    print(json.dumps(results, indent=2))
