package com.aram.mahakama.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AmbientAudioPolicyTest {
    @Test
    fun ambientTrackIsSilentWhenMutedOrBackgrounded() {
        assertEquals(0f, AmbientAudioPolicy.targetVolume(true, true, false))
        assertEquals(0f, AmbientAudioPolicy.targetVolume(false, false, false))
        assertEquals(false, AmbientAudioPolicy.shouldPlay(true, true))
        assertEquals(false, AmbientAudioPolicy.shouldPlay(false, false))
    }

    @Test
    fun speechDucksTheSameAmbientTrackInsteadOfReplacingIt() {
        assertEquals(AmbientAudioPolicy.NORMAL_VOLUME, AmbientAudioPolicy.targetVolume(true, false, false))
        assertEquals(AmbientAudioPolicy.SPEECH_DUCKED_VOLUME, AmbientAudioPolicy.targetVolume(true, false, true))
        assertEquals(true, AmbientAudioPolicy.shouldPlay(true, false))
    }
}
