# Android Sensor Solace Client

A simple Android Client that connects to Solace Router via MQTT and pubish some sensor data.

## Testing
1. Run a subscriber to gather whats being published from the client

This can be done using any tool such as sdkperf. Here Iam using standard Mosquitto client to 
subscribe to all the messages 

> $ mosquitto_sub -h <solace-router-IP>  -p <mqtt-port> -t '#'  -u default -P <password> –v

2. Run the Android Sensor Client.

The application can be deployed on any Android hardware. Here Iam running it on an Android
Emulator from Android Studio.

![](.README_images/96962c43.png)

![](github.com/rnatara/AndroidSimpleSensorClient/blob/master/.README_images/96962c43.png)