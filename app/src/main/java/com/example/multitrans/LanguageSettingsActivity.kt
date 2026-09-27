package com.example.multitrans

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LanguageSettingsActivity : AppCompatActivity() {

    private lateinit var adapter: LanguageAdapter
    private lateinit var preferenceManager: LanguagePreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_settings)

        preferenceManager = LanguagePreferenceManager(this)
        val preferredCodes = preferenceManager.getPreferredLanguages()

        val allLanguages = LanguageData.allLanguages.map { lang ->
            lang.copy(isSelected = preferredCodes.contains(lang.code))
        }.toMutableList()

        // Sort: selected ones first (in preferred order), then others
        val sortedList = mutableListOf<LanguageModel>()
        preferredCodes.forEach { code ->
            allLanguages.find { it.code == code }?.let { sortedList.add(it) }
        }
        allLanguages.forEach { lang ->
            if (!preferredCodes.contains(lang.code)) sortedList.add(lang)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_languages)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })

        adapter = LanguageAdapter(sortedList) { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }

        recyclerView.adapter = adapter
        itemTouchHelper.attachToRecyclerView(recyclerView)

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            preferenceManager.savePreferredLanguages(adapter.getSelectedLanguageCodes())
            setResult(RESULT_OK)
            finish()
        }
    }
}
