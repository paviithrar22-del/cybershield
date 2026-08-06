import os

class Settings:
    BASE_URL = os.environ.get("BASE_URL", "https://paviithrar22-del.github.io/cybershield/")
    IMPLICIT_WAIT = 10
    HEADLESS = True
    
    # App credentials and repository link for E2E tests
    EMAIL = os.environ.get("CYBERSHIELD_EMAIL", "testuser@cybershield.com")
    PASSWORD = os.environ.get("CYBERSHIELD_PASSWORD", "SecurePassword123!")
    GITHUB_REPO = "https://github.com/paviithrar22-del/cybershield.git"
