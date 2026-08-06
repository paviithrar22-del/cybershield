from selenium.webdriver.common.by import By
from automation.pages.base_page import BasePage

class DashboardPage(BasePage):
    # Locators matching cybershield_demo.html elements
    TITLE = (By.TAG_NAME, "h1")
    SCANNED_COUNT = (By.ID, "totalScanned")
    FLAGGED_COUNT = (By.ID, "totalFlagged")
    INCIDENTS_CONTAINER = (By.ID, "incidentsContainer")

    def get_title_text(self):
        return self.find_element(self.TITLE).text

    def get_scanned_count(self):
        return self.find_element(self.SCANNED_COUNT).text

    def get_flagged_count(self):
        return self.find_element(self.FLAGGED_COUNT).text
