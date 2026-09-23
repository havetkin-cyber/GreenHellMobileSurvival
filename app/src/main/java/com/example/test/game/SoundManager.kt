package com.example.test.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.sin

object SoundManager {
    private const val SAMPLE_RATE = 22050

    fun playChopSound() {
        playSynthesizedSound { generateNoisePulse(durationMs = 120, frequency = 180f, decay = 8f) }
    }

    fun playHitSound() {
        playSynthesizedSound { generateNoisePulse(durationMs = 90, frequency = 300f, decay = 12f) }
    }

    fun playFootstep() {
        playSynthesizedSound { generateNoisePulse(durationMs = 60, frequency = 100f, decay = 15f) }
    }

    fun playDrinkSound() {
        playSynthesizedSound { generateToneSequence(floatArrayOf(400f, 550f, 480f, 620f), stepMs = 60) }
    }

    fun playEatSound() {
        playSynthesizedSound { generateToneSequence(floatArrayOf(250f, 200f, 300f, 180f), stepMs = 70) }
    }

    fun playCraftSound() {
        playSynthesizedSound { generateToneSequence(floatArrayOf(523.25f, 659.25f, 783.99f), stepMs = 100) }
    }

    private fun playSynthesizedSound(bufferGenerator: () -> ByteArray) {
        thread {
            try {
                val pcmData = bufferGenerator()
                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(pcmData.size, minBufferSize)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(pcmData, 0, pcmData.size)
                audioTrack.play()

                Thread.sleep((pcmData.size / (SAMPLE_RATE * 2) * 1000).toLong() + 50)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun generateNoisePulse(durationMs: Int, frequency: Float, decay: Float): ByteArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
        val buffer = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val envelope = Math.exp((-decay * t).toDouble()).toFloat()
            val noise = (Math.random() * 2.0 - 1.0).toFloat()
            val tone = sin(2.0 * Math.PI * frequency * t).toFloat()
            val sample = ((noise * 0.7f + tone * 0.3f) * envelope * 32767).toInt().coerceIn(-32768, 32767)
            
            buffer[i * 2] = (sample and 0xFF).toByte()
            buffer[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    private fun generateToneSequence(frequencies: FloatArray, stepMs: Int): ByteArray {
        val samplesPerStep = (SAMPLE_RATE * (stepMs / 1000f)).toInt()
        val totalSamples = samplesPerStep * frequencies.size
        val buffer = ByteArray(totalSamples * 2)

        var sampleIdx = 0
        for (freq in frequencies) {
            for (i in 0 until samplesPerStep) {
                val t = i.toFloat() / SAMPLE_RATE
                val envelope = sin(Math.PI * i / samplesPerStep).toFloat()
                val tone = sin(2.0 * Math.PI * freq * t).toFloat()
                val sample = (tone * envelope * 28000).toInt().coerceIn(-32768, 32767)

                val bytePos = sampleIdx * 2
                buffer[bytePos] = (sample and 0xFF).toByte()
                buffer[bytePos + 1] = ((sample shr 8) and 0xFF).toByte()
                sampleIdx++
            }
        }
        return buffer
    }
}
