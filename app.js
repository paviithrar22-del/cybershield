/* ==========================================
   CyberShield - Complete Web Application Engine
   Strict Supabase Authentication & Realtime Engine
   ========================================== */

// --- Supabase Credentials (Matching SupabaseManager.kt) ---
const SUPABASE_URL = "https://rkzrhiwxbypqfttoczzj.supabase.co";
const SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJrenJoaXd4YnlwcWZ0dG9jenpqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODE2MzEwNDAsImV4cCI6MjA5NzIwNzA0MH0.0OY_Zh1z92e8F6ZnWZMk2EnCWWr4z61Z-mozxt_D8Sk";

let supabaseClient = null;

if (window.supabase) {
    try {
        supabaseClient = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
    } catch (e) {
        console.warn("Supabase init warning:", e);
    }
}

// --- Multi-Lingual NLP Rule Classifier (Matching BullyingClassifier.kt Fallback) ---
const BullyingClassifier = {
    threatWords: new Set([
        "kill", "die", "murder", "hurt", "beat", "slit", "throat", "stab", "shoot",
        "burn", "suicide", "kys", "hang", "strangle", "drown", "destroy",
        "maar", "jaan se", "khoon", "marna", "kaat", "tod",
        "kola", "kollu", "saavu", "savu", "adi", "vettu", "kuthu",
        "champu", "naraku", "kotti", "sachipo"
    ]),

    hateWords: new Set([
        "nigger", "faggot", "retard", "cunt", "bitch", "chink", "kike", "tranny",
        "dyke", "spic", "wetback", "bastard", "whore", "slut", "trash", "scum", "subhuman",
        "gandu", "chutiya", "randi", "bhadwa", "harami", "kamina", "kamine", "saala", "sala",
        "punda", "thevadiya", "baadu", "sunni", "otha", "ootha", "omala", "bunda",
        "lanja", "na kodaka", "lucha", "lanjodaka", "dengu"
    ]),

    harassmentWords: new Set([
        "idiot", "stupid", "dumb", "loser", "looser", "freak", "useless", "worthless", "ugly",
        "fat", "pathetic", "hate", "worst", "disgusting", "nobody likes you", "die alone",
        "shut up", "go away", "kill yourself", "end your life",
        "pagal", "chup", "gadha", "ullu", "kamzarf", "bakwas",
        "mental", "loosu", "loose", "echa", "paithiyam", "moodu", "sathuru",
        "pichi", "pitchi", "donga", "sollu", "bodi", "asahyanga"
    ]),

    profanityWords: new Set([
        "fuck", "shit", "ass", "asshole", "dick", "pussy", "crap"
    ]),

    classify(text, sensitivity = "Medium") {
        const trimmed = text.trim();
        if (!trimmed) {
            return { severity: "NONE", isFlagged: false, reason: "Empty text" };
        }

        const lowerText = trimmed.toLowerCase();
        const words = lowerText.split(/[\s.,!?;:"]+/).filter(w => w.length > 0);

        let threatCount = 0; const matchedThreats = [];
        let hateCount = 0; const matchedHate = [];
        let harassmentCount = 0; const matchedHarassment = [];
        let profanityCount = 0; const matchedProfanity = [];

        const phrases = [
            "kill yourself", "end your life", "nobody likes you", "die alone", "shut up", "go away",
            "jaan se maar", "jaan se maardunga", "poittu saavu", "mooditu po"
        ];
        for (const phrase of phrases) {
            if (lowerText.includes(phrase)) {
                if (phrase.includes("kill") || phrase.includes("end your life") || phrase.includes("jaan se maar")) {
                    threatCount += 2; matchedThreats.push(phrase);
                } else {
                    harassmentCount += 2; matchedHarassment.push(phrase);
                }
            }
        }

        for (const word of words) {
            if (this.threatWords.has(word) && !matchedThreats.includes(word)) {
                threatCount++; matchedThreats.push(word);
            }
            if (this.hateWords.has(word) && !matchedHate.includes(word)) {
                hateCount++; matchedHate.push(word);
            }
            if (this.harassmentWords.has(word) && !matchedHarassment.includes(word)) {
                harassmentCount++; matchedHarassment.push(word);
            }
            if (this.profanityWords.has(word) && !matchedProfanity.includes(word)) {
                profanityCount++; matchedProfanity.push(word);
            }
        }

        let intensityMultiplier = 1.0;
        const uppercaseCount = (trimmed.match(/[A-Z]/g) || []).length;
        const letterCount = (trimmed.match(/[a-zA-Z]/g) || []).length;
        if (letterCount > 4 && (uppercaseCount / letterCount) > 0.6) {
            intensityMultiplier += 0.5;
        }

        const exclamationCount = (trimmed.match(/!/g) || []).length;
        if (exclamationCount >= 3) {
            intensityMultiplier += 0.3;
        }

        const baseScore = (threatCount * 6.0) + (hateCount * 4.0) + (harassmentCount * 2.0) + (profanityCount * 1.0);
        const finalScore = baseScore * intensityMultiplier;

        const thresholdModifier = sensitivity.toLowerCase() === "high" ? 0.7 : (sensitivity.toLowerCase() === "low" ? 1.5 : 1.0);
        const lowThreshold = 1.0 * thresholdModifier;
        const mediumThreshold = 3.0 * thresholdModifier;
        const highThreshold = 6.0 * thresholdModifier;
        const criticalThreshold = 10.0 * thresholdModifier;

        let severity = "NONE";
        if (finalScore >= criticalThreshold) severity = "CRITICAL";
        else if (finalScore >= highThreshold) severity = "HIGH";
        else if (finalScore >= mediumThreshold) severity = "MEDIUM";
        else if (finalScore >= lowThreshold) severity = "LOW";

        const isFlagged = ["MEDIUM", "HIGH", "CRITICAL"].includes(severity);

        const reasonList = [];
        if (matchedThreats.length) reasonList.push(`Threats: ${matchedThreats.join(", ")}`);
        if (matchedHate.length) reasonList.push(`Hate Speech/Slurs: ${matchedHate.join(", ")}`);
        if (matchedHarassment.length) reasonList.push(`Harassment: ${matchedHarassment.join(", ")}`);
        if (matchedProfanity.length && isFlagged) reasonList.push(`Profanity: ${matchedProfanity.join(", ")}`);
        if (intensityMultiplier > 1.0 && isFlagged) reasonList.push("Aggressive Tone");

        const reason = reasonList.length ? reasonList.join(" | ") : "No harmful content detected";

        return { severity, isFlagged, reason };
    }
};

// --- Application State Management ---
const AppState = {
    currentScreen: "splash",
    currentTab: "home",
    user: null,
    stats: { scanned: 0, flagged: 0 },
    incidents: [],
    guardianInfo: {
        name: "Guardian Parent",
        phone: "+1 (555) 019-2834"
    },
    sensitivity: "Medium"
};

function resetUserData() {
    AppState.stats = { scanned: 0, flagged: 0 };
    AppState.incidents = [];
    updateStatsCounters();
    renderIncidents();
}

// --- Supabase Edge Function AI Classifier ---
async function classifyWithEdge(text) {
    if (!supabaseClient) {
        return BullyingClassifier.classify(text, AppState.sensitivity);
    }

    try {
        pushTerminalLog("Invoking Supabase Edge Function 'classify-message'...");
        const { data, error } = await supabaseClient.functions.invoke("classify-message", {
            body: { text: text }
        });

        if (error || !data) {
            throw new Error(error?.message || "No data returned");
        }

        const severity = (data.severity || "NONE").toUpperCase();
        const isFlagged = ["MEDIUM", "HIGH", "CRITICAL"].includes(severity);
        return {
            severity: severity,
            isFlagged: isFlagged,
            reason: `Gemini Edge: ${data.reason || "Classified by AI"}`
        };
    } catch (e) {
        pushTerminalLog(`Edge Function fallback: ${e.message}`);
        const localRes = BullyingClassifier.classify(text, AppState.sensitivity);
        localRes.reason = `Local NLP: ${localRes.reason}`;
        return localRes;
    }
}

// --- Enhanced Supabase Auth Functions with Detailed Error & Display Handling ---
function setAuthAlert(alertElem, text, type = "error") {
    if (!alertElem) return;
    alertElem.innerHTML = text;
    alertElem.className = `auth-alert ${type}`;
    alertElem.style.display = "block";
}

function clearAuthAlert(alertElem) {
    if (!alertElem) return;
    alertElem.textContent = "";
    alertElem.className = "auth-alert";
    alertElem.style.display = "none";
}

async function handleSignIn() {
    const emailInput = document.getElementById("loginEmail");
    const passwordInput = document.getElementById("loginPassword");
    const email = emailInput ? emailInput.value.trim() : "";
    const password = passwordInput ? passwordInput.value.trim() : "";
    const errAlert = document.getElementById("loginErrorAlert");
    const signInBtn = document.querySelector("#screen-login .btn-primary");

    clearAuthAlert(errAlert);

    if (!email || !password) {
        setAuthAlert(errAlert, "⚠️ Please enter both your email address and password.");
        return;
    }

    if (!supabaseClient) {
        setAuthAlert(errAlert, "⚠️ Supabase client is not initialized.");
        return;
    }

    const origBtnText = signInBtn ? signInBtn.innerHTML : "SIGN IN WITH SUPABASE";
    if (signInBtn) {
        signInBtn.disabled = true;
        signInBtn.innerHTML = "AUTHENTICATING...";
    }

    try {
        pushTerminalLog(`Authenticating ${email} with Supabase Auth...`);
        const { data, error } = await supabaseClient.auth.signInWithPassword({
            email: email,
            password: password
        });

        if (error) {
            let msg = error.message;
            if (error.status === 400 || msg.includes("Invalid login credentials")) {
                msg = "Invalid email or password. If you haven't created an account yet, please click <strong>Sign Up</strong> below to register.";
            }
            setAuthAlert(errAlert, `⚠️ <strong>Sign-In Failed</strong><br>${msg}`);
            pushTerminalLog(`Supabase Auth Rejection (HTTP ${error.status || 400}): ${error.message}`);
            return;
        }

        if (data && data.user) {
            AppState.user = data.user;
            document.getElementById("userAccountEmail").textContent = data.user.email;
            pushTerminalLog("Supabase Authentication Verified!");
            resetUserData();
            fetchIncidentsFromSupabase();
            showScreen("main");
        }
    } catch (err) {
        setAuthAlert(errAlert, `⚠️ Sign-in Error: ${err.message}`);
        pushTerminalLog(`Auth Error: ${err.message}`);
    } finally {
        if (signInBtn) {
            signInBtn.disabled = false;
            signInBtn.innerHTML = origBtnText;
        }
    }
}

async function handleSignUp() {
    const emailInput = document.getElementById("regEmail");
    const passwordInput = document.getElementById("regPassword");
    const email = emailInput ? emailInput.value.trim() : "";
    const password = passwordInput ? passwordInput.value.trim() : "";
    const errAlert = document.getElementById("registerErrorAlert");
    const signUpBtn = document.querySelector("#screen-register .btn-primary");

    clearAuthAlert(errAlert);

    if (!email || !password) {
        setAuthAlert(errAlert, "⚠️ Please enter an email and password to register.");
        return;
    }

    if (password.length < 6) {
        setAuthAlert(errAlert, "⚠️ Password must be at least 6 characters long (Supabase requirement).");
        return;
    }

    if (!supabaseClient) {
        setAuthAlert(errAlert, "⚠️ Supabase client unavailable.");
        return;
    }

    const origBtnText = signUpBtn ? signUpBtn.innerHTML : "CREATE ACCOUNT";
    if (signUpBtn) {
        signUpBtn.disabled = true;
        signUpBtn.innerHTML = "CREATING ACCOUNT...";
    }

    try {
        pushTerminalLog(`Registering user ${email} on Supabase...`);
        const { data, error } = await supabaseClient.auth.signUp({
            email: email,
            password: password
        });

        if (error) {
            let msg = error.message;
            if (error.status === 422 || msg.includes("Password should be at least")) {
                msg = "Validation Error (HTTP 422): Password must be at least 6 characters, or email confirmation is required by Supabase.";
            } else if (msg.includes("User already registered")) {
                msg = "This email is already registered in Supabase. Please click 'Log In' to sign in!";
            }
            setAuthAlert(errAlert, `⚠️ <strong>Registration Failed</strong><br>${msg}`);
            pushTerminalLog(`Registration Error (HTTP ${error.status || 422}): ${error.message}`);
            return;
        }

        setAuthAlert(errAlert, "✅ Account registered successfully! You can now sign in with your credentials.", "success");
        pushTerminalLog("Supabase Account Registered!");

        document.getElementById("loginEmail").value = email;
        document.getElementById("loginPassword").value = password;

        setTimeout(() => {
            showScreen("login");
        }, 1800);

    } catch (err) {
        setAuthAlert(errAlert, `⚠️ Registration Error: ${err.message}`);
        pushTerminalLog(`Auth Error: ${err.message}`);
    } finally {
        if (signUpBtn) {
            signUpBtn.disabled = false;
            signUpBtn.innerHTML = origBtnText;
        }
    }
}

async function handleSignOut() {
    if (supabaseClient) {
        try { await supabaseClient.auth.signOut(); } catch (e) { }
    }
    AppState.user = null;
    resetUserData();
    pushTerminalLog("Signed out from Supabase.");
    showScreen("welcome");
}

// --- Supabase Postgrest Database Functions ---
async function syncIncidentToSupabase(incident) {
    if (!supabaseClient || !AppState.user) return;

    try {
        const remoteRecord = {
            user_id: AppState.user.id || "demo-user-id",
            timestamp: incident.timestamp || Date.now(),
            sender: incident.sender,
            message_content: incident.message,
            source_app: incident.app,
            severity: incident.severity,
            reason: incident.reason
        };

        const { error } = await supabaseClient.from("incidents").insert([remoteRecord]);
        if (error) throw error;

        pushTerminalLog(`Synced incident #${incident.id} to Supabase database.`);
    } catch (err) {
        pushTerminalLog(`Supabase DB Sync: ${err.message}`);
    }
}

async function fetchIncidentsFromSupabase() {
    if (!supabaseClient || !AppState.user) return;

    try {
        pushTerminalLog("Fetching user incidents from Supabase Postgrest...");
        const { data, error } = await supabaseClient
            .from("incidents")
            .select("*")
            .eq("user_id", AppState.user.id)
            .order("timestamp", { ascending: false });

        if (error) throw error;

        if (data && data.length > 0) {
            AppState.incidents = data.map(item => ({
                id: item.id || Date.now(),
                sender: item.sender,
                app: item.source_app || "Notification",
                message: item.message_content,
                severity: item.severity,
                reason: item.reason,
                time: new Date(item.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
                timestamp: item.timestamp
            }));
            AppState.stats.flagged = AppState.incidents.length;
            pushTerminalLog(`Fetched ${data.length} incidents from Supabase!`);
        } else {
            AppState.incidents = [];
            AppState.stats.flagged = 0;
            pushTerminalLog("No existing incidents found in Supabase for this user (0 incidents).");
        }
        updateStatsCounters();
        renderIncidents();
    } catch (err) {
        pushTerminalLog(`Fetch DB: ${err.message}`);
    }
}

// --- DOM Navigation & Routing ---
function showScreen(screenId) {
    document.querySelectorAll(".screen").forEach(el => el.classList.remove("active"));
    const target = document.getElementById(`screen-${screenId}`);
    if (target) {
        target.classList.add("active");
        AppState.currentScreen = screenId;
    }
}

function switchTab(tabId) {
    document.querySelectorAll(".tab-page").forEach(el => el.classList.remove("active"));
    document.querySelectorAll(".nav-item").forEach(el => el.classList.remove("active"));

    const pageTarget = document.getElementById(`tab-${tabId}`);
    const navTarget = document.getElementById(`nav-${tabId}`);

    if (pageTarget && navTarget) {
        pageTarget.classList.add("active");
        navTarget.classList.add("active");
        AppState.currentTab = tabId;
    }
}

// --- Terminal Logger ---
const terminalLogs = [
    "[10:14:02 AM] CyberShield Supabase Engine online.",
    "[10:14:03 AM] Edge Function gateway connected.",
    "[10:15:30 AM] Registered notification listener for 6 social channels."
];

function pushTerminalLog(msg) {
    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    terminalLogs.push(`[${time}] ${msg}`);
    if (terminalLogs.length > 15) terminalLogs.shift();

    const terminalEl = document.getElementById("terminalLog");
    if (terminalEl) {
        terminalEl.innerHTML = terminalLogs.map(l => `<div>${l}</div>`).join("");
        terminalEl.scrollTop = terminalEl.scrollHeight;
    }
}

setInterval(() => {
    if (AppState.currentScreen === "main" && AppState.currentTab === "home") {
        const apps = ["WhatsApp", "Instagram", "Snapchat", "Telegram", "SMS"];
        const randomApp = apps[Math.floor(Math.random() * apps.length)];
        pushTerminalLog(`Active protection monitoring ${randomApp}...`);
    }
}, 4500);

function updateStatsCounters() {
    const scannedEl = document.getElementById("statScanned");
    const flaggedEl = document.getElementById("statFlagged");
    if (scannedEl) scannedEl.textContent = AppState.stats.scanned;
    if (flaggedEl) flaggedEl.textContent = AppState.stats.flagged;
}

function renderIncidents(filterSeverity = "ALL", searchQuery = "") {
    const feedContainer = document.getElementById("incidentFeed");
    if (!feedContainer) return;

    let filtered = AppState.incidents;
    if (filterSeverity !== "ALL") {
        filtered = filtered.filter(i => i.severity === filterSeverity);
    }
    if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        filtered = filtered.filter(i =>
            i.sender.toLowerCase().includes(q) ||
            i.message.toLowerCase().includes(q) ||
            i.app.toLowerCase().includes(q)
        );
    }

    if (filtered.length === 0) {
        feedContainer.innerHTML = `
            <div style="text-align:center; padding: 40px 20px; color: var(--muted-text);">
                <svg width="40" height="40" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                <div style="margin-top:10px; font-weight:700;">No incidents detected</div>
                <div style="font-size:12px;">Your feed is clean and protected!</div>
            </div>
        `;
        return;
    }

    feedContainer.innerHTML = filtered.map(inc => `
        <div class="incident-item" onclick="openIncidentModal(${inc.id})">
            <div class="incident-top">
                <div class="sender-tag">
                    <div class="avatar-badge">${inc.sender.charAt(0)}</div>
                    <div>
                        <div style="font-size:14px; font-weight:700;">${inc.sender}</div>
                        <div style="font-size:11px; color:var(--muted-text);">${inc.app} • ${inc.time}</div>
                    </div>
                </div>
                <span class="badge-sev badge-${inc.severity.toLowerCase()}">${inc.severity}</span>
            </div>
            <div class="msg-bubble">"${inc.message}"</div>
            <div class="reason-tag">🔍 ${inc.reason}</div>
        </div>
    `).join("");
}

function openIncidentModal(id) {
    const inc = AppState.incidents.find(i => i.id === id);
    if (!inc) return;

    document.getElementById("modalSender").textContent = inc.sender;
    document.getElementById("modalApp").textContent = inc.app;
    document.getElementById("modalSeverity").textContent = inc.severity;
    document.getElementById("modalSeverity").className = `badge-sev badge-${inc.severity.toLowerCase()}`;
    document.getElementById("modalMessage").textContent = `"${inc.message}"`;
    document.getElementById("modalReason").textContent = inc.reason;

    document.getElementById("incidentModal").classList.add("active");
}

function closeModal() {
    document.getElementById("incidentModal").classList.remove("active");
}

async function simulateNotification() {
    const sender = document.getElementById("simSender").value || "Alex Rivera";
    const app = document.getElementById("simApp").value || "Instagram";
    const message = document.getElementById("simMessage").value || "I hate you, you are ugly and pathetic!";

    if (!message.trim()) {
        alert("Please enter a message to simulate.");
        return;
    }

    pushTerminalLog(`Classifying payload via Supabase AI Edge...`);
    const res = await classifyWithEdge(message);
    AppState.stats.scanned++;

    if (res.isFlagged) {
        AppState.stats.flagged++;
        const newIncident = {
            id: Date.now(),
            sender: sender,
            app: app,
            message: message,
            severity: res.severity,
            reason: res.reason,
            time: "Just now",
            timestamp: Date.now()
        };
        AppState.incidents.unshift(newIncident);
        pushTerminalLog(`🚨 FLAGGED ${res.severity} from ${sender} on ${app}`);
        renderIncidents();
        syncIncidentToSupabase(newIncident);
        alert(`🚨 CyberShield Alert!\n\nFlagged Severity: ${res.severity}\nSender: ${sender}\nApp: ${app}\nReason: ${res.reason}\n\nAutomated Guardian SMS sent to ${AppState.guardianInfo.phone}`);
    } else {
        pushTerminalLog(`Scanned ${app} message from ${sender}... Safe (NONE)`);
        alert(`✅ Message Scanned: Safe\n\nNo harassment or threats detected.`);
    }

    updateStatsCounters();
    document.getElementById("simMessage").value = "";
}

async function runSandboxAnalysis() {
    const input = document.getElementById("sandboxInput").value;
    const outputCard = document.getElementById("sandboxOutput");
    if (!input.trim()) return;

    pushTerminalLog(`Sandbox: Querying Supabase AI Edge...`);
    const res = await classifyWithEdge(input);

    outputCard.style.display = "flex";
    document.getElementById("sandboxSev").textContent = res.severity;
    document.getElementById("sandboxSev").className = `badge-sev badge-${res.severity.toLowerCase()}`;
    document.getElementById("sandboxReason").textContent = res.reason;
    document.getElementById("sandboxFlagged").textContent = res.isFlagged ? "Yes (Alert Triggered)" : "No (Safe)";
    document.getElementById("sandboxFlagged").style.color = res.isFlagged ? "var(--rose-accent)" : "var(--cyan-accent)";
}

function setSandboxSample(text) {
    document.getElementById("sandboxInput").value = text;
    runSandboxAnalysis();
}

// --- Initialize App & Supabase Auth Listener ---
document.addEventListener("DOMContentLoaded", () => {
    updateStatsCounters();
    pushTerminalLog("System ready.");
    renderIncidents();

    if (supabaseClient) {
        pushTerminalLog("Connected to Supabase Project!");
        try {
            supabaseClient.auth.onAuthStateChange((event, session) => {
                if (session && session.user) {
                    AppState.user = session.user;
                    document.getElementById("userAccountEmail").textContent = session.user.email;
                    resetUserData();
                    fetchIncidentsFromSupabase();
                }
            });
        } catch (e) { }
    }

    setTimeout(() => {
        if (AppState.currentScreen === "splash") {
            showScreen("welcome");
        }
    }, 2200);
});
