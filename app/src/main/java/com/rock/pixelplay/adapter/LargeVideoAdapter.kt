package com.rock.pixelplay.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rock.pixelplay.R
import com.rock.pixelplay.databinding.ViewLargeVideoBinding
import com.rock.pixelplay.helper.VideoUtils
import com.rock.pixelplay.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LargeVideoAdapter(
    private val context: Context, private val videoList: List<VideoItem>
) : RecyclerView.Adapter<LargeVideoAdapter.VideoViewHolder>() {

    val videoUtils = VideoUtils();

    inner class VideoViewHolder(val binding: ViewLargeVideoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding =
            ViewLargeVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoItem = videoList[position]
        holder.binding.title.text = videoItem.title
        holder.binding.duration.text = "${videoItem.lastPlayed} / ${videoItem.duration}"

        CoroutineScope(Dispatchers.IO).launch {
            val thumbnailBitmap = videoUtils.getVideoThumbnail(videoItem.thumbnail)
            withContext(Dispatchers.Main) {
                holder.binding.thumbnail.setImageBitmap(thumbnailBitmap)
                Glide.with(context).load(thumbnailBitmap).placeholder(R.drawable.placeholder).into(holder.binding.thumbnail)
            }
        }

        holder.binding.root.setOnClickListener {
            videoUtils.playInApp(context, videoItem)
        }
        holder.binding.continueBtn.setOnClickListener {
            videoUtils.playInApp(context, videoItem)
        }

        holder.binding.progressBar.post {
            val parentWidth = (holder.binding.progressBar.parent as View).width
            val newWidth = parentWidth * videoItem.playedPercentage.toInt() / 100
            holder.binding.progressBar.layoutParams.width = newWidth
            holder.binding.progressBar.requestLayout()
        }
    }

    override fun getItemCount() = videoList.size


}