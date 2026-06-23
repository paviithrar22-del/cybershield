// @ts-nocheck
// NOTE: These are Deno Edge Functions - VS Code TypeScript errors are expected
// unless the Deno VS Code extension is installed. The function runs correctly on Supabase.



const GEMINI_API_KEY = Deno.env.get("GEMINI_API_KEY");

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { text } = await req.json();

    if (!text || text.trim() === "") {
      return new Response(
        JSON.stringify({ severity: "NONE", reason: "Empty text" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 200 }
      );
    }

    if (!GEMINI_API_KEY) {
      return new Response(
        JSON.stringify({ severity: "NONE", reason: "Gemini API key is not configured on Edge Function environment" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 200 }
      );
    }

    const prompt = `You are a precise cyberbullying classifier for a child protection app.
Analyze the message and classify it into one of these severity levels: NONE, LOW, MEDIUM, HIGH, CRITICAL.
Only classify as MEDIUM, HIGH, or CRITICAL if it contains clear cyberbullying, hate speech, threats, severe insults, or harassment.
Minor issues or normal words should be NONE or LOW.
Support multiple languages and slang: English, Hindi, Hinglish, Tamil, Tanglish, Telugu, and other regional Indian languages/slangs.

Input Message: "${text}"

Respond ONLY with a valid JSON object in the following format, with no markdown formatting wrappers, no backticks, and no extra text:
{"severity": "SEVERITY_LEVEL", "reason": "Short explanation in English"}

Example outputs:
{"severity": "CRITICAL", "reason": "Contains direct physical threat in Tamil"}
{"severity": "NONE", "reason": "No harmful content detected"}`;

    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${GEMINI_API_KEY}`;

    const response = await fetch(geminiUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        contents: [
          {
            parts: [
              {
                text: prompt,
              },
            ],
          },
        ],
        generationConfig: {
          responseMimeType: "application/json",
        },
      }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Gemini API call failed: ${errorText}`);
    }

    const geminiData = await response.json();
    const resultText: string = geminiData.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || "";

    // Parse the response
    let parsedResult: { severity: string; reason: string };
    try {
      parsedResult = JSON.parse(resultText);
    } catch (_e) {
      // Clean possible markdown wrapper if any
      const cleanedText = resultText.replace(/```json/g, "").replace(/```/g, "").trim();
      parsedResult = JSON.parse(cleanedText);
    }

    return new Response(
      JSON.stringify({
        severity: parsedResult.severity || "NONE",
        reason: parsedResult.reason || "Classified by AI",
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
