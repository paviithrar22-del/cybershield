import sys
import os
# Ensure root directory is in Python path for package resolution
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))

import pytest
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from automation.config.config import Settings

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
        return "rgba(139, 92, 246, 1)" 

# Mock Selenium WebDriver
class MockWebDriver:
    def __init__(self):
        self.title = "CyberShield | Real-time Bullying Protection Dashboard"
        self.current_url = "https://paviithrar22-del.github.io/cybershield/"
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

def pytest_addoption(parser):
    parser.addoption(
        "--dry-run", action="store_true", default=False, help="Run E2E tests in mock dry-run mode"
    )

@pytest.fixture(scope="session")
def driver(request):
    dry_run = request.config.getoption("--dry-run")
    if dry_run:
        driver = MockWebDriver()
        yield driver
        driver.quit()
    else:
        chrome_options = Options()
        if Settings.HEADLESS:
            chrome_options.add_argument("--headless")
        chrome_options.add_argument("--no-sandbox")
        chrome_options.add_argument("--disable-dev-shm-usage")
        
        driver = webdriver.Chrome(options=chrome_options)
        driver.implicitly_wait(Settings.IMPLICIT_WAIT)
        yield driver
        driver.quit()
