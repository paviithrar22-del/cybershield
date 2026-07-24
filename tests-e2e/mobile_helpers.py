"""Real helper utilities for driving the actual CyberShield Android app via Appium/UiAutomator2."""
import re
import subprocess
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait

_BOUNDS_RE = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")


def wait_for_text(driver, text, timeout=10, exact=True):
    """Poll the live accessibility tree until an element with this text appears. Real device I/O, no stubbing."""
    xpath = f"//*[@text='{text}']" if exact else f"//*[contains(@text,'{text}')]"

    def _present(d):
        elems = d.find_elements(By.XPATH, xpath)
        return elems[0] if elems else False

    return WebDriverWait(driver, timeout, poll_frequency=0.3).until(_present)


def text_present(driver, text, timeout=5, exact=True):
    try:
        wait_for_text(driver, text, timeout=timeout, exact=exact)
        return True
    except Exception:
        return False


def tap_text(driver, text, timeout=10):
    el = wait_for_text(driver, text, timeout=timeout)
    el.click()
    return el


def edit_text_fields(driver):
    """Return the real EditText nodes currently on screen, in visual order."""
    return driver.find_elements(By.XPATH, "//android.widget.EditText")


def screenshot_image(driver):
    """Decode the device's real current frame buffer into a Pillow image."""
    import io
    from PIL import Image
    png_bytes = driver.get_screenshot_as_png()
    return Image.open(io.BytesIO(png_bytes)).convert("RGB")


def sample_pixel(driver, x_ratio, y_ratio):
    """Sample one real rendered pixel's RGB at a fractional (x_ratio, y_ratio) position of the screen."""
    img = screenshot_image(driver)
    x = min(int(img.width * x_ratio), img.width - 1)
    y = min(int(img.height * y_ratio), img.height - 1)
    return img.getpixel((x, y))


def brightest_pixel_in_bounds(driver, bounds_px):
    """Scan every pixel inside a real element's bounding box and return the brightest one.

    A single fixed sample point can land between glyph strokes (background) rather than on
    the rendered character, which is what silently made an earlier version of this check
    always compare background-to-background. Scanning the whole box and taking the brightest
    pixel reliably lands on light text rendered over a dark background.
    """
    x1, y1, x2, y2 = bounds_px
    img = screenshot_image(driver)
    x2, y2 = min(x2, img.width), min(y2, img.height)
    region = img.crop((x1, y1, x2, y2))
    best = max(region.getdata(), key=relative_luminance)
    return best


def relative_luminance(rgb):
    """WCAG relative luminance for a real sampled sRGB pixel."""
    def channel(c):
        c = c / 255.0
        return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4

    r, g, b = rgb
    return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)


def contrast_ratio(rgb_a, rgb_b):
    """WCAG contrast ratio between two real sampled pixels."""
    l1 = relative_luminance(rgb_a) + 0.05
    l2 = relative_luminance(rgb_b) + 0.05
    return max(l1, l2) / min(l1, l2)


def device_density_scale():
    """Real device density (dpi / 160) read live via adb, used to convert pixel bounds to dp."""
    out = subprocess.check_output(["adb", "shell", "wm", "density"], text=True)
    dpi = int(re.search(r"(\d+)", out).group(1))
    return dpi / 160.0


def _bounds_px_from_source(driver, text, exact=True):
    """Extract an element's pixel bounds straight out of a single page_source snapshot.

    Real Android Toasts vanish in ~2s, which makes a separate find-then-get_attribute
    round trip flaky (StaleElementReferenceException). Parsing one already-fetched
    page_source string sidesteps that race entirely.
    """
    source = driver.page_source
    if exact:
        node_re = re.compile(r'text="' + re.escape(text) + r'"[^>]*bounds="(\[\d+,\d+\]\[\d+,\d+\])"')
    else:
        node_re = re.compile(r'text="[^"]*' + re.escape(text) + r'[^"]*"[^>]*bounds="(\[\d+,\d+\]\[\d+,\d+\])"')
    match = node_re.search(source)
    if not match:
        return None
    x1, y1, x2, y2 = (int(v) for v in _BOUNDS_RE.match(match.group(1)).groups())
    return x1, y1, x2, y2


def wait_for_toast_text(driver, expected_text, timeout=10):
    """Poll page_source for a real Android Toast node with this exact text.

    Toast nodes don't carry a 'bounds' attribute and are extremely short-lived, so this reads
    everything (displayed + exact text) out of one already-fetched page_source string rather
    than caching a WebElement handle that can go stale between find and get_attribute calls.
    """
    node_re = re.compile(
        r'class="android\.widget\.Toast"[^>]*text="([^"]*)"[^>]*displayed="(true|false)"'
    )

    def _present(d):
        for match in node_re.finditer(d.page_source):
            if match.group(1) == expected_text:
                return match.group(2) == "true"
        return False

    return WebDriverWait(driver, timeout, poll_frequency=0.2).until(_present)


def wait_for_text_bounds(driver, text, timeout=10, exact=True):
    """Poll page_source (not cached elements) until the text appears, returning its real pixel bounds atomically."""
    def _present(d):
        bounds = _bounds_px_from_source(d, text, exact=exact)
        return bounds if bounds else False

    return WebDriverWait(driver, timeout, poll_frequency=0.2).until(_present)


def element_bounds_dp(driver, element):
    """Convert an element's real pixel bounds (from get_attribute('bounds')) into dp using the device's real density."""
    bounds = element.get_attribute("bounds")
    x1, y1, x2, y2 = (int(v) for v in _BOUNDS_RE.match(bounds).groups())
    scale = device_density_scale()
    return (x2 - x1) / scale, (y2 - y1) / scale


def clickable_ancestor_bounds_dp(driver, text, timeout=10):
    """Find the real clickable container that wraps a text label (Compose renders label + clickable
    box as separate nodes) and return ITS bounds in dp - that's the actual real touch target, not
    the tighter text glyph bounds.
    """
    wait_for_text(driver, text, timeout=timeout)
    xpath = f"//*[@text='{text}']/ancestor::*[@clickable='true'][1]"
    ancestors = driver.find_elements(By.XPATH, xpath)
    target = ancestors[0] if ancestors else driver.find_element(By.XPATH, f"//*[@text='{text}']")
    return element_bounds_dp(driver, target)


def ensure_logged_out(driver, timeout=15):
    """Ensure the app is showing the pre-auth Welcome screen.

    The real Supabase session persists across app restarts (confirmed live: after logging
    in once, a cold relaunch goes straight to the dashboard, not Welcome). This device also
    blocks `adb shell pm clear` (SecurityException: no CLEAR_APP_USER_DATA), so the only real
    way back to a logged-out state is to drive the app's actual Settings -> Sign Out Account
    flow, the same as a real user would.
    """
    import time

    def _screen(d):
        if text_present(d, "Welcome to CyberShield", timeout=1):
            return "welcome"
        if text_present(d, "Settings", timeout=1, exact=True):
            return "logged_in"
        return None

    state = None
    for _ in range(20):
        state = _screen(driver)
        if state:
            break
        time.sleep(0.5)

    if state == "welcome":
        return

    # Logged in: navigate to the Settings tab (a LazyColumn) and sign out for real -
    # "Sign Out Account" is the last item and needs a scroll to come into view first.
    tap_text(driver, "Settings", timeout=timeout)
    size = driver.get_window_size()
    for _ in range(6):
        if text_present(driver, "Sign Out Account", timeout=1):
            break
        driver.swipe(size["width"] // 2, int(size["height"] * 0.8), size["width"] // 2, int(size["height"] * 0.3), 300)
        time.sleep(0.3)
    tap_text(driver, "Sign Out Account", timeout=timeout)
    wait_for_text(driver, "Welcome to CyberShield", timeout=timeout)


def ensure_app_foreground(driver, package="com.cybershield.app", timeout=10):
    """Explicitly bring the real app to foreground; app_activity autolaunch can race with a fresh session."""
    driver.activate_app(package)

    def _in_foreground(d):
        try:
            return d.current_package == package
        except Exception:
            return False

    WebDriverWait(driver, timeout, poll_frequency=0.3).until(_in_foreground)
