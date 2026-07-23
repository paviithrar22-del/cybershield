import sys
import subprocess
import os

# 1. Check and install dependencies automatically
def install_dependencies():
    print("Checking Python dependencies...")
    required_packages = ["pytest", "selenium", "Appium-Python-Client", "openpyxl", "pandas"]
    missing_packages = []
    
    for pkg in required_packages:
        # Map package import name
        import_name = pkg
        if pkg == "Appium-Python-Client":
            import_name = "appium"
            
        try:
            __import__(import_name)
        except ImportError:
            missing_packages.append(pkg)
            
    if missing_packages:
        print(f"Missing packages detected: {missing_packages}. Installing them now...")
        try:
            subprocess.check_call([sys.executable, "-m", "pip", "install", "-r", "requirements.txt"])
            print("Dependencies installed successfully!")
        except Exception as e:
            print(f"Error installing dependencies: {e}")
            sys.exit(1)
    else:
        print("All dependencies are satisfied!")

# Install dependencies before any other execution
# Get current file directory
current_dir = os.path.dirname(os.path.abspath(__file__))
os.chdir(current_dir)
install_dependencies()

import pytest
from report_generator import generate_excel_report

# Custom Pytest Plugin to collect test results in memory
class ResultCollectorPlugin:
    def __init__(self):
        self.results = []

    def pytest_runtest_logreport(self, report):
        # We only want 'call' phase results (or setup/teardown failures)
        if report.when == 'call' or (report.when in ['setup', 'teardown'] and report.failed):
            # Parse test name
            test_id = report.nodeid.split("::")[-1]
            
            # Map category and platform from test name and properties
            platform = "Web" if "web" in test_id.lower() else "Mobile"
            
            # Determine status
            status = "PASS"
            error_log = ""
            if report.failed:
                status = "FAIL"
                error_log = str(report.longrepr) if report.longrepr else "Test Assertion Failed"
            elif report.skipped:
                status = "SKIP"
                error_log = str(report.longrepr) if report.longrepr else "Test Skipped"
                
            # Read category and docstring from the test function
            category = "Functional"
            test_desc = test_id
            
            # Try to dynamically load the module and function to fetch docstring
            try:
                module_name = report.nodeid.split("::")[0].replace(".py", "")
                func_name = report.nodeid.split("::")[-1].split("[")[0]
                
                # Import module
                mod = __import__(module_name)
                func_obj = getattr(mod, func_name)
                doc = func_obj.__doc__
                if doc:
                    test_desc = doc.strip().split("\n")[0]
                    # Parse category
                    if "Category:" in doc:
                        category = doc.split("Category:")[1].split("-")[0].strip()
                        test_desc = doc.split("Category:")[1].split("-", 1)[1].strip()
            except Exception as e:
                pass
                
            self.results.append({
                "id": test_id,
                "name": test_desc,
                "platform": platform,
                "category": category,
                "status": status,
                "duration": report.duration,
                "error": error_log
            })

            # Print in the user-requested Appium log format
            import datetime
            import sys
            timestamp = datetime.datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z'
            tc_num = len(self.results)
            tc_id = f"TC-{tc_num}"
            duration_ms = int(report.duration * 1000)
            actual_detail = f"/{test_id}"
            
            sys.stdout.write(f"[{timestamp}] Running: {tc_id} - {test_desc} [{category}]\n")
            sys.stdout.write(f"[{timestamp}] Result: {tc_id} -> {status} ({duration_ms}ms). Actual: {actual_detail}\n")
            sys.stdout.flush()

def main():
    import argparse
    parser = argparse.ArgumentParser(description="CyberShield E2E Test Suite Runner")
    parser.add_argument(
        "--dry-run", action="store_true", default=False, help="Run test cases in Dry-Run Mock Mode"
    )
    args, unknown = parser.parse_known_args()
    
    print("\n" + "="*50)
    print("      CYBERSHIELD AUTOMATION TEST RUNNER      ")
    print("="*50)
    print(f"Mode: {'DRY-RUN (Mock Drivers)' if args.dry_run else 'LIVE (WebDrivers)'}")
    print("="*50 + "\n")
    
    # Configure pytest args
    pytest_args = ["-v"]
    if args.dry_run:
        pytest_args.append("--dry-run")
        
    # Append any unknown arguments (allows user to filter tests e.g., -k test_web)
    pytest_args.extend(unknown)
    
    # Instantiate custom collector
    collector = ResultCollectorPlugin()
    
    # Run pytest programmatically
    print("Executing tests...")
    exit_code = pytest.main(pytest_args, plugins=[collector])
    
    # Post-process results
    results = collector.results
    total_tests = len(results)
    
    if total_tests == 0:
        print("\n[WARNING] No tests were executed. Please check your patterns.")
        sys.exit(exit_code)
        
    passed_tests = len([r for r in results if r["status"] == "PASS"])
    failed_tests = len([r for r in results if r["status"] == "FAIL"])
    skipped_tests = len([r for r in results if r["status"] == "SKIP"])
    duration = sum(r["duration"] for r in results)
    
    # Matching the requested log format
    print(f"Suite Execution Finished. Duration: {duration:.2f}s. Passed: {passed_tests}/{total_tests} ({passed_tests/total_tests*100:.2f}%)")
    print("Writing reports...")
    output_xlsx = "cybershield_test_analysis.xlsx"
    print(f"Excel report generated: {os.path.abspath(output_xlsx)}")
    print(f"Reports written to: {os.path.dirname(os.path.abspath(output_xlsx))}")
    print(f"Pass rate: {passed_tests/total_tests*100:.2f}% ({passed_tests}/{total_tests})")
    
    # Generate report
    generate_excel_report(results, output_xlsx)
    
    sys.exit(exit_code)

if __name__ == "__main__":
    main()
