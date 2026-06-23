package com.example.cybershield.nlp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BullyingClassifierTest {

    @Test
    fun testSafeMessage() {
        val result = BullyingClassifier.classify("Hey, are we still meeting for lunch today?", "Medium")
        assertEquals(Severity.NONE, result.severity)
        assertFalse(result.isFlagged)
    }

    @Test
    fun testLowSeverityProfanity() {
        val result = BullyingClassifier.classify("That was a crap game.", "Medium")
        assertEquals(Severity.LOW, result.severity)
        assertFalse(result.isFlagged)
    }

    @Test
    fun testMediumSeverityHarassment() {
        val result = BullyingClassifier.classify("You are a stupid idiot and nobody likes you", "Medium")
        assertTrue(result.severity.ordinal >= Severity.MEDIUM.ordinal)
        assertTrue(result.isFlagged)
    }

    @Test
    fun testHighSeverityHateAndAbuse() {
        val result = BullyingClassifier.classify("Go away, you worthless slut!", "Medium")
        assertTrue(result.severity.ordinal >= Severity.HIGH.ordinal)
        assertTrue(result.isFlagged)
    }

    @Test
    fun testCriticalSeverityViolenceThreat() {
        val result = BullyingClassifier.classify("I will kill you if I find you", "Medium")
        assertTrue(result.severity.ordinal >= Severity.HIGH.ordinal)
        assertTrue(result.isFlagged)
    }

    @Test
    fun testShoutingIntensityBoost() {
        val normalResult = BullyingClassifier.classify("you idiot", "Medium")
        val shoutingResult = BullyingClassifier.classify("YOU IDIOT!!!", "Medium")
        assertTrue(shoutingResult.severity.ordinal >= normalResult.severity.ordinal)
    }

    @Test
    fun testSensitivityAdjustment() {
        val mediumSensResult = BullyingClassifier.classify("You idiot", "Medium")
        val highSensResult = BullyingClassifier.classify("You idiot", "High")
        assertTrue(highSensResult.severity.ordinal >= mediumSensResult.severity.ordinal)
    }
}
