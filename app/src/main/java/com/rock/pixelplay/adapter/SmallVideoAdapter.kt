package com.rock.pixelplay.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.rock.pixelplay.R
import com.rock.pixelplay.databinding.ViewSmallVideoBinding
import com.rock.pixelplay.helper.DiskBitmapCache
import com.rock.pixelplay.helper.VideoUtils
import com.rock.pixelplay.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmallVideoAdapter(
    private val context: Context, private val videoList: List<VideoItem>
) : RecyclerView.Adapter<SmallVideoAdapter.VideoViewHolder>() {

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

        // Show placeholder while loading
        val imageView = holder.binding.thumbnail
        val uri = videoItem.thumbnail.toUri().toString()

        imageView.setImageResource(R.drawable.placeholder)
        imageView.tag = uri  // Use thumbnail URI for precise tagging

        val cached = DiskBitmapCache.get(uri)
        if (cached != null) {
            if (imageView.tag == uri)
                imageView.setImageBitmap(cached)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val thumb = videoUtils.getVideoThumbnail(uri)
                DiskBitmapCache.put(uri, thumb)
                withContext(Dispatchers.Main) {
                    if (imageView.tag == uri)
                        imageView.setImageBitmap(thumb)
                }
            }
        }
        holder.binding.root.setOnClickListener {
            videoUtils.playInApp(context, videoItem)
        }
    }

    override fun getItemCount() = videoList.size


}