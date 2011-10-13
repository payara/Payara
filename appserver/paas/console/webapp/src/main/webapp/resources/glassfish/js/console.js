/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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
if (typeof Console == 'undefined') {
    $(document).ready(function () {
//        Console.Ajax.processLinks();
//        Console.Ajax.validateForms();
        
        jsf.ajax.addOnError(Console.Ajax.onError);
    });

    Console = {
        escapeClientId : function(id) {
            return "#" + id.replace(/:/g,"\\:");
        }
    };

    Console.Ajax = {
        processLinks: function() {
            $('#ajaxBody > a').click(function() {
                var href = $(this).prop('href');
                if (href != '') {
                    Console.Ajax.loadPage(href);
                }

                return false;
            });
        },
    
        validateForms: function() { 
            $('form').each(function(index, element) {
                var form = $(element);
                var span = form.children().filter('#tr_'+form.prop('id') +'_Postscript');
                if (typeof span != 'undefined') {
                    var vsSelector = 'input[name="javax.faces.ViewState"]';
                    var input = span.children().filter(vsSelector).first();
                    if (input.length == 0) {
                        $(vsSelector).first().clone().appendTo(span);
                    }
                }
            });
        },

        loadPage: function (url, source, event) {
            if (source == undefined) source = $('#content')[0];
            $('#content').val(url);
            jsf.ajax.request(source, event, {
                    execute:'@form',
                    render:'ajaxBody',
                    onevent: Console.Ajax.ajaxCallback
            });
            return true;
        },
        
        onError: function(data) {
            if (data.errorName != undefined && data.errorName.indexOf("ViewExpiredException")){
                console.debug(data.errorMessage);
                window.location = window.location.href.replace("index", "login");
            } else if (data.errorMessage != undefined) {
                console.debug(data.errorMessage);
            } else {
                console.debug(data.description);
            }
            return false;
        },
    
        ajaxCallback: function(data) {
            if (data.status === 'success') {
                var context = {};
                try {
//                    Console.Ajax.processElement(context, $("#ajaxBody")[0], true);
//                    Console.Ajax.processScripts(context);
                    Console.Ajax.processLinks();
                    Console.Ajax.validateForms();
                } catch (err) {
                    alert(err);
                }
            } else if (data.status === 'error') {
                alert('error');
                Console.Ajax.loadPage('/domain.xhtml');
            }
        },
    
        processElement : function (context, node, queueScripts) {
            var recurse = true;
            if (node.nodeName == 'A') {
            /*
            // FIXME: For exteral URLs, we should not replace... however, we
            // FIXME: may want to ensure they have a _blank target.  May need
            // FIXME: to compare the host to see if a URL is an external URL
            // FIXME: b/c M$ makes it hard to determine relative URLs, and full
            // FIXME: URLs to the same host "might" want be valid for
            // FIXME: replacement.
            if (!Console.Ajax._isTreeNodeControl(node) && (node.target == '')) { //  && (typeof node.onclick != 'function'))
                var shouldReplace = true;
                if ((typeof node.onclick == 'function') && (node.id.indexOf("treeForm:tree") == -1)) {
                    //admingui.util.log("*NOT* replacing href for " + node.id);
                    shouldReplace = false;
                }
                if (shouldReplace) {
                    var url = node.href;
                    //node.href = "#";
                    var oldOnClick = node.onclick;
                    node.onclick = function() {
                        Console.Ajax.loadPage({
                            url : url,
                            target: document.getElementById('content'),
                            oldOnClickHandler: oldOnClick,
                            sourceNode: node
                        });
                        return false;
                    };
                }
            }
            */
            } else if (node.nodeName == 'IFRAME') {
                recurse = false;
            } else if (node.nodeName == 'INPUT') {
            /*
            if (((node.type == 'submit') || (node.type == 'image'))
                && ((node.onclick === null) || (typeof(node.onclick) === 'undefined') || (node.onclick == ''))) {
                // Submit button w/o any JS, make it a partial page submit
                node.onclick = function() {
                    var args = {};
                    args[node.id] = node.id;
                    Console.Ajax.postAjaxRequest(this, args);
                    return false;
                };
            }
            */
            /*
        } else if (node.nodeName == 'FORM') {
            admingui.util.log("***** form action:  " + node.action);
            if (node.target == '') {
                node.onsubmit = function () {
                    Console.Ajax.submitFormAjax(node);
                    return false;
                };
            }
	    */
            } else if (node.nodeName == 'TITLE') {
                // bareLayout.xhtml handles this for ajax requests...
                recurse = false;
            } else if (node.nodeName == 'SCRIPT') {
                recurse = false;  // don't walk scripts
                if (queueScripts) {
                    // Queue it...
                    if (typeof(context.scriptQueue) === "undefined") {
                        context.scriptQueue = new Array();
                    }
                    context.scriptQueue.push(node);
                }
            }

            // If recurse flag is true... recurse
            if (recurse && node.childNodes) {
                for (var i = 0; i < node.childNodes.length; i++) {
                    Console.Ajax.processElement(context, node.childNodes[i], queueScripts);
                }
            }
        },

        _isTreeNodeControl : function (node) {
            return isTreeNodeControl = (node.id.indexOf("_turner") > -1); // probably needs some work.  This will do for now.
        },

        processScripts : function(context) {
            if (typeof(context.scriptQueue) === "undefined") {
                // Nothing to do...
                return;
            }
            globalEvalNextScript(context.scriptQueue);
        }
    }

    Console.UI = {
        switchTabs : function (tab, dest, event) {
            var source = $('#currentTab')[0];
            $('#currentTab').val(tab);
            jsf.ajax.request(source, event, {
                    execute:'@form',
                    onevent: function(data) {
                        if (data.status === 'success') {
                            var path = window.location.pathname;
                            var index = path.indexOf("/", 1);
                            dest = path.substring(0, index) + dest.replace(".xhtml", ".jsf");
                            window.location.pathname = dest;
                        }
                    }
            });
            return true;
        },

        processServerTargets: function() {
        
            var createDraggable = function (e, target) {
                var li = $('<li>' + e + '</li>');
                li.prop('source', e);
                return li.draggable({ 
                    opacity: 0.35, 
                    revert: true, 
                    revertDuration: 0, 
                    helper: 'clone',
                    scope: target
                });
            }

            var handleDrop = function(li, from, to, ul) {
                var serverName = li.draggable.prop('source');
                from.prop('value', from.prop('value').replace(','+serverName, '').replace(serverName, ''));
                li.draggable.context.parentNode.removeChild(li.draggable.context);
                to.prop('value', to.prop('value') + ',' + serverName);
                ul.append(createDraggable(serverName, to.prop('id')));
            }
            
            $('#available').prop('value').split(',').forEach(function (element) {
                $('#avul').append(createDraggable(element, 'available'));
            });
            $('#selected').prop('value').split(',').forEach(function (element) {
                $('#selul').append(createDraggable(element, 'selected'));
            });
            
            $('#avdiv').droppable({
                scope: 'selected', 
                drop : function (event, ui) {
                    handleDrop (ui, $('#selected'), $('#available'), $('#avul'));
                }
            });
            $('#seldiv').droppable({
                scope: 'available', 
                drop : function (event, ui) {
                    handleDrop (ui, $('#available'), $('#selected'), $('#selul'));
                }
            });
        }
    }

    var globalEvalNextScript = function(scriptQueue) {
        if (typeof(scriptQueue) === "undefined") {
            // Nothing to do...
            return;
        }
        var node = scriptQueue.shift();
        if (typeof(node) == 'undefined') {
            // Nothing to do...
            return;
        }
        if (node.src === "") {
            // use text...
            globalEval(node.text);
            globalEvalNextScript(scriptQueue);
        } else {
            // Get via Ajax
            Console.Ajax.getResource(node.src, function(result) {
                globalEval(result.content);
                globalEvalNextScript(scriptQueue);
            } );
        // This gets a relative URL vs. a full URL with http://... needed
        // when we properly serve resources w/ rlubke's recent fix that
        // will be integrated soon.  We need to handle the response
        // differently also.
        //Console.Ajax.getResource(node.attributes['src'].value, function(result) { globalEval(result.content); globalEvalNextScript(scriptQueue);} );
        }
    }

    var globalEval = function(src) {
        if (window.execScript) {
            try {
                window.execScript(src);
            } catch (error) {
                if (console && console.log) {
                    console.log(error);
                }
            }
            return;
        }
        var fn = function() {
            window.eval.call(window, src);
        };
        fn();
    };

}