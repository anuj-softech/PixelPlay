package com.rock.pixelplay

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rock.pixelplay.adapter.LargeVideoAdapter
import com.rock.pixelplay.adapter.SmallVideoAdapter
import com.rock.pixelplay.databinding.ActivityMainBinding
import com.rock.pixelplay.helper.HistoryHelper
import com.rock.pixelplay.helper.VideoUtils
import com.rock.pixelplay.model.VideoItem
import com.rock.pixelplay.recyclerview.SpaceItemDecoration
import com.rock.pixelplay.ui.SearchActivity

class MainActivity : AppCompatActivity() {
    lateinit var lb: ActivityMainBinding;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(lb.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        checkPermissionsAndLoadVideos()
        setupNewAdded()
        setupButtons()
    }

    private fun setupButtons() {
        lb.searchBtn.setOnClickListener {
            var intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun checkPermissionsAndLoadVideos() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED -> {
                    setupNewAdded()
                }

                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_VIDEO)
                }
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    setupNewAdded()
                }

                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setupNewAdded()
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

    private fun setupNewAdded() {
        setupHistory()
        val latestVideos = getLatestFiveVideos(this)
        latestVideos.forEach {
            println("Title: ${it.title}, Path: ${it.path}")
        }
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        lb.addedList.continueRv.layoutManager = linearLayoutManager
        lb.addedList.continueRv.adapter = SmallVideoAdapter(this, latestVideos)
        val spaceInPx = resources.getDimensionPixelSize(R.dimen.recycler_item_spacing)
        lb.addedList.continueRv.addItemDecoration(SpaceItemDecoration(spaceInPx))
    }

    override fun onResume() {
        lb.main.postDelayed({ setupHistory() }, 100)
        super.onResume()
    }

    private fun setupHistory() {
        val historyHelper = HistoryHelper(this);
        val history = historyHelper.getHistory()
        if (history.isEmpty()) {
            lb.recents.root.visibility = View.GONE
            return
        }
        val linearLayoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.HORIZONTAL
            initialPrefetchItemCount = 3
        }
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        lb.recents.continueRv.layoutManager = linearLayoutManager
        lb.recents.continueRv.adapter = LargeVideoAdapter(this, history)
        val spaceInPx = resources.getDimensionPixelSize(R.dimen.recycler_item_spacing)
        lb.recents.continueRv.addItemDecoration(SpaceItemDecoration(spaceInPx))

        lb.recents.continueRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollHorizontally(-1)) {
                    if (lb.recents.moreButton.visibility != View.VISIBLE) {
                        lb.recents.moreButton.visibility = View.VISIBLE
                    }
                } else {
                    if (lb.recents.moreButton.isVisible) {

                        lb.recents.moreButton.visibility = View.GONE

                    }
                }
            }
        })
    }


    private fun getLatestFiveVideos(context: Context): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        val contentResolver = context.contentResolver
        val videoUtils = VideoUtils()
        val uri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Video.Media.MIME_TYPE} IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        val selectionArgs = arrayOf(
            "video/x-matroska",  // MKV
            "video/webm",         // WebM
            "video/mp4",          // MP4
            "video/3gpp",         // 3GP
            "video/avi",          // AVI (may need custom support)
            "video/quicktime",    // MOV
            "video/x-flv",        // FLV
            "video/mpeg",         // MPEG
            "video/x-ms-wmv",     // WMV
            "video/x-msvideo",    // AVI
            "video/ogg"           // OGG
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            var count = 0
            while (cursor.moveToNext() && count < 5) {
                val title = cursor.getString(titleColumn)
                val path = cursor.getString(pathColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val duration = videoUtils.getVideoDuration(context, path)
                val thumbnail = path
                videos.add(
                    VideoItem(
                        title,
                        path,
                        dateAdded,
                        duration,
                        thumbnail,
                        lastPlayed = "00:00:00",
                        playedPercentage = 0F
                    )
                )
                count++
            }
        }

        return videos

    }


}