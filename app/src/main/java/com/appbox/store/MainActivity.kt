package com.appbox.store

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.DownloadListener
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.graphics.Color
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var lastDownloadId: Long = -1L
    private var installAfterSettings: Uri? = null
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var loadingOverlay: View? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val fileChooserRequestCode = 3021
    private val hideLoading = Runnable { loadingOverlay?.animate()?.alpha(0f)?.setDuration(220)?.withEndAction { loadingOverlay?.visibility = View.GONE } }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != fileChooserRequestCode) return
        val callback = fileChooserCallback ?: return
        fileChooserCallback = null
        val uris = if (resultCode == RESULT_OK && data != null) {
            val clip = data.clipData
            if (clip != null && clip.itemCount > 0) Array(clip.itemCount) { index -> clip.getItemAt(index).uri }
            else data.data?.let { arrayOf(it) }
        } else null
        callback.onReceiveValue(uris)
    }

    private fun createLoadingOverlay(): View {
        val frame = FrameLayout(this).apply { setBackgroundColor(Color.rgb(18, 18, 18)); alpha = 1f }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(24, 24, 24, 24) }
        val icon = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(30, 30, 30)); cornerRadius = 24f }
            clipToOutline = true
        }
        content.addView(icon, LinearLayout.LayoutParams(dp(86), dp(86)))
        val title = TextView(this).apply { text = "AppBox"; setTextColor(Color.WHITE); textSize = 18f; gravity = Gravity.CENTER; setPadding(0, dp(17), 0, dp(19)) }
        content.addView(title)
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            progressTintList = ColorStateList.valueOf(Color.rgb(168, 199, 250))
            indeterminateTintList = ColorStateList.valueOf(Color.rgb(168, 199, 250))
            max = 100
        }
        content.addView(progress, LinearLayout.LayoutParams(dp(112), dp(3)))
        frame.addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        return frame
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id != lastDownloadId) return
            val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val uri = manager.getUriForDownloadedFile(id)
            if (uri == null) {
                Toast.makeText(this@MainActivity, "No se pudo abrir el APK descargado.", Toast.LENGTH_LONG).show()
                return
            }
            installApk(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(18, 18, 18)
        window.navigationBarColor = android.graphics.Color.rgb(18, 18, 18)
        registerDownloadReceiver()
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        webView.clearCache(true)
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        val root = FrameLayout(this)
        root.addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        loadingOverlay = createLoadingOverlay()
        root.addView(loadingOverlay, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                mainHandler.removeCallbacks(hideLoading)
                loadingOverlay?.apply { alpha = 1f; visibility = View.VISIBLE }
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                mainHandler.postDelayed(hideLoading, 650)
            }
            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) mainHandler.postDelayed(hideLoading, 300)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.fileChooserCallback?.onReceiveValue(null)
                this@MainActivity.fileChooserCallback = filePathCallback ?: return false
                val intent = try {
                    fileChooserParams?.createIntent()?.apply {
                        if (fileChooserParams?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    } ?: Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/vnd.android.package-archive", "image/png", "image/jpeg", "image/webp", "video/mp4", "video/webm"))
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                } catch (_: Exception) {
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/vnd.android.package-archive", "image/png", "image/jpeg", "image/webp", "video/mp4", "video/webm"))
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                }
                return try {
                    startActivityForResult(intent, fileChooserRequestCode)
                    true
                } catch (_: Exception) {
                    this@MainActivity.fileChooserCallback?.onReceiveValue(null)
                    this@MainActivity.fileChooserCallback = null
                    false
                }
            }
        }
        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (!url.lowercase().contains(".apk")) {
                Toast.makeText(this, "Solo se descargan instaladores APK desde la tienda.", Toast.LENGTH_SHORT).show()
                return@DownloadListener
            }
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
                request.setTitle("Descargando aplicación")
                request.setDescription("El instalador de Android se abrirá al terminar.")
                request.setMimeType("application/vnd.android.package-archive")
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkFilename(url, contentDisposition))
                lastDownloadId = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(this, "Descarga iniciada. Android te pedirá confirmar la instalación.", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(this, "No se pudo iniciar la descarga. Revisa tu conexión.", Toast.LENGTH_LONG).show()
            }
        })
        setContentView(root)
        if (savedInstanceState == null) {
            webView.loadUrl("https://blopex67-cloud.github.io/AppBox-Android/")
        } else {
            webView.restoreState(savedInstanceState)
            mainHandler.postDelayed(hideLoading, 900)
        }
        checkForAppUpdates()
    }

    private fun checkForAppUpdates() {
        Thread {
            try {
                val connection = (java.net.URL("https://api.github.com/repos/blopex67-cloud/AppBox-Android/releases?per_page=100").openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "AppBox-Android")
                }
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                val releases = org.json.JSONArray(response)
                var bestVersion: String? = null
                var bestUrl: String? = null
                for (i in 0 until releases.length()) {
                    val release = releases.getJSONObject(i)
                    if (release.optBoolean("draft") || release.optBoolean("prerelease")) continue
                    val tag = release.optString("tag_name")
                    if (!tag.startsWith("appbox-v")) continue
                    val version = tag.removePrefix("appbox-v")
                    if (!isNewerVersion(version, bestVersion ?: "0.0.0")) continue
                    val assets = release.optJSONArray("assets") ?: continue
                    val apk = "AppBox-v$version.apk"
                    var url: String? = null
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        if (asset.optString("name") == apk) { url = asset.optString("browser_download_url"); break }
                    }
                    if (url != null) { bestVersion = version; bestUrl = url }
                }
                val installed = packageManager.getPackageInfo(packageName, 0).versionName ?: "0.0.0"
                val availableVersion = bestVersion
                val downloadUrl = bestUrl
                if (availableVersion != null && downloadUrl != null && isNewerVersion(availableVersion, installed)) {
                    mainHandler.post { showAppUpdateDialog(availableVersion, downloadUrl) }
                }
            } catch (error: Exception) {
                android.util.Log.w("AppBoxUpdater", "No se pudo revisar actualizaciones", error)
            }
        }.start()
    }

    private fun isNewerVersion(candidate: String, current: String): Boolean {
        val a = Regex("\\d+").findAll(candidate).mapNotNull { it.value.toLongOrNull() }.toList()
        val b = Regex("\\d+").findAll(current).mapNotNull { it.value.toLongOrNull() }.toList()
        for (i in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrElse(i) { 0L }
            val right = b.getOrElse(i) { 0L }
            if (left != right) return left > right
        }
        return false
    }

    private fun showAppUpdateDialog(version: String, url: String) {
        if (isFinishing || isDestroyed) return
        android.app.AlertDialog.Builder(this)
            .setTitle("AppBox $version disponible")
            .setMessage("Descarga la actualización. Al terminar, Android te pedirá confirmar la instalación.")
            .setNegativeButton("Ahora no", null)
            .setPositiveButton("Descargar") { _, _ ->
                try {
                    val request = DownloadManager.Request(Uri.parse(url))
                        .setTitle("Actualización de AppBox")
                        .setDescription("Descargando AppBox $version")
                        .setMimeType("application/vnd.android.package-archive")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AppBox-v$version.apk")
                    lastDownloadId = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                    Toast.makeText(this, "Actualización descargándose. Android te pedirá confirmar la instalación.", Toast.LENGTH_LONG).show()
                } catch (_: Exception) {
                    Toast.makeText(this, "No se pudo iniciar la descarga de AppBox.", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    private fun apkFilename(url: String, disposition: String?): String {
        val fromHeader = disposition?.substringAfter("filename=", "")?.trim('"', '\'')
        val fromUrl = Uri.parse(url).lastPathSegment
        val candidate = (fromHeader?.takeIf { it.endsWith(".apk", true) } ?: fromUrl ?: "AppBox-download.apk")
        return candidate.substringAfterLast('/').filter { it.isLetterOrDigit() || it in "._-" }.ifBlank { "application.apk" }
    }

    private fun registerDownloadReceiver() {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(downloadReceiver, filter)
    }

    private fun installApk(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            installAfterSettings = uri
            Toast.makeText(this, "Permite a AppBox instalar el APK para continuar.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            return
        }
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { startActivity(install) }
        catch (_: Exception) { Toast.makeText(this, "Abre el APK desde Descargas para instalarlo.", Toast.LENGTH_LONG).show() }
    }

    override fun onResume() {
        super.onResume()
        val uri = installAfterSettings
        if (uri != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls())) {
            installAfterSettings = null
            installApk(uri)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        try { unregisterReceiver(downloadReceiver) } catch (_: Exception) {}
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
