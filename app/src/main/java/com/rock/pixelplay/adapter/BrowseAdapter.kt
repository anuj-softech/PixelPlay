package com.rock.pixelplay.adapter

import android.content.Context
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.rock.pixelplay.R
import com.rock.pixelplay.databinding.ViewListFolderBinding
import com.rock.pixelplay.databinding.ViewListVideoBinding
import com.rock.pixelplay.helper.DiskBitmapCache
import com.rock.pixelplay.helper.VideoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BrowseAdapter(
    private val context : Context,
    private val folders: List<File>,
    private val videos: List<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_FOLDER = 0
    private val VIEW_TYPE_VIDEO = 1

    override fun getItemCount(): Int = folders.size + videos.size

    override fun getItemViewType(position: Int): Int {
        return if (position < folders.size) VIEW_TYPE_FOLDER else VIEW_TYPE_VIDEO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_FOLDER) {
            val binding = ViewListFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            FolderViewHolder(binding)
        } else {
            val binding = ViewListVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            VideoViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FolderViewHolder) {
            val file = folders[position]
            holder.bind(file)
        } else if (holder is VideoViewHolder) {
            val file = videos[position - folders.size]
            holder.bind(file)
        }
    }

    inner class FolderViewHolder(private val b: ViewListFolderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(file: File) {
            b.title.text = file.name
            b.count.text = "${countVideosInPath(file.path)} Items"

            b.root.setOnClickListener { onItemClick(file) }
        }
    }

    inner class VideoViewHolder(private val b: ViewListVideoBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(file: File) {
            b.title.text = file.name
            b.metadata.text = "${formatDuration(file)} ${file.length() / 1024 / 1024} Mb"
            b.root.setOnClickListener { onItemClick(file) }
            b.thumbnail.setImageResource(R.drawable.placeholder)

            // Offload thumbnail loading to background thread
            val uri = file.path.toUri().toString()
            val cached = DiskBitmapCache.get(uri)
            if (cached != null) {
                b.thumbnail.setImageBitmap(cached)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    val thumb = VideoUtils().getVideoThumbnail(uri)
                    DiskBitmapCache.put(uri, thumb)
                    withContext(Dispatchers.Main) {
                        b.thumbnail.setImageBitmap(thumb)
                    }
                }
            }

        }


        private fun formatDuration(file: File): String {
            // placeholder: use media extractor if needed
            return VideoUtils().getVideoDuration(file.absolutePath)
        }
    }
    private fun countVideosInPath(path: String): Int {
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Video.Media._ID)
        val selection = "${MediaStore.Video.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("$path%")

        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        val count = cursor?.count ?: 0
        cursor?.close()
        return count
    }

}
