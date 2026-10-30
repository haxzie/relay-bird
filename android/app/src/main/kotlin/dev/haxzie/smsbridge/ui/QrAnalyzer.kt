package dev.haxzie.smsbridge.ui

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.Closeable

/** Restricted to QR, which is meaningfully faster than scanning every supported format. */
class QrAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer, Closeable {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build(),
    )
    private var delivered = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image == null || delivered) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.rawValue }?.let { value ->
                    if (!delivered) {
                        delivered = true
                        onResult(value)
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    override fun close() {
        scanner.close()
    }
}
