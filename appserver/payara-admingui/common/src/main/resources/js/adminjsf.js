/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
/*
 * Common utility
 */

/* To work around a timing issue where for Firefox 2.0.0.3 on Mac OS X
 * We need to put in a little delay before returning the var
 */
function getConfirm(theButton, msg){
    var oldOnFocus = theButton.onfocus;
    theButton.onfocus = "";
    var val=confirm(msg);
    theButton.onfocus = oldOnFocus;
    return val;
}

function showAlert(msg) {
    setTimeout("alert('" + msg + "')", 100);
    return false;
}


function submitAndDisable(button, msg, target) {
    disableBtnComponent(button.id);
    button.value=msg;
    if (target) {
        // In this case we want the non-ajax behavior, but we still need the indicator
        admingui.ajax.ajaxStart();
        var oldaction = button.form.action;
        var oldtarget = button.form.target;
        button.form.target = target;
        var sep = (button.form.action.indexOf("?") > -1) ? "&" : "?";
        button.form.action += sep + button.name + "=" + encodeURI(button.value) + "&bare=false"; //bug# 6294035
        button.form.submit();
        button.form.action = oldaction;
        button.form.target = oldtarget;
        return false;
    }
    var args = {};
    args[button.id] = button.id;
    admingui.ajax.postAjaxRequest(button, args);
    return false;
}


function disableButton(id) {
    var button = document.getElementById(id);
    button.className='Btn1Dis_sun4'; // the LH styleClass for disabled buttons.
    button.disabled=true;
}

//To disable all buttons in the page.
//TODO: other components maybe of type "submit" even though it is not a button, need to fix this.
function disableAllButtons() {
    var inputs = document.getElementsByTagName("input");
    for ( i=0; i < inputs.length; i++) {
        component = inputs[i];
        if (component.type == "submit"){
            component.disabled=true;
        }
    }
}

function getField(theForm, fieldName) {
    for (i=0; i < theForm.elements.length; i++) {
        var value = theForm.elements[i].name;
        if (value == null) {
            continue;
        }
        var pos = value.lastIndexOf(':');
        var helpKeyFieldName = value.substring(pos+1);
        if (helpKeyFieldName == fieldName) {
            return theForm.elements[i];
        }
    }
    return null;
}

// FIXME: suntheme should not be used -- prevents theme from changing
function getTextElement(componentName) {
    var el = webui.suntheme.field.getInputElement(componentName);
    if (el == null) {
        el = document.getElementById(componentName); // This may get too deep inside WS, but it should work as a fall back
    }
    return el;
}

function getSelectElement(componentName) {
    return webui.suntheme.dropDown.getSelectElement(componentName);
}

function getFileInputElement(componentName) {
    var el = webui.suntheme.upload.getInputElement(componentName);
    if (el == null) {
        el = document.getElementById(componentName+"_com.sun.webui.jsf.upload");
    }

    return el;
}

function disableComponent(componentName, type) {
    var component = null;
    if (type != null && type == 'file') {
        component = getFileInputElement(componentName);
    } else if(type != null && type == 'select') {
        component = getSelectElement(componentName);
    } else {
        component = getTextElement(componentName);
        if (component != null) {
            component.value='';
        }
    }
    if (component != null) {
        if (typeof(component.setDisabled) === 'function') {
            component.setDisabled(true);
        } else {
            component.disabled=true;
            component.className='TxtFldDis_sun4';
        }
    }
}


/*
 * was trying to see if we can set the timeout in the function itself, instead of
 * at the calling time, refer to update.jsf
 * but just can't get this working.
 * saving the code for now.

function delayDisableComponent(componentName, type, timeouted) {
    var func = disableComponent[type] || getTextElement;
    var component = func(componentName);
    if(component == null && !timeouted) {
    	window.setTimeout("disableComponent('" + componentName + "','" + type + "', true)", 10);
    }
    if (component == null){
        window.console.log('component is NULL' + componentName);
        window.console.debug('component is NULL' + componentName);
    }

    component.disabled = true;
    component.className='TxtFldDis_sun4';
    if(func == getTextElement) {
    	component.value = "";
    }
}
disableComponent.file = getFileInputElement;
disableComponent.select = getSelectElement;
*/


function disableBtnComponent(componentName) {
    var el = document.getElementById(componentName);
    if (typeof(el.setDisabled) === 'function') {
        el.setDisabled(true);
    } else if (el.setProps) {
        document.getElementById(componentName).setProps({
            disabled: true,
            className: 'Btn1Dis_sun4'
        });
    } else {
        el.disabled = true;
        el.className = 'Btn1Dis_sun4'; // Primary style
    }
}

function enableBtnComponent(componentName) {
    var el = document.getElementById(componentName);
    if (typeof(el.setDisabled) === 'function') {
        el.setDisabled(false);
    } else if (el.setProps) {
        document.getElementById(componentName).setProps({
            disabled: false,
            className: 'Btn1_sun4'
        });
    } else {
        el.disabled = false;
        el.className = 'Btn1_sun4';  // Primary style
    }
}

function enableComponent(componentName, type) {
    var component = null;
    if (type != null && type == 'file') {
        component = getFileInputElement(componentName);
    } else if(type != null && type == 'select') {
        component = getSelectElement(componentName);
    } else {
        component = getTextElement(componentName);
    }
    if (component == null){
        return;
    }
    if (typeof(component.setDisabled) === 'function') {
        component.setDisabled(false);
    } else {
        component.className='TxtFld_sun4';
        component.disabled=false;
    }
}

function disableDOMComponent(componentName) {
    var el = document.getElementById(componentName);
    if (typeof(el.setDisabled) === 'function') {
        component.setDisabled(true);
    } else if (el.setProps) {
        document.getElementById(componentName).setProps({
            disabled: true,
            className: 'TxtFldDis_sun4',
            value: ' '
        });
    } else {
        //YAHOO.util.Dom.setStyle(el, 'disabled', 'true');
        el.disabled = true;
        el.className = 'TxtFldDis_sun4';
        el.value = ' ';
    }
}

function enableDOMComponent(componentName) {
    var el = document.getElementById(componentName);
    if (el.setProps) {
        document.getElementById(componentName).setProps({
            disabled: false,
            className: 'TxtFld_sun4'
        });
    } else {
        //YAHOO.util.Dom.setStyle(el, 'disabled', 'false');
        el.disabled = false;
        el.className = 'TxtFld_sun4';
    }
}

function isChecked (elementName) {
    var element = document.getElementById (elementName);
    if (element != null) {
        if (element.checked) {
            return true;
        } else {
            return false;
        }
    }
    return false;
}

function checkForValue(formField) {
    if (!formField) {
        return false; // No field, so no value
    }
    var value = formField.value;
    if (formField.getProps) {
        // Use Woodstock's api to get correct value
        value = formField.getProps().value;
    }
    var result = (value != '') && (isWhitespace(value) == false);
    if (!result) {
        formField.select();
    }
    return result;
}

//==========================================================
// Set a cookie

function setCookie(c_name,value,expiredays)
{
    //alert( c_name + ',' + value + ',' + expiredays);
    var exdate=new Date()
    exdate.setDate(exdate.getDate()+expiredays)
    document.cookie=c_name+ "=" +escape(value)+((expiredays==null) ? "" : ";expires="+exdate.toGMTString())
}

function getCookie(name) {
    var cookies = document.cookie.split(";");
    var cookieValue = null;

    for (var i = 0; i < cookies.length; i++) {
        var current = cookies[i].split("=");
        var currentName = current[0];
        if (typeof(current[0].trim) === 'function') {
            currentName = currentName.trim();
        }
        if (name == currentName) {
            if (current.length > 1) {
                cookieValue = unescape(current[1]);
                break;
            }
        }
    }

    return cookieValue;
}


//===========================================================


function extractName(value) {
    var appName="";
    var len=-1;
    if ((len = value.lastIndexOf('/')) != -1) {
        appName = value.substring(len+1, value.length);
    }
    else {
        //For window platform, use backsplash
        len = value.lastIndexOf('\\');
        appName = value.substring(len+1, value.length);
    }
    return appName;
}


function getPrefix(fullName){
    index = fullName.lastIndexOf(".");
    if (index == -1)
        return fullName;
    else
        return fullName.substring(0, index);
}

function getSuffix(fullName){
    index = fullName.lastIndexOf(".");
    if (index == -1)
        return "";
    else
        return fullName.substring(index, fullName.length);
}

// End of Deployment code

//===========================================================================

function findFrameRecursive( winOrFrame, frameName ) {
    // 1. CHECK THIS FRAME (winOrFrame)
    // home string is being checked to take care of the complex
    // frameset in PE homepage. Need to fix this in a Generic way later.
    if ( (winOrFrame.name && (winOrFrame.name == frameName)) ||
        winOrFrame.name == "home" )
        return winOrFrame;

    // 2. SEARCH SUBFRAMES.  note: when there are no sub-frames,
    // the frames array has 1 entry which is this frame,
    // hense this check for 2+ subframes
    if ( winOrFrame.frames.length < 2 )
        return null;

    // recurse
    for ( i= 0 ; i < winOrFrame.frames.length ; i++ ) {
        var x= findFrameRecursive( winOrFrame.frames[i], frameName );
        if ( x )
            return x;
    }
    return null;
}

function findFrame(frameName) {
    return findFrameRecursive(top, frameName);
}

var reasonsHidden = true;

function showRestartReasons() {
    var el = document.getElementById('restartReasons');
    var toggle = document.getElementById('toggle');
    if (reasonsHidden) {
        //toggle.src = "#{request.contextPath}/theme/woodstock4_3/suntheme/images/table/grouprow_expanded.gif";
        toggle.className = "expanded";
        el.style.visibility = "visible";
    } else {
        //toggle.src = "#{request.contextPath}/theme/woodstock4_3/suntheme/images/table/grouprow_collapsed.gif";
        toggle.className = "collapsed";
        el.style.visibility = "hidden";
    }
    reasonsHidden = !reasonsHidden;
}

//===========================================================================

if (typeof(admingui) === "undefined") {
    admingui = {};
}

/*
 *  The following functions are utility functions.
 */
admingui.util = {
    /**
     *	This function finds the Woodstock node which has the getProps
     *	function and returns the requested property.  If it does not exist
     *	on the given object, it will look at the parent.
     */
    getWoodstockProp: function(node, propName) {
        if (node == null) {
            return;
        }
        if (node.getProps != null) {
            return node.getProps()[propName];
        }
        return admingui.util.getWoodstockProp(node.parentNode, propName);
    },

    /**
     *	This function finds an Array[] of nodes matching the (checkFunc),
     *	which is a JS function that takes two arguments: the HTML node object
     *	to check, and an optional "argument" (arg) that is passed through.
     */
    findNodes: function(node, checkFunc, arg) {
        var results = new Array();
        if (node == null) {
            return null;
        }

        // Check for match
        if (checkFunc(node, arg)) {
            results[results.length] = node;
        }

        // Not what we want, walk its children if any
        var nodeList = node.childNodes;
        if (nodeList && (nodeList.length > 0)) {
            var moreResults;

            // Look for more matches...
            for (var count = 0; count<nodeList.length; count++) {
                // Recurse
                moreResults = admingui.util.findNodes(nodeList[count], checkFunc, arg);
                if (moreResults) {
                    // Append the results
                    results = results.concat(moreResults);
                }
            }
        }

        // Make sure we found something...
        if (results.length == 0) {
            results = null;
        }

        // Return what we found (if anything)
        return results;
    },

    /**
     *	This function sets the <code>key</code> / <code>value</code> pair as
     *	a persistent preference in the <code>root</code> path.  The root path
     *	will automatically prefix "glassfish/" to the given String.
     */
    setPreference: function(root, key, value) {
        root = 'glassfish/' + root;
        admingui.ajax.invoke("setPreference", {
            root:root,
            key:key,
            value:value
        });
    },

    log : function(msg) {
        if (!(typeof(console) === 'undefined') && (typeof(console.log) === 'function')) {
            console.log((new Date()).toString() + ":  " + msg);
        }
    }
}


/*
 *  The following functions provide tree functionality.
 */
admingui.nav = {
    TREE_ID: "treeForm:tree",

    refreshCluster: function(hasCluster){
        var node1 = admingui.nav.getTreeFrameElementById(admingui.nav.TREE_ID + ':clusters');
        var node2 = admingui.nav.getTreeFrameElementById(admingui.nav.TREE_ID + ':clusters2');
        var node3 = admingui.nav.getTreeFrameElementById(admingui.nav.TREE_ID + ':clusters2_children');
        var tree = admingui.nav.getTreeFrameElementById(admingui.nav.TREE_ID);
        // FIXME: This needs the viewId where clusters2 is defined
        admingui.nav.refreshTree(admingui.nav.TREE_ID + ':clusters2');
        if (hasCluster=='true' || hasCluster=='TRUE') {
            node1.style.display='none';
            node2.style.display='block';
            node3.style.display='block';
            tree.selectTreeNode(admingui.nav.TREE_ID + ':clusters2');
        } else {
            //there is a problem in hiding clusters2,  it doesn' hide it, maybe because of the
            //dynamic treenode under it ? still need to figure this out.
            node3.style.display='none';
            node2.style.display='none';
            node1.style.display='block';
            tree.selectTreeNode(admingui.nav.TREE_ID + ':clusters');
        }
    },

    /**
     *	<p> This function allows you to provide a clientId of a TreeNode in the
     *	    navigation frame to be "refreshed".  This means that it and its
     *	    children will be deleted, recreated, and redisplayed.</p>
     *	<dl>
     *      <dd>
     *          <code>refreshNodeId</code> - The clientId of the tree node to refresh
     *      </dd>
     *  </dl>
     */
    refreshTree: function(refreshNodeId) {
        admingui.util.log("Updating tree node " + refreshNodeId);
        var refreshNode = null;
        if (refreshNodeId) {
            refreshNode = admingui.nav.getTreeFrameElementById(refreshNodeId);
            if (!refreshNode) {
                admingui.util.log('refreshNode not found:'+refreshNode);
            }
        } else {
            refreshNode = admingui.nav.getSelectedTreeNode();
            refreshNodeId = refreshNode.id;
        }
        var updateTreeButton = document.getElementById('treeForm:update');
        if (refreshNode && updateTreeButton) {
            admingui.nav.requestTreeUpdate(
                updateTreeButton,
                {
                    type: 'click'
                },
                refreshNodeId, "",
                {
                    mainNode: document.getElementById(refreshNodeId),
                    childNodes: document.getElementById(refreshNodeId+"_children")
                }
                );
        }
        return false;
    },

    requestTreeUpdate: function(source, event, nodeId, params, previousState) {
        // Ping header to make sure header stays "fresh"
        admingui.ajax.pingHeader();
        jsf.ajax.request(source, event, {
            execute: "treeForm treeForm:update",
            render: nodeId + " " + nodeId + "_children",
            onevent: function(data) {
                admingui.nav.processUpdatedTreeNode(data, nodeId, previousState);
            },
            params: 'treeForm:update=' + params
        });
    },

    processUpdatedTreeNode: function(data, nodeId, previousState) {
        if (data.status == 'success') {
            var content = admingui.nav.getUpdateNode(data.responseXML, nodeId);
            if (content != null) {
                // Woodstock renders a tree node and its children as siblings in the DOM.
                // For example, the applications node is treeForm:tree:applications, and the
                // deployed applications listed as child nodes are in a DIV with the ID
                // treeForm:tree:applications_children.  What we do here, then, is take the
                // two DOM elements returned from the server when a given treeNode component
                // is rerendered.
                //
                var mainNode = document.getElementById(nodeId);
                var childNodes = document.getElementById(nodeId+"_children");

                try {
                    var oldNode = previousState.mainNode;
                    mainNode.className = oldNode.className;
                    mainNode.style["display"] = oldNode.style["display"];
                    try {
                        // Copy image src value to correct visual state of the node turner
                        // TODO: This should be smarter
                        mainNode.childNodes[0].childNodes[0].childNodes[0].src = oldNode.childNodes[0].childNodes[0].childNodes[0].src;
                    }catch (err1) {

                    }

                    if (childNodes) {
                        var newChildren = content.childNodes[1];
                        oldNode = previousState.childNodes;
                        newChildren.className = oldNode.className;
                        newChildren.style["display"] = oldNode.style["display"];
                        admingui.nav.copyStyleAndClass(oldNode, newChildren);
                        childNodes.innerHTML = newChildren.innerHTML;
                    }
                } catch (err) {
                //alert(err);
                // FIXME: Log error
                }
            } else {
                var element = document.getElementById(nodeId);
                // If the node exists, hide it
                if (element != null) {
                    element.style.display = 'none';
                    // If the node has a "children" node, hide it too.
                    element = document.getElementById(nodeId+"_children");
                    if (element != null) {
                        element.style.display = 'none';
                    }
                }
            }

            admingui.ajax.processElement(window, document.getElementById(nodeId), true);
            admingui.ajax.processElement(window, document.getElementById(nodeId+"_children"), true);
        }
    },

    /**
     * This function takes the given JSF 2 Ajax response XML and returns the <update>
     * node with the given id
     */
    getUpdateNode: function(xml, desiredId) {
        var responseType = xml.getElementsByTagName("partial-response")[0].firstChild;
        if (responseType.nodeName === "error") { // it's an error
            var errorMessage = responseType.firstChild.nextSibling.firstChild.nodeValue;
            //sendError(request, context, "serverError", null, errorName, errorMessage);
            admingui.util.log(errorMessage);
            return null;
        }

        if (responseType.nodeName === "redirect") {
            admingui.ajax.loadPage({
                url: responseType.getAttribute("url")
            });
            return null;
        }


        if (responseType.nodeName !== "changes") {
            admingui.util.log("Top level node must be one of: changes, redirect, error, received: " + responseType.nodeName + " instead.");
            return null;
        }

        var changes = responseType.childNodes;

        try {
            for (var i = 0; i < changes.length; i++) {
                var element = changes[i];
                if (element.nodeName === "update") {
                    var id = element.getAttribute('id');
                    if (id === desiredId) {
                        // join the CDATA sections in the markup
                        var content = '';
                        var markup = '';
                        for (var j = 0; j < element.childNodes.length; j++) {
                            content = element.childNodes[j];
                            markup += content.nodeValue;
                        }
                        var parserElement = document.createElement('div');
                        parserElement.innerHTML = markup;
                        return parserElement;
                    }
                }
            }
        } catch (ex) {
            //sendError(request, context, "malformedXML", ex.message);
            admingui.util.log(ex.message);
            return null;
        }

        return null;
    },

    /**
     *
     */
    copyStyleAndClass: function(src, dest) {
        if (!src || !dest || !src.childNodes || !dest.childNodes) {
            return;
        }
        var name = null;
        for (var idx=0; idx<src.childNodes.length; idx++) {
            name = src.childNodes[idx].id;
            if (name) {
                for (var cnt=0; cnt<dest.childNodes.length; cnt++) {
                    if (name == dest.childNodes[cnt].id) {
                        dest.childNodes[cnt].style["display"] = src.childNodes[idx].style["display"];
                        dest.childNodes[cnt].className = src.childNodes[idx].className;
                        if (src.childNodes[idx].nodeName == 'IMG'){
                            dest.childNodes[cnt].src = src.childNodes[idx].src;
                        }
                        admingui.nav.copyStyleAndClass(src.childNodes[idx], dest.childNodes[cnt]);
                    }
                }
            }
        }
    },

    /**
     *	This function clears all treeNode selections.
     */
    clearTreeSelection: function(treeId) {
        var tree = document.getElementById(treeId);
        if (tree) {
            tree.clearAllHighlight(treeId);
        }
    },

    /**
     *	This function selects a treeNode matching the given URL.
     */
    selectTreeNodeWithURL: function(url) {
        var location = window.location;
        var base = location.protocol + "//" + location.host;
        url = url.replace("?bare=true", "");
        url = url.replace("&bare=true", "");
        url = url.replace(base, "");

        var tree = document.getElementById(admingui.nav.TREE_ID);
        var qmark = url.indexOf("?");
        var matches = admingui.util.findNodes(tree, admingui.nav.matchURL,
            (qmark > -1) ? url.substring(0, qmark) : url);
        if (matches) {
            var bestMatch = null;
            if (qmark > -1) {
                var params = admingui.nav.createObjectFromQueryString(url);

                var hiscore = 0;
                for (var i = 0; i < matches.length; i++) {
                    var score = admingui.nav.compareQueryString(params, matches[i].href);
                    if (score > hiscore) {
                        hiscore = score;
                        bestMatch = matches[i];
                    }
                }
            }
            if (bestMatch) {
                admingui.nav.selectTreeNode(admingui.nav.getContainingTreeNode(bestMatch));
            }
        }
    },

    compareQueryString: function (params, url) {
        var score = 0;
        var qmark = url.indexOf("?");
        if (qmark > -1) {
            var otherParams = admingui.nav.createObjectFromQueryString(url);
            //admingui.util.log("params = " + params);
            for (var key in params) {
                if (typeof params[key] == "function") {
                    continue;
                }
                if (otherParams[key] === params[key]) {
                    score++;
                }
            }
        }

        return score;
    },

    createObjectFromQueryString: function(url) {
        var params = null;
        var qmark = url.indexOf("?");
        if (qmark > -1) {
            params = Object();
            var pieces = url.substring(qmark+1).split("&");
            for (var i = 0; i < pieces.length; i++) {
                var equals = pieces[i].indexOf("=");
                var key = pieces[i].substring(0, equals);
                var value = pieces[i].substring(equals+1);
                params[key] = value;
            }
        }

        return params;
    },

    /**
     *	This function selects the given treeNode.
     */
    selectTreeNode: function(treeNode) {
        var tree = document.getElementById(admingui.nav.TREE_ID);// admingui.nav.getTree(treeNode);
        if (tree) {
            try {
                admingui.nav.clearTreeSelection(admingui.nav.TREE_ID);
                tree.clearAllHighlight(tree.id);
                tree.selectTreeNode(treeNode.id);
                admingui.nav.expandNode(treeNode);
            } catch (err) {
            //console.log(err);
            }
        }
    },

    expandNode: function(treeNode) {
        var id = treeNode.id;
        var index = id.lastIndexOf(":");
        while (index > -1) {
            id = id.substring(0, index);
            var toSetStyle = document.getElementById(id+"_children");
            if (toSetStyle) {
                toSetStyle.style.display = "block";
            }
            index = id.lastIndexOf(":");
        }
    },

    /**=
     *	This function selects the given treeNode.
     */
    selectTreeNodeById: function(treeNodeId) {
        var tree = document.getElementById(admingui.nav.TREE_ID);
        //admingui.nav.getTreeFrameElementById(treeNodeId));
        if (tree) {
            tree.selectTreeNode(treeNodeId);
        }
    },

    /**
     *	This function looks for an "A" node with a url equal to the url
     *	passed in.
     */
    matchURL: function(node, url) {
        var result = null;
        if ((node.nodeType == 1) && (node.nodeName == "A") &&
            (node.href.indexOf(url) > -1) & (node.id.indexOf("link") > -1)) {
            result = node;
        }
        return result;
    },

    /**
     *	This function attempts to obtain the tree frame's tree object and
     *	return its selected Tree node.  It will return null if unable to do
     *	this.  It will <b>not</b> wait for the tree frame to load if it is not
     *	already loaded.
     */
    getSelectedTreeNode: function() {
        var tree = document.getElementById(admingui.nav.TREE_ID);
        if (tree && tree.getSelectedTreeNode) {
            return tree.getSelectedTreeNode(tree.id);
        }
    },

    /**
     *	This function provides access to DOM objects in the tree window.
     */
    getTreeFrameElementById: function(id) {
        return document.getElementById(id);
    },

    /**
     *	This function returns the parent TreeNode for the given TreeNode.
     */
    getParentTreeNode: function(treeNode) {
        return document.getElementById(admingui.nav.TREE_ID).getParentTreeNode(treeNode);
    },

    getContainingTreeNode: function(href) {
        var node =  document.getElementById(admingui.nav.TREE_ID).findContainingTreeNode(href);
        return node;
    },

    /* @deprecated :) */
    getTree: function(treeNode) {
        if (treeNode) {
            var node = document.getElementById(admingui.nav.TREE_ID);
            return node.getTree(treeNode);
        }
        return null;
    }
};

admingui.help = {
    pluginId : null,

    showHelpPage: function(url, targetNode) {
        if (targetNode) {
            if (typeof(targetNode) === 'string') {
                // We have a String
                targetNode = document.getElementById(targetNode);
            }
        }
        if (targetNode) {
            var req = admingui.ajax.getXMLHttpRequestObject();
            if (req) {
                req.onreadystatechange =
                function() {
                    if (req.readyState == 4) {
                        // Make a tempoary elemnt to contain the help content
                        var tmpDiv = document.createElement("div");
                        tmpDiv.innerHTML = req.responseText;

                        // Fix URLs in the help content...
                        admingui.help.fixHelpURLs(url, tmpDiv);

                        // Show the help content...
                        targetNode.innerHTML = tmpDiv.innerHTML;
                    }
                };
                req.open("GET", url, true);
                req.send("");
            }
        }
    },

    fixTreeOnclick: function(node) {
        if ((node.nodeType == 1) && (node.nodeName == "A")) {
            if (node.href) {
                node.oldonclick = null;
                if (node.onclick) {
                    node.oldonclick = node.onclick;
                }
                node.onclick = function () {
                    if (this.oldonclick != null) {
                        this.oldonclick();
                    }
                    admingui.help.showHelpPage(this.href, 'helpContent');
                    return false;
                };
            }
        } else {
            // Not a href, so walk its children
            for (var idx=node.childNodes.length-1; idx>-1; idx--) {
                admingui.help.fixTreeOnclick(node.childNodes[idx]);
            }
        }
    },

    fixHelpURLs: function(baseURL, node) {
        // Walk the DOM looking for "A" nodes, repair their URLs
        if ((node.nodeType == 1) && (node.nodeName == "A")) {
            var relPath = node.getAttribute("href");
            if (relPath) {
                if (relPath.indexOf("#") == 0) {
                    // In-page link...
                    return;
                }
                if (relPath.indexOf("://") !== -1) {
                    // Full URL or IE7...
                    if (relPath.indexOf(window.location.href) == 0) {
                        // Same Path...
                        if (relPath.indexOf("#") == -1) {
                            // Not an in-page link, make it one...
                            node.href = "#";
                        }

                        // Nothing to do here...
                        return;
                    }
                    var idx = relPath.indexOf("/common/help/");
                    if (idx != -1) {
                        // IE7 does not give the real value, but instead tranlates it
                        // all urls will be relative to "/common/help/" in this case,
                        // so strip it off...
                        relPath = relPath.substring(idx+13);
                    } else {
                        if (relPath.indexOf(window.location.hostname) != -1) {
                            // From same host... Assume IE7 messed up URL
                            idx = relPath.indexOf('/', relPath.indexOf('://') + 3);
                            relPath = "../../../" + relPath.substring(idx+1);
                        } else {
                            // Must be a real external URL...
                            if ((node.target == null)
                                || (node.target == "")
                                || (typeof(node.target) === "undefined")) {
                                // Default external targets to _blank
                                node.target = "_blank";
                            }
                            return;
                        }
                    }
                }

                // Fix for Issue #: 11017
                if ((idx = relPath.indexOf('#')) != -1) {
                    // Remove '#' from IE Ajax URLs b/c IE can't handle it!!
                    relPath = relPath.substring(0, idx);
                }

                // Take filename off baseURL
                baseURL = baseURL.substring(0, baseURL.lastIndexOf('/'));

                // Remove leading ../'s
                while (relPath.indexOf("../") != -1) {
                    relPath = relPath.substring(3);
                    var idx = baseURL.lastIndexOf("/");
                    if (idx != 0) {
                        baseURL = baseURL.substring(0, idx);
                    }
                }

                // Fix href...
                node.href = baseURL + "/" + relPath;
                node.setAttribute("onclick", "admingui.help.showHelpPage('" + node.href + "', 'helpContent'); return false;");
            }
        } else {
            // Not a href, so walk its children
            for (var idx=node.childNodes.length-1; idx>-1; idx--) {
                admingui.help.fixHelpURLs(baseURL, node.childNodes[idx]);
            }
        }
    },

    launchHelp: function(url) {
        var helpLink = "/common/help/help.jsf";
        var helpKeys = admingui.util.findNodes(document,
            function(node, name) {
                if ((typeof(node.id) === "undefined") || (node.id == null)) {
                    return false;
                }
                var pos = node.id.lastIndexOf(':');
                var shortName = (pos > -1) ? node.id.substring(pos+1) : node.id;
                return (shortName == name);
            },
            "helpKey");
        if (helpKeys !== null) {
            admingui.ajax.invoke("calculateHelpUrl", {
                pluginId: admingui.help.pluginId,
                helpKey: helpKeys[0].value,
                url:"url"
            },
            function(result) {
                admingui.help.openHelpWindow(helpLink + "?contextRef=" + "/resource/" + admingui.help.pluginId + result.url);
            }, 3, false);
        } else {
            admingui.help.openHelpWindow(helpLink);
        }
    },

    openHelpWindow: function (url) {
        var win = window.open(url, "HelpWindow" , "width=800, height=530, resizable");
        if (win) {
            win.focus();
        }
    },

    switchTab: function(tabElement, toShow, toHide) {
        //
        // Perform an ajax request on the tab panel element
        //

        // set up the parameters to the ajax request
        var props = {};
        var tabsetId = document.getElementById('tabForm:helpTabs').id;
        props.render = tabsetId;
        props.execute = tabElement.id + ', ' + tabsetId;
        props[tabElement.id + '_submittedField'] = tabElement.id;

        // launch the request
        // Note: in help window, don't ping -- only 1 JSF page
        jsf.ajax.request(tabElement, null, props);

        //
        // Use DOM to show/hide the proper tree
        //

        var tree = document.getElementById(toHide);
        tree.style.display = "none";
        tree = document.getElementById(toShow);
        tree.style.display = "block";
    },

    loadHelpPageFromContextRef: function(contextRef, targetNode) {
        if (typeof contextRef == 'undefined' || contextRef === "") {
            contextRef = "docinfo.html";
        }
	dd = decodeURIComponent(contextRef);
        // Derive the prefix somehow
        //contextRef = prefix + contextRef;
        admingui.help.showHelpPage(dd, targetNode);
        admingui.help.nav.selectTreeNodeWithURL(dd);
    },

    nav: {
        TREE_ID: "tocTree",
        lastTreeNodeSelected: null,

        /**
	 *	This function selects a treeNode matching the given URL.
	 */
        selectTreeNodeWithURL: function(url) {
            var tree = document.getElementById(admingui.help.nav.TREE_ID);
            var matches = admingui.util.findNodes(tree, admingui.nav.matchURL, url);
            if (matches) {
                // FIXME: Find "best" match... this will be needed if the URL
                // is ambiguous, which may happen if post requests occur which
                // leave off QUERY_STRING data that is needed to identify the
                // URL.  It's probably best to leave the highlighting alone in
                // many of these cases... perhaps search for the nearest match
                // to the currently selected node.  Anyway, for now I will
                // ignore this until we need to fix it...
                // FIXME: This really should highlight the selected node.
                admingui.help.nav.selectTreeNode(document.getElementById(matches[0].id));
            }
        },

        /**
	 *	This function selects the given treeNode.
	 */
        selectTreeNode: function(treeNode) {
            var tree = document.getElementById(admingui.help.nav.TREE_ID);// admingui.help.nav.getTree(treeNode);
            if (tree) {
                try {
                    admingui.nav.clearTreeSelection(admingui.help.nav.TREE_ID);
                    tree.clearAllHighlight(tree.id);
                    tree.selectTreeNode(treeNode.id);
                    admingui.nav.expandNode(treeNode);
                } catch (err) {
                //console.log(err);
                }
            }
        }
    }
};

//============================================================
/**
  *   Validation functions
  */

function checkPSW(ps1Id, ps2Id, notMatchMsg, emptyMsg) {
    var ps1Comp = getTextElement(ps1Id);
    var ps2Comp = getTextElement(ps2Id);
    var ps1 = ps1Comp.value;
    var ps2 = ps2Comp.value;
    if (ps1 != ps2){
        ps1Comp.select();
        ps1Comp.focus();
        showAlert(notMatchMsg);
        return false;
    }
    if ( (ps1==null || ps1=='') && (ps2==null || ps2=='')){
        ps1Comp.select();
        ps1Comp.focus();
        if ( getConfirm(this, emptyMsg) ){
            return true;
        }else{
            return false;
        }
    }
    return true;
}



function guiValidate(reqMsg, reqInt, reqPort) {
    var inputs = document.getElementsByTagName("input");
    var styleClass = null;
    var component = null;
    for ( i=0; i < inputs.length; i++) {
        component = inputs[i];
        // Find the styleClass for this input
        // styleClass = admingui.util.getWoodstockProp(inputs[i], "className");  This is the woodstock 4.4.0.1 style
        styleClass = component.className;
        if (styleClass == null || styleClass == '') {
            continue;
        }
        if (styleClass.match("require")) {
            if (component.value=='') {
                component.select();
                component.focus();
                return showAlert(reqMsg + ' ' + getLabel(component));
            }
        }

        if (styleClass.match("intAllowMinusOne")) {
            if (component.value =='' || component.value == '-1'){
                continue;
            }
            if (! checkForIntValue(component.value)) {
                component.select();
                component.focus();
                return showAlert(reqInt + ' ' + getLabel( component ));
            }
        }

        if (styleClass.match("intAllowMinus")) {
            var num = 0;
            if (component.value =='') {
                continue;
            }
            if ((num + component.value) <=0) {
                continue;
            }
            if (! checkForIntValue(component.value)) {
                component.select();
                component.focus();
                return showAlert(reqInt + ' ' + getLabel( component ));
            }
        }


        if (styleClass.match("integer")) {
            if (! checkForIntValueOrEmpty(component.value)) {
                component.select();
                component.focus();
                return showAlert(reqInt + ' ' + getLabel( component ));
            }
        }


        if (styleClass.match("port")) {
            if (! checkForPortOrEmpty(component.value)) {
                component.select();
                component.focus();
                return showAlert(reqPort + ' ' + getLabel( component ));
            }
        }
    }
    return true;
}

// FIXME: We should combine guiValidate() and guiValidateWithDropDown() these
// FIXME: perform similar operations but b/c of testing reasons we
// FIXME: added two methods.   We should combine these in the future.

function guiValidateWithDropDown(reqMsg,reqInt, reqPort, reqMsgSelect){
    var selectFields = document.getElementsByTagName("select");
    if (!guiValidate(reqMsg, reqInt, reqPort)) {
        return false;
    }
    var component = null;
    var styleClass = null;
    for (i=0; i < selectFields.length; i++) {
        component = selectFields[i];
        // Find the styleClass for this input
        // styleClass = admingui.util.getWoodstockProp(selectFields[i], "className");  This is the woodstock 4.4.0.1 style
        styleClass = component.className;
        if (styleClass == null || styleClass == '') {
            continue;
        }
        if (styleClass.match("require")) {
            if (component.value=='') {
                component.focus();
                return showAlert(reqMsgSelect + ' ' + getLabel(component));
            }
        }
    }
    return true;
}

function getLabel(component) {
    var id = component.id;
    var propId = id.substring(0,id.lastIndexOf(":"));
    var ss = propId.substring(propId.lastIndexOf(":")+1);
    var labelid=propId+':'+ss+'_label';
    var label = document.getElementById(labelid);
    var val = '';
    if (label != null) {
        //IE doesn't have textContent, need to use innerText;
        //firefox 2.0.0.1 doesn't have innerText, so need to test both.
        //val = label.textContent.substring(1);
        //val = label.innerText.substring(1);

        val = label.innerText;
        if (val ==null) {
            val = label.textContent;
        }

        // Need to remove leading newline characters...
        // FIXME: Consider using isWhitespace(val.charAt(0))
        // FIXME: I didn't add it now b/c isWhitespace is defined in selectElements.js
        // FIXME: and I don't have time to test that that file is included everywhere
        // FIXME: that this function is called.
        while (val.charAt(0) == '\n') {
            val = val.substring(1);
        }

        // Need to remove trailing newline characters...
        // FIXME: Consider using isWhitespace(val.charAt(val.length-1))
        // FIXME: I didn't add it now b/c isWhitespace is defined in selectElements.js
        // FIXME: and I don't have time to test that that file is included everywhere
        // FIXME: that this function is called.
        while ((val.charAt(val.length-1) == '\n') || (val.charAt(val.length-1) == ' ')) {
            val = val.substring(0, val.length-1);
        }

        // Strip off the ':' so that it doesn't show in the alert.
        if (val.charAt(val.length-1) == ':') {
            val = val.substring(0, val.length-1);
        }
    }
    return val;
}


function checkForIntValueOrEmpty(value) {
    if (value == '')
        return true;
    return checkForIntValue(value);
}

function checkForIntValue(value) {
    var result = (value != '') && isInCharSet(value, "0123456789");
    return result;
}

function checkForPortOrEmpty(value) {
    if (value == '')
        return true;
    return checkForPort(value);
}

function checkForPort(value) {
    if (value == '') return false;
    if (value.indexOf('${') == 0) return true;
    if (checkForIntValue(value) == false) return false;
    return checkNumbericRange(value, 1, 65535);
}

function checkNumbericRange(value, min, max) {
    var num = 0 + value;
    if (num < min || num > max)
        return false;
    return true;
}

function isInCharSet(str, charSet) {

    var i;
    for (i = 0; i < str.length; i++) {
        var c = str.charAt(i);
        if (charSet.indexOf(c) < 0) {
            return false;
        }
    }
    return true;
}

function checkForNumericValueOrEmpty(value) {
    if (value == '')
        return true;
    return checkForNumericValue(value);
}

function checkForNumericValue(value) {
    var result = (value != '') && isInCharSet(value, "0123456789.");
    //if (result == false) {
    //This comment is by Senthil on Apr 11 2007. I think this is an
    //existing bug in this API.
    //formField isn't defined, or passed to this method, so just return the
    //result for now. Fixing this API now might involve lots of other changes, at this release time, so decided to live with this bug for now.
    //formField.select();
    //}
    return result;
}




//Special check for StatementTimeout for JDBC and connector connection pool

function checkPoolAttr(componentId, msg){
    var component = getTextElement(componentId);
    var value = component.value;
    if (value == '' || value == '-1' || checkForIntValue(value))
        return true;
    showAlert(msg + ' ' + getLabel(component));
    component.focus();
    return false;

}

function checkForBackslash(componentId, msg){
    var component = getTextElement(componentId);
    var val = component.value;
    var result = (val != null) && (val != '') && (val.indexOf('\\') == -1);
    if (!result) {
        showAlert(msg + ' ' + getLabel(component));
        component.focus();
    }
    return result;
}

function checkRequired(componentId, reqMsg){
    //component = document.getElementById(componentId);
    //var value = component.getProps().value;
    var component = getTextElement(componentId);
    var value = component.value;
    var result = (value != null) && (value != '') && (isWhitespace(value) == false);
    if (result == false) {
        if (reqMsg == '') {
            showAlert(getLabel(component) + ' is a required field.');
        } else {
            showAlert(reqMsg + ' ' + getLabel(component));
        }
        component.select();
        component.focus();
    }
    return result;
}

function checkEmpty(componentId){
    var component = getTextElement(componentId);
    var value = component.value;
    if ( (value != null) && (value != '') && (isWhitespace(value) == false)){
        return false;
    }
    return true;
}

function isWhitespace(s) {
    var i;
    var whitespace = " \t\n\r";
    // Search through string's characters one by one
    // until we find a non-whitespace character.
    // When we do, return false; if we don't, return true.

    for (i = 0; i < s.length; i++) {
        // Check that current character isn't whitespace.
        var c = s.charAt(i);
        if (whitespace.indexOf(c) == -1) return false;
    }

    // All characters are whitespace.
    return true;
}

function compareDate(beginDate, endDate, pattern) {
    var endDateSet = false;
    var formatNumber = getDateFormat(pattern);
    var returnValue = true;
    if(beginDate == '') {
        return false;
    }
    if(endDate == '') {
        endDate = new Date();
        endDateSet = true;
    }
    beginDate = getUSDateFormat(beginDate, formatNumber);
    var endDateArr;
    var endDateValue;
    if(!endDateSet) {
        endDate = getUSDateFormat(endDate, formatNumber);
        endDateArr = endDate.split('/');
        if(endDateArr[2].length == 2) {
            endDateArr[2] = '20' + endDateArr[2];
        }
        endDateValue = new Date(endDateArr[2], endDateArr[0], endDateArr[1]);
    }
    if(endDateSet) {
        endDateValue = endDate;
    }
    var beginDateArr = beginDate.split('/');
    if(beginDateArr[2].length == 2) {
        //make sure this is in YYYY format
        beginDateArr[2] = '20' + beginDateArr[2];
    }
    var beginDateValue = new Date(beginDateArr[2], beginDateArr[0]-1, beginDateArr[1]);
    if(beginDateValue > endDateValue) {
        returnValue = false;
    }
    return returnValue;
}

function checkDatePattern(date, pattern, delim) {
    var separatorChar;
    var format = new Array();
    var regExp = new RegExp(/\s+/);

    if(delim == '') {
        separatorChar = new Array("/", "-", ":", " ");
    }
    else {
        separatorChar = delim;
    }

    if(pattern != '') {
        for(i = 0; i < separatorChar.length; i++) {
            if(pattern.indexOf(separatorChar[i]) != -1) {
                if(separatorChar[i] == ' ') {
                    //split any number of whitespaces
                    separatorChar[i] = regExp;
                }
                delim = '/';
                format = pattern.split(separatorChar[i]);
                dateArr = date.split(separatorChar[i]);
                if(format.length != dateArr.length) {
                    return false;
                }
                pattern = '';
                break;
            }
        }
        for(i = 0; i < format.length; i++) {
            if(pattern.length > 0) {
                pattern += delim;
            }
            if(format[i].toLowerCase == "yy") {
                format[i] += format[i];
            }
            pattern += format[i];
        }
    }
    formatNumber = getDateFormat(pattern);
    if(!checkForValidDate(date, formatNumber, '')) {
        return false;
    }
    return true;
}

//This API returns the format number for the given date pattern
function getDateFormat(pattern) {
    if(pattern == '') {
        return 1; //default mm/dd/yyyy pattern
    }
    pattern = pattern.toLowerCase();
    format = new Array("mm/dd/yyyy", "dd/mm/yyyy", "mm/yyyy/dd",
        "dd/yyyy/mm", "yyyy/mm/dd", "yyyy/dd/mm" );

    for(i=0; i < format.length; i++) {
        if(format[i] == pattern) {
            return i+1;
        }
    }
    //default mm/dd/yyyy pattern
    return 1;

}

//format defines whether mm/dd/yyyy format, or dd/mm/yyyy format.
//We support only two formats for now

function checkDateRanges(startComponent, endComponent, format, separatorChar) {
    start = getTextElement(startComponent);
    end = getTextElement(endComponent);

    startDate = start.value;
    endDate = end.value;

    if(startDate != '') {
        if(!checkForValidDate(startDate, format, separatorChar)){
            start.focus;
            return false;
        }
    }
    if(endDate != '') {
        if(!checkForValidDate(endDate, format, separatorChar)){
            end.focus;
            return false;
        }
    }
    return true;
}

function getUSDateFormat(date, format) {
    if(format == '' || format == 1 || date == '' || date.length < 3) {
        //In US Date format already, no need to convert
        return date;
    }
    else if(format == 2) {
        // We received date in dd//mm/yyyy format
        // Our API always treats in mm/dd/yyyy format, so shuffle accordingly.
        tmp = date[0];
        date[0] = date[1];
        date[1] = tmp;
    }
    else if(format == 3) {
        // We received date in mm/yyyy/dd format
        // Our API always treats in mm/dd/yyyy format, so shuffle accordingly.
        tmp = date[1];
        date[1] = date[2];
        date[2] = tmp;
    }
    else if(format == 4) {
        // We received date in dd/yyyy/mm format
        // Our API always treats in mm/dd/yyyy format, so shuffle accordingly.
        tmp = date[1];
        date[1] = date[0];
        date[0] = date[2];
        date[2] = tmp;
    }
    else if(format == 5) {
        // We received date in yyyy/mm/dd format
        // Our API always treats in mm/dd/yyyy format, so shuffle accordingly.
        tmp = date[1];
        date[0] = date[1];
        date[1] = date[2];
        date[2] = tmp;
    }
    else if(format == 6) {
        // We received date in yyyy/dd/mm format
        // Our API always treats in mm/dd/yyyy format, so shuffle accordingly.
        tmp = date[2];
        date[0] = date[2];
        date[2] = tmp;
    }
    return date;
}

function checkForValidDate(date, format, delim) {
    var dateValue;
    var splitChar;
    var separatorChar;
    var regExp = new RegExp(/\s+/);

    if(delim == '') {
        separatorChar = new Array("/", "-", ":", " ");
    }
    else {
        separatorChar = delim;
    }
    var dateFound = false;

    if(format == '') {
        //default format mm/dd/yyyy
        format = 1;
    }

    for(i = 0; i < separatorChar.length; i++) {
        if(date.indexOf(separatorChar[i]) != -1) {
            if(separatorChar[i] == ' ') {
                //split any number of whitespaces
                separatorChar[i] = regExp;
            }
            dateValue = date.split(separatorChar[i]);
            dateFound = true;
            break;
        }
    }

    if(dateValue == '' || dateFound == false || dateValue.length != 3) {
        return false;
    }

    if(format > 1) {
        // We received date in non-us format
        // Our API always treats in mm/dd/yyyy format, so shuffle accordingly.
        dateValue = getUSDateFormat(dateValue, format);
    }

    if(dateValue[2].length == 2) {
        //make sure this is in YYYY format
        dateValue[2] = '20' + dateValue[2];
    }
    else {
        if(dateValue[2].length != 4) {
            return false;
        }
    }

    var range = new Array(3);
    range[0] = new Array(1, 12);
    range[1] = new Array(1, 31);
    range[2] = new Array(2000, 2100);

    for(i=0; i < 3; i++) {
        if(!checkForNumericValue(dateValue[i])) {
            return false;
        }

        if(!checkNumbericRange(dateValue[i], range[i][0], range[i][1])) {
            return false;
        }
    }
    if(!checkForAllowedDays(dateValue[0], dateValue[1], dateValue[2])) {
        return false;
    }
    return true;
}

function checkForAllowedDays(month, day, year) {
    if(day < 1) {
        return false;
    }
    if((month == 1 || month == 3 || month == 5 || month == 7 || month == 8 ||
        month == 10 || month == 12) && (day > 31 )) {
        return false;
    }
    if((month == 4 || month == 6 || month == 9 || month == 11) &&
        (day > 30)) {
        return false;
    }
    if(month == 2) {
        if(leapYear(year) && (day > 29)) {
            return false;
        }
        else {
            if(day > 28) {
                return false;
            }
        }
    }
    return true;
}

function leapYear(year) {
    if((year % 4 == 0) && !(year % 100 == 0 || year % 400 == 0)) {
        return true;
    }
    return false;
}

var lastSelectedIndex = 0;

function initLastSelected (objId) {
    var obj=document.getElementById(objId);
    setLastSelected (obj.selectedIndex);
}

function setLastSelected (value) {
    lastSelectedIndex = value;
}

function disableUnselect(obj) {
    if (obj.selectedIndex == -1) {
        obj.selectedIndex = lastSelectedIndex;
    }
    lastSelectedIndex = obj.selectedIndex;
}

function setSelectOption(index,obj,value) {
    obj.options[index].selected = value;
    lastSelectedIndex = index;
}


function setAllOptions(obj,value) {
    if (!hasOptions(obj)) {
        return;
    }
    for (var i=0;i<obj.options.length;i++) {
        setSelectOption(i,obj,value);
    }
}


function findSelectOptionIndex(obj,value) {
    if (!hasOptions(obj)) {
        return;
    }
    for (var i=0;i<obj.options.length;i++) {
        var optionValue = obj.options[i].text;
        if (optionValue == value) {
            return i;
        }
    }
    return -1;
}


function hasOptions(obj) {
    if (obj!=null && obj.options!=null) {
        return true;
    }
    return false;
}


function toggleSelectAll(checkbox,optionListId,dropDownId) {
    var optionList=document.getElementById(optionListId);
    var dropDownObj=document.getElementById(dropDownId);
    var dropDownSelectedValue = dropDownObj.value;
    setAllOptions (optionList, checkbox.checked);
    index = findSelectOptionIndex(optionList,dropDownSelectedValue);
    if (index > -1) {
        setSelectOption(index,optionList,true);
    }
}

/*
 * This functions submits the form when user hits enter
 */

function submitenter(e, id, msg) {
    var keyCode;
    if(window.event) {
        keyCode = window.event.keyCode;
    }
    else if(e) {
        keyCode = e.which;
    }
    else {
        return true;
    }
    if(keyCode == 13) {
        button = document.getElementById(id);
        submitAndDisable(button, msg);
        return false;
    }
    else {
        return true;
    }
}

function getSelectedValue(field) {
    var theForm = document.forms[0];
    var selectedValue;
    for(i = 0; i < theForm.elements.length; i++) {
        var value = theForm.elements[i].name;
        if(value == null) {
            continue;
        }
        var extnsn = value.lastIndexOf(".");
        var name = value.substr(extnsn+1);
        var fieldName = theForm.elements[i];
        if(name == field && fieldName.checked) {
            selectedValue = fieldName.value;
            break;
        }
    }
    return selectedValue;
}

function getSelectedValueFromForm(theForm, field) {
    var selectedValue = null;
    var testField = null;
    var name = null;
    if (theForm) {
        for (var i = 0; i < theForm.elements.length; i++) {
            testField = theForm.elements[i];
            name = testField.name;
            if (name == null) {
                continue;
            }
            name = name.substr(name.lastIndexOf(".")+1);
            if ((name == field) && testField.checked) {
                selectedValue = testField.value;
                break;
            }
        }
    }
    return selectedValue;
}

function checkForSelectedValue(fieldId) {
    var field = document.getElementById(fieldId);
    if (field.value == '' || isWhitespace(field.value)) {
        return false;
    }
    return true;
}

function reloadHeaderFrame() {
    var mastheadForm = document.getElementById('af');
    admingui.ajax.postAjaxRequest(mastheadForm, { render: 'af' }, 'af', false);
}

admingui.deploy = {
    uploadInit: function(dirPathId, dirSelectBtnId, filSelectBtnId, fileuploadId) {
        //
        //We need to set a timeout to delay the call to getTextElement inside disable component.
        //otherwise getTextElement will always return null, causing JS error.
        //disableComponent(dirPathId, 'text');
        window.setTimeout("disableComponent('" + dirPathId+ "', 'text')", 1);
        if(getSelectedValueFromForm(document.forms['form'], 'uploadRdBtn')=='serverSide'){
            enableDOMComponent(dirPathId);
            if ( (dirSelectBtnId != null) && (dirSelectBtnId != '') && (isWhitespace(dirSelectBtnId) == false)){
                enableBtnComponent(dirSelectBtnId);
            }
            enableBtnComponent(filSelectBtnId);
            disableComponent(fileuploadId, 'file');
        }
    },

    uploadRdBtnAction : function(dirPathId, dirSelectBtnId, filSelectBtnId, fileuploadId, radioChoosenId) {
        //disableDOMComponent(dirPathId);
        window.setTimeout("disableComponent('" + dirPathId + "', 'text')", 1);
        if ( (dirSelectBtnId != null) && (dirSelectBtnId != '') && (isWhitespace(dirSelectBtnId) == false)){
        disableBtnComponent(dirSelectBtnId);
        }
        disableBtnComponent(filSelectBtnId);
        enableComponent(fileuploadId, 'file');
        comp = getTextElement(radioChoosenId);
        comp.value='client';
    },

    fileChooseAction : function(dirPathId, dirSelectBtnId, filSelectBtnId, fileuploadId, radioChoosenId) {
        enableDOMComponent(dirPathId);
        if ( (dirSelectBtnId != null) && (dirSelectBtnId != '') && (isWhitespace(dirSelectBtnId) == false)){
        enableBtnComponent(dirSelectBtnId);
        }
        enableBtnComponent(filSelectBtnId);
        disableComponent(fileuploadId, 'file');
        comp = getTextElement(radioChoosenId);
        comp.value='local';
    },

    populateDir : function (fileChooserId, dirPathId){
        var component = document.getElementById(fileChooserId);
        var fc = component.getSelectionValue();        
        window.opener.getTextElement(dirPathId).value = fc;
        return true;
    },


    showPropertySheet : function(propSheetId, obj, appNameId, contextRootId, appTypeString, appName){
        var cc = null;
        var comp = null;

        var sheets = appTypeString.split(',');
        if (propSheetId.length <=0){
            for( ix=0; ix < sheets.length; ix++){
                comp = obj.document.getElementById('form:' + sheets[ix]);
                if (comp != null)
                    comp.style.display='none';
            }
        }else{
            for (i=0; i < sheets.length; i++){
                cc = obj.document.getElementById('form:'+sheets[i]);
                if (cc == null){
                    continue;
                }
                if (propSheetId == sheets[i]){
                    cc.style.display='block';
                }else{
                    cc.style.display='none';
                }
            }
        }

        if (typeof(appName) != 'undefined' ) {
            //appName should be up to the last dot. eg. tmpName of  hello.123.war should be hello.123
            var ix = appName.lastIndexOf(".");
            if (ix != -1){
                appName = appName.substring(0, ix);
            }
            admingui.deploy.setAppName(appNameId, appName, obj, appTypeString);
            //may as well set context root if it exist.
            var component = obj.document.getElementById(contextRootId);
            if (component != null){
                component.value = appName;
            }
        }
    },

    setAppName : function (appNameId, appName, obj, appTypeString){
        // Fill in application name
        if (appNameId==null || appNameId.length <=0){
        // shouldn't be.
        }else{
            var ix = appNameId.indexOf(":");
            var ix2 = appNameId.substr(ix+1).indexOf(":");
            var str3 = appNameId.substr(ix+1+ix2);
            var sheets = appTypeString.split(',');
            for( idx=0; idx < sheets.length; idx++){
                var comp = obj.document.getElementById('form:'+sheets[idx]+str3);
                if (comp != null){
                    comp.value=appName;
                }
            }
        }
    },

    setFieldValue : function(appNameId, value, dropDownProp, typeId, contextRootId, extensionId, obj, appTypeString) {
        var appName = extractName(value);
        var sfex = getSuffix(appName);
        var sfex2 = '';
        obj.document.getElementById(extensionId).value=sfex;
        var appTypes = ','+appTypeString+',';

        //If no extension for file choosen, or no plugin for that extension, show dropDown type and don't fill in anything, then return;
        if (sfex != null && sfex.length > 0){
            sfex2 = sfex.substr(1);
            var tests = ','+sfex2+',';
            var inx = appTypes.indexOf(tests) ;
            if (inx == -1){
                sfex2 = '';
            }
        }

        //for redeploy, there is no dropdown type to choose from.
        if (typeId != ""){
            obj.document.getElementById(typeId).value = sfex2;
            obj.document.getElementById(dropDownProp).style.display = 'block';
            admingui.deploy.showPropertySheet(sfex2, obj, appNameId, contextRootId, appTypeString, appName);
        }
    },

    populateDirAndAppName : function(fileChooserId, dirPathId, appNameId, typeId, dropDownProp, contextRootId, extensionId, appTypeString){
        var fc = document.getElementById(fileChooserId).getSelectionValue();
        window.opener.getTextElement(dirPathId).value = fc;
        //for redeploy, there is no dropdown for app type, there is no need to fill in any field.
        if (dropDownProp != ""){
            admingui.deploy.setFieldValue(appNameId, fc, dropDownProp, typeId, contextRootId, extensionId, window.opener, appTypeString);
        }
    },

    checkFileInputRequired : function (componentId, reqMsg){
        var component = getFileInputElement(componentId);
        var value = component.value;
        var result = (value != '') && (isWhitespace(value) == false);
        if (result == false) {
            if (reqMsg == '') {
                showAlert(getLabel(component) + ' is a required field.');
            } else {
                showAlert(reqMsg + ' ' + getLabel(component));
            }
            component.select();
            component.focus();
        }
        return result;
    },


    checkTarget : function (onlyDAS, addRemoveId, confirmMsg){
        if (onlyDAS=='true') return true;
        var component = document.getElementById(addRemoveId);
        if (component != null){
            var target = document.getElementById(addRemoveId).value;
            if (target==null || target==""){
                return getConfirm(this, confirmMsg);
            }
        }
        return true;
    },

    checkType: function( typeId, alertMsg){
        var value = document.getElementById(typeId).value;
        var result = (value != null) && (value != '') && (isWhitespace(value) == false);
        if (result == false) {
            showAlert(alertMsg);
            return false;
        }
        return true;
    }

}

admingui.table = {
    changeOneTableButton : function(topActionGroup, tableId){
        var buttons = new Array();
        buttons[0] = topActionGroup.concat(":button1");
        admingui.table.changeButtons(buttons,tableId);
    },

    changeThreeTableButtons : function(topActionGroup, tableId){
        var buttons = new Array();
        buttons[0] = topActionGroup.concat(":button1");
        buttons[1] = topActionGroup.concat(":button2");
        buttons[2] = topActionGroup.concat(":button3");
        admingui.table.changeButtons(buttons,tableId);
    },

    toggleButtons : function(topActionGroup, tableId) {
        var buttons = new Array();
        var tag = document.getElementById(topActionGroup);
        for (var i = 0; i < tag.childNodes.length; i++) {
            var child = tag.childNodes[i];
            if ((child.nodeName == "INPUT") && (child.id.indexOf("new") == -1)) {
                buttons.push(child.id);
            }
        }
        admingui.table.changeButtons(buttons,tableId);
    },

    changeButtons : function (buttons,tableId){
        try {
            var table = document.getElementById(tableId);// + ":_table");
            var selections = table.getAllSelectedRowsCount();
            var disabled = (selections > 0) ? false : true;
            for (count=0; count < buttons.length; count++) {
                var element = document.getElementById(buttons[count]);
                if (element) {
                    element.disabled = disabled;
                    element.className = disabled ? "Btn2Dis_sun4" : "Btn1_sun4";
                }
            }
        } catch (err) {
            alert(err);
        }
    },

    initAllRows : function (tableId) {
        var table = document.getElementById(tableId);
        table.initAllRows();
    }
}

admingui.ajax = {
    lastPageLoaded : '',
    ajaxCount: 0,
    ajaxTimer: null,

    getXMLHttpRequestObject: function() {
        var reqObj = null;
        if (window.XMLHttpRequest) {
            reqObj = new XMLHttpRequest();
        } else if (window.ActiveXObject) {
            try {
                reqObj = new ActiveXObject("Msxml2.XMLHTTP");
            } catch (ex) {
                reqObj = new ActiveXObject("Microsoft.XMLHTTP");
            }
        }
        return reqObj;
    },

    get: function(url, targetId, callback, beforesend) {
        // Ping header to make sure header stays "fresh"
        if (targetId && (targetId == 'content')) {
            admingui.ajax.pingHeader();
        }

        var req = admingui.ajax.getXMLHttpRequestObject();
        if (req) {
            req.targetId = targetId;
            req.onreadystatechange =
            function() {
                if (req.readyState == 4) {
                    callback(req, targetId, url);
                /*
			// Make a tempoary elemnt to contain the help content
			var tmpDiv = document.createElement("div");
			tmpDiv.innerHTML = req.responseText;

			// Fix URLs in the help content...
			admingui.help.fixHelpURLs(url, tmpDiv);

			// Show the help content...
			targetNode.innerHTML = tmpDiv.innerHTML;
			*/
                }
            };
            //IE caches GUI page and many pages are not loaded correctly.  So we force the reload
            //in IE.  Refer to GLASSFISH-15628
            if (navigator != null) {
                var ua = navigator.userAgent;
                if (ua.indexOf("MSIE") > -1) {
                    if (url.indexOf("?") > -1) {
                        url = url + "&cachebuster=" + new Date().getTime();
                    } else {
                        url = url + "?cachebuster=" + new Date().getTime();
                    }
		}
	    }
            req.open("GET", url, true);
            if (beforesend) {
                // Callback that can be used to modify request before it is sent
                beforesend(req);
            }
            req.send("");
        }
    },

    ajaxStart : function() {
//        admingui.ajax._setVisibility('ajaxIndicator', 'visible');
        admingui.ajax._clearAjaxTimer();
        admingui.ajax.ajaxTimer = setTimeout("admingui.ajax._displayAjaxLoadingPanel()", 2000);
    },

    _displayAjaxLoadingPanel : function() {
        var ajaxPanel = document.getElementById('ajaxPanel');
        if (ajaxPanel != null) {
            window.onscroll = function () {
                ajaxPanel.style.top = document.body.scrollTop;
            };
            ajaxPanel.style.display = "block";
            ajaxPanel.style.top = document.body.scrollTop;
            ajaxPanel.style.visibility = "visible";
            document.getElementById('ajaxPanelClose').focus();
        }
    },

    _setVisibility : function (id, state) {
        var el = document.getElementById(id);
        if (el != null) {
            el.style.visibilty = state;
        }
    },

    _clearAjaxTimer : function() {
        if (admingui.ajax.ajaxTimer != null) {
            clearTimeout(admingui.ajax.ajaxTimer);
            admingui.ajax.ajaxTimer = null;
        }
    },

    ajaxComplete : function() {
        admingui.ajax._clearAjaxTimer();
        var ajaxPanel = document.getElementById('ajaxPanel');
        if (ajaxPanel != null) {
            ajaxPanel.style.display = "none";
            ajaxPanel.style.visibility = "hidden";
        }
        admingui.ajax._setVisibility('ajaxIndicator',  'hidden');
    },

    loadPage : function (args) {
        admingui.ajax.ajaxStart();
        var url = admingui.ajax.modifyUrl(args.url);
        //args.lastPage = document.getElementById(admingui.nav.TREE_ID).getSelectedTreeNode;
        //admingui.util.log("Loading " + url + " via ajax.");

        // Make cursor spin...
        document.body.style.cursor = 'wait';

        // Make request
        admingui.ajax.get(url, "content", admingui.ajax.defaultGetCallback);
        if (typeof oldOnClick == 'function') {
            admingui.util.log('Skipping onclick...' + oldOnClick);
        //          oldOnClick();
        }
        return false;
    },

    processPageAjax : function (o) {
        var tree = document.getElementById(admingui.nav.TREE_ID);
        tree.clearAllHighlight(admingui.nav.TREE_ID);
        var selnode = tree.getSelectedTreeNode(admingui.nav.TREE_ID);

        admingui.ajax.updateCurrentPageLink(o.argument.url);
        var contentNode = o.argument.target;
        if (contentNode == null) {
            contentNode = document.getElementById("content");
        }
        contentNode.innerHTML = o.responseText;
        // FIXME: These 2 functions only need to be replaced after a FPR...
        webui.suntheme.hyperlink.submit = admingui.woodstock.hyperLinkSubmit;
        webui.suntheme.jumpDropDown.changed = admingui.woodstock.dropDownChanged;
        admingui.ajax.processElement(o, contentNode, true);
        admingui.ajax.processScripts(o);
        // Restore cursor
        document.body.style.cursor = 'auto';
        var node = o.argument.sourceNode;
        if (typeof node != 'undefined') {
        //admingui.nav.selectTreeNodeById(node.parentNode.parentNode.id);
        }
        admingui.nav.selectTreeNodeWithURL(o.argument.url);
    },

    postAjaxRequest : function (component, args, respTarget, displayLoading) {
        if (displayLoading !== false) {
            admingui.ajax.ajaxStart();
        }
        if ((respTarget === null) || (typeof(respTarget) === 'undefined')) {
            respTarget = 'content';
        }
        component.respTarget = respTarget;
        var params = {
            // I need to do this by default so all form values get processed.
            execute: '@all',
            bare: true,
            render: '@all'
        };
        if ((args !== null) && (typeof(args) !== 'undefined')) {
            for (var name in args) {
                params[name] = args[name];
            }
        }
        if (params.render == "@all") {
            // Don't do this for user-defined render value
            params.onComplete = admingui.ajax.handleResponse;

            // Make cursor spin... (only do this when we're handling the response)
            document.body.style.cursor = 'wait';
        }
        // Ping header to make sure header stays "fresh"
        admingui.ajax.pingHeader();
        jsf.ajax.request(component, null, params);
    },

    defaultGetCallback: function(xmlReq, target, url) {
        admingui.ajax.ajaxComplete();
        if (window != top) {
            // May be inside a frame...
            return top.admingui.ajax.defaultGetCallback(xmlReq, target, url);
        }
        var contentNode = target;
        if (typeof(contentNode) === 'string') {
            contentNode = document.getElementById(contentNode);
        }
        if ((contentNode === null) || (typeof(contentNode) === 'undefined')) {
            contentNode = top.document.getElementById("content");
        }

        if (typeof(webui) !== 'undefined') {
            // FIXME: These 2 functions (should) only need be replaced after FPR...
            webui.suntheme.hyperlink.submit = admingui.woodstock.hyperLinkSubmit;
            webui.suntheme.jumpDropDown.changed = admingui.woodstock.dropDownChanged;
        }

        contentNode.innerHTML = xmlReq.responseText;

        var contextObj = {};
        admingui.ajax.processElement(contextObj, contentNode, true);
        admingui.ajax.processScripts(contextObj);

        // Restore cursor
        document.body.style.cursor = 'auto';

        admingui.nav.selectTreeNodeWithURL(url);
    },

    /**
     *	This function handles JSF2 Ajax responses.  It is expected that the
     *	response will replace the innerHTML of an element rather than the whole
     *	page.  If the content contains JSF2 markup, it will attempt to use the
     *	content defined in the javax.faces.ViewRoot "update" element.  It will
     *	also attempt to update the ViewState.  If this is not found in the
     *	response, it will try to use the entire response for the value of
     *	innerHTML.
     */
    handleResponse : function () {
        admingui.ajax.ajaxComplete();
        admingui.ajax.fixQue(this.que);
        //admingui.ajax.updateCurrentPageLink(o.argument.url);  <-- find a better way to get the viewId
        var contentNode = null;
        if ((this.context) && (this.context.source)) {
            contentNode = this.context.source.respTarget;
        }
        if ((contentNode === null) || (typeof(contentNode) === 'undefined')) {
            contentNode = document.getElementById("content");
        } else if (typeof(contentNode) === 'string') {
            contentNode = document.getElementById(contentNode);
        }
        var result = this.xmlReq.responseText;
        var len = (result.length > 200) ? 200 : result.length;
        var testString = result.substring(0, len);
        var viewState = null;
        if (testString.indexOf("<changes>") > 0) {
            // We have a JSF response... if id="javax.faces.ViewRoot", handle it
            var idx = testString.indexOf('id="javax.faces.ViewRoot"');
            if (idx > 0) {
                try {
                    var nodes = this.xmlReq.responseXML.getElementsByTagName("partial-response")[0].childNodes[0].childNodes;
                    for (var cnt=0; cnt<nodes.length; cnt++) {
                        var node = nodes[cnt];
                        if (node.getAttribute('id') === 'javax.faces.ViewRoot') {
                            result = node.textContent;
                        }
                        if (node.getAttribute('id') === 'javax.faces.ViewState') {
                            // NOTE: see jsf.ajax.doUpdate for more info....
                            viewState = node.firstChild;
                        }
                    }
                } catch (ex) {
                    admingui.util.log("***** Unable to parse XML:  " + ex);
                }
            }
        }
        contentNode.innerHTML = result;
        if (viewState != null) {
            var form = document.getElementById(this.context.formid);
            if (!form) {
                admingui.util.log("***** Unable to find form! " + this.context.formid);
                return;
            }
            var field = form.elements['javax.faces.ViewState'];
            if (typeof(field) === 'undefined') {
                field = document.createElement("input");
                field.type = "hidden";
                field.name = "javax.faces.ViewState";
                field.id = "javax.faces.ViewState";
                form.appendChild(field);
            }
            field.value = viewState.nodeValue;
        }

        // FIXME: These 2 functions (should) only need be replaced after FPR...
        webui.suntheme.hyperlink.submit = admingui.woodstock.hyperLinkSubmit;
        webui.suntheme.jumpDropDown.changed = admingui.woodstock.dropDownChanged;
        var contextObj = {};
        admingui.ajax.processElement(contextObj, contentNode, true);
        admingui.ajax.processScripts(contextObj);

        // Restore cursor
        document.body.style.cursor = 'auto';

        // Tree select code??  FIXME: broken...
        /*
        var node = o.argument.sourceNode;
        if (typeof node != 'undefined') {
            admingui.nav.selectTreeNodeById(node.parentNode.parentNode.id);
        }
	*/
    },

    fixQue: function(que) {
        while (!que.isEmpty()) {
            // dump everything for now...
            que.dequeue();
        }
    },

    updateCurrentPageLink : function (url) {
        admingui.ajax.lastPageLoaded = url;
    //document.getElementById("currentPageLink").href = url;
    },

    processElement : function (context, node, queueScripts) {
        var recurse = true;
        //console.log("nodeName = " + node.nodeName);
        if (node.nodeName == 'A') {
            // FIXME: For exteral URLs, we should not replace... however, we
            // FIXME: may want to ensure they have a _blank target.  May need
            // FIXME: to compare the host to see if a URL is an external URL
            // FIXME: b/c M$ makes it hard to determine relative URLs, and full
            // FIXME: URLs to the same host "might" want be valid for
            // FIXME: replacement.
            if (!admingui.ajax._isTreeNodeControl(node) && (node.target == '')) { //  && (typeof node.onclick != 'function'))
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
                        admingui.ajax.loadPage({
                            url : url,
                            target: document.getElementById('content'),
                            oldOnClickHandler: oldOnClick,
                            sourceNode: node
                        });
                        return false;
                    };
                }
            }
        } else if (node.nodeName == 'IFRAME') {
            recurse = false;
        } else if (node.nodeName == 'INPUT') {
            if (((node.type == 'submit') || (node.type == 'image'))
                && ((node.onclick === null) || (typeof(node.onclick) === 'undefined') || (node.onclick == ''))) {
                // Submit button w/o any JS, make it a partial page submit
                node.onclick = function() {
                    var args = {};
                    args[node.id] = node.id;
                    admingui.ajax.postAjaxRequest(this, args);
                    return false;
                };
            }
        /*
        } else if (node.nodeName == 'FORM') {
            admingui.util.log("***** form action:  " + node.action);
            if (node.target == '') {
                node.onsubmit = function () {
                    admingui.ajax.submitFormAjax(node);
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
                admingui.ajax.processElement(context, node.childNodes[i], queueScripts);
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
    },

    modifyUrl : function (url) {
        // If the url does not start with 'http' (or 'https' by extension), calculate
        // the "base" URL based off of window.location
        if (url.substr(0,4) != 'http') {
            //http://localhost:4848/common/applications/applications.jsf
            //http://admin.foo.com/common/applications/applications.jsf
            var location = window.location;
            url = location.protocol + "//" + location.host + url
        }
        if (url.indexOf('bare=') > -1) {
            return url;
        }

        var insert = '?bare=true';
        var changed = url;

        if (url.indexOf("?") > -1) {
            insert = "&bare=true"
        }
        var hash = url.indexOf("#");
        if (hash > 1) {
            changed = url.substr(0, hash) + insert + url.substr(hash);
        } else {
            changed = url + insert;
        }

        return changed;
    },

    /**
     *	handler - The name of the handler to invoke.
     *	args - An object containing properties / values for the parameters.
     *	callback - A JS function that should be notified.
     *	depth - the max depth of all return variables to be encoded in json
     *	async - false if a syncronous request is desired, default: true
     */
    invoke: function(handler, args, callback, depth, async) {
        if ((typeof(handler) === 'undefined') || (handler == '')) {
            return;
        }
        if (typeof(callback) === 'undefined') {
            callback = function() {};
        }
        var params = '';
        for (var param in args) {
            // Create a String to represent all the parameters
            // escape, this will prevent the server-side from (fully)
            // urldecoding it.  Allowing me to first parse the commas, then
            // decode the content.
            params += param + ':' + escape(args[param]) + ',';
        }
        if (typeof(async) === 'undefined') {
            async = true;
        }
        if (!(typeof(jsf) === 'undefined') && !(typeof(jsf.ajax) === 'undefined')) {
            // Warp user's function to make easier to use
            var func = function(data) {
                if (data.status === 'success') {
                    var respElt = document.getElementById('execResp');
                    if (typeof(respElt) !== 'undefined') {
                        var result = '';
                        if (respElt.value != '') {
                            result = '(' + respElt.value + ')';
                            result = eval(result);
                        }
                        callback(result, data);
                    }
                }
            }
            if (typeof(depth) === 'undefined') {
                depth = 3;
            }
            var src = document.getElementById('execButton');
            if ((src == null) || (typeof(src) === 'undefined')) {
                alert("'execButton' not found!  Unable to submit JSF2 Ajax Request!");
            } else {
                // Don't ping b/c this is from the header and therefor is a ping
                jsf.ajax.request(src, null,
                {
                    execute: 'execButton',
                    render: 'execResp',
                    execButton: 'execButton',
                    h: handler,
                    d: depth,
                    a: params,
                    onevent: func,
                    async: async
                });
            }
        } else {
            alert('JSF2+ Ajax Missing!');
        }
    },

    getResource: function(path, callback) {
        admingui.ajax.invoke("gf.serveResource", {
            path:path,
            content:content
        }, callback, 1, true);
    },

    /**
     *	This ensure the header "page" (view state data) stays in JSF's history
     */
    pingHeader: function() {
        // Ping every 6 Ajax requests...
        if ((++admingui.ajax.ajaxCount) > 5) {
            // Reset counter...
            admingui.ajax.ajaxCount = 0;

            // Get element from header form...
            var src = document.getElementById('execButton');
            var options = {
                // noop
                execute: '@none',
                render: '@none'
            };
            jsf.ajax.request(src, null, options);
        }
    }
}

admingui.woodstock = {
    hyperLinkSubmit: function(hyperlink, formId, params) {
        var form = document.getElementById(formId);

        // Add any extra args that are necessary...
        var args = {};
        var linkId = hyperlink.id;
        args[linkId + "_submittedField"] = linkId;
        /*
	 * Not needed we're executing everything anyway
	var idx = linkId.indexOf('row');
	if (idx > 0) {
	    idx = linkId.indexOf(':', idx + 3);
	    if (idx > 0) {
		// We have a "*row*:" in the id name, backup the execute id to
		// the row portion to ensure an entire table gets executed,
		// otherwise it might not get executed at all.
		args['execute'] = '@all';//linkId.substring(0, idx);
	    }
	}
	*/

        //params are name value pairs but all one big string array
        //so params[0] and params[1] form the name and value of the first param
        if (params != null) {
            for (var i = 0; i < params.length; i+=2) {
                args[params[i]] = params[i+1];
            }
        }

        // Check target
        var oldtarget = form.target;
        if (hyperlink.target != "") {
            form.target = hyperlink.target;
        }
        admingui.ajax.postAjaxRequest(hyperlink, args);

        // Retore form
        form.target = oldtarget;

        return false;
    },

    dropDownChanged: function(jumpDropdown) {
        if (typeof(jumpDropdown) === "string") {
            jumpDropdown = webui.suntheme.dropDown.getSelectElement(jumpDropdown);
        }

        // Force WS "submitter" flag to true
        var submitterFieldId = jumpDropdown.id + "_submitter";
        var submitterField = document.getElementById(submitterFieldId);
        if (!submitterField) {
            submitterFieldId = jumpDropdown.parentNode.id + "_submitter";
            submitterField = document.getElementById(submitterFieldId);
            if (!submitterField) {
                admingui.util.log("Unable to find dropDown submitter for: "
                    + jumpDropdown.id);
                return false;
            }
        }
        submitterField.value = "true";

        // FIXME: Not sure why the following is done...
        var listItem = jumpDropdown.options;
        for (var cntr=0; cntr < listItem.length; ++cntr) {
            if (listItem[cntr].className ==
                webui.suntheme.props.jumpDropDown.optionSeparatorClassName
                || listItem[cntr].className ==
                webui.suntheme.props.jumpDropDown.optionGroupClassName) {
                continue;
            } else if (listItem[cntr].disabled) {
                // Regardless if the option is currently selected or not,
                // the disabled option style should be used when the option
                // is disabled. So, check for the disabled item first.
                // See CR 6317842.
                listItem[cntr].className = webui.suntheme.props.jumpDropDown.optionDisabledClassName;
            } else if (listItem[cntr].selected) {
                listItem[cntr].className = webui.suntheme.props.jumpDropDown.optionSelectedClassName;
            } else {
                listItem[cntr].className = webui.suntheme.props.jumpDropDown.optionClassName;
            }
        }
        admingui.ajax.postAjaxRequest(jumpDropdown);
        return false;
    },

    commonTaskHandler : function(treeNode, targetUrl) {
        admingui.ajax.loadPage({
            url: targetUrl
        });
        if ((treeNode != null) && (treeNode != '')) {
            admingui.nav.selectTreeNodeById(treeNode);
        }
        return false;
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
        admingui.ajax.getResource(node.src, function(result) {
            globalEval(result.content);
            globalEvalNextScript(scriptQueue);
        } );
    // This gets a relative URL vs. a full URL with http://... needed
    // when we properly serve resources w/ rlubke's recent fix that
    // will be integrated soon.  We need to handle the response
    // differently also.
    //admingui.ajax.getResource(node.attributes['src'].value, function(result) { globalEval(result.content); globalEvalNextScript(scriptQueue);} );
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
