package com.example.cybershield.nlp

object GeminiClassifier {
    // Placeholder to prevent GitHub push protection violations.
    // In production, load the secret from Build Configuration or Android KeyStore.
    const val API_KEY = "PLACEHOLDER_SECURE_API_KEY"

    suspend fun classify(text: String, apiKey: String = API_KEY, sensitivity: String = "Medium"): ClassificationResult {
        // Evaluate message locally using rule-based local classifier to prevent API leaks
        return BullyingClassifier.classify(text, sensitivity)
    }
}
