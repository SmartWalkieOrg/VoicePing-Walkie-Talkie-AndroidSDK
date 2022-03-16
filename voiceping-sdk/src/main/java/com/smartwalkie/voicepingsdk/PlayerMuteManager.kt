package com.smartwalkie.voicepingsdk

import android.content.Context
import com.smartwalkie.voicepingsdk.model.Channel
import com.smartwalkie.voicepingsdk.model.local.VoicePingPrefs
import com.smartwalkie.voicepingsdk.model.ChannelType
import com.smartwalkie.voicepingsdk.model.Message

/**
 * Created by kukuhsain on 16/03/18.
 */
internal class PlayerMuteManager(private val context: Context) {
    private val mutedChannels: MutableSet<String> =
        VoicePingPrefs.getInstance(context).mutedChannels
    private val unmutedChannels: MutableSet<String> =
        VoicePingPrefs.getInstance(context).unmutedChannels
    private var isMutedAll = false

    fun mute(targetId: String, channelType: Int) {
        val channel = getChannel(targetId, channelType)
        VoicePingPrefs.getInstance(context).removeUnmutedChannel(channel.toString())
        VoicePingPrefs.getInstance(context).addMutedChannel(channel.toString())
        if (unmutedChannels.contains(channel.toString())) {
            unmutedChannels.remove(channel.toString())
        } else if (!mutedChannels.contains(channel.toString())) {
            mutedChannels.add(channel.toString())
        }
    }

    fun muteAll() {
        isMutedAll = true
        VoicePingPrefs.getInstance(context).clearUnmutedChannels()
        unmutedChannels.clear()
    }

    fun unmute(targetId: String, channelType: Int) {
        val channel = getChannel(targetId, channelType)
        VoicePingPrefs.getInstance(context).removeMutedChannel(channel.toString())
        VoicePingPrefs.getInstance(context).addUnmutedChannel(channel.toString())
        if (mutedChannels.contains(channel.toString())) {
            mutedChannels.remove(channel.toString())
        } else if (!unmutedChannels.contains(channel.toString())) {
            unmutedChannels.add(channel.toString())
        }
    }

    fun unmuteAll() {
        isMutedAll = false
        VoicePingPrefs.getInstance(context).clearMutedChannels()
        mutedChannels.clear()
    }

    fun isMuted(message: Message): Boolean {
        val channel: Channel = if (message.channelType == ChannelType.GROUP) {
            getChannel(message.receiverId, ChannelType.GROUP)
        } else {
            getChannel(message.senderId, ChannelType.PRIVATE)
        }
        return mutedChannels.contains(channel.toString()) ||
                isMutedAll && !unmutedChannels.contains(channel.toString())
    }

    private fun getChannel(targetId: String, channelType: Int): Channel {
        return if (channelType == ChannelType.GROUP) {
            Channel(channelType, null, targetId)
        } else {
            Channel(channelType, targetId, null)
        }
    }
}