package com.aram.mahakama.audio

/** Pure audio-state policy shared by playback code and unit tests. */
internal object AmbientAudioPolicy {
    const val NORMAL_VOLUME = 0.17f
    const val SPEECH_DUCKED_VOLUME = 0.085f

    fun targetVolume(isForeground: Boolean, isMuted: Boolean, isSpeaking: Boolean): Float = when {
        !isForeground || isMuted -> 0f
        isSpeaking -> SPEECH_DUCKED_VOLUME
        else -> NORMAL_VOLUME
    }

    fun shouldPlay(isForeground: Boolean, isMuted: Boolean): Boolean = isForeground && !isMuted
}
