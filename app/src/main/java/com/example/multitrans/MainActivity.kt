package com.example.multitrans

import android.graphics.Rect
import android.os.Bundle
import android.os.LocaleList
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.*
import kotlinx.coroutines.*
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var editTexts: Map<String, EditText>
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollView: ScrollView
    private var translationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var activeDownloads = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progress_bar)
        scrollView = findViewById(R.id.main_scroll)
        findViewById<Button>(R.id.btn_clear).setOnClickListener { clearAll() }

        val map = HashMap<String, EditText>()
        setupField(map, "en", R.id.et_en, "en")
        setupField(map, "fr", R.id.et_fr, "fr")
        setupField(map, "es", R.id.et_es, "es")
        setupField(map, "ca", R.id.et_ca, "ca")
        setupField(map, "el", R.id.et_el, "el")
        setupField(map, "de", R.id.et_de, "de")

        editTexts = map
        setupListeners()
        setupKeyboardListener()
    }

    private fun setupField(map: HashMap<String, EditText>, langCode: String, resId: Int, localeCode: String) {
        findViewById<EditText>(resId)?.let { editText ->
            map[langCode] = editText
            editText.imeHintLocales = LocaleList(Locale(localeCode))

            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    editText.postDelayed({
                        scrollView.smoothScrollTo(0, editText.top - 100)
                    }, 300)
                }
            }
        }
    }

    private fun setupListeners() {
        for ((langCode, editText) in editTexts) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (editText.isFocused) debounceTranslation(langCode, s.toString())
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
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
                if (targetLang != sourceLang) translateText(text, sourceLang, targetLang, targetEditText)
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
                    .addOnSuccessListener { result -> targetView.setText(result) }
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
    }
}