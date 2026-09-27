package com.example.multitrans

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.LocaleList
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.*
import kotlinx.coroutines.*
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var editTexts: Map<String, EditText> = emptyMap()
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollView: ScrollView
    private lateinit var llContainer: LinearLayout
    private lateinit var preferenceManager: LanguagePreferenceManager
    
    private var tts: TextToSpeech? = null
    private var translationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var activeDownloads = 0

    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            populateFields()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferenceManager = LanguagePreferenceManager(this)
        progressBar = findViewById(R.id.progress_bar)
        scrollView = findViewById(R.id.main_scroll)
        llContainer = findViewById(R.id.ll_container)

        // Initialize Native Android Text To Speech
        tts = TextToSpeech(this) { status ->
            // Checked implementation confirmation
        }

        findViewById<Button>(R.id.btn_clear).setOnClickListener { clearAll() }
        findViewById<ImageButton>(R.id.btn_settings).setOnClickListener {
            val intent = Intent(this, LanguageSettingsActivity::class.java)
            settingsLauncher.launch(intent)
        }

        populateFields()
        setupKeyboardListener()
    }

    private fun populateFields() {
        llContainer.removeAllViews()
        val newMap = mutableMapOf<String, EditText>()
        val preferredCodes = preferenceManager.getPreferredLanguages()

        for (code in preferredCodes) {
            val langModel = LanguageData.allLanguages.find { it.code == code } ?: continue
            val fieldView = layoutInflater.inflate(R.layout.item_language_field, llContainer, false)
            val tvLabel = fieldView.findViewById<TextView>(R.id.tv_label)
            val etField = fieldView.findViewById<EditText>(R.id.et_field)
            val btnSpeak = fieldView.findViewById<ImageButton>(R.id.btn_speak)

            tvLabel.text = "${langModel.flag} ${langModel.name.uppercase()}"
            etField.imeHintLocales = LocaleList(Locale(code))

            newMap[code] = etField
            llContainer.addView(fieldView)

            btnSpeak.setOnClickListener {
                val utterance = etField.text.toString()
                if (utterance.isNotBlank()) {
                    tts?.language = Locale(code)
                    tts?.speak(utterance, TextToSpeech.QUEUE_FLUSH, null, "${code}_speak")
                }
            }

            etField.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    etField.postDelayed({
                        val rect = Rect()
                        fieldView.getGlobalVisibleRect(rect)
                        scrollView.smoothScrollTo(0, fieldView.top - 100)
                    }, 300)
                }
            }

            etField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (etField.isFocused) debounceTranslation(code, s.toString())
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        editTexts = newMap
    }

    private fun debounceTranslation(sourceLang: String, text: String) {
        translationJob?.cancel()
        translationJob = scope.launch {
            delay(500)
            if (text.isBlank()) {
                clearAll()
                return@launch
            }
            for ((targetLang, targetEditText) in editTexts) {
                if (targetLang != sourceLang) {
                    translateText(text, sourceLang, targetLang, targetEditText)
                }
            }
        }
    }

    private fun translateText(text: String, source: String, target: String, targetView: EditText) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val translator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder().build()

        updateLoading(true)
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                updateLoading(false)
                translator.translate(text)
                    .addOnSuccessListener { result -> 
                        if (!targetView.isFocused) {
                            targetView.setText(result)
                        }
                    }
                    .addOnCompleteListener { translator.close() }
            }
            .addOnFailureListener {
                updateLoading(false)
                translator.close()
            }
    }

    private fun setupKeyboardListener() {
        scrollView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            scrollView.getWindowVisibleDisplayFrame(r)
            val screenHeight = scrollView.rootView.height
            val keypadHeight = screenHeight - r.bottom

            if (keypadHeight < screenHeight * 0.15) {
                currentFocus?.clearFocus()
            }
        }
    }

    private fun updateLoading(isLoading: Boolean) {
        if (isLoading) activeDownloads++ else activeDownloads--
        if (activeDownloads < 0) activeDownloads = 0
        progressBar.visibility = if (activeDownloads > 0) View.VISIBLE else View.GONE
    }

    private fun clearAll() {
        for (view in editTexts.values) {
            view.text.clear()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        translationJob?.cancel()
        scope.cancel()
        tts?.stop()
        tts?.shutdown()
    }
}
