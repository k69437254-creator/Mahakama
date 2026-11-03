package com.aram.mahakama.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/** Incremental mono PCM16 playback for Supertonic callback chunks. */
internal class StreamingPcmPlayer(
    sampleRate: Int,
    private val onPlaybackComplete: () -> Unit,
    private val onPlaybackError: (Throwable) -> Unit
) {
    companion object {
        private const val TAG = "MahakamaPcmStream"
        private const val QUEUE_CAPACITY = 3
    }

    private val stopped = AtomicBoolean(false)
    private val inputFinished = AtomicBoolean(false)
    private val queue = ArrayBlockingQueue<ShortArray>(QUEUE_CAPACITY)
    private val audioTrack: AudioTrack
    private val playbackThread: Thread

    init {
        require(sampleRate > 0) { "معدل عينة الصوت غير صالح: $sampleRate" }

        val minBufferBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferBytes <= 0) {
            throw IOException("تعذر تحديد حجم مخزن AudioTrack: $minBufferBytes")
        }

        val bufferBytes = maxOf(minBufferBytes * 2, sampleRate)
        val created = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        if (created.state != AudioTrack.STATE_INITIALIZED) {
            try { created.release() } catch (_: Exception) { }
            throw IOException("تعذر تهيئة AudioTrack للبث الصوتي")
        }

        try { created.setVolume(1.0f) } catch (_: Exception) { }
        audioTrack = created
        playbackThread = Thread({ consumeAudio() }, "MahakamaPcmPlayback").apply {
            start()
        }
    }

    /** Copies callback samples into a bounded queue; returns false after stop. */
    fun enqueue(samples: FloatArray): Boolean {
        if (samples.isEmpty()) return true
        if (stopped.get() || inputFinished.get()) return false

        val pcm = ShortArray(samples.size)
        for (index in samples.indices) {
            val sample = samples[index]
            if (!sample.isFinite()) {
                throw IOException("وصلت عينة صوتية غير رقمية إلى مشغل البث")
            }
            pcm[index] = (sample.coerceIn(-1.0f, 1.0f) * 32767.0f)
                .roundToInt()
                .toShort()
        }

        while (!stopped.get() && !inputFinished.get()) {
            try {
                if (queue.offer(pcm, 100, TimeUnit.MILLISECONDS)) return true
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
        return false
    }

    /** Called only after Sherpa has returned from its final callback. */
    fun finishInput() {
        inputFinished.set(true)
    }

    /** Stops immediately; the playback thread releases AudioTrack in its finally block. */
    fun stop() {
        if (!stopped.compareAndSet(false, true)) return
        queue.clear()
        try { audioTrack.pause() } catch (_: Exception) { }
        try { audioTrack.flush() } catch (_: Exception) { }
        playbackThread.interrupt()
    }

    private fun consumeAudio() {
        var started = false
        var writtenFrames = 0L
        var completedNormally = false
        var failure: Throwable? = null

        try {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            } catch (_: SecurityException) {
            }

            while (!stopped.get()) {
                val chunk = queue.poll(100, TimeUnit.MILLISECONDS)
                if (chunk != null) {
                    if (!started) {
                        audioTrack.play()
                        started = true
                        Log.i(TAG, "PCM_STREAM_PLAYBACK_STARTED: sampleRate=${audioTrack.sampleRate}")
                    }

                    var offset = 0
                    while (offset < chunk.size && !stopped.get()) {
                        val written = audioTrack.write(
                            chunk,
                            offset,
                            chunk.size - offset,
                            AudioTrack.WRITE_NON_BLOCKING
                        )
                        when {
                            written > 0 -> {
                                offset += written
                                writtenFrames += written.toLong()
                            }
                            written == 0 -> Thread.sleep(5)
                            else -> throw IOException("فشل AudioTrack.write: $written")
                        }
                    }
                } else if (inputFinished.get() && queue.isEmpty()) {
                    break
                }
            }

            // Do not report completion until AudioTrack has played every written frame.
            if (!stopped.get() && started) {
                while (!stopped.get()) {
                    val playedFrames = audioTrack.playbackHeadPosition.toLong() and 0xFFFFFFFFL
                    if (playedFrames >= writtenFrames) break
                    Thread.sleep(10)
                }
            }
            completedNormally = !stopped.get()
        } catch (error: InterruptedException) {
            if (!stopped.get()) failure = error
        } catch (error: Throwable) {
            if (!stopped.get()) failure = error
        } finally {
            // Close the producer side before releasing the track. This prevents a
            // late callback from adding audio after the playback thread has exited.
            stopped.set(true)
            queue.clear()
            try {
                if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop()
                }
            } catch (_: Exception) {
            }
            try { audioTrack.release() } catch (_: Exception) { }
        }

        when {
            failure != null -> onPlaybackError(failure!!)
            completedNormally -> onPlaybackComplete()
        }
    }
}
