package dev.jamescullimore.wifiwizard.util

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.TimeUnit

class QrCodeAnalyzer (val onAnalyze: (barcode: Barcode) -> Unit) : ImageAnalysis.Analyzer {
    private var lastAnalyzedTimeStamp = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimeStamp >= TimeUnit.SECONDS.toMillis(1)) {

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)
            val mediaImage = imageProxy.image
            mediaImage?.let {
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        barcodes.filter { it.valueType == Barcode.TYPE_WIFI }.let {
                            if (it.isNotEmpty()) {
                                onAnalyze(it.first())
                            }
                        }
                    }
                    .addOnFailureListener {
                        Log.d("QRCODE", "QrCodeAnalyzer: Something went wrong $it")
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        } else {
            imageProxy.close()
        }
    }
}