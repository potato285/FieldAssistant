package org.fieldassistant.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import org.fieldassistant.core.*
import org.json.JSONArray
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.security.MessageDigest
import kotlin.math.abs

class LiteRtVisionEngine(context: Context) : VisionEngine {
    companion object {
        const val MODEL_ID = "Plantulus Doctor / EfficientNetV2-B0 fp16 / ee2cb607"
        const val SHA256 = "3f9b1ef6e7bcc603c6844429ae4ac28607cbfb48fd7b1f856362c824a29a3e65"
        const val ASSET = "vision/plantulus_doctor_fp16.tflite"
        fun readLabels(context: Context): List<CropLabel> {
            val ordered = JSONArray(context.assets.open("vision/classes.json").bufferedReader().use { it.readText() })
            val info = JSONArray(context.assets.open("vision/class_info.json").bufferedReader().use { it.readText() })
            require(ordered.length() == 54 && info.length() == 54)
            return (0 until info.length()).map { i ->
                val row = info.getJSONObject(i)
                require(row.getString("label") == ordered.getString(i)) { "Label order mismatch" }
                CropLabel(row.getString("label"), row.getString("plant"), row.getString("disease"), row.getBoolean("is_healthy"))
            }
        }
    }

    override val labels = readLabels(context)
    val actualSha256: String
    private val interpreter: Interpreter

    init {
        val digest = MessageDigest.getInstance("SHA-256")
        context.assets.open(ASSET).use { stream ->
            val bytes = ByteArray(65536)
            while (true) {
                val n = stream.read(bytes)
                if (n < 0) break
                digest.update(bytes, 0, n)
            }
        }
        actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
        require(actualSha256 == SHA256) { "Vision model checksum mismatch" }
        val mapped = context.assets.openFd(ASSET).use { fd ->
            FileInputStream(fd.fileDescriptor).channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
        interpreter = Interpreter(mapped, Interpreter.Options().setNumThreads(4).setUseXNNPACK(true))
        try {
            require(interpreter.getInputTensor(0).shape().contentEquals(intArrayOf(1, 224, 224, 3))) { "Unexpected input shape" }
            require(interpreter.getInputTensor(0).dataType() == DataType.FLOAT32) { "Unexpected input type" }
            require(interpreter.getOutputTensor(0).shape().contentEquals(intArrayOf(1, 54))) { "Unexpected output shape" }
            require(interpreter.getOutputTensor(0).dataType() == DataType.FLOAT32) { "Unexpected output type" }
        } catch (error: Exception) {
            interpreter.close()
            throw error
        }
    }

    @Synchronized override fun analyze(bitmap: Bitmap): VisionResult {
        val start = SystemClock.elapsedRealtimeNanos()
        val input = ImageInput.tensor(bitmap)
        val ready = SystemClock.elapsedRealtimeNanos()
        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(input, output)
        val end = SystemClock.elapsedRealtimeNanos()
        require(output[0].all { it.isFinite() && it in 0f..1f } && abs(output[0].sum() - 1f) < 0.02f) { "Invalid model output" }
        return VisionResult(
            labels.indices.map { Candidate(labels[it], output[0][it]) }.sortedByDescending { it.score },
            MODEL_ID, actualSha256, (end - ready) / 1_000_000.0, (ready - start) / 1_000_000.0
        )
    }

    @Synchronized override fun close() = interpreter.close()
}
