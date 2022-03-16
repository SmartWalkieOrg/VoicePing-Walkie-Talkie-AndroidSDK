package com.smartwalkie.voicepingsdk

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.jvm.Volatile

/**
 * Created by kukuhsain on 21/11/17.
 */
internal class AudioLocalSaver(private val localPath: String) {
    @Volatile
    private var fileOutputStream: FileOutputStream? = null

    fun init() {
        val file = File(localPath)
        try {
            fileOutputStream = FileOutputStream(file)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    fun write(data: ByteArray?) {
        if (fileOutputStream == null) return
        try {
            fileOutputStream!!.write(data)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun close() {
        if (fileOutputStream == null) return
        try {
            fileOutputStream!!.close()
            fileOutputStream = null
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}