package com.hknk.mctrainer

import android.content.Intent
import android.media.*
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : AppCompatActivity() {
    private lateinit var answerBox: EditText
    private lateinit var seekBar: SeekBar

    private val sampleRate = 8000
    private var speed = 13
    private var freq = 800.0
    private var maxWordLength = 25
    private var code = ArrayList<Int>()
    private var words = ArrayList<String>()
    private var filteredWords = ArrayList<String>()

    private var generated = ByteArray(0)
    private lateinit var currentWord: String
    private var isPlaying = false
    private var totalTime = 0L
    private var lastPlaybackPosition = 0

    private val activityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        loadSettings()
        generateAudio()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings_option -> {
                val intent = Intent(this, SettingsActivity::class.java)
                activityLauncher.launch(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadWords()
        loadSettings()
        nextWord()

        answerBox = findViewById(R.id.answer_box)
        seekBar = findViewById(R.id.seekbar)

        findViewById<Button>(R.id.playBtn).setOnClickListener {
            if (isPlaying) {
                pause()
            } else {
                if (seekBar.progress in 1..99) {
                    continueAudio()
                } else {
                    play()
                }
            }
        }
        val submitBtn = findViewById<Button>(R.id.submitBtn)
        submitBtn.setOnClickListener {
            submit()
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    val newValue = generated.size * seekBar.progress / 100
                    lastPlaybackPosition = newValue
                    audioTrack?.playbackHeadPosition = newValue
                }
            }
        })
    }

    private fun submit() {
        val answer = answerBox.text.toString()
        if (answer.lowercase() == currentWord.lowercase()) {
            Toast.makeText(this, "Correct", Toast.LENGTH_SHORT).show()
            nextWord()
            answerBox.setText("")
            play()
        }
    }

    private fun nextWord() {
        currentWord = filteredWords[random(filteredWords.size - 1)]
        generateAudio()
    }

    private fun generateAudio() {
        code = ArrayList()
        generateCodeFromText(
            currentWord.lowercase(Locale.getDefault())
                .toCharArray()
        )

        val duration = code.size * 1.0 / speed
        val numSamples = (sampleRate * duration).roundToInt()
        val samples = FloatArray(numSamples)
        generated = ByteArray(2 * numSamples)

        val waveLength = sampleRate / freq
        for (i in 0 until numSamples) {
            val index = (i / (sampleRate / speed)).coerceAtMost(code.size - 1)
            samples[i] = (sin(2 * Math.PI * i / waveLength) * code[index]).toFloat()
        }
        var idx = 0
        for (dVal in samples) {
            val value: Int = (((dVal * 32767).toInt()))
            generated[idx++] = (value and 0x00ff).toByte()
            generated[idx++] = ((value and 0xff00) shr 8).toByte()
        }
        totalTime = (duration * 10).toLong()
    }

    private fun random(to: Int): Int {
        val random = Random()
        return random.nextInt(to)
    }

    private fun loadSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        speed = prefs.getInt("speed", 5)
        freq = prefs.getString("frequency", "800")?.toDoubleOrNull()!!
        maxWordLength = prefs.getInt("max_word_length", 25)

        filterWords()
    }

    private fun filterWords() {
        filteredWords = ArrayList()
        for (w in words) {
            if (w.length <= maxWordLength) {
                filteredWords.add(w)
            }
        }
    }

    private fun loadWords() {
        val ins = assets.open("words.txt")
        val reader = BufferedReader(InputStreamReader(ins))
        var str: String? = reader.readLine()
        while (str != null) {
            words.add(str)
            str = reader.readLine()
        }
    }

    private fun generateCodeFromText(charArray: CharArray) {
        val json = mapOf(
            'a' to "._",
            'b' to "_...",
            'c' to "_._.",
            'd' to "_..",
            'e' to ".",
            'f' to ".._.",
            'g' to "__.",
            'h' to "....",
            'i' to "..",
            'j' to ".___",
            'k' to "_._",
            'l' to "._..",
            'm' to "__",
            'n' to "_.",
            'o' to "___",
            'p' to ".__.",
            'q' to "__._",
            'r' to "._.",
            's' to "...",
            't' to "_",
            'u' to ".._",
            'v' to "..._",
            'w' to ".__",
            'x' to "_.._",
            'y' to "_.__",
            'z' to "__..",
            ' ' to " "
        )
        for (c in charArray) {
            val exp = json[c]?.toCharArray()
            if (exp != null) {
                for (e in exp) {
                    when (e) {
                        '.' -> addDot()
                        '_' -> addDash()
                        else -> addSpace()
                    }
                }
            }
            addSpace()
        }
    }

    private fun addDot() {
        code.add(0)
        code.add(1)
    }

    private fun addSpace() {
        code.add(0)
        code.add(0)
    }

    private fun addDash() {
        code.add(0)
        code.add(1)
        code.add(1)
        code.add(1)
    }

    private var timer: Timer? = null
    private var audioTrack: AudioTrack? = null

    private fun pause() {
        if (audioTrack != null) {
            lastPlaybackPosition = audioTrack?.playbackHeadPosition!!
            audioTrack?.pause()
            timer?.cancel()
        }
        isPlaying = false
    }

    private fun continueAudio() {
        if (audioTrack != null) {
            isPlaying = true
            audioTrack?.play()
            startTimer(totalTime * (1 - seekBar.progress / 100))
        }
    }

    private fun play() {
//        val mp = MediaPlayer()
//        val file = File(filesDir.absolutePath + "/tmp.wav")
//        mp.setDataSource(FileInputStream(file).fd)
//        mp.prepare()
//        mp.start()
        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            generated.size,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack?.write(generated, 0, generated.size)
        isPlaying = true
        audioTrack?.play()


        seekBar.progress = 0
        startTimer(totalTime)
    }

    private fun startTimer(delay: Long) {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (seekBar.progress < 100) {
                        seekBar.progress = seekBar.progress + 1
                    } else {
                        endAudio()
                    }
                }
            }
        }, 0, delay)
    }

    private fun endAudio() {
        seekBar.progress = 0
        isPlaying = false
        audioTrack?.stop()
        audioTrack = null
        timer?.cancel()
        timer = null
    }
}
