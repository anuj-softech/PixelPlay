package com.rock.pixelplay.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.media3.common.TrackSelectionOverride
import androidx.recyclerview.widget.RecyclerView
import com.rock.pixelplay.R

data class TrackItem(
    val title: String,
    val subtitle: String,
    val override: TrackSelectionOverride?
)

class TrackAdapter(
    private val tracks: List<TrackItem>,
    private val selectedIndex: Int,
    private val onCaptionClick: (TrackSelectionOverride?) -> Unit
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TrackViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
    )

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val item = tracks[position]
        holder.titleView.text = item.title
        holder.subtitleView.text = item.subtitle
        holder.radioButton.isChecked = position == selectedIndex
        holder.itemView.setOnClickListener { onCaptionClick(item.override) }
    }

    override fun getItemCount() = tracks.size

    class TrackViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(R.id.trackTitle)
        val subtitleView: TextView = view.findViewById(R.id.trackSubtitle)
        val radioButton: RadioButton = view.findViewById(R.id.selectCheck)
    }
}