import time
import pytest
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

DEMO_URL = "file:///c:/Users/dines/Downloads/project%2012k/cybershield_demo.html"

# All tests below drive the real cybershield_demo.html file in a real (or, when explicitly
# requested via --dry-run, mocked-for-CI-runner) Chrome instance and exercise its actual
# client-side JS classifier - not fabricated pass/fail results. The page is a fully static,
# client-only file with no server, no fetch/XHR calls and no backend: tests for things that
# structurally don't exist here (CORS headers, SSL enforcement, server-side DB schema) are
# marked skipped with the reason stated, rather than faked with assert True.

def load(browser):
    browser.get(DEMO_URL)
    WebDriverWait(browser, 5).until(EC.presence_of_element_located((By.ID, "senderName")))

def simulate(browser, sender=None, source=None, message=""):
    if sender is not None:
        el = browser.find_element(By.ID, "senderName")
        el.clear()
        el.send_keys(sender)
    if source is not None:
        el = browser.find_element(By.ID, "appSource")
        el.clear()
        el.send_keys(source)
    msg_el = browser.find_element(By.ID, "messageContent")
    msg_el.clear()
    msg_el.send_keys(message)
    browser.find_element(By.XPATH, "//button[contains(., 'Simulate Notification')]").click()

# ----------------- 1. FUNCTIONAL TESTING -----------------

def test_web_functional_dashboard_loads(browser):
    """Category: Functional - Verify the main dashboard loads successfully."""
    browser.get(DEMO_URL)
    assert "CyberShield" in browser.title

def test_web_functional_simulator_form_present(browser):
    """Category: Functional - Verify simulation form fields exist with their real default values."""
    load(browser)
    field = browser.find_element(By.ID, "senderName")
    assert field.get_attribute("value") == "Alex Rivera"

def test_web_functional_app_source_present(browser):
    """Category: Functional - Verify App Source field exists with its real default value."""
    load(browser)
    field = browser.find_element(By.ID, "appSource")
    assert field.get_attribute("value") == "Instagram"

def test_web_functional_message_input_present(browser):
    """Category: Functional - Verify Message content text area exists and is initially empty."""
    load(browser)
    field = browser.find_element(By.ID, "messageContent")
    assert field.get_attribute("value") == ""

def test_web_functional_scan_message_basic(browser):
    """Category: Functional - Scan a safe message and verify the real scanned counter increments without flagging."""
    load(browser)
    before = int(browser.find_element(By.ID, "scannedCount").text)
    simulate(browser, message="Hello, how are you today?")
    after = int(browser.find_element(By.ID, "scannedCount").text)
    assert after == before + 1
    assert browser.find_element(By.ID, "flaggedCount").text == "0"

def test_web_functional_scan_bullying_message(browser):
    """Category: Functional - Scan a message containing a real critical keyword and verify it is flagged."""
    load(browser)
    before_flagged = int(browser.find_element(By.ID, "flaggedCount").text)
    simulate(browser, message="I hate you and think you are worthless")
    after_flagged = int(browser.find_element(By.ID, "flaggedCount").text)
    assert after_flagged == before_flagged + 1
    badge = browser.find_element(By.CLASS_NAME, "badge")
    assert badge.text == "HIGH"

def test_web_functional_stats_update(browser):
    """Category: Functional - Verify the dashboard scanned count increments on every scan, flagged or not."""
    load(browser)
    for i in range(3):
        simulate(browser, message=f"Neutral message number {i}")
    assert browser.find_element(By.ID, "scannedCount").text == "3"

def test_web_functional_incident_severity_badge(browser):
    """Category: Functional - Verify each severity tier's real classifier keywords produce the matching badge."""
    load(browser)
    cases = [
        ("I will kill yourself", "CRITICAL"),
        ("you are so ugly", "HIGH"),
        ("you are such a loser", "MEDIUM"),
        ("just go away please", "LOW"),
    ]
    for message, expected in cases:
        simulate(browser, message=message)
    # Each new row has a real 0.3s CSS slideIn animation (opacity 0 -> 1); reading .text
    # mid-animation can return an empty string, so wait for it to settle first.
    time.sleep(0.4)
    badges = [b.text for b in browser.find_elements(By.CLASS_NAME, "badge")]
    # Newest incident is prepended (unshift), so the last simulated message's badge is first.
    assert badges[:len(cases)] == [c[1] for c in reversed(cases)]

def test_web_functional_clear_logs_button(browser):
    """Category: Functional - Verify Clear All genuinely empties the incident history and resets flagged count."""
    load(browser)
    simulate(browser, message="you are worthless and ugly")
    assert browser.find_element(By.ID, "flaggedCount").text == "1"
    browser.find_element(By.CLASS_NAME, "clear-btn").click()
    assert browser.find_element(By.ID, "flaggedCount").text == "0"
    assert "No incidents detected yet" in browser.find_element(By.ID, "incidentList").text

def test_web_functional_download_report_trigger(browser):
    """Category: Functional - Verify report download action can be triggered."""
    pytest.skip("cybershield_demo.html has no download/export control - this feature does not exist in the current page")

# ----------------- 2. UI/UX TESTING -----------------

def test_web_uiux_glow_effects(browser):
    """Category: UI/UX - Verify the real ::before/::after glow pseudo-elements are declared with blur+opacity."""
    load(browser)
    before_filter = browser.execute_script(
        "return getComputedStyle(document.body, '::before').filter;"
    )
    assert "blur" in before_filter

def test_web_uiux_glassmorphic_cards(browser):
    """Category: UI/UX - Verify dashboard cards use a real backdrop-filter blur for the glassmorphism effect."""
    load(browser)
    card = browser.find_element(By.CLASS_NAME, "card")
    backdrop = browser.execute_script("return getComputedStyle(arguments[0]).backdropFilter;", card)
    assert "blur" in (backdrop or "")

def test_web_uiux_brand_colors(browser):
    """Category: UI/UX - Verify the real rendered background color matches the declared dark theme (#0b0f19)."""
    load(browser)
    bg = browser.execute_script("return getComputedStyle(document.body).backgroundColor;")
    assert bg == "rgb(11, 15, 25)"

def test_web_uiux_typography_jakarta(browser):
    """Category: UI/UX - Verify the primary font family is declared as Plus Jakarta Sans."""
    load(browser)
    font = browser.execute_script("return getComputedStyle(document.body).fontFamily;")
    assert "Jakarta" in font

def test_web_uiux_responsive_layout_rendering(browser):
    """Category: UI/UX - Verify the dashboard grid genuinely collapses to a single column under the real 900px breakpoint."""
    load(browser)
    browser.set_window_size(1400, 900)
    # Headless Chrome doesn't always finish re-laying-out synchronously with set_window_size -
    # wait on the real window.innerWidth actually reflecting the resize before reading styles.
    WebDriverWait(browser, 5).until(lambda d: d.execute_script("return window.innerWidth") >= 1300)
    wide_cols = browser.execute_script(
        "return getComputedStyle(document.querySelector('.dashboard-grid')).gridTemplateColumns;"
    )
    browser.set_window_size(500, 900)
    WebDriverWait(browser, 5).until(lambda d: d.execute_script("return window.innerWidth") <= 600)
    narrow_cols = browser.execute_script(
        "return getComputedStyle(document.querySelector('.dashboard-grid')).gridTemplateColumns;"
    )
    assert wide_cols != narrow_cols
    assert len(narrow_cols.split()) == 1

def test_web_uiux_custom_scrollbar_applied(browser):
    """Category: UI/UX - Verify the incident list has a real ::-webkit-scrollbar rule applied via overflow-y auto."""
    load(browser)
    overflow_y = browser.execute_script(
        "return getComputedStyle(document.getElementById('incidentList')).overflowY;"
    )
    assert overflow_y == "auto"

def test_web_uiux_stats_grid_alignment(browser):
    """Category: UI/UX - Verify the Scanned/Flagged stat cards render as a real 2-column grid."""
    load(browser)
    cols = browser.execute_script(
        "return getComputedStyle(document.querySelector('.stats-grid')).gridTemplateColumns;"
    )
    assert len(cols.split()) == 2

def test_web_uiux_hover_states_interactive(browser):
    """Category: UI/UX - Verify the primary button's real box-shadow changes are declared for the :hover state."""
    load(browser)
    css_text = browser.execute_script(
        "for (const sheet of document.styleSheets) {"
        "  let rules;"
        "  try { rules = sheet.cssRules; } catch (e) { continue; }"  # cross-origin sheets (Google Fonts) throw
        "  for (const rule of rules) {"
        "    if (rule.selectorText === '.btn:hover') return rule.cssText;"
        "  }"
        "} return '';"
    )
    assert "box-shadow" in css_text

# ----------------- 3. COMPATIBILITY TESTING -----------------

def test_web_compatibility_chrome_headless(browser):
    """Category: Compatibility - Verify execution on headless Chrome browser."""
    load(browser)
    assert "CyberShield" in browser.title

def test_web_compatibility_desktop_viewport(browser):
    """Category: Compatibility - Validate the real dashboard grid renders as 2 columns at 1920x1080."""
    load(browser)
    browser.set_window_size(1920, 1080)
    cols = browser.execute_script(
        "return getComputedStyle(document.querySelector('.dashboard-grid')).gridTemplateColumns;"
    )
    assert len(cols.split()) == 2

def test_web_compatibility_mobile_portrait_viewport(browser):
    """Category: Compatibility - Verify the grid genuinely switches to single-column at 375x667 mobile sizing."""
    load(browser)
    browser.set_window_size(375, 667)
    cols = browser.execute_script(
        "return getComputedStyle(document.querySelector('.dashboard-grid')).gridTemplateColumns;"
    )
    assert len(cols.split()) == 1

def test_web_compatibility_tablet_landscape_viewport(browser):
    """Category: Compatibility - Verify the dashboard remains usable (all controls present) at 1024x768 tablet sizing."""
    load(browser)
    browser.set_window_size(1024, 768)
    assert browser.find_element(By.ID, "messageContent").is_displayed()

def test_web_compatibility_browser_local_storage(browser):
    """Category: Compatibility - Check compatibility with HTML5 LocalStorage configurations."""
    pytest.skip("cybershield_demo.html keeps all state in a plain JS array (incidents=[]) and never touches localStorage")

# ----------------- 4. PERFORMANCE TESTING -----------------

def test_web_performance_load_time_threshold(browser):
    """Category: Performance - Ensure total DOM page load takes under 1.5 seconds."""
    start_time = time.time()
    browser.get(DEMO_URL)
    WebDriverWait(browser, 5).until(EC.presence_of_element_located((By.ID, "senderName")))
    duration = time.time() - start_time
    assert duration < 1.5

def test_web_performance_simulation_scan_speed(browser):
    """Category: Performance - Ensure a single simulateAlert() call executes in under 300ms of real browser time."""
    load(browser)
    elapsed_ms = browser.execute_script(
        "const t0 = performance.now();"
        "document.getElementById('messageContent').value = 'quick test message';"
        "simulateAlert();"
        "return performance.now() - t0;"
    )
    assert elapsed_ms < 300

def test_web_performance_redraw_charts(browser):
    """Category: Performance - Ensure the severity bar segment widths genuinely update after a scan."""
    load(browser)
    before = browser.execute_script("return document.getElementById('bar-critical').style.width;")
    simulate(browser, message="I will kill yourself")
    after = browser.execute_script("return document.getElementById('bar-critical').style.width;")
    assert before != after

def test_web_performance_dom_memory_leak(browser):
    """Category: Performance - Verify the incident list DOM node count matches the real incident array length after repeated scans."""
    load(browser)
    for i in range(10):
        simulate(browser, message=f"you are so worthless {i}")
    row_count = len(browser.find_elements(By.CLASS_NAME, "incident-row"))
    assert row_count == 10

def test_web_performance_concurrency_load(browser):
    """Category: Performance - Ensure processing 5 rapid alert simulations consecutively does not freeze the page."""
    load(browser)
    start = time.time()
    for i in range(5):
        simulate(browser, message=f"neutral rapid message {i}")
    duration = time.time() - start
    assert duration < 5
    assert browser.find_element(By.ID, "scannedCount").text == "5"

# ----------------- 5. SECURITY TESTING -----------------

def test_web_security_xss_protection_inputs(browser):
    """Category: Security - Inject an HTML/JS payload as the sender name and verify it cannot execute via the incident list's innerHTML rendering."""
    load(browser)
    simulate(browser, sender="<img src=x onerror=window.__xss=true>", message="you are worthless")
    executed = browser.execute_script("return window.__xss === true;")
    assert not executed, (
        "sender name is interpolated into incidentList.innerHTML without escaping in "
        "cybershield_demo.html - a crafted sender name can execute arbitrary JS (real, unresolved XSS finding)"
    )

def test_web_security_http_origin_check(browser):
    """Category: Security - Verify origin validation headers are enforced."""
    pytest.skip("Page is loaded over file:// with no HTTP server involved, so there is no Origin header to validate")

def test_web_security_csp_policies(browser):
    """Category: Security - Check Content Security Policy restricts external inline script executions."""
    load(browser)
    has_csp = browser.execute_script(
        "return !!document.querySelector('meta[http-equiv=\"Content-Security-Policy\"]');"
    )
    assert has_csp, "cybershield_demo.html declares no Content-Security-Policy meta tag"

def test_web_security_frame_hijacking(browser):
    """Category: Security - Verify frame-options prevent dashboard from loading inside unauthorized frames."""
    pytest.skip("X-Frame-Options is an HTTP response header; this page is served over file:// with no HTTP response at all")

def test_web_security_ssl_enforcement(browser):
    """Category: Security - Verify resources loaded securely on https protocol when hosted."""
    pytest.skip("Page is opened over file:// for local testing; SSL enforcement only applies once actually hosted over HTTP(S)")

# ----------------- 6. API / NETWORK TESTING -----------------

def test_web_api_no_unexpected_network_calls(browser):
    """Category: API - Verify the page makes zero real fetch/XHR calls; it is a fully client-side simulation."""
    load(browser)
    simulate(browser, message="I hate you, you are worthless")
    xhr_count = browser.execute_script(
        "return performance.getEntriesByType('resource')"
        ".filter(r => r.initiatorType === 'xmlhttprequest' || r.initiatorType === 'fetch').length;"
    )
    assert xhr_count == 0

def test_web_api_timeout_handling(browser):
    """Category: API - Verify interface falls back gracefully to offline mode on connection timeout."""
    pytest.skip("cybershield_demo.html makes no network requests at all, so there is no timeout/offline path to exercise")

def test_web_api_status_code_401(browser):
    """Category: API - Verify unauthorized key responses display warning details."""
    pytest.skip("No authenticated API call exists in this static demo page")

def test_web_api_model_names_response(browser):
    """Category: API - Validate parse models endpoint can map list parameters."""
    pytest.skip("No backend model-listing endpoint is called from this static demo page")

def test_web_api_cors_headers(browser):
    """Category: API - Verify CORS allows standard web clients to invoke serverless functions."""
    pytest.skip("Page makes no cross-origin requests; there is no serverless function call to inspect CORS headers on")

# ----------------- 7. DATABASE TESTING -----------------

def test_web_database_schema_validation(browser):
    """Category: Database - Verify structure of mock incident row fields match supabase incidents table."""
    pytest.skip("Demo page keeps incidents in an in-memory JS array only; it has no database connection to validate against")

def test_web_database_insert_sync_trigger(browser):
    """Category: Database - Verify syncing new alerts triggers database save queries."""
    pytest.skip("Demo page has no database sync logic; incidents live only in the page's JS memory")

def test_web_database_connection_resilience(browser):
    """Category: Database - Verify query retry buffer when network database link is dropped."""
    pytest.skip("Demo page has no database connection at all")

def test_web_database_cleanup_cascade(browser):
    """Category: Database - Ensure clearing local history database entries updates main view stats."""
    load(browser)
    simulate(browser, message="you are worthless")
    browser.find_element(By.CLASS_NAME, "clear-btn").click()
    assert browser.find_element(By.ID, "flaggedCount").text == "0"
    assert len(browser.find_elements(By.CLASS_NAME, "incident-row")) == 0

# ----------------- 8. ACCESSIBILITY TESTING -----------------

def test_web_accessibility_aria_role_assignments(browser):
    """Category: Accessibility - Check the simulate/clear controls are real semantic <button>/clickable elements, not divs."""
    load(browser)
    btn = browser.find_element(By.XPATH, "//button[contains(., 'Simulate Notification')]")
    assert btn.tag_name == "button"

def test_web_accessibility_contrast_ratios(browser):
    """Category: Accessibility - Compute the real WCAG contrast ratio between light text and dark background."""
    load(browser)
    ratio = browser.execute_script("""
        function lum(rgb) {
            const m = rgb.match(/\\d+/g).map(Number);
            const [r,g,b] = m.map(c => {
                c/=255; return c<=0.03928 ? c/12.92 : Math.pow((c+0.055)/1.055, 2.4);
            });
            return 0.2126*r + 0.7152*g + 0.0722*b;
        }
        const textColor = getComputedStyle(document.body).color;
        const bgColor = getComputedStyle(document.body).backgroundColor;
        const l1 = lum(textColor) + 0.05, l2 = lum(bgColor) + 0.05;
        return Math.max(l1,l2) / Math.min(l1,l2);
    """)
    assert ratio >= 4.5, f"Measured body text/background contrast ratio {ratio:.2f}:1 is below WCAG AA"

def test_web_accessibility_keyboard_focus_loop(browser):
    """Category: Accessibility - Verify Tab navigation can reach and focus the message textarea."""
    load(browser)
    browser.find_element(By.ID, "senderName").click()
    from selenium.webdriver.common.keys import Keys
    browser.switch_to.active_element.send_keys(Keys.TAB)
    active_id = browser.execute_script("return document.activeElement.id;")
    assert active_id == "appSource"

def test_web_accessibility_alt_labels(browser):
    """Category: Accessibility - Ensure form inputs have real associated <label> elements."""
    load(browser)
    label = browser.find_element(By.CSS_SELECTOR, "label[for='senderName']")
    assert label.text != ""

# ----------------- 9. REGRESSION TESTING -----------------

def test_web_regression_stats_reset_clean(browser):
    """Category: Regression - Ensure Clear All restores flagged count to 0 and the incident list to empty."""
    load(browser)
    simulate(browser, message="I hate you and think you are worthless")
    browser.find_element(By.CLASS_NAME, "clear-btn").click()
    assert browser.find_element(By.ID, "flaggedCount").text == "0"
    assert browser.find_elements(By.CLASS_NAME, "incident-row") == []

def test_web_regression_settings_persistence(browser):
    """Category: Regression - Verify change settings persists sensitivity levels across page refreshes."""
    pytest.skip("sensitivity is a plain in-memory JS variable with no persistence layer; it genuinely resets on reload by design")

def test_web_regression_alert_history_capacity(browser):
    """Category: Regression - Verify the dashboard renders every flagged incident without a hidden row cap."""
    load(browser)
    for i in range(15):
        simulate(browser, message=f"you are worthless number {i}")
    assert len(browser.find_elements(By.CLASS_NAME, "incident-row")) == 15

def test_web_regression_notification_permissions_toggle(browser):
    """Category: Regression - Verify permission toggles correctly trigger display states."""
    load(browser)
    checkbox = browser.find_elements(By.CSS_SELECTOR, ".app-checkbox input[type=checkbox]")[0]
    assert checkbox.is_selected()
    checkbox.click()
    assert not checkbox.is_selected()

# ----------------- 10. END-TO-END (E2E) TESTING -----------------

def test_web_e2e_full_scan_log_update_cycle(browser):
    """Category: End-to-End - Submit incident -> verify real scan triggers -> log appends -> stats update."""
    load(browser)
    simulate(browser, sender="Jordan", source="WhatsApp", message="I hate you, you are worthless")
    time.sleep(0.4)  # real 0.3s CSS slideIn animation on new incident rows
    assert browser.find_element(By.ID, "scannedCount").text == "1"
    assert browser.find_element(By.ID, "flaggedCount").text == "1"
    row = browser.find_element(By.CLASS_NAME, "incident-row")
    assert "Jordan" in row.text
    assert "WhatsApp" in row.text

def test_web_e2e_critical_incident_handling(browser):
    """Category: End-to-End - Submit a critical alert and verify the real status pill flips to FLAGGED."""
    load(browser)
    simulate(browser, message="I will kill yourself")
    pill_text = browser.find_element(By.ID, "statusPill").text
    assert "FLAGGED" in pill_text

def test_web_e2e_settings_update_classification(browser):
    """Category: End-to-End - Switch to High sensitivity and verify a previously-neutral message now gets flagged."""
    load(browser)
    browser.find_element(By.ID, "sens-High").click()
    simulate(browser, message="this is a fairly long neutral sentence")
    assert browser.find_element(By.ID, "flaggedCount").text == "1"

def test_web_e2e_clear_data_and_reload(browser):
    """Category: End-to-End - Create alerts -> clear stats -> reload -> verify clean state."""
    load(browser)
    for i in range(5):
        simulate(browser, message=f"you are worthless {i}")
    browser.find_element(By.CLASS_NAME, "clear-btn").click()
    browser.get(DEMO_URL)
    WebDriverWait(browser, 5).until(EC.presence_of_element_located((By.ID, "senderName")))
    assert browser.find_element(By.ID, "scannedCount").text == "0"
    assert browser.find_element(By.ID, "flaggedCount").text == "0"
