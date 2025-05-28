package com.rock.pixelplay.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.rock.pixelplay.R
import com.rock.pixelplay.adapter.LargeVideoAdapter
import com.rock.pixelplay.adapter.SearchAdapter
import com.rock.pixelplay.databinding.ActivityMainBinding
import com.rock.pixelplay.helper.BrowseUtils
import com.rock.pixelplay.helper.HistoryHelper
import com.rock.pixelplay.helper.onResultInterface
import com.rock.pixelplay.recyclerview.SpaceItemDecoration
import java.net.HttpURLConnection
import java.net.URL


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
        countAllVideos();
        setupExternalStorage()
        val spaceInPx = resources.getDimensionPixelSize(R.dimen.recycler_item_spacing)
        lb.recents.continueRv.addItemDecoration(SpaceItemDecoration(spaceInPx))
        lb.addedList.continueRv.addItemDecoration(SpaceItemDecoration(spaceInPx))
        logNewAppCount()
    }

    private fun countAllVideos() {
        lb.storageGrid.internalBrowse.totalVideos.text = ""
        Thread({
            BrowseUtils(this).countAllVideos(object : onResultInterface {
                override fun onValue(count: Int) {
                    runOnUiThread {
                        lb.storageGrid.internalBrowse.totalVideos.text =
                            count.toString() + " Videos"
                    }
                }
            })
        }).start()

    }

    private fun setupExternalStorage() {
        val extStorageDirs = getExternalFilesDirs(null)
        var externalPath: String? = null

        val internalPath = Environment.getExternalStorageDirectory().absolutePath

        for (file in extStorageDirs) {
            if (file != null && Environment.getExternalStorageState(file) == Environment.MEDIA_MOUNTED && !file.absolutePath.startsWith(
                    internalPath
                )
            ) {
                externalPath = file.absolutePath.substringBefore("/Android")
                break
            }
        }
        val card = lb.storageGrid.externalStorage
        card.title.text = "External Storage"

        if (externalPath != null) {
            val videoCount = BrowseUtils(this).countVideosInPath(externalPath)
            card.totalVideos.text = "$videoCount Videos"
            card.root.alpha = 1f
            card.root.isClickable = true
            card.root.setOnClickListener {
                val i = Intent(this, BrowseActivity::class.java)
                i.putExtra("path", externalPath)
                startActivity(i)
            }
        } else {
            card.totalVideos.text = "Not Connected"
            card.root.alpha = 0.4f
            card.root.isClickable = false
        }
    }

    private fun setupButtons() {
        lb.settingsBtn.setOnClickListener { v ->
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        lb.storageGrid.internalBrowse.root.setOnClickListener { v ->
            startActivity(Intent(this, BrowseActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        lb.searchBtn.setOnClickListener {
            var intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun checkPermissionsAndLoadVideos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED -> {
                    setupNewAdded()
                }

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                }
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    setupNewAdded()
                }

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setupNewAdded()
                setupButtons()
                countAllVideos();
                setupExternalStorage()
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

    private fun setupNewAdded() {
        setupHistory()
        val latestVideos = BrowseUtils(this).getLatestVideos(this)
        latestVideos.forEach {
            println("Title: ${it.title}, Path: ${it.path}")
        }
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        lb.addedList.continueRv.layoutManager = linearLayoutManager
        lb.addedList.continueRv.adapter = SearchAdapter(this, latestVideos)

    }

    private var lastHistoryHash: Int? = null

    override fun onResume() {
        lb.main.postDelayed({ setupHistory() }, 1000)
        super.onResume()
    }

    private fun setupHistory() {
        val historyHelper = HistoryHelper(this)
        val history = historyHelper.getHistory()

        val currentHash = history.hashCode()
        if (currentHash == lastHistoryHash) return // No change

        lastHistoryHash = currentHash

        if (history.isEmpty()) {
            lb.recents.root.visibility = View.GONE
            return
        }

        lb.recents.root.visibility = View.VISIBLE

        val linearLayoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.HORIZONTAL
            initialPrefetchItemCount = 3
        }

        lb.recents.continueRv.layoutManager = linearLayoutManager
        lb.recents.continueRv.adapter = LargeVideoAdapter(this, history)

        val existingSnapHelper = lb.recents.continueRv.onFlingListener
        if (existingSnapHelper != null) {
            lb.recents.continueRv.onFlingListener = null
        }

        lb.recents.continueRv.setHasFixedSize(true)
        val snapHelper: SnapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(lb.recents.continueRv)

        lb.recents.continueRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                lb.recents.moreButton.visibility =
                    if (!recyclerView.canScrollHorizontally(-1)) View.VISIBLE else View.GONE
            }
        })
    }

    fun logNewAppCount() {
        Thread {
            try {
                val url = URL("https://pixelplay-analytics.main-rock-inc.workers.dev/app")
                val resp = (url.openConnection() as HttpURLConnection).run {
                    requestMethod = "GET"
                    inputStream.bufferedReader().readText()
                }
                Log.d("Analytics", "New app count: ${resp}")
            } catch (e: Exception) {
                Log.e("Analytics", "Error fetching app count", e)
            }
        }.start()
    }

}