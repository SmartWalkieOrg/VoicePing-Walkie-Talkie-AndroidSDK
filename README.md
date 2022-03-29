# [VoicePing Walkie Talkie Android SDK](https://github.com/SmartWalkieOrg/VoicePing-Walkie-Talkie-AndroidSDK)

<strong>Need to add walkie talkie or push-to-talk functionality to your Android app?</strong>
<br /><br />
Worry no more! You can add it quickly with VoicePing Android SDK that works together with [VoicePing Open Source Router](#voiceping-router).
<br />
Simple integration, customizable, and free! What are you waiting for? 🎉

<br />  
<center><a href="https://www.voicepingapp.com" target="_blank"><img alt="VoicePing Walkie Talkie Android Free SDK Banner" src="https://i.ibb.co/9pKKf4J/Group-2.png" title="VoicePing Walkie Talkie Android Free SDK Banner" /></a></center>
<br /><br />

VoicePing Android SDK is an Android library, provided by
[Smart Walkie Talkie](http://www.smartwalkie.com), for enabling Push-To-Talk (PTT) functionality to
your Android project. It allows you to quickly add group voice broadcast capability to your app. VoicePing Android SDK comes with a reference Android App (with UI) that demonstrates the one button Push-To-Talk interface.

## Get Started

You can test our sample app here: [Download VP Demo app](https://github.com/SmartWalkieOrg/VoicePingAndroidSDK/releases). The sample app allows you to test the Walkie Talkie function. You will need at least two android devices to test properly.  
You can input any user ID and company name. To communicate, devices should have same company name but different user ID.

## Documentation

If you need more detailed info, you can have a look at [VoicePing walkie talkie Push-to-Talk (PTT) sdk documentation page](https://opensource.voiceping.info).  
Check out the [Introduction](https://opensource.voiceping.info/docs/introduction) page for a quick review.

## Features of VoicePing Android SDK (Push-To-Talk)

1. Easy to integrate to your app
2. Low data consumption suitable for Mobile Devices: Opus Codec, defined as 16Khz, 60ms Frame size. ~300KB per 1 minute of speech.
3. Works over all network conditions (2G, 3G, 4G or Wifi)
4. Auto-reconnect feature when Internet connection is lost
5. Uses secure WebSocket for transport
6. Works for Android SDK (16 to 30) and Android OS version 4.1 to 11
7. Low battery consumption

## Use Cases (Add Group Walkie Talkie)

1. An Uber like application can connect a group of drivers together based on their location or zipcode
2. For Enterprise applications like housekeeping applications, allow a group call to all housekeepers on a certain floor (level) of the hotel
3. For SOS apps, activate voice broadcast if a user is in distress
4. For Chat Apps, allow some users to send instant voice messages that do not need to be manually played.

## Installation

To install this SDK in your Android project, you need to do the following steps,

1. Clone the project
2. Build ```voiceping-sdk``` module using the following command:
   - For Mac/Linux: ```./gradlew :voiceping-sdk:assembleRelease```
   - For Windows: ```gradlew :voiceping-sdk:assembleRelease```
3. Go to ```./voiceping-sdk/build/outputs/aar``` directory, and add the aar file into your project. [Click here](https://stackoverflow.com/a/23326397) to learn how.
4. Sync gradle and use it

<div name="voiceping-router"></div>

## VoicePing Server
VoicePing Walkie Talkie Android SDK needs a VoicePing Router Server to work. You can test with our hosted server.

The public server URL: `wss://router-lite.voiceping.info`

If you need to self-host the server, you can find more documentation on the server repo:

* [VoicePing Push-To-Talk Server](https://github.com/SmartWalkieOrg/voiceping-router)

## Maintainers

* [VoicePing team](https://www.voicepingapp.com/)

## VoicePing Enterprise

VoicePing Enterprise is the full featured closed source version with support. More features available are [https://www.voicepingapp.com](https://www.voicepingapp.com). You can try VoicePing on:

* [VoicePing Web](https://web.voiceoverping.net/)
* [VoicePing Android](https://play.google.com/store/apps/details?id=com.media2359.voiceping.store)
* [VoicePing iOS](https://itunes.apple.com/us/app/voiceping/id1249953303?ls=1&mt=8)

Join the same free channel ID and try PTT from the web to the Android/iOS app and vice versa. You will find VoicePing has very clear audio and low latency<sup>1</sup>.

VoicePing Enterprise has more features than VoicePing Open Source which can be found here: https://www.voicepingapp.com/blog/design-a-stunning-blog


### Multi Platform Support

**[Android Supported](https://play.google.com/store/apps/details?id=com.media2359.voiceping.store):** Android 5 to Android 11 supported. With or Without Google Services.

**[iPhone Supported](https://itunes.apple.com/us/app/voiceping/id1249953303?ls=1&mt=8):** iPhone version available. Runs in Background to allow for Real Time receiving of PTT.

**[Desktop Version](https://www.voicepingapp.com/blog/voiceping-desktop-web-ptt):** Web Based version to connect office and field workers.


## Consulting/Partnership, Services & Pricing  

If you would like help on server setup, maintenance, customization, please contact us at sales@smartwalkietalkie.com. VoicePing Enterprise is also available for customisation, rebranding and source code purchase. 

## Footnote

[1] Latency: time between someone talks in a device until the other hears the audio on other device
