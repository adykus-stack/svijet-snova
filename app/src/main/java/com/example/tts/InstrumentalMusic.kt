package com.example.tts

import com.example.data.model.MusicMood
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/** Original instrumental scores synthesized locally; no downloads or third-party recordings. */
internal object InstrumentalMusic {
    const val SAMPLE_RATE = 22050
    private const val TWO_PI = 2.0 * PI

    fun render(mood: MusicMood, bars: Int = 8): ByteArray {
        require(bars in 1..8)
        val bpm = when (mood) {
            MusicMood.CALM -> 76
            MusicMood.LULLABY -> 68
            MusicMood.MAGIC -> 84
            MusicMood.ADVENTURE -> 96
            MusicMood.MYSTERY -> 80
            MusicMood.HAPPY -> 100
        }
        val beat = 60.0 / bpm
        val barLength = beat * 4
        val samples = FloatArray((bars * barLength * SAMPLE_RATE).roundToInt())
        val minor = mood == MusicMood.MYSTERY
        val roots = if (minor) intArrayOf(45, 41, 50, 52) else intArrayOf(48, 45, 41, 43)
        val tune = when (mood) {
            MusicMood.LULLABY -> intArrayOf(7, 4, 2, 0, 2, 4, 7, 4)
            MusicMood.CALM -> intArrayOf(0, 4, 7, 9, 7, 4, 2, 0)
            MusicMood.MAGIC -> intArrayOf(0, 7, 12, 9, 4, 7, 9, 12)
            MusicMood.ADVENTURE -> intArrayOf(0, 4, 7, 12, 9, 7, 4, 7)
            MusicMood.MYSTERY -> intArrayOf(0, 3, 7, 10, 7, 5, 3, 2)
            MusicMood.HAPPY -> intArrayOf(4, 7, 9, 7, 4, 2, 0, 4)
        }
        for (bar in 0 until bars) {
            val start = bar * barLength
            val root = roots[bar % roots.size]
            val third = if (minor) (if (bar % 4 == 1) 4 else 3)
                else (if (bar % 4 == 1) 3 else 4)
            for (interval in intArrayOf(0, third, 7)) {
                note(samples, root + interval, start, barLength, 0.075, bell = false, pad = true)
            }
            val lively = mood == MusicMood.HAPPY || mood == MusicMood.ADVENTURE
            val beats = if (lively) doubleArrayOf(0.0, 1.0, 2.0, 3.0) else doubleArrayOf(0.0, 2.0)
            beats.forEachIndexed { position, offset ->
                val pitch = (if (minor) 69 else 72) + tune[(bar * 2 + position) % tune.size]
                note(samples, pitch, start + offset * beat, beat * if (lively) 0.95 else 1.8,
                    0.19, bell = mood == MusicMood.MAGIC || mood == MusicMood.HAPPY, pad = false)
            }
        }
        // Keep plenty of headroom for the independent narration layer.
        val peak = samples.maxOf { abs(it) }.coerceAtLeast(0.001f)
        val scale = 0.65 / peak
        val dataSize = samples.size * 2
        return ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataSize); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(dataSize)
            samples.forEach { putShort((it * scale * Short.MAX_VALUE).roundToInt().toShort()) }
        }.array()
    }

    private fun note(samples: FloatArray, midi: Int, start: Double, duration: Double,
                     amplitude: Double, bell: Boolean, pad: Boolean) {
        val frequency = 440.0 * 2.0.pow((midi - 69) / 12.0)
        val first = (start * SAMPLE_RATE).roundToInt()
        val count = (duration * SAMPLE_RATE).roundToInt()
        val attack = if (pad) 0.25 else 0.025
        val release = if (pad) 0.3 else 0.12
        for (index in 0 until min(count, samples.size - first)) {
            val time = index.toDouble() / SAMPLE_RATE
            val envelope = min(1.0, time / attack) * min(1.0, (duration - time) / release)
            val phase = TWO_PI * frequency * time
            val tone = if (bell) {
                (sin(phase) + 0.25 * sin(phase * 2.01)) * exp(-time * 1.5)
            } else {
                sin(phase) + 0.15 * sin(phase * 2) + 0.035 * sin(phase * 3)
            }
            samples[first + index] += (tone * amplitude * envelope).toFloat()
        }
    }
}
