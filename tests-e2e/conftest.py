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
        self.current_url = "file:///c:/Users/dines/Downloads/project%2012k/cybershield_demo.html"
        self.page_source = "<html>Mock CyberShield HTML</html>"

    def get(self, url):
        pass

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
        yield driver
        driver.quit()
    else:
        from selenium import webdriver
        from selenium.webdriver.chrome.service import Service
        options = webdriver.ChromeOptions()
        options.add_argument("--headless")
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        driver = webdriver.Chrome(options=options)
        yield driver
        driver.quit()

@pytest.fixture(scope="function")
def mobile_device(request):
    dry_run = request.config.getoption("--dry-run")
    if dry_run:
        driver = MockAppiumDriver()
        yield driver
        driver.quit()
    else:
        from appium import webdriver as appium_webdriver
        desired_caps = {
            "platformName": "Android",
            "deviceName": "emulator-5554",
            "appPackage": "com.cybershield.app",
            "appActivity": "com.example.cybershield.MainActivity",
            "automationName": "UiAutomator2",
            "noReset": True
        }
        driver = appium_webdriver.Remote("http://localhost:4723/wd/hub", desired_caps)
        yield driver
        driver.quit()
