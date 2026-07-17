package com.smartwalkie.voicepingdemo

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import kotlin.Throws
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.*
import java.io.FileOutputStream
import java.io.IOException
import java.lang.RuntimeException
import kotlin.math.abs
import kotlin.math.sqrt

object Utils {
    private const val TAG = "Utils"

    fun enhanceLoudnessIfPossible(audioSessionId: Int, gain: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                LoudnessEnhancer(audioSessionId).apply {
                    setTargetGain(gain)
                    enabled = true
                }
            } catch (e: RuntimeException) {
                // Do nothing
            }
        }
    }

    fun boostBassIfPossible(audioSessionId: Int, strength: Short) {
        try {
            BassBoost(10, audioSessionId).apply {
                setStrength(strength)
                enabled = true
            }
        } catch (e: RuntimeException) {
            // Do nothing
        }
    }

    fun getRmsAmplitude(dataShortArray: ShortArray): Double {
        var sum = 0.0
        for (singleShort in dataShortArray) {
            sum += (singleShort * singleShort).toDouble()
        }
        val meanSquare = sum / dataShortArray.size
        return sqrt(meanSquare)
    }

    fun getMaxAmplitude(dataShortArray: ShortArray): Double {
        var max: Double = abs(dataShortArray[0].toInt()).toDouble()
        for (singleShort in dataShortArray) {
            if (max < singleShort) max = abs(singleShort.toInt()).toDouble()
        }
        return max
    }

    fun downloadFileAsync(context: Context, downloadUrl: String) {
        Log.d(TAG, "start to download file from: $downloadUrl")
        val client = OkHttpClient()
        val request = Request.Builder().url(downloadUrl).build()
        client.newCall(request)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to download file!")
                        Toast.makeText(context, "Failed to download file!", Toast.LENGTH_SHORT)
                            .show()
                        return
                    }
                    val split = downloadUrl.split("/").toTypedArray()
                    val fileName = split[split.size - 1]
                    val destinationPath =
                        context.getExternalFilesDir(null).toString() + "/" + fileName
                    val body = response.body ?: return
                    val fileOutputStream = FileOutputStream(destinationPath)
                    fileOutputStream.write(body.bytes())
                    fileOutputStream.close()
                    Log.d(TAG, "file downloaded to: $destinationPath")
                }
            })
    }

    fun closeKeyboard(context: Context, view: View?) {
        if (view == null) return
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Apps targeting API 35+ are always edge-to-edge; pad the content root so it is not drawn
     * behind the system bars.
     */
    fun applyEdgeToEdgeInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}