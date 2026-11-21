package com.example.cardtally.cloud

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import com.example.cardtally.util.LanguageHelper
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import java.util.UUID

/** Private, QR-only capture launched by the cloud page; no barcode image is saved. */
class WebDavQrCaptureActivity : CaptureActivity() {
    private lateinit var scanner: DecoratedBarcodeView

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.attachLanguageContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        super.onCreate(savedInstanceState)
        // Replace CaptureManager's raw-result callback before the camera starts in onResume.
        // ActivityResultRegistry may persist pending Intents: return a memory token, not passwords.
        scanner.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                scanner.pause()
                val text = result.text
                val token = if (text.length <= WebDavQrCodec.MAX_BYTES &&
                    text.toByteArray(Charsets.UTF_8).size <= WebDavQrCodec.MAX_BYTES)
                    WebDavQrScanResults.put(text) else null
                setResult(RESULT_OK, Intent().putExtra(WebDavQrScanResults.EXTRA_TOKEN, token))
                finish()
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) = Unit
        })
    }

    override fun initializeContent(): DecoratedBarcodeView =
        super.initializeContent().also { scanner = it }
}

/** Process-local, one-shot handoff. Process death discards credentials rather than persisting them. */
internal object WebDavQrScanResults {
    const val EXTRA_TOKEN = "com.example.cardtally.WEBDAV_QR_TOKEN"
    private var token: String? = null
    private var payload: String? = null
    private var createdAt = 0L

    @Synchronized fun put(value: String): String {
        payload = value
        createdAt = System.nanoTime()
        return UUID.randomUUID().toString().also { token = it }
    }

    @Synchronized fun take(value: String?): String? {
        val result = if (value != null && value == token &&
            System.nanoTime() - createdAt <= 60_000_000_000L) payload else null
        payload = null
        token = null
        return result
    }
}
