
/**
 *
 * @author Jitendra Kotamraju
 */

var network = function (eventSource) {
    return {
        initialize: function() {
            var url = 'http://' + document.location.host
                + '/videoplayer/notifications';

            eventSource = new EventSource(url);
            eventSource.onmessage = function (event) {
                var command = JSON.parse(event.data);
                if (command.type == "pause") {
                    APP.pauseVideo();
                } else if (command.type == "play") {
                    APP.playVideo();
                } else if (command.type == "seeked") {
                    APP.seekVideo(command.currentTime);
                } else {
                    alert("Unknown command " + command);
                }
            };
        },
        send: function(command) {
            eventSource.send(command);
        }
    }
};

var APP = {
    id: Math.floor(Math.random() * 10000),

    network: network(null),

    // Cannot use 'this' here after updating window.onload (see below)
    initialize: function () {
        APP.network.initialize();

        var video = APP.getVideo();
    },

    getVideo: function () {
        return document.getElementsByTagName("video")[0];
    },

    pauseVideo: function () {
        var video = this.getVideo();
        video.pause();
    },

    playVideo: function () {
        var video = this.getVideo();
        video.play();
    },

    seekVideo: function (currentTime) {
        var video = this.getVideo();
        video.currentTime = currentTime;
    }

};

window.onload = APP.initialize;
