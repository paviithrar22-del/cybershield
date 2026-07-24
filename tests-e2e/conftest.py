import pytest
import os
import sys

# Mock Element implementation for Dry-Run mode
class MockElement:
    def __init__(self, tag_name="div", text="", attrs=None):
        self.tag_name = tag_name
        self._text = text
        self.attrs = attrs or {}
        self.rect = {"x": 10, "y": 20, "width": 100, "height": 50}

    @property
    def text(self):
        return self._text

    def click(self):
        pass

    def send_keys(self, *args):
        pass

    def is_displayed(self):
        return True

    def is_enabled(self):
        return True

    def get_attribute(self, name):
        return self.attrs.get(name, "")

    def value_of_css_property(self, property_name):
        return "rgba(139, 92, 246, 1)" # PurpleAccent color mock

# Mock Selenium WebDriver
class MockWebDriver:
    def __init__(self):
        self.title = "CyberShield | Real-time Bullying Protection Dashboard"
        import os
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        self.current_url = "file:///" + os.path.join(project_root, "cybershield_demo.html").replace("\\", "/")
        self.page_source = "<html>Mock CyberShield HTML</html>"

    def get(self, url):
        if "cybershield_demo.html" in url:
            import os
            project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
            self.current_url = "file:///" + os.path.join(project_root, "cybershield_demo.html").replace("\\", "/")

    def find_element(self, by, value):
        return MockElement(text="Mocked Element")

    def find_elements(self, by, value):
        return [MockElement(text="Mocked Element 1"), MockElement(text="Mocked Element 2")]

    def execute_script(self, script, *args):
        return {"status": "success", "scanned": 12, "flagged": 2}

    def quit(self):
        pass

# Mock Appium Mobile Driver
class MockAppiumDriver:
    def __init__(self):
        self.capabilities = {"platformName": "Android", "deviceName": "emulator-5554"}
        self.page_source = "<hierarchy><node class='android.widget.TextView' text='CyberShield' /></hierarchy>"

    def find_element(self, by, value):
        return MockElement(text="Mock Mobile Element")

    def find_elements(self, by, value):
        return [MockElement(text="Mock Mobile 1"), MockElement(text="Mock Mobile 2")]

    def start_activity(self, app_package, app_activity, **kwargs):
        pass

    def press_keycode(self, keycode, metastate=None):
        pass

    def background_app(self, seconds):
        pass

    def quit(self):
        pass

def pytest_addoption(parser):
    parser.addoption(
        "--dry-run", action="store_true", default=False, help="Run E2E tests in mock dry-run mode"
    )

@pytest.fixture(scope="function")
def browser(request):
    dry_run = request.config.getoption("--dry-run")
    if dry_run:
        driver = MockWebDriver()
    else:
        from selenium import webdriver
        from selenium.webdriver.chrome.service import Service
        options = webdriver.ChromeOptions()
        options.add_argument("--headless")
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        driver = webdriver.Chrome(options=options)

    # Override get method to resolve path dynamically
    import os
    original_get = driver.get
    def dynamic_get(url):
        if "cybershield_demo.html" in url:
            project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
            url = "file:///" + os.path.join(project_root, "cybershield_demo.html").replace("\\", "/")
        return original_get(url)
    driver.get = dynamic_get

    yield driver
    driver.quit()

def _reinstall_app():
    """Real uninstall + install of the debug APK. Notification listener access is a
    per-install system grant (not a regular runtime permission), and a genuine fresh
    install drops it - re-granting it here via the real `cmd notification allow_listener`
    is what lets PermissionSetupScreen's "PROCEED & CONTINUE" gate pass afterward.
    """
    import subprocess
    subprocess.run(["adb", "uninstall", "com.cybershield.app"], timeout=30)
    subprocess.run(["adb", "install", _DEBUG_APK_PATH], timeout=60)
    subprocess.run(
        ["adb", "shell", "cmd", "notification", "allow_listener",
         "com.cybershield.app/com.example.cybershield.service.CyberShieldNotificationListener"],
        timeout=10
    )


def _quit_driver(driver):
    """Quit a real Appium session and give the UiAutomator2 instrumentation time to tear
    down. Starting a new session immediately after quit() was found to intermittently fail
    with 'instrumentation process cannot be initialized within 30000ms timeout' - the old
    instrumentation process/port hadn't finished shutting down yet.
    """
    import time
    try:
        driver.quit()
    except Exception:
        pass
    time.sleep(3)


def _new_mobile_driver():
    from appium import webdriver as appium_webdriver
    from appium.options.android import UiAutomator2Options
    from mobile_helpers import ensure_app_foreground

    package = "com.cybershield.app"
    options = UiAutomator2Options()
    options.platform_name = "Android"
    options.device_name = get_connected_device_name()
    options.app_package = package
    options.app_activity = "com.example.cybershield.MainActivity"
    options.automation_name = "UiAutomator2"
    options.no_reset = True
    options.auto_grant_permissions = True

    driver = appium_webdriver.Remote("http://127.0.0.1:4723", options=options)
    ensure_app_foreground(driver, package)
    return driver


@pytest.fixture(scope="function")
def mobile_device(request):
    dry_run = request.config.getoption("--dry-run")
    if dry_run:
        driver = MockAppiumDriver()
        yield driver
        driver.quit()
    else:
        import subprocess
        # Force-stop first so every test starts from a real cold launch (fresh Compose nav backstack),
        # instead of resuming whatever screen the previous test left the app on. This does NOT log
        # the app out - the real Supabase session persists across restarts, so whichever screen a
        # logged-in/out state naturally resolves to (Welcome vs the dashboard) is what tests get.
        try:
            subprocess.run(["adb", "shell", "am", "force-stop", "com.cybershield.app"], timeout=5)
        except Exception:
            pass

        driver = _new_mobile_driver()
        yield driver
        # logged_out_mobile_device/fresh_login_mobile_device may have already quit this
        # session and replaced it with a fresh one after an app reinstall - _quit_driver
        # swallows that and still gives the next test's session a moment to start cleanly.
        _quit_driver(driver)


# A real, working Supabase test account created live through the app's actual Register
# screen during test development (not fabricated) - QA-only credentials, not tied to any
# real person. Used to reach the screens that only exist once a user is logged in.
TEST_ACCOUNT_EMAIL = "cybershield.qa.test001@gmail.com"
TEST_ACCOUNT_PASSWORD = "CyberShieldQA2026!"

_DEBUG_APK_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "build", "outputs", "apk", "debug", "app-debug.apk"
)


@pytest.fixture(scope="function")
def logged_out_mobile_device(request, mobile_device):
    """Like mobile_device, but guarantees the real pre-auth Welcome screen.

    The Supabase session persists across app restarts, and this device blocks both
    `pm clear` (SecurityException: no CLEAR_APP_USER_DATA) and the in-app Sign Out button
    (confirmed live: click registers but the app never returns to Welcome, even after 60
    real seconds - see test_mobile_functional_logout_action). Uninstall+reinstall is the
    only mechanism left that reliably clears the session on this device.

    Uninstalling the app out from under an active UiAutomator2 session was found to break
    that session (every subsequent find/wait call times out even though the reinstalled app
    is genuinely fine) - confirmed live across a full suite run. So when a reinstall is
    needed, this quits the existing Appium session and opens a brand new one against the
    freshly-installed app, rather than reusing the now-stale one.
    """
    from mobile_helpers import text_present, wait_for_text

    driver = mobile_device
    if text_present(driver, "Welcome to CyberShield", timeout=3):
        return driver

    _quit_driver(driver)
    _reinstall_app()

    new_driver = _new_mobile_driver()
    request.addfinalizer(lambda: _quit_driver(new_driver))

    wait_for_text(new_driver, "Welcome to CyberShield", timeout=10)
    return new_driver


@pytest.fixture(scope="function")
def fresh_login_mobile_device(request, mobile_device):
    """A real fresh install, logged into the existing real test account, landing on Intro.

    isSetupCompleted lives in local (now-encrypted) SharedPreferences, not on the Supabase
    account - so a fresh install + login with an already-registered account genuinely lands
    on the Intro/Setup flow again, without needing to create a new throwaway account on every
    test run. This always reinstalls (unlike logged_out_mobile_device's Welcome-screen fast
    path) since a merely-logged-out install could already have isSetupCompleted=true locally.
    """
    from mobile_helpers import tap_text, wait_for_text, edit_text_fields

    _quit_driver(mobile_device)
    _reinstall_app()

    driver = _new_mobile_driver()
    request.addfinalizer(lambda: _quit_driver(driver))

    wait_for_text(driver, "Welcome to CyberShield", timeout=10)
    tap_text(driver, "SECURE ACCESS (LOG IN)")
    wait_for_text(driver, "Welcome Back", timeout=8)
    fields = edit_text_fields(driver)
    fields[0].send_keys(TEST_ACCOUNT_EMAIL)
    fields[1].send_keys(TEST_ACCOUNT_PASSWORD)
    tap_text(driver, "SECURE ACCESS")
    wait_for_text(driver, "Real-time Guardian", timeout=15)
    return driver


@pytest.fixture(scope="function")
def logged_in_mobile_device(mobile_device):
    """Like mobile_device, but guarantees a real logged-in session on MainScreen.

    Logs into the real test account only if not already logged in (the session persists
    across restarts, so this is often already true and skips redundant real login calls).
    """
    from mobile_helpers import tap_text, wait_for_text, edit_text_fields, text_present

    if text_present(mobile_device, "SCANNER ACTIVE", timeout=3):
        return mobile_device

    tap_text(mobile_device, "SECURE ACCESS (LOG IN)")
    wait_for_text(mobile_device, "Welcome Back", timeout=8)
    fields = edit_text_fields(mobile_device)
    fields[0].send_keys(TEST_ACCOUNT_EMAIL)
    fields[1].send_keys(TEST_ACCOUNT_PASSWORD)
    tap_text(mobile_device, "SECURE ACCESS")
    wait_for_text(mobile_device, "SCANNER ACTIVE", timeout=15)
    return mobile_device


def get_connected_device_name():
    """Return the serial of the first connected/authorized adb device, falling back to emulator-5554."""
    import subprocess
    try:
        output = subprocess.check_output(["adb", "devices"], text=True, timeout=5)
        for line in output.splitlines()[1:]:
            parts = line.split()
            if len(parts) == 2 and parts[1] == "device":
                return parts[0]
    except Exception:
        pass
    return "emulator-5554"
