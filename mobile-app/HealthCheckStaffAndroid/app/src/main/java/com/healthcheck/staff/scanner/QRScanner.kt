package com.healthcheck.staff.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX ImageAnalysis.Analyzer + ML Kit Barcode Scanning（僅 QR Code）。
 * 只負責讀出字串，不做任何健檢資格判斷。
 */
class QRScanner(
    private val onScanError: (String) -> Unit,
    private val onQrDetected: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @Volatile
    private var busy = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || busy) {
            imageProxy.close()
            return
        }
        busy = true
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf { v -> v.isNotBlank() } }
                    ?.let(onQrDetected)
            }
            .addOnFailureListener {
                onScanError("暫時無法辨識 QR Code，請重新對準；若持續失敗，可返回手動輸入 ID。")
            }
            .addOnCompleteListener {
                busy = false
                imageProxy.close()
            }
    }

    fun close() {
        scanner.close()
    }

    companion object {

        /**
         * QR 第一版格式：純文字 ID（受檢者 "P001"、站點 "S002"）。
         * 長度上限依 openapi.json：patientId 20、stationId 10。
         * @return 錯誤訊息；null 表示合法
         */
        fun validate(raw: String, maxLength: Int, expectedPrefix: Char): String? {
            val v = raw.trim()
            return when {
                v.isEmpty() -> "QR 內容為空"
                v.length > maxLength -> "QR 內容過長（上限 $maxLength 字元）：$v"
                !Regex("^${expectedPrefix}[0-9]+$").matches(v) ->
                    if (expectedPrefix == 'P') "請掃描受檢者 QR（P 開頭加數字，例如 P001）"
                    else "請掃描檢查站 QR（S 開頭加數字，例如 S002）"
                else -> null
            }
        }
    }
}
