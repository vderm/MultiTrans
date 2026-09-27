package com.example.multitrans

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

class LanguageAdapter(
    private val languages: MutableList<LanguageModel>,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language_setting, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val lang = languages[position]
        holder.tvFlag.text = lang.flag
        holder.tvName.text = lang.name
        holder.checkBox.isChecked = lang.isSelected

        // English is required and cannot be unselected or moved easily, or let user order it
        if (lang.code == "en") {
            holder.checkBox.isEnabled = false
            holder.checkBox.isChecked = true
        } else {
            holder.checkBox.isEnabled = true
        }

        holder.itemView.setOnClickListener {
            if (lang.code != "en") {
                lang.isSelected = !lang.isSelected
                holder.checkBox.isChecked = lang.isSelected
                holder.ivDragHandle.visibility = if (lang.isSelected) View.VISIBLE else View.GONE
            }
        }

        holder.ivDragHandle.visibility = if (lang.isSelected || lang.code == "en") View.VISIBLE else View.GONE

        holder.ivDragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                onStartDrag(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int = languages.size

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(languages, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(languages, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getSelectedLanguageCodes(): List<String> {
        return languages.filter { it.isSelected || it.code == "en" }.map { it.code }
    }

    class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFlag: TextView = itemView.findViewById(R.id.tv_flag)
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val checkBox: CheckBox = itemView.findViewById(R.id.cb_selected)
        val ivDragHandle: ImageView = itemView.findViewById(R.id.iv_drag_handle)
    }
}
