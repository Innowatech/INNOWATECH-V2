package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.TextUtils
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat

object TorchController {
    sealed interface TorchOutcome {
        data object LedOn : TorchOutcome
        data object LedOff : TorchOutcome
        data object FallbackScreenTorch : TorchOutcome
    }

    fun toggleLedTorch(context: Context, currentlyOn: Boolean): TorchOutcome {
        val cm = ContextCompat.getSystemService(context, CameraManager::class.java)
            ?: return TorchOutcome.FallbackScreenTorch
        return try {
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId == null) {
                TorchOutcome.FallbackScreenTorch
            } else {
                val target = !currentlyOn
                cm.setTorchMode(cameraId, target)
                if (target) TorchOutcome.LedOn else TorchOutcome.LedOff
            }
        } catch (_: Exception) {
            TorchOutcome.FallbackScreenTorch
        }
    }

    fun turnOffLedIfAvailable(context: Context) {
        val cm = ContextCompat.getSystemService(context, CameraManager::class.java) ?: return
        try {
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            cm.setTorchMode(cameraId, false)
        } catch (_: Exception) {
        }
    }
}

object SoundAndVibrateHelper {
    fun playTimerAlert(context: Context, beepEnabled: Boolean) {
        if (beepEnabled) {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_ALARM, 90)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
            } catch (_: Exception) {
            }
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 260, 100, 260), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 260, 100, 260), -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(longArrayOf(0, 260, 100, 260), -1)
                }
            }
        } catch (_: Exception) {
        }
    }
}

object PlatformActions {
    fun copyToClipboard(context: Context, label: String, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openEmailIntent(
        context: Context,
        toList: List<String>,
        ccList: List<String> = emptyList(),
        subject: String,
        body: String
    ): Boolean {
        if (toList.isEmpty()) return false
        val mailtoUri = Uri.parse(
            buildString {
                append("mailto:")
                append(toList.joinToString(",") { Uri.encode(it) })
                append("?subject=").append(Uri.encode(subject))
                if (ccList.isNotEmpty()) {
                    append("&cc=").append(ccList.joinToString(",") { Uri.encode(it) })
                }
                append("&body=").append(Uri.encode(body))
            }
        )
        val intent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Fallback to ACTION_SEND
            try {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_EMAIL, toList.toTypedArray())
                    if (ccList.isNotEmpty()) {
                        putExtra(Intent.EXTRA_CC, ccList.toTypedArray())
                    }
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                val chooser = Intent.createChooser(share, "E-Mail senden").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun sharePlainText(context: Context, title: String, text: String): Boolean {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun printTextReport(
        context: Context,
        documentTitle: String,
        heading: String,
        subtitle: String,
        rows: List<Pair<String, String>>,
        bodyText: String = ""
    ) {
        val escHeading = TextUtils.htmlEncode(heading)
        val escSub = TextUtils.htmlEncode(subtitle)
        val rowsHtml = rows.joinToString("") { (k, v) ->
            "<tr><td class='k'>${TextUtils.htmlEncode(k)}</td><td class='v'>${TextUtils.htmlEncode(v)}</td></tr>"
        }
        val bodyHtml = if (bodyText.isNotBlank()) {
            "<div class='notes'>${TextUtils.htmlEncode(bodyText).replace("\n", "<br>")}</div>"
        } else ""

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8"/>
            <style>
              body { font-family: sans-serif; color: #181c22; margin: 20px; font-size: 12px; }
              .header { border-bottom: 3px solid #0d47a1; padding-bottom: 8px; margin-bottom: 14px; display: flex; justify-content: space-between; align-items: baseline; }
              .brand { font-size: 18px; font-weight: 800; color: #181c22; }
              .sub { font-size: 11px; color: #4b5563; }
              table { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
              td { padding: 6px 8px; border-bottom: 1px solid #e2e5ea; vertical-align: top; }
              td.k { font-weight: 700; width: 42%; color: #374151; }
              td.v { color: #111827; font-family: monospace; }
              .notes { white-space: normal; border: 1px solid #d1d5db; background: #f9fafb; padding: 10px; border-radius: 6px; line-height: 1.45; }
            </style>
            </head>
            <body>
              <div class="header">
                <div class="brand">INNOWATΞCH · $escHeading</div>
                <div class="sub">$escSub</div>
              </div>
              <table>$rowsHtml</table>
              $bodyHtml
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                val adapter = view.createPrintDocumentAdapter(documentTitle)
                printManager.print(
                    documentTitle,
                    adapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    }
}
