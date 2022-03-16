package com.smartwalkie.voicepingsdk.model.local

import android.content.Context
import android.content.SharedPreferences
import java.util.HashSet

/**
 * Created by kukuhsain on 8/4/17.
 */
class VoicePingPrefs private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("voiceping_sdk.sp", Context.MODE_PRIVATE)

    private val MUTED_CHANNEL_SET = "muted_channel_set"
    private val UNMUTED_CHANNEL_SET = "unmuted_channel_set"

    val mutedChannels: MutableSet<String>
        get() = sharedPreferences.getStringSet(MUTED_CHANNEL_SET, HashSet()) ?: HashSet()

    fun addMutedChannel(channel: String) {
        val channelSet = mutedChannels
        if (channelSet.contains(channel)) return
        channelSet.add(channel)
        sharedPreferences.edit().putStringSet(MUTED_CHANNEL_SET, channelSet).apply()
    }

    fun removeMutedChannel(channel: String) {
        val channelSet = mutedChannels
        if (!channelSet.contains(channel)) return
        channelSet.remove(channel)
        sharedPreferences.edit().putStringSet(MUTED_CHANNEL_SET, channelSet).apply()
    }

    fun clearMutedChannels() {
        sharedPreferences.edit().remove(MUTED_CHANNEL_SET).apply()
    }

    val unmutedChannels: MutableSet<String>
        get() = sharedPreferences.getStringSet(UNMUTED_CHANNEL_SET, HashSet()) ?: HashSet()

    fun addUnmutedChannel(channel: String) {
        val channelSet = unmutedChannels
        if (channelSet.contains(channel)) return
        channelSet.add(channel)
        sharedPreferences.edit().putStringSet(UNMUTED_CHANNEL_SET, channelSet).apply()
    }

    fun removeUnmutedChannel(channel: String) {
        val channelSet = unmutedChannels
        if (!channelSet.contains(channel)) return
        channelSet.remove(channel)
        sharedPreferences.edit().putStringSet(UNMUTED_CHANNEL_SET, channelSet).apply()
    }

    fun clearUnmutedChannels() {
        sharedPreferences.edit().remove(UNMUTED_CHANNEL_SET).apply()
    }

    companion object {
        private var INSTANCE: VoicePingPrefs? = null

        fun getInstance(context: Context): VoicePingPrefs {
            if (INSTANCE == null) {
                INSTANCE = VoicePingPrefs(context)
            }
            return INSTANCE ?: VoicePingPrefs(context)
        }
    }

}