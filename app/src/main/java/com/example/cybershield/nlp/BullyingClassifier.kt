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

    fun classify(text: String, sensitivity: String, customKeywords: Set<String> = emptySet()): ClassificationResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return ClassificationResult(Severity.NONE, false, "Empty text")
        }

        // Split text into chunks (e.g., sentences or clauses using common delimiters)
        val chunks = trimmed.split(Regex("[.\\n!?;]+")).map { it.trim() }.filter { it.isNotEmpty() }
        
        if (chunks.isEmpty()) {
            return ClassificationResult(Severity.NONE, false, "Empty text")
        }

        var maxSeverity = Severity.NONE
        var isAnyFlagged = false
        val allMatchedThreats = mutableSetOf<String>()
        val allMatchedHate = mutableSetOf<String>()
        val allMatchedHarassment = mutableSetOf<String>()
        val allMatchedProfanity = mutableSetOf<String>()
        val allMatchedCustom = mutableSetOf<String>()
        var totalIntensityMultiplier = 1.0

        for (chunk in chunks) {
            val lowerChunk = chunk.lowercase(Locale.ROOT)
            val words = lowerChunk.split(Regex("[\\s.,!?;:\"]+")).filter { it.isNotEmpty() }
            
            var threatCount = 0
            val matchedThreats = mutableListOf<String>()
            var hateCount = 0
            val matchedHate = mutableListOf<String>()
            var harassmentCount = 0
            val matchedHarassment = mutableListOf<String>()
            var profanityCount = 0
            val matchedProfanity = mutableListOf<String>()
            var customCount = 0
            val matchedCustom = mutableListOf<String>()

            // Check multi-word phrases first
            val phrases = listOf(
                "kill yourself", "end your life", "nobody likes you", "die alone", "shut up", "go away",
                "jaan se maar", "jaan se maardunga", "poittu saavu", "mooditu po"
            )
            for (phrase in phrases) {
                if (lowerChunk.contains(phrase)) {
                    if (phrase == "kill yourself" || phrase == "end your life" || phrase == "jaan se maar" || phrase == "jaan se maardunga") {
                        threatCount += 2
                        matchedThreats.add(phrase)
                    } else {
                        harassmentCount += 2
                        matchedHarassment.add(phrase)
                    }
                }
            }

            // Check custom keywords (single words and phrases)
            for (keyword in customKeywords) {
                val lowerKeyword = keyword.trim().lowercase(Locale.ROOT)
                if (lowerKeyword.isNotEmpty()) {
                    if (lowerKeyword.contains(" ")) {
                        if (lowerChunk.contains(lowerKeyword)) {
                            customCount += 2
                            matchedCustom.add(keyword)
                        }
                    } else {
                        if (words.contains(lowerKeyword) && !matchedCustom.contains(keyword)) {
                            customCount++
                            matchedCustom.add(keyword)
                        }
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

            // Check intensity indicators for this chunk
            var intensityMultiplier = 1.0
            val uppercaseCount = chunk.count { it.isUpperCase() }
            val letterCount = chunk.count { it.isLetter() }
            if (letterCount > 4 && (uppercaseCount.toDouble() / letterCount.toDouble()) > 0.6) {
                intensityMultiplier += 0.5
            }
            val exclamationCount = chunk.count { it == '!' }
            if (exclamationCount >= 3) {
                intensityMultiplier += 0.3
            }

            // Calculate weighted score for this chunk
            val baseScore = (threatCount * 6.0) + (hateCount * 4.0) + (harassmentCount * 2.0) + (profanityCount * 1.0) + (customCount * 3.0)
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

            val chunkSeverity = when {
                finalScore >= criticalThreshold -> Severity.CRITICAL
                finalScore >= highThreshold -> Severity.HIGH
                finalScore >= mediumThreshold -> Severity.MEDIUM
                finalScore >= lowThreshold -> Severity.LOW
                else -> Severity.NONE
            }

            if (chunkSeverity.ordinal > maxSeverity.ordinal) {
                maxSeverity = chunkSeverity
            }

            val chunkFlagged = chunkSeverity.ordinal >= Severity.MEDIUM.ordinal
            if (chunkFlagged) {
                isAnyFlagged = true
            }

            // Save all matches
            allMatchedThreats.addAll(matchedThreats)
            allMatchedHate.addAll(matchedHate)
            allMatchedHarassment.addAll(matchedHarassment)
            allMatchedProfanity.addAll(matchedProfanity)
            allMatchedCustom.addAll(matchedCustom)
            if (intensityMultiplier > 1.0) {
                totalIntensityMultiplier = maxOf(totalIntensityMultiplier, intensityMultiplier)
            }
        }

        val reasonList = mutableListOf<String>()
        if (allMatchedThreats.isNotEmpty()) reasonList.add("Threats: ${allMatchedThreats.joinToString(", ")}")
        if (allMatchedHate.isNotEmpty()) reasonList.add("Hate Speech/Slurs: ${allMatchedHate.joinToString(", ")}")
        if (allMatchedHarassment.isNotEmpty()) reasonList.add("Harassment/Insults: ${allMatchedHarassment.joinToString(", ")}")
        if (allMatchedProfanity.isNotEmpty() && isAnyFlagged) reasonList.add("Profanity: ${allMatchedProfanity.joinToString(", ")}")
        if (allMatchedCustom.isNotEmpty()) reasonList.add("Custom Keywords: ${allMatchedCustom.joinToString(", ")}")
        
        if (totalIntensityMultiplier > 1.0 && isAnyFlagged) {
            reasonList.add("Aggressive Tone")
        }

        val reason = if (reasonList.isEmpty()) {
            "No harmful content detected"
        } else {
            reasonList.joinToString(" | ")
        }

        return ClassificationResult(maxSeverity, isAnyFlagged, reason)
    }
}
