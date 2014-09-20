/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

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
                // We should use JSON.parse to avoid any security holes,
                // but that also needs json2.js
                //var command = JSON.parse(event.data);
                var command = eval('('+event.data+')');
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
