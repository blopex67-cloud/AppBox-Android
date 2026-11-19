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
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var lastDownloadId: Long = -1L
    private var installAfterSettings: Uri? = null

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
        window.statusBarColor = android.graphics.Color.rgb(16, 19, 27)
        window.navigationBarColor = android.graphics.Color.rgb(16, 19, 27)
        registerDownloadReceiver()
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
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
        setContentView(webView)
        if (savedInstanceState == null) {
            webView.loadUrl("https://blopex67-cloud.github.io/AppBox-Android/")
        } else {
            webView.restoreState(savedInstanceState)
        }
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
