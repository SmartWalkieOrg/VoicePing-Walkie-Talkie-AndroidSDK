# VoicePing Android SDK

VoicePing Android SDK is an Android library, provided by 
[Smart Walkie Talkie](http://www.smartwalkie.com), 
for enabling Push-To-Talk (PTT) functionality to your Android project.

## Installation

How to install this SDK to your Android project

1. Clone the project
2. Built voiceping-sdk module
3. Add .aar file to your project
4. Sync
5. Use it

## Steps to use VoicePing Android SDK

1. Initialization

Initialize and instantiate VoicePing in your Application code, inside onCreate method

```java
public class VoicePingClientApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        VoicePing voicePing = VoicePing.init(this, "ws://vpjsex.southeastasia.cloudapp.azure.com");
    }
}
```

You can then use the instance to connect to server, subscribe a channel, or do PTT.
In order to use the instance, we need to expose the instance to public.

```java
public class VoicePingClientApp extends Application {

    private static VoicePing mVoicePing;

    @Override
    public void onCreate() {
        super.onCreate();
        mVoicePing = VoicePing.init(this, "ws://vpjsex.southeastasia.cloudapp.azure.com");
    }

    public static VoicePing getVoicePing() {
        return mVoicePing;
    }
}
```

2. Connect

Before you can start talking using PTT, you need connect to server. You can do that by call connect
method from VoicePing instance.

```java
String userId = "your_user_id";
VoicePingClientApp.getVoicePing().connect(userId, new ConnectCallback() {
            @Override
            public void onConnected() {
                // Do something
            }

            @Override
            public void onFailed(PingException exception) {
                // Do something
            }
        });
```

3. Start Talking

After successfully connected, you can now start talking. You can start talking to individual 
receiver using,

```java
String receiverId = "your_receiver_id";
VoicePingClientApp.getVoicePing().startTalking(receiverId, ChannelType.PRIVATE);
```

or in a group using,

```java
String groupId = "your_group_id";
VoicePingClientApp.getVoicePing().startTalking(groupId, ChannelType.GROUP);
```

4. Stop Talking

To stop talking, for both Private and Group PTT, you can use,

```java
VoicePingClientApp.getVoicePing().stopTalking();
```

5. Disconnect

You can disconnect to stop receiving PTT by using,

```java
VoicePingClientApp.getVoicePing().disconnect(new DisconnectCallback() {
            @Override
            public void onDisconnected() {
                // Do something
            }
    
            @Override
            public void onFailed(final PingException exception) {
                // Do something
            }
        });
```

### TO DO

1. Add Subscribe - Unsubscribe. User can subscribes to a group channel to enable listening for 
incoming PTT message in that channel.
2. Add documentation inside the code (Javadoc).
3. Improve sample app, add some advanced customizations.
4. Unit tests.
