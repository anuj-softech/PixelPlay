package com.rock.pixelplay.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.rock.pixelplay.R
import com.rock.pixelplay.databinding.ViewListVideoBinding
import com.rock.pixelplay.helper.DiskBitmapCache
import com.rock.pixelplay.helper.VideoUtils
import com.rock.pixelplay.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchAdapter(
    private val context: Context, private val videoList: List<VideoItem>
) : RecyclerView.Adapter<SearchAdapter.VideoViewHolder>() {

    val videoUtils = VideoUtils();

    inner class VideoViewHolder(val binding: ViewListVideoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding =
            ViewListVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoItem = videoList[position]
        holder.binding.title.text = videoItem.title
        holder.binding.metadata.text = videoItem.duration

        val imageView = holder.binding.thumbnail
        val uri = videoItem.thumbnail.toUri().toString()

        // Show placeholder while loading
        imageView.setImageResource(R.drawable.placeholder)
        imageView.tag = uri

        val cached = DiskBitmapCache.get(uri)
        if (cached != null) {
            if (imageView.tag == uri) {
                imageView.setImageBitmap(cached)
            }
        } else {
            // Offload thumbnail loading to background thread
            CoroutineScope(Dispatchers.IO).launch {
                val thumbnail = videoUtils.getVideoThumbnail(uri)
                DiskBitmapCache.put(uri, thumbnail)

                // Update the UI on the main thread
                withContext(Dispatchers.Main) {
                    if (imageView.tag == uri) {
                        imageView.setImageBitmap(thumbnail)
                    }
                }
            }
        }

        holder.binding.root.setOnClickListener {
            videoUtils.playInApp(context, videoItem)
        }
    }

    override fun getItemCount() = videoList.size


}