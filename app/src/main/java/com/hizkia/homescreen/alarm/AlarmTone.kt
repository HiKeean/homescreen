package com.hizkia.homescreen.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import kotlin.math.PI
import kotlin.math.sign
import kotlin.math.sin

/**
 * Memutar nada alarm lewat stream ALARM (tetap bunyi walau HP di mode senyap/getar).
 * Nada bawaan dibuat di sini: beep cepat nada tinggi berselang-seling yang sengaja melengking.
 * Ini bukan rekaman nada iPhone (hak cipta Apple); user bisa memilih file sendiri.
 */
object AlarmTone {
    private const val SAMPLE_RATE = 44_100
    private var track: AudioTrack? = null
    private var player: MediaPlayer? = null

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun start(context: Context, customUri: String?) {
        stop()
        if (customUri != null && startCustom(context, Uri.parse(customUri))) return
        startBuiltIn()
    }

    fun stop() {
        runCatching { track?.stop() }
        runCatching { track?.release() }
        track = null
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    private fun startCustom(context: Context, uri: Uri): Boolean = runCatching {
        val mp = MediaPlayer()
        mp.setAudioAttributes(attributes)
        mp.setDataSource(context, uri)
        mp.isLooping = true
        mp.setVolume(1f, 1f)
        mp.prepare()
        mp.start()
        player = mp
    }.isSuccess

    private fun startBuiltIn() {
        val samples = buildPattern()
        val bytes = samples.size * 2
        val t = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(bytes)
            .build()
        t.write(samples, 0, samples.size)
        t.setLoopPoints(0, samples.size, -1)
        t.setVolume(1f)
        t.play()
        track = t
    }

    /** Empat beep cepat (dua nada bergantian) lalu jeda singkat, diulang terus. */
    private fun buildPattern(): ShortArray {
        val beepFreqs = doubleArrayOf(2093.0, 2637.0, 2093.0, 2637.0)
        val beepLen = (SAMPLE_RATE * 0.11).toInt()
        val gapLen = (SAMPLE_RATE * 0.05).toInt()
        val restLen = (SAMPLE_RATE * 0.35).toInt()
        val out = ShortArray(beepFreqs.size * (beepLen + gapLen) + restLen)
        var pos = 0
        for (f in beepFreqs) {
            for (i in 0 until beepLen) {
                val phase = 2 * PI * f * i / SAMPLE_RATE
                // Campuran sinus dan kotak supaya tajam; envelope pendek biar tidak "klik".
                val wave = 0.6 * sin(phase) + 0.4 * sign(sin(phase))
                val edge = minOf(i, beepLen - i).coerceAtMost(200) / 200.0
                out[pos++] = (wave * edge * Short.MAX_VALUE * 0.95).toInt().toShort()
            }
            pos += gapLen
        }
        return out
    }
}
