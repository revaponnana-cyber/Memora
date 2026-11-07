package com.memora.app.intelligence

import com.memora.app.domain.model.Intent
import com.memora.app.domain.model.Modality
import org.junit.Assert.assertEquals
import org.junit.Test

class IntentClassifierTest {

    private fun modality(sentence: String): Modality =
        IntentClassifier.modalityOf(TextNormalizer.normalize(sentence))

    private fun intent(sentence: String): Intent =
        IntentClassifier.intentOf(TextNormalizer.normalize(sentence))

    @Test
    fun `statements of completed actions are SET_LOCATION`() {
        assertEquals(Intent.SET_LOCATION, intent("I put my car keys in my drawer in bedroom"))
        assertEquals(Intent.SET_LOCATION, intent("I left my charger in the kitchen"))
        assertEquals(Intent.SET_LOCATION, intent("My passport is in the bedroom cupboard"))
        assertEquals(Intent.SET_LOCATION, intent("I moved my charger to the kitchen"))
    }

    @Test
    fun `where and find questions are FIND_ITEM`() {
        assertEquals(Intent.FIND_ITEM, intent("Where is my passport"))
        assertEquals(Intent.FIND_ITEM, intent("Where are my keys"))
        assertEquals(Intent.FIND_ITEM, intent("find my wallet"))
        assertEquals(Intent.FIND_ITEM, intent("I cannot find my wallet"))
    }

    @Test
    fun `unknown sentences do not pretend to be understood`() {
        assertEquals(Intent.UNKNOWN, intent("hello there"))
    }

    @Test
    fun `completed actions are STATEMENT modality`() {
        assertEquals(Modality.STATEMENT, modality("I put my car keys in my drawer"))
        assertEquals(Modality.STATEMENT, modality("I left my charger on the table"))
    }

    @Test
    fun `guesses about the past are UNCERTAIN`() {
        assertEquals(Modality.UNCERTAIN, modality("I think I left my charger in the kitchen"))
        assertEquals(Modality.UNCERTAIN, modality("I might have left my keys in the car"))
        assertEquals(Modality.UNCERTAIN, modality("Maybe I put it in the box"))
    }

    @Test
    fun `future plans are INTENTION and never facts`() {
        assertEquals(Modality.INTENTION, modality("I will move my key to the drawer"))
        assertEquals(Modality.INTENTION, modality("I am going to take it tomorrow"))
        assertEquals(Modality.INTENTION, modality("I might put my key in the drawer"))
    }

    @Test
    fun `hypotheticals are HYPOTHETICAL`() {
        assertEquals(Modality.HYPOTHETICAL, modality("What if I put my key in the drawer"))
        assertEquals(Modality.HYPOTHETICAL, modality("If I put my key in the drawer"))
    }

    @Test
    fun `intention wording wins over uncertainty filler`() {
        assertEquals(Modality.INTENTION, modality("I will probably put it in the box"))
    }
}
