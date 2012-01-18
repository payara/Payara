This sample tests Server Sent Events. To test

* Deploy the target/videoplayer.war file to the server avatar distribution

$cd appserver/distributions/avatar/target/stage/glassfish3/glassfish/bin
$./asadmin start-domain
$./asadmin deploy videoplayer.war

* Open the multiple browser windows with http://localhost:8080/videoplayer
  that opens SSE connection at http://localhost:8080/videoplayer/notifications
* From some other windows, play/pause the video using the following URLs respectively:
  http://localhost:8080/videoplayer/remotecontrol/play
  http://localhost:8080/videoplayer/remotecontrol/pause
  These send SSE notifications


