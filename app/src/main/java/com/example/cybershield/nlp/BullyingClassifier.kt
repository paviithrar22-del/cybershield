package com.example.cybershield.nlp

import java.util.Locale

enum class Severity {
    NONE, LOW, MEDIUM, HIGH, CRITICAL
}

data class ClassificationResult(
    val severity: Severity,
    val isFlagged: Boolean,
    val reason: String
)

object BullyingClassifier {

    // Physical threats / self-harm across languages
    val threatWords = setOf(
        // English
        "kill", "die", "murder", "hurt", "beat", "slit", "throat", "stab", "shoot", 
        "burn", "suicide", "kys", "hang", "strangle", "drown", "destroy",
        // Hindi / Hinglish
        "maar", "jaan se", "khoon", "marna", "kaat", "tod",
        // Tamil / Tanglish
        "kola", "kollu", "saavu", "savu", "adi", "vettu", "kuthu",
        // Telugu
        "champu", "naraku", "kotti", "sachipo"
    )

    // Hate speech, severe slurs across languages
    val hateWords = setOf(
        // English
        "nigger", "faggot", "retard", "cunt", "bitch", "chink", "kike", "tranny", 
        "dyke", "spic", "wetback", "bastard", "whore", "slut", "trash", "scum", "subhuman",
        // Hindi / Hinglish
        "gandu", "chutiya", "randi", "bhadwa", "harami", "kamina", "kamine", "saala", "sala",
        // Tamil / Tanglish
        "punda", "thevadiya", "baadu", "sunni", "otha", "ootha", "omala", "bunda",
        // Telugu
        "lanja", "na kodaka", "lucha", "lanjodaka", "dengu"
    )

    // Harassment, insults across languages
    val harassmentWords = setOf(
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
    )

    val profanityWords = setOf(
        "fuck", "shit", "ass", "asshole", "dick", "pussy", "crap"
    )

    fun classify(text: String, sensitivity: String): ClassificationResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return ClassificationResult(Severity.NONE, false, "Empty text")
        }

        val lowerText = trimmed.lowercase(Locale.ROOT)
        
        // Tokenize into words, removing basic punctuation
        val words = lowerText.split(Regex("[\\s.,!?;:\"]+")).filter { it.isNotEmpty() }
        
        var threatCount = 0
        val matchedThreats = mutableListOf<String>()
        var hateCount = 0
        val matchedHate = mutableListOf<String>()
        var harassmentCount = 0
        val matchedHarassment = mutableListOf<String>()
        var profanityCount = 0
        val matchedProfanity = mutableListOf<String>()

        // Check multi-word phrases first
        val phrases = listOf(
            "kill yourself", "end your life", "nobody likes you", "die alone", "shut up", "go away",
            "jaan se maar", "jaan se maardunga", "poittu saavu", "mooditu po"
        )
        for (phrase in phrases) {
            if (lowerText.contains(phrase)) {
                if (phrase == "kill yourself" || phrase == "end your life" || phrase == "jaan se maar" || phrase == "jaan se maardunga") {
                    threatCount += 2
                    matchedThreats.add(phrase)
                } else {
                    harassmentCount += 2
                    matchedHarassment.add(phrase)
                }
            }
        }

        // Check individual words
        for (word in words) {
            if (threatWords.contains(word) && !matchedThreats.contains(word)) {
                threatCount++
                matchedThreats.add(word)
            }
            if (hateWords.contains(word) && !matchedHate.contains(word)) {
                hateCount++
                matchedHate.add(word)
            }
            if (harassmentWords.contains(word) && !matchedHarassment.contains(word)) {
                harassmentCount++
                matchedHarassment.add(word)
            }
            if (profanityWords.contains(word) && !matchedProfanity.contains(word)) {
                profanityCount++
                matchedProfanity.add(word)
            }
        }

        // Check intensity indicators
        var intensityMultiplier = 1.0
        
        // Check for SHOUTING (all caps or mostly caps)
        val uppercaseCount = trimmed.count { it.isUpperCase() }
        val letterCount = trimmed.count { it.isLetter() }
        if (letterCount > 4 && (uppercaseCount.toDouble() / letterCount.toDouble()) > 0.6) {
            intensityMultiplier += 0.5
        }

        // Check for multiple exclamation marks
        val exclamationCount = trimmed.count { it == '!' }
        if (exclamationCount >= 3) {
            intensityMultiplier += 0.3
        }

        // Calculate weighted score
        val baseScore = (threatCount * 6.0) + (hateCount * 4.0) + (harassmentCount * 2.0) + (profanityCount * 1.0)
        val finalScore = baseScore * intensityMultiplier

        val thresholdModifier = when (sensitivity.lowercase(Locale.ROOT)) {
            "high" -> 0.7
            "low" -> 1.5
            else -> 1.0
        }

        val lowThreshold = 1.0 * thresholdModifier
        val mediumThreshold = 3.0 * thresholdModifier
        val highThreshold = 6.0 * thresholdModifier
        val criticalThreshold = 10.0 * thresholdModifier

        val severity = when {
            finalScore >= criticalThreshold -> Severity.CRITICAL
            finalScore >= highThreshold -> Severity.HIGH
            finalScore >= mediumThreshold -> Severity.MEDIUM
            finalScore >= lowThreshold -> Severity.LOW
            else -> Severity.NONE
        }

        val isFlagged = severity.ordinal >= Severity.MEDIUM.ordinal

        val reasonList = mutableListOf<String>()
        if (matchedThreats.isNotEmpty()) reasonList.add("Threats: ${matchedThreats.joinToString(", ")}")
        if (matchedHate.isNotEmpty()) reasonList.add("Hate Speech/Slurs: ${matchedHate.joinToString(", ")}")
        if (matchedHarassment.isNotEmpty()) reasonList.add("Harassment/Insults: ${matchedHarassment.joinToString(", ")}")
        if (matchedProfanity.isNotEmpty() && isFlagged) reasonList.add("Profanity: ${matchedProfanity.joinToString(", ")}")
        
        if (intensityMultiplier > 1.0 && isFlagged) {
            reasonList.add("Aggressive Tone")
        }

        val reason = if (reasonList.isEmpty()) {
            "No harmful content detected"
        } else {
            reasonList.joinToString(" | ")
        }

        return ClassificationResult(severity, isFlagged, reason)
    }
}
