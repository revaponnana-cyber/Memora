package com.memora.app.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun `normalizes case punctuation and whitespace`() {
        assertEquals(
            "i put my car keys in my drawer in bedroom",
            TextNormalizer.normalize("  I put my CAR keys in my drawer in bedroom!! "),
        )
    }

    @Test
    fun `expands contractions`() {
        assertEquals("where is my passport", TextNormalizer.normalize("Where's my passport?"))
        assertEquals("i cannot find my wallet", TextNormalizer.normalize("I can't find my wallet."))
        assertEquals("i will move it tomorrow", TextNormalizer.normalize("I'll move it tomorrow."))
    }

    @Test
    fun `strips determiners`() {
        assertEquals("car keys", TextNormalizer.stripDeterminer("my car keys"))
        assertEquals("drawer", TextNormalizer.stripDeterminer("the drawer"))
        assertEquals("window", TextNormalizer.stripDeterminer("at the window".removePrefix("at ")))
    }

    @Test
    fun `match key singularizes light plurals`() {
        assertEquals("car key", TextNormalizer.matchKey("car keys"))
        assertEquals("car key", TextNormalizer.matchKey("Car key"))
        assertEquals("car key", TextNormalizer.matchKey("my Car Keys"))
        // double-s and short words are left alone
        assertEquals("glass", TextNormalizer.matchKey("glass"))
        assertEquals("usb", TextNormalizer.matchKey("usb"))
    }

    @Test
    fun `extracts trailing time expressions`() {
        val (text, offset) = TextNormalizer.extractTimeOffsetMs("i moved my charger yesterday")
        assertEquals("i moved my charger", text)
        assertEquals(-24L * 3_600_000L, offset)

        val none = TextNormalizer.extractTimeOffsetMs("i put my keys in the drawer")
        assertEquals("i put my keys in the drawer", none.first)
        assertNull(none.second)
    }

    @Test
    fun `time expression today maps to zero offset`() {
        val (text, offset) = TextNormalizer.extractTimeOffsetMs("i left it in the kitchen today")
        assertEquals("i left it in the kitchen", text)
        assertEquals(0L, offset)
        assertTrue((offset ?: -1L) >= 0L)
    }
}
