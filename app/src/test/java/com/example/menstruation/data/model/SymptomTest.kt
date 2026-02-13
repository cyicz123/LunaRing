package com.example.menstruation.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Symptom enum JSON serialization.
 */
class SymptomTest {

    @Test
    fun `toJson serializes empty list`() {
        val symptoms = emptyList<Symptom>()
        val json = Symptom.toJson(symptoms)
        assertEquals("[]", json)
    }

    @Test
    fun `toJson serializes single symptom`() {
        val symptoms = listOf(Symptom.CRAMPS)
        val json = Symptom.toJson(symptoms)
        assertEquals("[\"CRAMPS\"]", json)
    }

    @Test
    fun `toJson serializes multiple symptoms`() {
        val symptoms = listOf(Symptom.CRAMPS, Symptom.BLOATING, Symptom.HEADACHE)
        val json = Symptom.toJson(symptoms)
        assertTrue(json.contains("CRAMPS"))
        assertTrue(json.contains("BLOATING"))
        assertTrue(json.contains("HEADACHE"))
    }

    @Test
    fun `fromJson deserializes empty list`() {
        val json = "[]"
        val symptoms = Symptom.fromJson(json)
        assertTrue(symptoms.isEmpty())
    }

    @Test
    fun `fromJson deserializes single symptom`() {
        val json = "[\"CRAMPS\"]"
        val symptoms = Symptom.fromJson(json)
        assertEquals(1, symptoms.size)
        assertEquals(Symptom.CRAMPS, symptoms[0])
    }

    @Test
    fun `fromJson deserializes multiple symptoms`() {
        val json = "[\"CRAMPS\",\"BLOATING\",\"HEADACHE\"]"
        val symptoms = Symptom.fromJson(json)
        assertEquals(3, symptoms.size)
        assertTrue(symptoms.contains(Symptom.CRAMPS))
        assertTrue(symptoms.contains(Symptom.BLOATING))
        assertTrue(symptoms.contains(Symptom.HEADACHE))
    }

    @Test
    fun `fromJson handles invalid values gracefully`() {
        val json = "[\"INVALID_SYMPTOM\",\"CRAMPS\"]"
        val symptoms = Symptom.fromJson(json)
        // Should skip invalid values and return only valid ones
        assertEquals(1, symptoms.size)
        assertEquals(Symptom.CRAMPS, symptoms[0])
    }

    @Test
    fun `fromJson handles malformed json`() {
        val json = "not valid json"
        val symptoms = Symptom.fromJson(json)
        assertTrue(symptoms.isEmpty())
    }

    @Test
    fun `round trip conversion preserves symptoms`() {
        val original = listOf(
            Symptom.FATIGUE,
            Symptom.INSOMNIA,
            Symptom.ACNE,
            Symptom.BACK_PAIN
        )
        val json = Symptom.toJson(original)
        val converted = Symptom.fromJson(json)
        assertEquals(original.size, converted.size)
        assertTrue(converted.containsAll(original))
    }

    @Test
    fun `all symptoms have correct category`() {
        // Verify symptom categories
        assertEquals(SymptomCategory.BODY_GENERAL, Symptom.FATIGUE.category)
        assertEquals(SymptomCategory.HEAD, Symptom.HEADACHE.category)
        assertEquals(SymptomCategory.ABDOMEN, Symptom.BLOATING.category)
        assertEquals(SymptomCategory.SKIN, Symptom.ACNE.category)
        assertEquals(SymptomCategory.DISCHARGE, Symptom.DISCHARGE_WHITE.category)
        assertEquals(SymptomCategory.OTHER, Symptom.CRAMPS.category)
    }

    @Test
    fun `all symptoms have labels`() {
        // Verify that all symptoms have non-empty labels
        Symptom.values().forEach { symptom ->
            assertTrue(symptom.label.isNotEmpty())
        }
    }
}
