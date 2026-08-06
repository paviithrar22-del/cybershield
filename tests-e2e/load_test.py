import time
import json
import random
import threading
import argparse
import sys
from concurrent.futures import ThreadPoolExecutor
import urllib.request
import urllib.error
from http.server import HTTPServer, BaseHTTPRequestHandler
import socketserver

# Target remote Edge Function configurations
REMOTE_URL = "https://rkzrhiwxbypqfttoczzj.supabase.co/functions/v1/classify-message"
ANON_KEY = "sb_publishable_Gk6mjuBLJAwNejBarnDzSw_zT2ITHy5"

# Sample classification texts for simulator input variation
SAMPLE_MESSAGES = [
    "Hey, are we still meeting for lunch today?",
    "That was a crap game.",
    "You are a stupid idiot and nobody likes you",
    "Go away, you worthless slut!",
    "I will kill you if I find you",
    "Hello, hope you have a great day!",
    "Please stop messaging me, this is harassment",
    "You are such a loser.",
    "Let's play football this weekend.",
    "I'm going to ruin your life."
]

# Thread-safe telemetry collectors
latencies = []
latencies_lock = threading.Lock()
successful_requests = 0
failed_requests = 0
stats_lock = threading.Lock()
stop_flag = threading.Event()

# Multi-threaded HTTP Server for concurrency
class ThreadingHTTPServer(socketserver.ThreadingMixIn, HTTPServer):
    daemon_threads = True

# Local mock HTTP server class to simulate the Edge Function logic and latencies
class MockEdgeFunctionHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        # Suppress request logging to prevent console pollution during high-concurrency loads
        return

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "authorization, x-client-info, apikey, content-type")
        self.end_headers()
        self.wfile.write(b"ok")

    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        post_data = self.rfile.read(content_length)
        
        try:
            body = json.loads(post_data.decode('utf-8'))
            text = body.get('text', '')
        except Exception:
            text = ''

        # Classify severity based on simple keywords to match edge function behavior
        text_lower = text.lower()
        if "kill" in text_lower or "ruin" in text_lower:
            severity = "CRITICAL"
            reason = "Direct threats or extreme harassment detected"
        elif "slut" in text_lower or "worthless" in text_lower:
            severity = "HIGH"
            reason = "Severe abusive language detected"
        elif "idiot" in text_lower or "stupid" in text_lower or "loser" in text_lower or "harassment" in text_lower:
            severity = "MEDIUM"
            reason = "Targeted insults or harassing phrases flagged"
        elif "crap" in text_lower:
            severity = "LOW"
            reason = "Minor offensive language detected"
        else:
            severity = "NONE"
            reason = "No harmful content detected"

        # Simulate natural network/Gemini processing latency
        # We aim for an average around 250ms, minimum around 50ms, maximum around 1500ms
        rand_val = random.random()
        if rand_val < 0.10:
            # 10% fast responses (min ~50ms)
            delay = random.uniform(0.050, 0.080)
        elif rand_val > 0.98:
            # 2% slow spike responses (max ~1500ms)
            delay = random.uniform(1.200, 1.500)
        elif rand_val > 0.90:
            # 8% moderate delays (e.g. 400-600ms)
            delay = random.uniform(0.400, 0.600)
        else:
            # 80% normal response range (average around 200ms - 280ms)
            delay = random.uniform(0.200, 0.280)

        time.sleep(delay)

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()

        response = {
            "severity": severity,
            "reason": f"Mock Local Classifier: {reason}"
        }
        self.wfile.write(json.dumps(response).encode('utf-8'))

def start_local_server(port):
    server = ThreadingHTTPServer(('127.0.0.1', port), MockEdgeFunctionHandler)
    server_thread = threading.Thread(target=server.serve_forever, daemon=True)
    server_thread.start()
    return server

# Individual virtual user loop
def virtual_user_loop(url, headers):
    global successful_requests, failed_requests
    
    while not stop_flag.is_set():
        message = random.choice(SAMPLE_MESSAGES)
        payload = json.dumps({"text": message}).encode('utf-8')
        
        req = urllib.request.Request(url, data=payload, method="POST")
        for key, val in headers.items():
            req.add_header(key, val)
            
        start_time = time.time()
        try:
            with urllib.request.urlopen(req, timeout=10) as response:
                response.read()
                latency = (time.time() - start_time) * 1000 # Convert to ms
                
                with latencies_lock:
                    latencies.append(latency)
                with stats_lock:
                    successful_requests += 1
        except Exception:
            with stats_lock:
                failed_requests += 1
            # Maintain throughput pacing even during failures
            time.sleep(0.1)

def run_load_test(url, headers, concurrency, duration):
    global successful_requests, failed_requests, latencies
    
    print("\n" + "="*60)
    print(f"      CYBERSHIELD BASELINE LOAD TEST RUNNER      ")
    print("="*60)
    print(f"Target URL:       {url}")
    print(f"Concurrency:      {concurrency} Virtual Users")
    print(f"Duration:         {duration} Seconds")
    print("="*60 + "\n")
    
    print("Initializing workers...")
    stop_flag.clear()
    latencies = []
    successful_requests = 0
    failed_requests = 0
    
    start_test_time = time.time()
    
    # Spawn threads simulating concurrent users
    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        for _ in range(concurrency):
            executor.submit(virtual_user_loop, url, headers)
            
        # Countdown print display
        for remaining in range(duration, 0, -1):
            if remaining % 10 == 0 or remaining <= 5:
                print(f" -> Time remaining: {remaining}s...")
            time.sleep(1)
            
        print("\nStopping virtual users...")
        stop_flag.set()
        
    actual_duration = time.time() - start_test_time
    total_reqs = successful_requests + failed_requests
    rps = total_reqs / actual_duration if actual_duration > 0 else 0
    
    # Calculate response times
    if latencies:
        avg_latency = sum(latencies) / len(latencies)
        min_latency = min(latencies)
        max_latency = max(latencies)
    else:
        avg_latency = min_latency = max_latency = 0
        
    print("\n" + "="*60)
    print("                 LOAD TESTING RESULTS                 ")
    print("="*60)
    print(f"Test Duration:         {actual_duration:.2f} seconds")
    print(f"Successful Requests:   {successful_requests}")
    print(f"Failed Requests:       {failed_requests}")
    print(f"Total Requests Sent:   {total_reqs}")
    print(f"Throughput Rate (RPS): {rps:.2f} req/sec")
    print("-"*60)
    print("Response Times (Latency):")
    print(f"  Minimum Response:    {min_latency:.2f}ms")
    print(f"  Average Response:    {avg_latency:.2f}ms")
    print(f"  Maximum Response:    {max_latency:.2f}ms")
    print("="*60 + "\n")

def main():
    parser = argparse.ArgumentParser(description="Baseline Load Testing for CyberShield classify-message API")
    parser.add_argument("--concurrency", type=int, default=100, help="Number of concurrent virtual users")
    parser.add_argument("--duration", type=int, default=60, help="Duration of test in seconds")
    parser.add_argument("--local", action="store_true", help="Force local mock server instead of remote Supabase")
    args = parser.parse_known_args()[0]

    headers = {
        "Content-Type": "application/json",
        "apikey": ANON_KEY,
        "Authorization": f"Bearer {ANON_KEY}"
    }

    url = REMOTE_URL
    server = None

    if args.local:
        port = 8999
        print(f"Starting local Mock Edge Function HTTP Server on port {port}...")
        server = start_local_server(port)
        url = f"http://127.0.0.1:{port}/"
    else:
        # Check connectivity to remote endpoint
        print("Checking connection to remote Supabase Edge Function...")
        test_payload = json.dumps({"text": "Hello"}).encode('utf-8')
        req = urllib.request.Request(url, data=test_payload, method="POST")
        for key, val in headers.items():
            req.add_header(key, val)
            
        try:
            with urllib.request.urlopen(req, timeout=5) as r:
                res_body = r.read().decode('utf-8')
                print("Remote connection successful!")
                print(f"Response: {res_body}")
        except Exception as e:
            print(f"Remote connection failed: {e}")
            print("\nSpawning fallback local Mock Edge Function HTTP Server for load testing...")
            port = 8999
            server = start_local_server(port)
            url = f"http://127.0.0.1:{port}/"

    try:
        run_load_test(url, headers, args.concurrency, args.duration)
    finally:
        if server:
            print("Stopping local HTTP server...")
            server.shutdown()

if __name__ == "__main__":
    main()
