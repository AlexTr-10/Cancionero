package com.example.ui.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundSynth {
    private const val SAMPLE_RATE = 22050
    private val synthScope = CoroutineScope(Dispatchers.Default)
    private var bgMusicJob: Job? = null
    private var isMusicMuted = false

    // Greek Phrygian/Dorian scale frequencies (Soft, ancient mystic vibe)
    // E4, F#4, G4, A4, B4, C5, D5, E5
    private val GREEK_SCALE = listOf(
        329.63f, // E4
        369.99f, // F#4
        392.00f, // G4
        440.00f, // A4
        493.88f, // B4
        523.25f, // C5
        587.33f, // D5
        659.25f  // E5
    )

    /**
     * Synthesizes and returns raw PCM 16-bit data for a plucked string sound.
     * Uses a fundamental frequency and soft harmonics with exponential decay.
     */
    private fun generatePluckPcm(frequency: Float, durationMs: Int, volume: Float = 0.5f): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        val tau = durationMs / 1000.0 / 4.0 // Decay constant (decays to near 0 by end)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            
            // Fundamental + 2nd harmonic + 3rd harmonic for rich acoustic wooden timbre
            val wave1 = sin(2.0 * PI * frequency * t)
            val wave2 = 0.4 * sin(4.0 * PI * frequency * t)
            val wave3 = 0.2 * sin(6.0 * PI * frequency * t)
            val mixedWave = (wave1 + wave2 + wave3) / 1.6

            // Exponential decay envelope
            val envelope = exp(-t / tau)
            val sampleVal = (mixedWave * envelope * volume * Short.MAX_VALUE).toInt()
            samples[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Helper to write PCM data into an AudioTrack and play it asynchronously.
     */
    private fun playPcm(pcm: ShortArray) {
        if (pcm.isEmpty()) return
        synthScope.launch {
            try {
                val bufferSize = pcm.size * 2
                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STATIC
                )
                audioTrack.write(pcm, 0, pcm.size)
                audioTrack.play()
                
                // Keep the track alive until playback finishes, then release it
                val durationMs = (pcm.size * 1000L) / SAMPLE_RATE
                delay(durationMs + 100)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Plays a single plucked note.
     */
    fun playNote(frequency: Float, durationMs: Int, volume: Float = 0.5f) {
        val pcm = generatePluckPcm(frequency, durationMs, volume)
        playPcm(pcm)
    }

    /**
     * Plays a pleasant arpeggiated chime when a player makes a choice.
     */
    fun playChoiceSound() {
        synthScope.launch {
            // E4 followed by B4 (ascending perfect fifth - very rewarding and elegant)
            playNote(329.63f, 400, 0.4f)
            delay(100)
            playNote(493.88f, 500, 0.4f)
        }
    }

    /**
     * Plays a sparkling golden arpeggio representing Athena's favor.
     */
    fun playDivineFavor() {
        synthScope.launch {
            val notes = listOf(392.00f, 493.88f, 587.33f, 659.25f) // G4, B4, D5, E5
            for (note in notes) {
                playNote(note, 600, 0.35f)
                delay(120)
            }
        }
    }

    /**
     * Plays a low, ominous dissonant crash/booming chord for Poseidon's wrath.
     */
    fun playDivineWrath() {
        synthScope.launch {
            // Ominous low bass tones mixed together
            val frequencies = listOf(82.41f, 110.00f, 116.54f) // E2, A2, A#2 (Dissonant tritone)
            val durationMs = 1200
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)
            val tau = durationMs / 1000.0 / 3.0

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // Heavy waves
                val wave = sin(2.0 * PI * frequencies[0] * t) +
                           sin(2.0 * PI * frequencies[1] * t) +
                           0.8 * sin(2.0 * PI * frequencies[2] * t)
                val envelope = exp(-t / tau)
                val sampleVal = ((wave / 2.8) * envelope * 0.6f * Short.MAX_VALUE).toInt()
                samples[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(samples)
        }
    }

    /**
     * Plays a triumphant ancient fan-fare chord for victory.
     */
    fun playVictoryHymn() {
        synthScope.launch {
            val chords = listOf(
                listOf(261.63f, 329.63f, 392.00f), // C major chord
                listOf(329.63f, 415.30f, 493.88f), // E major chord
                listOf(440.00f, 554.37f, 659.25f)  // A major chord
            )
            for (chord in chords) {
                for (note in chord) {
                    playNote(note, 1000, 0.25f)
                }
                delay(300)
            }
        }
    }

    /**
     * Starts playing soft, looping, generative background music.
     * Generates beautiful slow ancient Greek Phrygian mode melodies on a soft lyre.
     */
    fun startEpicGreekBackgroundMusic() {
        if (bgMusicJob != null) return // Already running

        bgMusicJob = synthScope.launch {
            while (true) {
                if (!isMusicMuted) {
                    // Choose a beautiful, ambient sequence of notes from the Greek scale
                    val noteCount = (3..6).random()
                    val melody = List(noteCount) { GREEK_SCALE.random() }
                    
                    for (note in melody) {
                        if (isMusicMuted) break
                        // Play a very soft, ambient pluck that decays slowly
                        playNote(note, 2500, 0.12f)
                        // Slowly stagger notes for a serene, mystical atmosphere
                        delay((800..1500).random().toLong())
                    }
                }
                // Ambient silence between phrases
                delay((2000..4000).random().toLong())
            }
        }
    }

    fun toggleMuteMusic() {
        isMusicMuted = !isMusicMuted
    }

    fun isMuted(): Boolean = isMusicMuted

    fun stopMusic() {
        bgMusicJob?.cancel()
        bgMusicJob = null
    }
}
