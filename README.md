[![Build Status](https://travis-ci.com/Immueggpain/simple-streaming.svg?branch=master)](https://travis-ci.com/Immueggpain/simple-streaming)

## QuickStart for Server
* Make sure you have **[Java](https://jdk.java.net/11/) 8+** installed
* [Download the zip file of latest release](https://github.com/Immueggpain/simple-streaming/releases). Unzip it
* Run server `java -jar simple-streaming-x.x.x.jar server -d <download_port> -u <upload_port>`.
* Enjoy!

## QuickStart for Client(Streamer)
* Make sure you have **[Java](https://jdk.java.net/11/) 8+** installed
* [Download the zip file of latest release](https://github.com/Immueggpain/simple-streaming/releases). Unzip it
* Run client `java -jar simple-streaming-x.x.x.jar client -p <upload_port> -s <server> -f <video_file>`.
* Enjoy!

## QuickStart for Viewer
* Use VLC player
* Open url: tcp://<server>:<download_port>
* Enjoy!
