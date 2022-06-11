package com.hknk.mctrainer

import android.content.Intent
import android.content.res.Resources
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.TypedValue
import android.view.Display
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : AppCompatActivity() {
    var handler: Handler = Handler()
    private lateinit var answerBox: EditText

    private var speed = 13
    private var freq = 800.0
    private var code = ArrayList<Int>()
    private var words = ArrayList<String>()

    private lateinit var currentWord: String

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private val activityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        loadSettings()
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

        loadSettings()
        loadWords()
        nextWord()

        answerBox = findViewById(R.id.answer_box)
        findViewById<Button>(R.id.playBtn).setOnClickListener {
            playWord()
        }
        val submitBtn = findViewById<Button>(R.id.submitBtn)
        submitBtn.setOnClickListener {
            submit()
        }
        submitBtn.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> submitBtn.setPadding(
                    submitBtn.paddingLeft,
                    5.toPx.toInt(),
                    submitBtn.paddingRight,
                    submitBtn.paddingBottom
                )
                MotionEvent.ACTION_MOVE ->
                    if (event.x > submitBtn.width || event.x < 0 ||
                        event.y < 0 || event.y > submitBtn.height
                    ) {
                        submitBtn.setPadding(
                            submitBtn.paddingLeft,
                            0,
                            submitBtn.paddingRight,
                            submitBtn.paddingBottom
                        )
                    }
                MotionEvent.ACTION_UP -> submitBtn.setPadding(
                    submitBtn.paddingLeft,
                    0,
                    submitBtn.paddingRight,
                    submitBtn.paddingBottom
                )
            }
            return@setOnTouchListener false
        }
    }

    val Number.toPx
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        )

    private fun submit() {
        val answer = answerBox.text.toString()
        if (answer.lowercase() == currentWord.lowercase()) {
            Toast.makeText(this, "Correct", Toast.LENGTH_SHORT).show()
            nextWord()
            answerBox.setText("")
            playWord()
        }
    }

    private fun nextWord() {
        currentWord = words[random(words.size - 1)]
    }

    private fun random(to: Int): Int {
        val random = Random()
        return random.nextInt(to)
    }

    private fun loadSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        speed = prefs.getInt("speed", 5)
        freq = prefs.getString("frequency", "800")?.toDoubleOrNull()!!
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

    private fun playWord() {
        val thread = Thread {
            handler.post {
                code = ArrayList()
                generateCodeFromText(
                    currentWord.lowercase(Locale.getDefault())
                        .toCharArray()
                )
                play(freq, code)
            }
        }
        thread.start()
    }

    private fun play(freqOfTone: Double, bits: List<Int>) {
        val sampleRate = 8000
        val duration = bits.size * 1.0 / speed
        val numSamples = (sampleRate * duration).roundToInt()
        val samples = FloatArray(numSamples)
        val generated = ByteArray(2 * numSamples)

        val waveLength = sampleRate / freqOfTone
        for (i in 0 until numSamples) {
            val index = (i / (sampleRate / speed)).coerceAtMost(bits.size - 1)
            samples[i] = (sin(2 * Math.PI * i / waveLength) * bits[index]).toFloat()
        }
        var idx = 0
        for (dVal in samples) {
            val value: Int = (((dVal * 32767).toInt()))
            generated[idx++] = (value and 0x00ff).toByte()
            generated[idx++] = ((value and 0xff00) shr 8).toByte()
        }
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.ENCODING_PCM_16BIT, generated.size,
            AudioTrack.MODE_STATIC
        )
        audioTrack.write(generated, 0, generated.size)
        audioTrack.play()
    }
}
