package com.rock.pixelplay.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.rock.pixelplay.R
import com.rock.pixelplay.adapter.BrowseAdapter
import com.rock.pixelplay.databinding.ActivityBrowseBinding
import com.rock.pixelplay.helper.VideoUtils
import com.rock.pixelplay.model.VideoItem
import java.io.File

class BrowseActivity : AppCompatActivity() {
    private lateinit var lb: ActivityBrowseBinding

    var isAtHome = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lb = ActivityBrowseBinding.inflate(layoutInflater)
        setContentView(lb.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        lb.backButton.setOnClickListener { finish() }
        lb.searchBtn.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        lb.backpath.setOnClickListener {
            showFolders();
         /*
            if (lb.pathText.text.toString()
                    .equals(Environment.getExternalStorageDirectory().absolutePath)
            ) {
                Toast.makeText(this, "Already at root", Toast.LENGTH_SHORT).show()
            } else {
                val path = lb.pathText.text.toString()
                val rPath = path.substring(0, path.lastIndexOf("/"))
                showFiles(rPath)
            }*/
        }
        showFolders();
    }

    @SuppressLint("SuspiciousIndentation")
    private fun showFolders() {
        isAtHome = true
        val path = intent.getStringExtra("path") ?: Environment.getExternalStorageDirectory().absolutePath
        if (path == null) return
        lb.pathText.setText(path)
        var dir = File(path)
        val folders = mutableSetOf<File>()

        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val selection = "${MediaStore.Video.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("$path%")
        val sortOrder = null

        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            while (cursor.moveToNext()) {
                val filePath = cursor.getString(dataIndex)
                val parent = File(filePath).parentFile
                if (parent != null && parent.exists()) {
                    if(parent.path != dir.path)
                    folders.add(parent)
                }
            }
        }

        val folderList = folders.toList().sortedBy { it.name.lowercase() }
        if(folderList.size == 0){
            Toast.makeText(this, "No folders or videos found", Toast.LENGTH_SHORT).show()
        }
        val videos = mutableListOf<File>();
        var files = dir.listFiles();
        files?.forEach {
            if (it.extension in listOf("mp4", "mkv", "avi")) videos.add(it)
        }
        lb.recyclerView.layoutManager = LinearLayoutManager(this)
        lb.recyclerView.adapter = BrowseAdapter(this, folderList, videos) { file ->
            onItemClick(file)
        }

    }

    private fun showFiles(rPath: String) {
        var path = rPath
        lb.pathText.setText(path)
        if (rPath.isNotEmpty()) {
            path = rPath;
            lb.pathText.setText(path)
        }
        isAtHome = false;
        val dir = File(path)
        val files = dir.listFiles()

        val folders = mutableListOf<File>()
        val videos = mutableListOf<File>()

        files?.forEach {
            if (it.isDirectory) folders.add(it)
            else if (it.extension in listOf("mp4", "mkv", "avi")) videos.add(it)
        }
        folders.sortBy { it.name.lowercase() }
        videos.sortBy { it.name.lowercase() }
        lb.recyclerView.layoutManager = LinearLayoutManager(this)
        lb.recyclerView.adapter = BrowseAdapter(this, emptyList(), videos) { file ->
            onItemClick(file)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isAtHome) finish()
                else showFolders()
            }
        })
    }

    fun onItemClick(file: File) {
        if (file.isDirectory) {
            showFiles(file.path)
        } else {
            val title = file.name
            val vpath = file.path
            val dateAdded = file.lastModified()
            val duration = VideoUtils().getVideoDuration(this, vpath) // your util method
            val thumbnail = vpath

            VideoUtils().playInApp(
                this, VideoItem(
                    title = title,
                    path = vpath,
                    dateAdded = dateAdded,
                    duration = duration,
                    thumbnail = thumbnail,
                    lastPlayed = "00:00:00",
                    playedPercentage = 0f
                )
            )
        }
    }
}