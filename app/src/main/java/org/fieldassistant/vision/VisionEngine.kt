package org.fieldassistant.vision

import android.graphics.Bitmap
import org.fieldassistant.core.CropLabel
import org.fieldassistant.core.VisionResult

interface VisionEngine : AutoCloseable {
    val labels: List<CropLabel>
    fun analyze(bitmap: Bitmap): VisionResult
}
