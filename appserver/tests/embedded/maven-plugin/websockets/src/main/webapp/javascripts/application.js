/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

var count = 0;
var loop = 0;
var websocket = null;
var name  = null;
var url = 'ws://' + document.location.host + document.location.pathname + 'chat';

var app = {
    initialize: function() {
        if ("WebSocket" in window) {
            $('login-name').focus();
            app.listen();
        } else {
            $('missing-sockets').style.display = 'inherit';
            $('login-name').style.display = 'none';
            $('login-button').style.display = 'none';
            $('display').style.display = 'none';
        }
    },
    listen: function() {
        $('websockets-frame').src = app.url + '?' + count;
        count ++;
    },
    login: function() {
        name = $F('login-name');
        if (! name.length > 0) {
            $('system-message').style.color = 'red';
            $('login-name').focus();
            return;
        }
        $('system-message').style.color = '#2d2b3d';
        $('system-message').innerHTML = name + ':';

        $('login-button').disabled = true;
        $('login-form').style.display = 'none';
        $('message-form').style.display = '';

        websocket = new WebSocket(url);
        websocket.name = name;
        websocket.onopen = function() {
            // Web Socket is connected. You can send data by send() method
            websocket.send('login:' + name);
        };
        websocket.onmessage = function (evt) {
            eval(evt.data);
            $('message').disabled = false;
            $('post-button').disabled = false;
            $('message').focus();
            $('message').value = '';
        };
        websocket.onclose = function() {
            var p = document.createElement('p');
            p.innerHTML = name + ': has left the chat';

            $('display').appendChild(p);

            new Fx.Scroll('display').down();
        };
    },
    post: function() {
        var message = $F('message');
        if (!message > 0) {
            return;
        }
        $('message').disabled = true;
        $('post-button').disabled = true;

        websocket.send(message);
    },
    update: function(data) {
        if (data) {
            var p = document.createElement('p');
            p.innerHTML = data.message;

            $('display').appendChild(p);

            new Fx.Scroll('display').down();
        }
    }
};

var rules = {
    '#login-name': function(elem) {
        Event.observe(elem, 'keydown', function(e) {
            if (e.keyCode == 13) {
                $('login-button').focus();
            }
        });
    },
    '#login-button': function(elem) {
        elem.onclick = app.login;
    },
    '#message': function(elem) {
        Event.observe(elem, 'keydown', function(e) {
            if (e.shiftKey && e.keyCode == 13) {
                $('post-button').focus();
            }
        });
    },
    '#post-button': function(elem) {
        elem.onclick = app.post;
    }
};
Behaviour.addLoadEvent(app.initialize);
Behaviour.register(rules);

