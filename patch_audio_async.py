from pathlib import Path

p = Path("app/src/main/java/com/aram/mahakama/audio/AppAudioController.kt")
s = p.read_text()

release_start = s.index("    @Synchronized\n    fun release() {")
release_end = s.index("\n    override fun onInit", release_start)

new_release = """    @Synchronized
    fun release() {
        released = true
        playerGeneration++
        playerLoading = false

        volumeAnimator?.cancel()
        volumeAnimator = null
        queuedText = null
        finishedCallback = null
        activeUtteranceId = null
        speaking = false

        trackSwitchRunnable?.let { mainHandler.removeCallbacks(it) }
        trackSwitchRunnable = null
        trackSwitchPending = false

        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) { }
        tts = null

        try {
            loadingPlayer?.setOnPreparedListener(null)
            loadingPlayer?.setOnErrorListener(null)
            loadingPlayer?.release()
        } catch (_: Exception) { }
        loadingPlayer = null

        try { player?.release() } catch (_: Exception) { }
        player = null
        playerTrackId = null
        currentMusicVolume = 0f
    }
"""

ensure_start = s.index("    @Synchronized\n    private fun ensurePlayer() {")
ensure_end = s.index("\n    private fun normalizeTrackId", ensure_start)

new_ensure = """    @Synchronized
    private fun ensurePlayer() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { ensurePlayer() }
            return
        }

        if (released || player != null || playerLoading) return

        val trackToLoad = selectedTrackId
        val resourceId = when (trackToLoad) {
            "sin" -> R.raw.track_sin
            "fate" -> R.raw.track_fate
            else -> R.raw.track_death
        }

        val generation = ++playerGeneration
        var next: MediaPlayer? = null
        playerLoading = true

        try {
            next = MediaPlayer()
            loadingPlayer = next

            next.isLooping = true
            next.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            next.setOnPreparedListener { prepared ->
                synchronized(this) {
                    if (released || generation != playerGeneration) {
                        try { prepared.release() } catch (_: Exception) { }
                        if (loadingPlayer === prepared) {
                            loadingPlayer = null
                            playerLoading = false
                        }
                        return@synchronized
                    }

                    if (selectedTrackId != trackToLoad) {
                        try { prepared.release() } catch (_: Exception) { }
                        loadingPlayer = null
                        playerLoading = false
                        ensurePlayer()
                        return@synchronized
                    }

                    loadingPlayer = null
                    playerLoading = false
                    player = prepared
                    playerTrackId = trackToLoad
                    currentMusicVolume = 0f

                    if (AmbientAudioPolicy.shouldPlay(appIsForeground, muted)) {
                        try {
                            if (!prepared.isPlaying) prepared.start()
                        } catch (e: IllegalStateException) {
                            Log.w(TAG, "Ambient audio could not start", e)
                        }
                        fadeMusicTo(
                            ambientTargetVolume(speaking),
                            if (speaking) 520L else 950L
                        )
                    } else {
                        setMusicVolume(0f)
                    }
                }
            }

            next.setOnErrorListener { failed, what, extra ->
                synchronized(this) {
                    if (generation == playerGeneration) {
                        loadingPlayer = null
                        playerLoading = false
                    }
                    try { failed.release() } catch (_: Exception) { }
                    Log.e(TAG, "Ambient audio preparation failed: $what/$extra")
                }
                true
            }

            appContext.resources.openRawResourceFd(resourceId).use { descriptor ->
                next.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            }

            next.prepareAsync()
        } catch (e: Exception) {
            if (generation == playerGeneration) {
                playerLoading = false
                loadingPlayer = null
            }
            try { next?.release() } catch (_: Exception) { }
            Log.e(TAG, "Ambient track could not be loaded", e)
        }
    }
"""

s = s[:release_start] + new_release + s[release_end:]
ensure_start = s.index("    @Synchronized\n    private fun ensurePlayer() {")
ensure_end = s.index("\n    private fun normalizeTrackId", ensure_start)
s = s[:ensure_start] + new_ensure + s[ensure_end:]

p.write_text(s)
print("تم تعديل release() و ensurePlayer() بنجاح.")
