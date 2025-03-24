package com.rock.pixelplay.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.rock.pixelplay.databinding.ViewSmallVideoBinding
import com.rock.pixelplay.helper.VideoUtils
import com.rock.pixelplay.model.VideoItem

class LargeVideoAdapter(
    private val context: Context, private val videoList: List<VideoItem>
) : RecyclerView.Adapter<LargeVideoAdapter.VideoViewHolder>() {

    val videoUtils = VideoUtils();

    inner class VideoViewHolder(val binding: ViewSmallVideoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding =
            ViewSmallVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoItem = videoList[position]
        holder.binding.title.text = videoItem.title
        holder.binding.duration.text = videoItem.duration
        val uri = videoItem.thumbnail.toUri()
        holder.binding.thumbnail.setImageBitmap(videoUtils.getVideoThumbnail(uri.toString()))
        holder.binding.root.setOnClickListener {
            videoUtils.playInApp(context,videoItem);
        }
    }

    override fun getItemCount() = videoList.size


}