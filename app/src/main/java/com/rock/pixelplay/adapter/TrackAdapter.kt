package com.rock.pixelplay.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.media3.common.TrackSelectionOverride
import androidx.recyclerview.widget.RecyclerView

class TrackAdapter(
    private val tracks: List<Pair<String, TrackSelectionOverride?>>,
    private val selectedIndex: Int,
    private val onCaptionClick: (TrackSelectionOverride?) -> Unit
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TrackViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_single_choice, parent, false)
    )

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.textView.text = tracks[position].first
        holder.itemView.isSelected = position == selectedIndex
        if (position == selectedIndex) {
            holder.textView.isChecked = true
        }
        holder.itemView.setOnClickListener { onCaptionClick(tracks[position].second) }
    }

    override fun getItemCount() = tracks.size

    class TrackViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: CheckedTextView = view.findViewById(android.R.id.text1)
    }
}