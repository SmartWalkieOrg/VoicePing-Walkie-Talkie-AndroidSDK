package com.smartwalkie.voicepingdemo

import androidx.appcompat.app.AppCompatActivity
import android.widget.SeekBar
import com.smartwalkie.voicepingsdk.VoicePingPlayer
import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import android.widget.SeekBar.OnSeekBarChangeListener
import android.content.Context
import android.util.Log
import com.smartwalkie.voicepingdemo.databinding.ActivityPlayerBinding
import com.smartwalkie.voicepingsdk.VoicePing
import java.io.File
import java.io.FileNotFoundException
import java.util.*

class PlayerActivity : AppCompatActivity() {
    private val TAG = "PlayerActivity"
    private val RC_PICK_FILE = 100
    private lateinit var binding: ActivityPlayerBinding

    private var mFilePath: String? = null
    private var mVoicePingPlayer: VoicePingPlayer? = null
    private var mTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mFilePath = intent.getStringExtra(FILE_PATH_DATA)
        if (mFilePath.isNullOrEmpty()) {
            showToast("You need to do PTT call first!")
            finish()
            return
        }

        binding.filePath.text = mFilePath
        binding.pickFileButton.setOnClickListener { pickFile() }
        binding.playButton.setOnClickListener { playAudio() }
        binding.pauseButton.setOnClickListener { pauseAudio() }
        binding.stopButton.setOnClickListener { stopAudio() }
    }

    override fun onStart() {
        super.onStart()
        initPlayer()
    }

    private fun initPlayer() {
        val audioParam = VoicePing.getAudioParam()
        val bufferSize = if (audioParam.isUsingOpusCodec) 133 else audioParam.rawBufferSize
        mVoicePingPlayer = VoicePingPlayer(audioParam, bufferSize)
        try {
            mVoicePingPlayer?.setDataSource(mFilePath)
            mVoicePingPlayer?.prepare()
            binding.seekBar.max = mVoicePingPlayer?.duration?.toInt() ?: 0
            binding.timeDuration.text = getTimeFromMillis(mVoicePingPlayer?.duration ?: 0)
            binding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    log("progress updated to: ${seekBar.progress}")
                    mVoicePingPlayer?.seekTo(seekBar.progress.toLong())
                }
            })
            mVoicePingPlayer?.setOnPlaybackStartedListener { audioSessionId ->
                log("OnPlaybackStartedListener, session id: $audioSessionId")
            }
            mVoicePingPlayer?.setOnCompletionListener {
                VoicePing.unmuteAll()
                mTimer?.cancel()
                binding.seekBar.progress = mVoicePingPlayer?.duration?.toInt() ?: 0
                binding.timeProgress.text = getTimeFromMillis(mVoicePingPlayer?.duration ?: 0)
                showToast("Playback Completed!")
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun getTimeFromMillis(millis: Long): String {
        val secs = Math.round((millis / 1000).toFloat())
        val timeMins = secs / 60
        val timeSecs = secs % 60
        return String.format(Locale.US, "%02d:%02d", timeMins, timeSecs)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_PICK_FILE && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            mFilePath = uri?.path
            binding.filePath.text = mFilePath
        }
    }

    override fun onStop() {
        super.onStop()
        mVoicePingPlayer?.stop()
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "file/*"
        startActivityForResult(intent, RC_PICK_FILE)
    }

    private fun playAudio() {
        log("playAudio")
        if (mVoicePingPlayer == null) return
        val file = File(mFilePath)
        if (!file.exists()) {
            showToast("File not exist!")
            return
        }
        VoicePing.muteAll()
        mVoicePingPlayer?.start()
        mTimer = Timer()
        mTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    binding.seekBar.progress = mVoicePingPlayer?.currentPosition?.toInt() ?: 0
                    binding.timeProgress.text =
                        getTimeFromMillis(mVoicePingPlayer?.currentPosition ?: 0)
                }
            }
        }, 0, 500)
    }

    private fun pauseAudio() {
        log("pauseAudio")
        VoicePing.unmuteAll()
        mVoicePingPlayer?.pause()
        mTimer?.cancel()
    }

    private fun stopAudio() {
        log("stopAudio")
        VoicePing.unmuteAll()
        mVoicePingPlayer?.stop()
        mTimer?.cancel()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }

    companion object {
        private const val FILE_PATH_DATA = "file_path_data"

        fun generateIntent(context: Context?, filePath: String?): Intent {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra(FILE_PATH_DATA, filePath)
            return intent
        }
    }
}