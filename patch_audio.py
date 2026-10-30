from pathlib import Path

p = Path("app/src/main/java/com/aram/mahakama/audio/AppAudioController.kt")
s = p.read_text()

old = """    private var player: MediaPlayer? = null
    private var playerTrackId: String? = null
"""

new = """    private var player: MediaPlayer? = null
    private var playerTrackId: String? = null
    private var loadingPlayer: MediaPlayer? = null
    private var playerLoading = false
    private var playerGeneration = 0L
    private var released = false
"""

if s.count(old) != 1:
    raise SystemExit("توقف: لم يتم العثور على موضع التعديل بدقة.")

p.write_text(s.replace(old, new, 1))
print("نجح الجزء الأول: إضافة حقول حماية تحميل الصوت.")
