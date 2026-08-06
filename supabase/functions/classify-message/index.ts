// @ts-nocheck
// NOTE: These are Deno Edge Functions - VS Code TypeScript errors are expected
// unless the Deno VS Code extension is installed. The function runs correctly on Supabase.

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

// Physical threats / self-harm across languages
const threatWords = new Set([
  // English
  "kill", "die", "murder", "hurt", "beat", "slit", "throat", "stab", "shoot", 
  "burn", "suicide", "kys", "hang", "strangle", "drown", "destroy",
  // Hindi / Hinglish
  "maar", "jaan se", "khoon", "marna", "kaat", "tod",
  // Tamil / Tanglish
  "kola", "kollu", "saavu", "savu", "adi", "vettu", "kuthu",
  // Telugu
  "champu", "naraku", "kotti", "sachipo"
]);

// Hate speech, severe slurs across languages
const hateWords = new Set([
  // English
  "nigger", "faggot", "retard", "cunt", "bitch", "chink", "kike", "tranny", 
  "dyke", "spic", "wetback", "bastard", "whore", "slut", "trash", "scum", "subhuman",
  // Hindi / Hinglish
  "gandu", "chutiya", "randi", "bhadwa", "harami", "kamina", "kamine", "saala", "sala",
  // Tamil / Tanglish
  "punda", "thevadiya", "baadu", "sunni", "otha", "ootha", "omala", "bunda",
  // Telugu
  "lanja", "na kodaka", "lucha", "lanjodaka", "dengu"
]);

// Harassment, insults across languages
const harassmentWords = new Set([
  // English
  "idiot", "stupid", "dumb", "loser", "looser", "freak", "useless", "worthless", "ugly", 
  "fat", "pathetic", "hate", "worst", "disgusting", "nobody likes you", "die alone",
  "shut up", "go away", "kill yourself", "end your life",
  // Hindi / Hinglish
  "pagal", "chup", "gadha", "ullu", "kamzarf", "bakwas",
  // Tamil / Tanglish
  "mental", "loosu", "loose", "echa", "paithiyam", "moodu", "sathuru",
  // Telugu
  "pichi", "pitchi", "donga", "sollu", "bodi", "asahyanga"
]);

const profanityWords = new Set([
  "fuck", "shit", "ass", "asshole", "dick", "pussy", "crap"
]);

const phrases = [
  "kill yourself", "end your life", "nobody likes you", "die alone", "shut up", "go away",
  "jaan se maar", "jaan se maardunga", "poittu saavu", "mooditu po"
];

Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const body = await req.json().catch(() => ({}));
    const text = body.text || "";
    const sensitivity = body.sensitivity || "Medium";

    if (!text || text.trim() === "") {
      return new Response(
        JSON.stringify({ severity: "NONE", reason: "Empty text" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 200 }
      );
    }

    const trimmed = text.trim();
    const lowerText = trimmed.toLowerCase();
    
    // Tokenize into words, removing basic punctuation
    const words = lowerText.split(/[\s.,!?;:":]+/).filter((w: string) => w.length > 0);
    
    let threatCount = 0;
    const matchedThreats: string[] = [];
    let hateCount = 0;
    const matchedHate: string[] = [];
    let harassmentCount = 0;
    const matchedHarassment: string[] = [];
    let profanityCount = 0;
    const matchedProfanity: string[] = [];

    // Check multi-word phrases first
    for (const phrase of phrases) {
      if (lowerText.includes(phrase)) {
        if (phrase === "kill yourself" || phrase === "end your life" || phrase === "jaan se maar" || phrase === "jaan se maardunga") {
          threatCount += 2;
          matchedThreats.push(phrase);
        } else {
          harassmentCount += 2;
          matchedHarassment.push(phrase);
        }
      }
    }

    // Check individual words
    for (const word of words) {
      if (threatWords.has(word) && !matchedThreats.includes(word)) {
        threatCount++;
        matchedThreats.push(word);
      }
      if (hateWords.has(word) && !matchedHate.includes(word)) {
        hateCount++;
        matchedHate.push(word);
      }
      if (harassmentWords.has(word) && !matchedHarassment.includes(word)) {
        harassmentCount++;
        matchedHarassment.push(word);
      }
      if (profanityWords.has(word) && !matchedProfanity.includes(word)) {
        profanityCount++;
        matchedProfanity.push(word);
      }
    }

    // Check intensity indicators
    let intensityMultiplier = 1.0;
    
    // Check for SHOUTING (all caps or mostly caps)
    const letters = trimmed.replace(/[^a-zA-Z]/g, "");
    const uppercaseLetters = letters.replace(/[^A-Z]/g, "");
    if (letters.length > 4 && (uppercaseLetters.length / letters.length) > 0.6) {
      intensityMultiplier += 0.5;
    }

    // Check for multiple exclamation marks
    const exclamationCount = (trimmed.match(/!/g) || []).length;
    if (exclamationCount >= 3) {
      intensityMultiplier += 0.3;
    }

    // Calculate weighted score
    const baseScore = (threatCount * 6.0) + (hateCount * 4.0) + (harassmentCount * 2.0) + (profanityCount * 1.0);
    const finalScore = baseScore * intensityMultiplier;

    let thresholdModifier = 1.0;
    const sensLower = sensitivity.toLowerCase();
    if (sensLower === "high") {
      thresholdModifier = 0.7;
    } else if (sensLower === "low") {
      thresholdModifier = 1.5;
    }

    const lowThreshold = 1.0 * thresholdModifier;
    const mediumThreshold = 3.0 * thresholdModifier;
    const highThreshold = 6.0 * thresholdModifier;
    const criticalThreshold = 10.0 * thresholdModifier;

    let severity = "NONE";
    if (finalScore >= criticalThreshold) {
      severity = "CRITICAL";
    } else if (finalScore >= highThreshold) {
      severity = "HIGH";
    } else if (finalScore >= mediumThreshold) {
      severity = "MEDIUM";
    } else if (finalScore >= lowThreshold) {
      severity = "LOW";
    }

    const isFlagged = ["MEDIUM", "HIGH", "CRITICAL"].includes(severity);

    const reasonList: string[] = [];
    if (matchedThreats.length > 0) reasonList.push(`Threats: ${matchedThreats.join(", ")}`);
    if (matchedHate.length > 0) reasonList.push(`Hate Speech/Slurs: ${matchedHate.join(", ")}`);
    if (matchedHarassment.length > 0) reasonList.push(`Harassment/Insults: ${matchedHarassment.join(", ")}`);
    if (matchedProfanity.length > 0 && isFlagged) reasonList.push(`Profanity: ${matchedProfanity.join(", ")}`);
    
    if (intensityMultiplier > 1.0 && isFlagged) {
      reasonList.push("Aggressive Tone");
    }

    const reason = reasonList.length === 0 ? "No harmful content detected" : reasonList.join(" | ");

    return new Response(
      JSON.stringify({
        severity,
        reason
      }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 200 }
    );

  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : String(error);
    return new Response(
      JSON.stringify({ severity: "NONE", reason: `Error: ${message}` }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
    );
  }
});
