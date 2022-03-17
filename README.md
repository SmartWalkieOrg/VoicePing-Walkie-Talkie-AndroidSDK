# VoicePing Android SDK

VoicePing Android SDK is an Android library, provided by
[Smart Walkie Talkie](http://www.smartwalkie.com), for enabling Push-To-Talk (PTT) functionality to
your Android project.

1. Need to add walkie talkie or push-to-talk functionality to your Android app?
2. Worry no more, you can add it quickly with VoicePing Android SDK that works with VoicePing Open Source Router.
3. Simple integration, customizable, and free! What are you waiting for? 🎉

VoicePing Android SDK works together with <span style="text-decoration:underline;"> VoicePing Open Source Router</span> to allow you to quickly add group voice broadcast capability to your app. The Android SDK comes with a reference Android App with UI that demonstrates the one button Push-To-Talk interface.


## Features of VoicePing Android SDK

1. Easy to integrate to your app
2. Low data consumption suitable for Mobile Devices: Opus Codec, defined as 16Khz, 60ms Frame size. ~300KB per 1 minute of speech.
3. Works over all network conditions (2G, 3G, 4G or Wifi)
4. Auto-reconnect feature when Internet connection is lost
5. Uses secure WebSocket for transport
6. Works for Android SDK (16 to 30) and Android OS version 4.1 to 11
7. Low battery consumption


## Use Cases

1. An Uber like application can connect a group of drivers together based on their location or zipcode
2. For Enterprise applications like housekeeping applications, allow a group call to all housekeepers on a certain floor (level) of the hotel
3. For SOS apps, activate voice broadcast if a user is in distress
4. For Chat Apps, allow some users to send instant voice messages that do not need to be manually played.


## Get Started

You can test our sample app here: [Download VP Demo app](https://drive.google.com/file/d/1mI91tOuwPWiYsiU24fQuziiC9bOQRQuI/view?usp=sharing). To get the latest release [click here](https://github.com/SmartWalkieOrg/VoicePingAndroidSDK/releases).
The sample app allows you to test the Walkie Talkie function.
Input any user ID and company name. Devices should have same company name to be able to communicate, but different user ID.


## How simple is it to add VoicePing SDK to your app?

<center><a href="https://www.voicepingapp.com" target="_blank"><img alt="SDK in your app" src="https://i.ibb.co/jzbbcbx/Group-4-1.png" title="This is a sample how the SDK will look like in your app" /></a></center>

See those mic buttons?

Yup, that button is the only thing you need to add to your app to have PTT functionality.

But what if you don't want a mic button but another UI instead?

We have got it covered. You can customize it.


## Installation

How to install this SDK to your Android project

1. Clone the project
2. Built voiceping-sdk module
3. Add .aar file to your project
4. Sync
5. Use it


## Documentation

You can find the documentation [on the website](https://voiceping-sdk.netlify.app).

Check out the [Introduction](https://voiceping-sdk.netlify.app/docs/introduction) page for a quick review.

You can improve it by sending pull requests to [this repo](https://github.com/SmartWalkieOrg/VoicePing-sdk-doc).


## VoicePing Server

VoicePing Android SDK needs a VoicePing Server to work. You can test with our hosted server.

The public server URL: `wss://router-lite.voiceping.info`

If you need to self-host the server, you can find more documentation on the server repo:

* [VoicePing Server](https://github.com/SmartWalkieOrg/voiceping-router)


## Maintainers

* [VoicePing team](https://www.voicepingapp.com/)
