/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2011 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notices:
 *
 * Copyright 2004 The Apache Software Foundation
 * Copyright 2004-2008 Emmanouil Batsis, mailto: mbatsis at users full stop sourceforge full stop net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 @project JSF JavaScript Library
 @version 2.0
 @description This is the standard implementation of the JSF JavaScript Library.
 */

/**
 * Register with OpenAjax
 */
if (typeof OpenAjax !== "undefined" &&
    typeof OpenAjax.hub.registerLibrary !== "undefined") {
    OpenAjax.hub.registerLibrary("jsf", "www.sun.com", "2.0", null);
}

// Detect if this is already loaded, and if loaded, if it's a higher version
if (!((jsf && jsf.specversion && jsf.specversion >= 20000 ) &&
      (jsf.implversion && jsf.implversion >= 3))) {

    /**
     * The top level global namespace for JavaServer Faces functionality.
     * @name jsf
     * @namespace
     */
    var jsf = {};

    /**
     * The namespace for Ajax functionality.
     * @name jsf.ajax
     * @namespace
     * @exec
     */
    jsf.ajax = function() {

        var eventListeners = [];
        var errorListeners = [];

        /**
         * Determine if the current browser is part of Microsoft's failed attempt at
         * standards modification.
         * @ignore
         */
        var isIE = function isIE() {
            if (typeof isIECache !== "undefined") {
                return isIECache;
            }
            isIECache =
                   document.all && window.ActiveXObject &&
                   navigator.userAgent.toLowerCase().indexOf("msie") > -1 &&
                   navigator.userAgent.toLowerCase().indexOf("opera") == -1;
            return isIECache;
        };
        var isIECache;

        /**
         * Determine if loading scripts into the page executes the script.
         * This is instead of doing a complicated browser detection algorithm.  Some do, some don't.
         * @returns {boolean} does including a script in the dom execute it?
         * @ignore
         */
        var isAutoExec = function isAutoExec() {
            try {
                if (typeof isAutoExecCache !== "undefined") {
                    return isAutoExecCache;
                }
                var autoExecTestString = "<script>var mojarra = mojarra || {};mojarra.autoExecTest = true;</script>";
                var tempElement = document.createElement('span');
                tempElement.innerHTML = autoExecTestString;
                var body = document.getElementsByTagName('body')[0];
                var tempNode = body.appendChild(tempElement);
                if (mojarra && mojarra.autoExecTest) {
                    isAutoExecCache = true;
                    delete mojarra.autoExecTest;
                } else {
                    isAutoExecCache = false;
                }
                deleteNode(tempNode);
                return isAutoExecCache;
            } catch (ex) {
                // OK, that didn't work, we'll have to make an assumption
                if (typeof isAutoExecCache === "undefined") {
                    isAutoExecCache = false;
                }
                return isAutoExecCache;
            }
        };
        var isAutoExecCache;

        /**
         * @ignore
         */
        var getTransport = function getTransport() {
            var methods = [
                function() {
                    return new XMLHttpRequest();
                },
                function() {
                    return new ActiveXObject('Msxml2.XMLHTTP');
                },
                function() {
                    return new ActiveXObject('Microsoft.XMLHTTP');
                }
            ];

            var returnVal;
            for (var i = 0, len = methods.length; i < len; i++) {
                try {
                    returnVal = methods[i]();
                } catch(e) {
                    continue;
                }
                return returnVal;
            }
            throw new Error('Could not create an XHR object.');
        };

        /**
         * Find instance of passed String via getElementById
         * @ignore
         */
        var $ = function $() {
            var results = [], element;
            for (var i = 0; i < arguments.length; i++) {
                element = arguments[i];
                if (typeof element == 'string') {
                    element = document.getElementById(element);
                }
                results.push(element);
            }
            return results.length > 1 ? results : results[0];
        };

        /**
         * Get the form element which encloses the supplied element.
         * @param element - element to act against in search
         * @returns form element representing enclosing form, or first form if none found.
         * @ignore
         */
        var getForm = function getForm(element) {
            if (element) {
                var form = $(element);
                while (form) {

                    if (form.nodeName && (form.nodeName.toLowerCase() == 'form')) {
                        return form;
                    }
                    if (form.form) {
                        return form.form;
                    }
                    if (form.parentNode) {
                        form = form.parentNode;
                    } else {
                        form = null;
                    }
                }
                return document.forms[0];
            }
            return null;
        };

        /**
         * Check if a value exists in an array
         * @ignore
         */
        var isInArray = function isInArray(array, value) {
            for (var i = 0; i < array.length; i++) {
                if (array[i] === value) {
                    return true;
                }
            }
            return false;
        };


        /**
         * Evaluate JavaScript code in a global context.
         * @param src JavaScript code to evaluate
         * @ignore
         */
        var globalEval = function globalEval(src) {
            if (window.execScript) {
                window.execScript(src);
                return;
            }
            // We have to wrap the call in an anon function because of a firefox bug, where this is incorrectly set
            // We need to explicitly call window.eval because of a Chrome peculiarity
            var fn = function() {
                window.eval.call(window,src);
            };
            fn();
        };

        /**
         * Get all scripts from supplied string, return them as an array for later processing.
         * @param str
         * @returns {array} of script text
         * @ignore
         */
        var stripScripts = function stripScripts(str) {
            // Regex to find all scripts in a string
            var findscripts = /<script[^>]*>([\S\s]*?)<\/script>/igm;
            // Regex to find one script, to isolate it's content [2] and attributes [1]
            var findscript = /<script([^>]*)>([\S\s]*?)<\/script>/im;
            // Regex to remove leading cruft
            var stripStart = /^\s*(<!--)*\s*(\/\/)*\s*(\/\*)*\s*(<!\[CDATA\[)*/;
            // Regex to find src attribute
            var findsrc = /src="([\S]*?)"/im;
            var initialnodes = [];
            var scripts = [];
            initialnodes = str.match(findscripts);
            while (!!initialnodes && initialnodes.length > 0) {
                var scriptStr = [];
                scriptStr = initialnodes.shift().match(findscript);
                var src = [];
                // check if src specified
                src = scriptStr[1].match(findsrc);
                var script;
                if ( !!src && src[1]) {
                    // if this is a file, load it
                    var url = src[1];
                    // if this is another copy of jsf.js, don't load it
                    // it's never necessary, and can make debugging difficult
                    if (/\/javax.faces.resource\/jsf.js\?ln=javax\.faces/.test(url)) {
                        script = false;
                    } else {
                        script = loadScript(url);
                    }
                } else if (!!scriptStr && scriptStr[2]){
                    // else get content of tag, without leading CDATA and such
                    script = scriptStr[2].replace(stripStart,"");
                } else {
                    script = false;
                }
                if (!!script) {
                    scripts.push(script);
                }
            }
            return scripts;
        };

        /**
         * Load a script via a url, use synchronous XHR request.  This is liable to be slow,
         * but it's probably the only correct way.
         * @param url the url to load
         * @ignore
         */
        var loadScript = function loadScript(url) {
            var xhr = getTransport();
            if (xhr === null) {
                return "";
            }

            xhr.open("GET", url, false);
            xhr.setRequestHeader("Content-Type", "application/x-javascript");
            xhr.send(null);

            // PENDING graceful error handling
            if (xhr.readyState == 4 && xhr.status == 200) {
                    return xhr.responseText;
            }

            return "";
        };

        /**
         * Run an array of scripts text
         * @param scripts array of script nodes
         * @ignore
         */
        var runScripts = function runScripts(scripts) {
            if (!scripts || scripts.length === 0) {
                return;
            }

            var head = document.getElementsByTagName('head')[0] || document.documentElement;
            while (scripts.length) {
                // create script node
                var scriptNode = document.createElement('script');
                scriptNode.type = 'text/javascript';
                scriptNode.text = scripts.shift(); // add the code to the script node
                head.appendChild(scriptNode); // add it to the page
                head.removeChild(scriptNode); // then remove it
            }
        };

        /**
         * Replace DOM element with a new tagname and supplied innerHTML
         * @param element element to replace
         * @param tempTagName new tag name to replace with
         * @param src string new content for element
         * @ignore
         */
        var elementReplaceStr = function elementReplaceStr(element, tempTagName, src) {

            var temp = document.createElement(tempTagName);
            if (element.id) {
                temp.id = element.id;
            }

            // Creating a head element isn't allowed in IE, and faulty in most browsers,
            // so it is not allowed
            if (element.nodeName.toLowerCase() === "head") {
                throw new Error("Attempted to replace a head element - this is not allowed.");
            } else {
                var scripts = [];
                if (isAutoExec()) {
                    temp.innerHTML = src;
                } else {
                    // Get scripts from text
                    scripts = stripScripts(src);
                    // Remove scripts from text
                    src = src.replace(/<script[^>]*>([\S\s]*?)<\/script>/igm,"");
                    temp.innerHTML = src;
                }
            }

            replaceNode(temp, element);
            runScripts(scripts);

        };

        /**
         * Get a string with the concatenated values of all string nodes under the given node
         * @param  oNode the given DOM node
         * @param  deep boolean - whether to recursively scan the children nodes of the given node for text as well. Default is <code>false</code>
         * @ignore
         * Note:  This code originally from Sarissa: http://dev.abiss.gr/sarissa
         * It has been modified to fit into the overall codebase
         */
        var getText = function getText(oNode, deep) {
            var Node = {ELEMENT_NODE: 1, ATTRIBUTE_NODE: 2, TEXT_NODE: 3, CDATA_SECTION_NODE: 4,
                ENTITY_REFERENCE_NODE: 5,  ENTITY_NODE: 6, PROCESSING_INSTRUCTION_NODE: 7,
                COMMENT_NODE: 8, DOCUMENT_NODE: 9, DOCUMENT_TYPE_NODE: 10,
                DOCUMENT_FRAGMENT_NODE: 11, NOTATION_NODE: 12};

            var s = "";
            var nodes = oNode.childNodes;
            for (var i = 0; i < nodes.length; i++) {
                var node = nodes[i];
                var nodeType = node.nodeType;
                if (nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE) {
                    s += node.data;
                } else if (deep === true && (nodeType == Node.ELEMENT_NODE ||
                                             nodeType == Node.DOCUMENT_NODE ||
                                             nodeType == Node.DOCUMENT_FRAGMENT_NODE)) {
                    s += getText(node, true);
                }
            }
            return s;
        };

        var PARSED_OK = "Document contains no parsing errors";
        var PARSED_EMPTY = "Document is empty";
        var PARSED_UNKNOWN_ERROR = "Not well-formed or other error";
        var getParseErrorText;
        if (isIE()) {
            /**
             * Note: This code orginally from Sarissa: http://dev.abiss.gr/sarissa
             * @ignore
             */
            getParseErrorText = function (oDoc) {
                var parseErrorText = PARSED_OK;
                if (oDoc && oDoc.parseError && oDoc.parseError.errorCode && oDoc.parseError.errorCode !== 0) {
                    parseErrorText = "XML Parsing Error: " + oDoc.parseError.reason +
                                     "\nLocation: " + oDoc.parseError.url +
                                     "\nLine Number " + oDoc.parseError.line + ", Column " +
                                     oDoc.parseError.linepos +
                                     ":\n" + oDoc.parseError.srcText +
                                     "\n";
                    for (var i = 0; i < oDoc.parseError.linepos; i++) {
                        parseErrorText += "-";
                    }
                    parseErrorText += "^\n";
                }
                else if (oDoc.documentElement === null) {
                    parseErrorText = PARSED_EMPTY;
                }
                return parseErrorText;
            };
        } else { // (non-IE)

            /**
             * <p>Returns a human readable description of the parsing error. Useful
             * for debugging. Tip: append the returned error string in a &lt;pre&gt;
             * element if you want to render it.</p>
             * @param  oDoc The target DOM document
             * @returns {String} The parsing error description of the target Document in
             *          human readable form (preformated text)
             * @ignore
             * Note:  This code orginally from Sarissa: http://dev.abiss.gr/sarissa
             */
            getParseErrorText = function (oDoc) {
                var parseErrorText = PARSED_OK;
                if ((!oDoc) || (!oDoc.documentElement)) {
                    parseErrorText = PARSED_EMPTY;
                } else if (oDoc.documentElement.tagName == "parsererror") {
                    parseErrorText = oDoc.documentElement.firstChild.data;
                    parseErrorText += "\n" + oDoc.documentElement.firstChild.nextSibling.firstChild.data;
                } else if (oDoc.getElementsByTagName("parsererror").length > 0) {
                    var parsererror = oDoc.getElementsByTagName("parsererror")[0];
                    parseErrorText = getText(parsererror, true) + "\n";
                } else if (oDoc.parseError && oDoc.parseError.errorCode !== 0) {
                    parseErrorText = PARSED_UNKNOWN_ERROR;
                }
                return parseErrorText;
            };
        }

        if ((typeof(document.importNode) == "undefined") && isIE()) {
            try {
                /**
                 * Implementation of importNode for the context window document in IE.
                 * If <code>oNode</code> is a TextNode, <code>bChildren</code> is ignored.
                 * @param oNode the Node to import
                 * @param bChildren whether to include the children of oNode
                 * @returns the imported node for further use
                 * @ignore
                 * Note:  This code orginally from Sarissa: http://dev.abiss.gr/sarissa
                 */
                document.importNode = function(oNode, bChildren) {
                    var tmp;
                    if (oNode.nodeName == '#text') {
                        return document.createTextNode(oNode.data);
                    }
                    else {
                        if (oNode.nodeName == "tbody" || oNode.nodeName == "tr") {
                            tmp = document.createElement("table");
                        }
                        else if (oNode.nodeName == "td") {
                            tmp = document.createElement("tr");
                        }
                        else if (oNode.nodeName == "option") {
                            tmp = document.createElement("select");
                        }
                        else {
                            tmp = document.createElement("div");
                        }
                        if (bChildren) {
                            tmp.innerHTML = oNode.xml ? oNode.xml : oNode.outerHTML;
                        } else {
                            tmp.innerHTML = oNode.xml ? oNode.cloneNode(false).xml : oNode.cloneNode(false).outerHTML;
                        }
                        return tmp.getElementsByTagName("*")[0];
                    }
                };
            } catch(e) {
            }
        }
        // Setup Node type constants for those browsers that don't have them (IE)
        var Node = {ELEMENT_NODE: 1, ATTRIBUTE_NODE: 2, TEXT_NODE: 3, CDATA_SECTION_NODE: 4,
            ENTITY_REFERENCE_NODE: 5,  ENTITY_NODE: 6, PROCESSING_INSTRUCTION_NODE: 7,
            COMMENT_NODE: 8, DOCUMENT_NODE: 9, DOCUMENT_TYPE_NODE: 10,
            DOCUMENT_FRAGMENT_NODE: 11, NOTATION_NODE: 12};

        // PENDING - add support for removing handlers added via DOM 2 methods
        /**
         * Delete all events attached to a node
         * @param node
         * @ignore
         */
        var clearEvents = function clearEvents(node) {
            if (!node) {
                return;
            }

            // don't do anything for text and comment nodes - unnecessary
            if (node.nodeType == Node.TEXT_NODE || node.nodeType == Node.COMMENT_NODE) {
                return;
            }

            var events = ['abort', 'blur', 'change', 'error', 'focus', 'load', 'reset', 'resize', 'scroll', 'select', 'submit', 'unload',
            'keydown', 'keypress', 'keyup', 'click', 'mousedown', 'mousemove', 'mouseout', 'mouseover', 'mouseup', 'dblclick' ];
            try {
                for (var e in events) {
                    if (events.hasOwnProperty(e)) {
                        node[e] = null;
                    }
                }
            } catch (ex) {
                // it's OK if it fails, at least we tried
            }
        };

        /**
         * Deletes node
         * @param node
         * @ignore
         */
        var deleteNode = function deleteNode(node) {
            if (!node) {
                return;
            }
            if (!node.parentNode) {
                // if there's no parent, there's nothing to do
                return;
            }
            if (!isIE()) {
                // nothing special required
                node.parentNode.removeChild(node);
                return;
            }
            // The rest of this code is specialcasing for IE
            if (node.nodeName.toLowerCase() === "body") {
                // special case for removing body under IE.
                deleteChildren(node);
                try {
                    node.outerHTML = '';
                } catch (ex) {
                    // fails under some circumstances, but not in RI
                    // supplied responses.  If we've gotten here, it's
                    // fairly safe to leave a lingering body tag rather than
                    // fail outright
                }
                return;
            }
            var temp = node.ownerDocument.createElement('div');
            var parent = node.parentNode;
            temp.appendChild(parent.removeChild(node));
            // Now clean up the temporary element
            try {
                temp.outerHTML = ''; //prevent leak in IE
            } catch (ex) {
                // at least we tried.  Fails in some circumstances,
                // but not in RI supplied responses.  Better to leave a lingering
                // temporary div than to fail outright.
            }
        };

        /**
         * Deletes all children of a node
         * @param node
         * @ignore
         */
        var deleteChildren = function deleteChildren(node) {
            if (!node) {
                return;
            }
            for (var x = node.childNodes.length - 1; x >= 0; x--) { //delete all of node's children
                var childNode = node.childNodes[x];
                deleteNode(childNode);
            }
        };

        /**
         * <p> Copies the childNodes of nodeFrom to nodeTo</p>
         *
         * @param  nodeFrom the Node to copy the childNodes from
         * @param  nodeTo the Node to copy the childNodes to
         * @ignore
         * Note:  This code originally from Sarissa:  http://dev.abiss.gr/sarissa
         * It has been modified to fit into the overall codebase
         */
        var copyChildNodes = function copyChildNodes(nodeFrom, nodeTo) {

            if ((!nodeFrom) || (!nodeTo)) {
                throw "Both source and destination nodes must be provided";
            }

            deleteChildren(nodeTo);
            var nodes = nodeFrom.childNodes;
            // if within the same doc, just move, else copy and delete
            if (nodeFrom.ownerDocument == nodeTo.ownerDocument) {
                while (nodeFrom.firstChild) {
                    nodeTo.appendChild(nodeFrom.firstChild);
                }
            } else {
                var ownerDoc = nodeTo.nodeType == Node.DOCUMENT_NODE ? nodeTo : nodeTo.ownerDocument;
                var i;
                if (typeof(ownerDoc.importNode) != "undefined") {
                    for (i = 0; i < nodes.length; i++) {
                        nodeTo.appendChild(ownerDoc.importNode(nodes[i], true));
                    }
                } else {
                    for (i = 0; i < nodes.length; i++) {
                        nodeTo.appendChild(nodes[i].cloneNode(true));
                    }
                }
            }
        };


        /**
         * Replace one node with another.  Necessary for handling IE memory leak.
         * @param node
         * @param newNode
         * @ignore
         */
        var replaceNode = function replaceNode(newNode, node) {
               if(isIE()){
                    node.parentNode.insertBefore(newNode, node);
                    deleteNode(node);
               } else {
                    node.parentNode.replaceChild(newNode, node);
               }
        };


        /**
         * copy all attributes from one element to another - except id
         * @param target element to copy attributes to
         * @param source element to copy attributes from
         * @ignore
         */
        var cloneAttributes = function cloneAttributes(target, source) {

            // enumerate core element attributes - without 'dir' as special case
            var coreElementAttributes = ['className', 'title', 'lang', 'xml:lang'];

            // Enumerate additional input element attributes
            var inputElementAttributes =
                    [   'name', 'value', 'checked', 'disabled', 'readOnly',
                        'size', 'maxLength', 'src', 'alt', 'useMap', 'isMap',
                        'tabIndex', 'accessKey', 'accept', 'type'
                    ];

            // Enumerate all the names of the event listeners
            var listenerNames =
                    [ 'onclick', 'ondblclick', 'onmousedown', 'onmousemove', 'onmouseout',
                        'onmouseover', 'onmouseup', 'onkeydown', 'onkeypress', 'onkeyup',
                        'onhelp', 'onblur', 'onfocus', 'onchange', 'onload', 'onunload', 'onabort',
                        'onreset', 'onselect', 'onsubmit'
                    ];

            var iIndex, iLength; // for loop variables
            var attributeName; // name of the attribute to set
            var newValue, oldValue; // attribute values in each element

            // First, copy over core attributes
            for (iIndex = 0,iLength = coreElementAttributes.length; iIndex < iLength; iIndex++) {
                attributeName = coreElementAttributes[iIndex];
                newValue = source[attributeName];
                oldValue = target[attributeName];
                if (oldValue != newValue) {
                    target[attributeName] = newValue;
                }
            }

            // Next, if it's an input, copy those over
            if (target.nodeName.toLowerCase() === 'input') {
                for (iIndex = 0,iLength = inputElementAttributes.length; iIndex < iLength; iIndex++) {
                    attributeName = inputElementAttributes[iIndex];
                    newValue = source[attributeName];
                    oldValue = target[attributeName];
                    if (oldValue != newValue) {
                        target[attributeName] = newValue;
                    }
                }
            }
            //'style' attribute special case
            var newStyle = source.getAttribute('style');
            var oldStyle = target.getAttribute('style');
            if (newStyle != oldStyle) {
                if (isIE()) {
                    target.style.setAttribute('cssText', newStyle, 0);
                } else {
                    target.setAttribute('style',newStyle);
                }
            }
            for (var lIndex = 0, lLength = listenerNames.length; lIndex < lLength; lIndex++) {
                var name = listenerNames[lIndex];
                target[name] = source[name] ? source[name] : null;
                if (source[name]) {
                    source[name] = null;
                }
            }
            // Special case for 'dir' attribute
            if (!isIE() && source.dir != target.dir) {
                target.dir = source.dir ? source.dir : null;
            }
        };

        /**
         * Replace an element from one document into another
         * @param newElement new element to put in document
         * @param origElement original element to replace
         * @ignore
         */
        var elementReplace = function elementReplace(newElement, origElement) {
            copyChildNodes(newElement, origElement);
            // sadly, we have to reparse all over again
            // to reregister the event handlers and styles
            // PENDING do some performance tests on large pages
            origElement.innerHTML = origElement.innerHTML;

            try {
                cloneAttributes(origElement, newElement);
            } catch (ex) {
                // if in dev mode, report an error, else try to limp onward
                if (jsf.getProjectStage() == "Development") {
                    throw new Error("Error updating attributes");
                }
            }
            deleteNode(newElement);

        };

        /**
         * Create a new document, then select the body element within it
         * @param docStr Stringified version of document to create
         * @return element the body element
         * @ignore
         */
        var getBodyElement = function getBodyElement(docStr) {

            var doc;  // intermediate document we'll create
            var body; // Body element to return

            if (typeof DOMParser !== "undefined") {  // FF, S, Chrome
                doc = (new DOMParser()).parseFromString(docStr, "text/xml");
            } else if (typeof ActiveXObject !== "undefined") { // IE
                doc = new ActiveXObject("MSXML2.DOMDocument");
                doc.loadXML(docStr);
            } else {
                throw new Error("You don't seem to be running a supported browser");
            }

            if (getParseErrorText(doc) !== PARSED_OK) {
                throw new Error(getParseErrorText(doc));
            }

            body = doc.getElementsByTagName("body")[0];

            if (!body) {
                throw new Error("Can't find body tag in returned document.");
            }

            return body;
        };

        /**
         * Do update.
         * @param element element to update
         * @param context context of request
         * @ignore
         */
        var doUpdate = function doUpdate(element, context) {
            var id, content, markup, state;
            var stateForm;
            var scripts = []; // temp holding value for array of script nodes

            id = element.getAttribute('id');
            if (id === "javax.faces.ViewState") {

                state = element.firstChild;

                // Now set the view state from the server into the DOM
                // but only for the form that submitted the request.

                stateForm = document.getElementById(context.formid);
                if (!stateForm || !stateForm.elements) {
                    // if the form went away for some reason, or it lacks elements
                    // we're going to just return silently.
                    return;
                }
                var field = stateForm.elements["javax.faces.ViewState"];
                if (typeof field == 'undefined') {
                    field = document.createElement("input");
                    field.type = "hidden";
                    field.name = "javax.faces.ViewState";
                    stateForm.appendChild(field);
                }
                field.value = state.nodeValue;

                // Now set the view state from the server into the DOM
                // for any form that is a render target.

                if (typeof context.render !== 'undefined' && context.render !== null) {
                    var temp = context.render.split(' ');
                    for (var i = 0; i < temp.length; i++) {
                        if (temp.hasOwnProperty(i)) {
                            // See if the element is a form and
                            // the form is not the one that caused the submission..
                            var f = document.forms[temp[i]];
                            if (typeof f !== 'undefined' && f !== null && f.id !== context.formid) {
                                field = f.elements["javax.faces.ViewState"];
                                if (typeof field === 'undefined') {
                                    field = document.createElement("input");
                                    field.type = "hidden";
                                    field.name = "javax.faces.ViewState";
                                    f.appendChild(field);
                                }
                                field.value = state.nodeValue;
                            }
                        }
                    }
                }
                return;
            }

            // join the CDATA sections in the markup
            markup = '';
            for (var j = 0; j < element.childNodes.length; j++) {
                content = element.childNodes[j];
                markup += content.nodeValue;
            }

            var src = markup;

            // If our special render all markup is present..
            if (id === "javax.faces.ViewRoot" || id === "javax.faces.ViewBody") {
                var bodyStartEx = new RegExp("< *body[^>]*>", "gi");
                var bodyEndEx = new RegExp("< */ *body[^>]*>", "gi");
                var newsrc;

                var docBody = document.getElementsByTagName("body")[0];
                var bodyStart = bodyStartEx.exec(src);

                if (bodyStart !== null) { // replace body tag
                    // First, try with XML manipulation
                    try {
                        // Get scripts from text
                        scripts = stripScripts(src);
                        // Remove scripts from text
                        newsrc = src.replace(/<script[^>]*>([\S\s]*?)<\/script>/igm, "");
                        elementReplace(getBodyElement(newsrc), docBody);
                        runScripts(scripts);
                    } catch (e) {
                        // OK, replacing the body didn't work with XML - fall back to quirks mode insert
                        var srcBody, bodyEnd;
                        // if src contains </body>
                        bodyEnd = bodyEndEx.exec(src);
                        if (bodyEnd !== null) {
                            srcBody = src.substring(bodyStartEx.lastIndex,
                                    bodyEnd.index);
                        } else { // can't find the </body> tag, punt
                            srcBody = src.substring(bodyStartEx.lastIndex);
                        }
                        // replace body contents with innerHTML - note, script handling happens within function
                        elementReplaceStr(docBody, "body", srcBody);

                    }

                } else {  // replace body contents with innerHTML - note, script handling happens within function
                    elementReplaceStr(docBody, "body", src);
                }
            } else if (id === "javax.faces.ViewHead") {
                throw new Error("javax.faces.ViewHead not supported - browsers cannot reliably replace the head's contents");
            } else {
                var d = $(id);
                if (!d) {
                    throw new Error("During update: " + id + " not found");
                }
                var parent = d.parentNode;
                // Trim space padding before assigning to innerHTML
                var html = src.replace(/^\s+/g, '').replace(/\s+$/g, '');
                var parserElement = document.createElement('div');
                var tag = d.nodeName.toLowerCase();
                var tableElements = ['td', 'th', 'tr', 'tbody', 'thead', 'tfoot'];
                var isInTable = false;
                for (var tei = 0, tel = tableElements.length; tei < tel; tei++) {
                    if (tableElements[tei] == tag) {
                        isInTable = true;
                        break;
                    }
                }
                if (isInTable) {

                    if (isAutoExec()) {
                        // Create html
                        parserElement.innerHTML = '<table>' + html + '</table>';
                    } else {
                        // Get the scripts from the text
                        scripts = stripScripts(html);
                        // Remove scripts from text
                        html = html.replace(/<script[^>]*>([\S\s]*?)<\/script>/igm,"");
                        parserElement.innerHTML = '<table>' + html + '</table>';
                    }
                    var newElement = parserElement.firstChild;
                    //some browsers will also create intermediary elements such as table>tbody>tr>td
                    while ((null !== newElement) && (id !== newElement.id)) {
                        newElement = newElement.firstChild;
                    }
                    parent.replaceChild(newElement, d);
                    runScripts(scripts);
                } else if (d.nodeName.toLowerCase() === 'input') {
                    // special case handling for 'input' elements
                    // in order to not lose focus when updating,
                    // input elements need to be added in place.
                    parserElement = document.createElement('div');
                    parserElement.innerHTML = html;
                    newElement = parserElement.firstChild;

                    cloneAttributes(d, newElement);
                    deleteNode(parserElement);
                } else if (html.length > 0) {
                    if (isAutoExec()) {
                        // Create html
                        parserElement.innerHTML = html;
                    } else {
                        // Get the scripts from the text
                        scripts = stripScripts(html);
                        // Remove scripts from text
                        html = html.replace(/<script[^>]*>([\S\s]*?)<\/script>/igm,"");
                        parserElement.innerHTML = html;
                    }
                    if (typeof jQuery == 'undefined' || d.className != 'af_chart') {
                        replaceNode(parserElement.firstChild, d);
                        deleteNode(parserElement);
                    } else if (typeof jQuery != 'undefined') {
                        var apacheChart = jQuery('body').data(d.id);
                        if (typeof apacheChart != 'undefined') {
                            apacheChart.clear();
                        }
                    }
                    runScripts(scripts);
                }
            }
        };

        /**
         * Delete a node specified by the element.
         * @param element
         * @ignore
         */
        var doDelete = function doDelete(element) {
            var id = element.getAttribute('id');
            var target = $(id);
            deleteNode(target);
        };

        /**
         * Insert a node specified by the element.
         * @param element
         * @ignore
         */
        var doInsert = function doInsert(element) {
            var tablePattern = new RegExp("<\\s*(td|th|tr|tbody|thead|tfoot)", "i");
            var scripts = [];
            var target = $(element.firstChild.getAttribute('id'));
            var parent = target.parentNode;
            var html = element.firstChild.firstChild.nodeValue;
            var isInTable = tablePattern.test(html);

            if (!isAutoExec())  {
                // Get the scripts from the text
                scripts = stripScripts(html);
                // Remove scripts from text
                html = html.replace(/<script[^>]*>([\S\s]*?)<\/script>/igm,"");
            }
            var tempElement = document.createElement('div');
            var newElement = null;
            if (isInTable)  {
                tempElement.innerHTML = '<table>' + html + '</table>';
                newElement = tempElement.firstChild;
                //some browsers will also create intermediary elements such as table>tbody>tr>td
                //test for presence of id on the new element since we do not have it directly
                while ((null !== newElement) && ("" == newElement.id)) {
                    newElement = newElement.firstChild;
                }
            } else {
                tempElement.innerHTML = html;
                newElement = tempElement.firstChild;
            }

            if (element.firstChild.nodeName === 'after') {
                // Get the next in the list, to insert before
                target = target.nextSibling;
            }  // otherwise, this is a 'before' element
            if (!!tempElement.innerHTML) { // check if only scripts were inserted - if so, do nothing here
                parent.insertBefore(newElement, target);
            }
            runScripts(scripts);
            deleteNode(tempElement);
        };

        /**
         * Modify attributes of given element id.
         * @param element
         * @ignore
         */
        var doAttributes = function doAttributes(element) {

            // Get id of element we'll act against
            var id = element.getAttribute('id');

            var target = $(id);

            if (!target) {
                throw new Error("The specified id: " + id + " was not found in the page.");
            }

            // There can be multiple attributes modified.  Loop through the list.
            var nodes = element.childNodes;
            for (var i = 0; i < nodes.length; i++) {
                var name = nodes[i].getAttribute('name');
                var value = nodes[i].getAttribute('value');
                if (!isIE()) {
                    target.setAttribute(name, value);
                } else { // if it's IE, then quite a bit more work is required
                    if (name === 'class') {
                        name = 'className';
                        target.setAttribute(name, value, 0);
                    } else if (name === "for") {
                        name = 'htmlFor';
                        target.setAttribute(name, value, 0);
                    } else if (name === 'style') {
                        target.style.setAttribute('cssText', value, 0);
                    } else if (name.substring(0, 2) === 'on') {
                        var fn = function(value) {
                            return function() {
                                window.execScript(value);
                            };
                        }(value);
                        target.setAttribute(name, fn, 0);
                    } else if (name === 'dir') {
                        if (jsf.getProjectStage() == 'Development') {
                            throw new Error("Cannot set 'dir' attribute in IE");
                        }
                    } else {
                        target.setAttribute(name, value, 0);
                    }
                }
            }
        };

        /**
         * Eval the CDATA of the element.
         * @param element to eval
         * @ignore
         */
        var doEval = function doEval(element) {
            var evalText = element.firstChild.nodeValue;
            globalEval(evalText);
        };

        /**
         * Ajax Request Queue
         * @ignore
         */
        var Queue = new function Queue() {

            // Create the internal queue
            var queue = [];


            // the amount of space at the front of the queue, initialised to zero
            var queueSpace = 0;

            /** Returns the size of this Queue. The size of a Queue is equal to the number
             * of elements that have been enqueued minus the number of elements that have
             * been dequeued.
             * @ignore
             */
            this.getSize = function getSize() {
                return queue.length - queueSpace;
            };

            /** Returns true if this Queue is empty, and false otherwise. A Queue is empty
             * if the number of elements that have been enqueued equals the number of
             * elements that have been dequeued.
             * @ignore
             */
            this.isEmpty = function isEmpty() {
                return (queue.length === 0);
            };

            /** Enqueues the specified element in this Queue.
             *
             * @param element - the element to enqueue
             * @ignore
             */
            this.enqueue = function enqueue(element) {
                // Queue the request
                queue.push(element);
            };


            /** Dequeues an element from this Queue. The oldest element in this Queue is
             * removed and returned. If this Queue is empty then undefined is returned.
             *
             * @returns Object The element that was removed from the queue.
             * @ignore
             */
            this.dequeue = function dequeue() {
                // initialise the element to return to be undefined
                var element = undefined;

                // check whether the queue is empty
                if (queue.length) {
                    // fetch the oldest element in the queue
                    element = queue[queueSpace];

                    // update the amount of space and check whether a shift should occur
                    if (++queueSpace * 2 >= queue.length) {
                        // set the queue equal to the non-empty portion of the queue
                        queue = queue.slice(queueSpace);
                        // reset the amount of space at the front of the queue
                        queueSpace = 0;
                    }
                }
                // return the removed element
                try {
                    return element;
                } finally {
                    element = null; // IE 6 leak prevention
                }
            };

            /** Returns the oldest element in this Queue. If this Queue is empty then
             * undefined is returned. This function returns the same value as the dequeue
             * function, but does not remove the returned element from this Queue.
             * @ignore
             */
            this.getOldestElement = function getOldestElement() {
                // initialise the element to return to be undefined
                var element = undefined;

                // if the queue is not element then fetch the oldest element in the queue
                if (queue.length) {
                    element = queue[queueSpace];
                }
                // return the oldest element
                try {
                    return element;
                } finally {
                    element = null; //IE 6 leak prevention
                }
            };
        }();


        /**
         * AjaxEngine handles Ajax implementation details.
         * @ignore
         */
        var AjaxEngine = function AjaxEngine() {

            var req = {};                  // Request Object
            req.url = null;                // Request URL
            req.context = {};              // Context of request and response
            req.context.sourceid = null;   // Source of this request
            req.context.onerror = null;    // Error handler for request
            req.context.onevent = null;    // Event handler for request
            req.context.formid = null;     // Form that's the context for this request
            req.xmlReq = null;             // XMLHttpRequest Object
            req.async = true;              // Default - Asynchronous
            req.parameters = {};           // Parameters For GET or POST
            req.queryString = null;        // Encoded Data For GET or POST
            req.method = null;             // GET or POST
            req.status = null;             // Response Status Code From Server
            req.fromQueue = false;         // Indicates if the request was taken off the queue
            // before being sent.  This prevents the request from
            // entering the queue redundantly.

            req.que = Queue;

            // Get an XMLHttpRequest Handle
            req.xmlReq = getTransport();
            if (req.xmlReq === null) {
                return null;
            }

            function noop() {}

            // Set up request/response state callbacks
            /**
             * @ignore
             */
            req.xmlReq.onreadystatechange = function() {
                if (req.xmlReq.readyState === 4) {
                    req.onComplete();
                    // next two lines prevent closure/ciruclar reference leaks
                    // of XHR instances in IE
                    req.xmlReq.onreadystatechange = noop;
                    req.xmlReq = null;
                }
            };

            /**
             * This function is called when the request/response interaction
             * is complete.  If the return status code is successfull,
             * dequeue all requests from the queue that have completed.  If a
             * request has been found on the queue that has not been sent,
             * send the request.
             * @ignore
             */
            req.onComplete = function onComplete() {
                if (req.xmlReq.status && (req.xmlReq.status >= 200 && req.xmlReq.status < 300)) {
                    sendEvent(req.xmlReq, req.context, "complete");
                    jsf.ajax.response(req.xmlReq, req.context);
                } else {
                    sendEvent(req.xmlReq, req.context, "complete");
                    sendError(req.xmlReq, req.context, "httpError");
                }

                // Regardless of whether the request completed successfully (or not),
                // dequeue requests that have been completed (readyState 4) and send
                // requests that ready to be sent (readyState 0).

                var nextReq = req.que.getOldestElement();
                if (nextReq === null || typeof nextReq === 'undefined') {
                    return;
                }
                while ((typeof nextReq.xmlReq !== 'undefined' && nextReq.xmlReq !== null) &&
                       nextReq.xmlReq.readyState === 4) {
                    req.que.dequeue();
                    nextReq = req.que.getOldestElement();
                    if (nextReq === null || typeof nextReq === 'undefined') {
                        break;
                    }
                }
                if (nextReq === null || typeof nextReq === 'undefined') {
                    return;
                }
                if ((typeof nextReq.xmlReq !== 'undefined' && nextReq.xmlReq !== null) &&
                    nextReq.xmlReq.readyState === 0) {
                    nextReq.fromQueue = true;
                    nextReq.sendRequest();
                }
            };

            /**
             * Utility method that accepts additional arguments for the AjaxEngine.
             * If an argument is passed in that matches an AjaxEngine property, the
             * argument value becomes the value of the AjaxEngine property.
             * Arguments that don't match AjaxEngine properties are added as
             * request parameters.
             * @ignore
             */
            req.setupArguments = function(args) {
                for (var i in args) {
                    if (args.hasOwnProperty(i)) {
                        if (typeof req[i] === 'undefined') {
                            req.parameters[i] = args[i];
                        } else {
                            req[i] = args[i];
                        }
                    }
                }
            };

            /**
             * This function does final encoding of parameters, determines the request method
             * (GET or POST) and sends the request using the specified url.
             * @ignore
             */
            req.sendRequest = function() {
                if (req.xmlReq !== null) {
                    // if there is already a request on the queue waiting to be processed..
                    // just queue this request
                    if (!req.que.isEmpty()) {
                        if (!req.fromQueue) {
                            req.que.enqueue(req);
                            return;
                        }
                    }
                    // If the queue is empty, queue up this request and send
                    if (!req.fromQueue) {
                        req.que.enqueue(req);
                    }
                    // Some logic to get the real request URL
                    if (req.generateUniqueUrl && req.method == "GET") {
                        req.parameters["AjaxRequestUniqueId"] = new Date().getTime() + "" + req.requestIndex;
                    }
                    var content = null; // For POST requests, to hold query string
                    for (var i in req.parameters) {
                        if (req.parameters.hasOwnProperty(i)) {
                            if (req.queryString.length > 0) {
                                req.queryString += "&";
                            }
                            req.queryString += encodeURIComponent(i) + "=" + encodeURIComponent(req.parameters[i]);
                        }
                    }
                    if (req.method === "GET") {
                        if (req.queryString.length > 0) {
                            req.url += ((req.url.indexOf("?") > -1) ? "&" : "?") + req.queryString;
                        }
                    }
                    req.xmlReq.open(req.method, req.url, req.async);
                    if (req.method === "POST") {
                        if (typeof req.xmlReq.setRequestHeader !== 'undefined') {
                            req.xmlReq.setRequestHeader('Faces-Request', 'partial/ajax');
                            req.xmlReq.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
                        }
                        content = req.queryString;
                    }
                    // note that async == false is not a supported feature.  We may change it in ways
                    // that break existing programs at any time, with no warning.
                    if(!req.async) {
                        req.xmlReq.onreadystatechange = null; // no need for readystate change listening
                    }
                    sendEvent(req.xmlReq, req.context, "begin");
                    req.xmlReq.send(content);
                    if(!req.async){
                        req.onComplete();
                }
                }
            };

            return req;
        };

        var IframeEngine = function(context) {
            var req = {
                FRAME_ID: 'mojarra_ajax_iframe',
                frame: false,
                context: context
            };

            req._createIframe = function() {
                var frame = document.getElementById(this.FRAME_ID);
                if (!frame) {
                    if (isIE()) {
                        frame = document.createElement("div");
                        frame.innerHTML = '<iframe id="' + this.FRAME_ID + 
                            '" name="' + this.FRAME_ID + 
                            '" src="javascript:false;" type="content" collapsed1="true" style="width: 500px; height: 100px" style1="display: none"></iframe>';
                        document.body.appendChild(frame);
                    } else {
                        frame = this.createElement(document.body, 'iframe', {
                            src: 'about:blank',
                            id: this.FRAME_ID,
                            name: this.FRAME_ID,
                            type: "content",
                            collapsed: true,
                            style: {display:'none'}
                        });
//                        document.body.appendChild(frame);
                    }
                }

                return frame;
            };

            req.onload = function() {
                alert('onload!');
                var document = this.frame.contentWindow.document || this.frame.contentDocument || this.frame.document;
                var body = document.body || document.documentElement ;
                var request = {};
                try {
                    request.responseText = this.frame.innerHTML;
                    request.responseXML = document;
                    request.readyState = 4;

                    // assume the best for now
                    request.status = 201;
                    jsf.ajax.response(request, this.context);
                } catch (e) {
                } finally {
                    // TODO: remove the iframe?
                }
            }

            req.createElement = function(parent, type, attrs) {
                var el = document.getElementById(type);
                if (el) {
                    el.value = attrs.value;
                } else {
                    el = document.createElement(type);

                    if (attrs) {
                        for (key in attrs) {
                            if (key !== 'style') {
                                el[key] = attrs[key];
                            }
                        }

                        if (attrs.style) {
                            for (key in attrs.style) {
                                    el.style[key] = attrs.style[key];
                            }
                        }
                    }

                    parent.appendChild(el);
                    el.style.visibility = "hidden";
                }

                return el;
            }

            req.addAjaxParameters = function(form, args) {
                this.createElement(form, "input", {id: 'Faces-Request', name: 'Faces-Request', value:'partial/ajax', style: {display:'none'}});
                this.createElement(form, "input", {id: 'javax.faces.partial.execute', name: 'javax.faces.partial.execute', value: args["javax.faces.partial.execute"], style: {display:'none'}});
                this.createElement(form, "input", {id: 'javax.faces.partial.render', name: 'javax.faces.partial.render', value: args["javax.faces.partial.render"], style: {display:'none'}});
                this.createElement(form, "input", {id: 'javax.faces.partial.ajax', name: 'javax.faces.partial.ajax', value: args["javax.faces.partial.ajax"], style: {display:'none'}});
                this.createElement(form, "input", {id: 'javax.faces.partial.event', name: 'javax.faces.partial.event', value: args["javax.faces.partial.event"], style: {display:'none'}});
                this.createElement(form, "input", {id: 'javax.faces.behavior.event', name: 'javax.faces.behavior.event', value: args["javax.faces.behavior.event"], style: {display:'none'}});
                this.createElement(form, "input", {id: 'javax.faces.source', name: 'javax.faces.source', value: args["javax.faces.source"], style: {display:'none'}});
            }

            req.removeAjaxParameters = function(form) {
                var deleteElement = function(parent, id) {
                    console.debug('Deleting element ' + id);
                    var el = document.getElementById(id);
                    if (el) {
                        parent.removeChild(el);
                    } else {
                        var arr = document.getElementsByName(id);
                        for (var i = 0; i < arr.length; i++) {
                            parent.removeChild(arr[i]);
                        }
                    }
                }

                try {
                    deleteElement(form, 'Faces-Request');
                    deleteElement(form, 'javax.faces.partial.execute');
                    deleteElement(form, 'javax.faces.partial.render');
                    deleteElement(form, 'javax.faces.partial.ajax');
                    deleteElement(form, 'javax.faces.partial.event');
                    deleteElement(form, 'javax.faces.behavior.event');
                    deleteElement(form, 'javax.faces.source');
                    deleteElement(document.body, this.FRAME_ID);
                } catch (err) {
                    alert("Error! " + err);
                }
            }

            req.sendRequest = function(form, args) {
                this._createIframe();
                this.frame = document.getElementById(this.FRAME_ID);

                if (!isIE()) {
                    this.frame.onload = this.hitch(this, this.onload);
                } else {
                    this.frame.onload_IE = function() { alert ('grr');}
                    this.frame.load = function() { alert('onload'); }; //this.onload;
                    this.frame.onreadystatechange = this.hitch(this, function() {
                        var frame = document.getElementById(this.FRAME_ID);
                        function log(label, value) {
                            try {
                                console.log(label + ".body.innerHTML = " + value.body.innerHTML.substring(0, 50));
                            } catch(err) { }
                            try {
                                console.log(label + ".documentElement.innerHTML = " + value.documentElement.innerHTML.substring(0, 50));
                            } catch(err) { }
                        }
                        
                        try { log ("frame.contentWindow.document", frame.contentWindow.document); } catch(err) { }
                        try { log ("frame.contentDocument", frame.contentDocument); } catch(err) { }
                        try { log ("frame.document", frame.document); } catch(err) { }

                        try {
                            if (frame.contentDocument.documentElement.innerHTML != '') {
                                this.onload();
                            }
                        } catch (err) {
                            
                        }
                    });
//                    this.frame.onload_IE = this.hitch(this, this.onload);
                }

                var saveTarget = form.target;
                var saveMethod = form.method;

                try {
                    this.addAjaxParameters(form, args);
                    form.target = this.FRAME_ID;
                    form.method = "POST";
                    form.submit();
                } finally {
//                    this.removeAjaxParameters(form);
                    form.target = saveTarget;
                    form.method = saveMethod;
                }
            }

            // Taken from MyFaces, which was taken from dojo. May need to rewrite
            req.hitch = function(/*Object*/scope, /*Function|String*/method /*,...*/) {
                //	summary:
                //		Returns a function that will only ever execute in the a given scope.
                //		This allows for easy use of object member functions
                //		in callbacks and other places in which the "this" keyword may
                //		otherwise not reference the expected scope.
                //		Any number of default positional arguments may be passed as parameters
                //		beyond "method".
                //		Each of these values will be used to "placehold" (similar to curry)
                //		for the hitched function.
                //	scope:
                //		The scope to use when method executes. If method is a string,
                //		scope is also the object containing method.
                //	method:
                //		A function to be hitched to scope, or the name of the method in
                //		scope to be hitched.
                //	example:
                //	|	myfaces._impl._util._Lang.hitch(foo, "bar")();
                //		runs foo.bar() in the scope of foo
                //	example:
                //	|	myfaces._impl._util._Lang.hitch(foo, myFunction);
                //		returns a function that runs myFunction in the scope of foo
                if (arguments.length > 2) {
                    return this._hitchArgs._hitchArgs.apply(this._hitchArgs, arguments); // Function
                }
                if (!method) {
                    method = scope;
                    scope = null;
                }
                if (typeof method === 'string') {
                    scope = scope || window || function() {
                    };
                    /*since we do not have dojo global*/
                    if (!scope[method]) {
                        throw('error');
                    }
                    return function() {
                        return scope[method].apply(scope, arguments || []);
                    }; // Function
                }
                return !scope ? method : function() {
                    return method.apply(scope, arguments || []);
                }; // Function
            }

            req._hitchArgs = function(scope, method /*,...*/) {
                var pre = this.objToArray(arguments, 2);
                var named = this.isString(method);
                return function() {
                    // array-fy arguments
                    var args = this.objToArray(arguments);
                    // locate our method
                    var f = named ? (scope || this.global)[method] : method;
                    // invoke with collected args
                    return f && f.apply(scope || this, pre.concat(args)); // mixed
                }; // Function
            }

            return req;

        }
        /**
         * Error handling callback.
         * Assumes that the request has completed.
         * @ignore
         */
        var sendError = function sendError(request, context, status, description, serverErrorName, serverErrorMessage) {

            // Possible errornames:
            // httpError
            // emptyResponse
            // serverError
            // malformedXML

            var sent = false;
            var data = {};  // data payload for function
            data.type = "error";
            data.status = status;
            data.source = context.sourceid;
            data.responseCode = request.status;
            data.responseXML = request.responseXML;
            data.responseText = request.responseText;

            // ensure data source is the dom element and not the ID
            // per 14.4.1 of the 2.0 specification.
            if (typeof data.source === 'string') {
                data.source = document.getElementById(data.source);
            }

            if (description) {
                data.description = description;
            } else if (status == "httpError") {
                if (data.responseCode === 0) {
                    data.description = "The Http Transport returned a 0 status code.  This is usually the result of mixing ajax and full requests.  This is usually undesired, for both performance and data integrity reasons.";
                } else {
                    data.description = "There was an error communicating with the server, status: " + data.responseCode;
                }
            } else if (status == "serverError") {
                data.description = serverErrorMessage;
            } else if (status == "emptyResponse") {
                data.description = "An empty response was received from the server.  Check server error logs.";
            } else if (status == "malformedXML") {
                if (getParseErrorText(data.responseXML) !== PARSED_OK) {
                    data.description = getParseErrorText(data.responseXML);
                } else {
                    data.description = "An invalid XML response was received from the server.";
                }
            }

            if (status == "serverError") {
                data.errorName = serverErrorName;
                data.errorMessage = serverErrorMessage;
            }

            // If we have a registered callback, send the error to it.
            if (context.onerror) {
                context.onerror.call(null, data);
                sent = true;
            }

            for (var i in errorListeners) {
                if (errorListeners.hasOwnProperty(i)) {
                    errorListeners[i].call(null, data);
                    sent = true;
                }
            }

            if (!sent && jsf.getProjectStage() === "Development") {
                if (status == "serverError") {
                    alert("serverError: " + serverErrorName + " " + serverErrorMessage);
                } else {
                    alert(status + ": " + data.description);
                }
            }
        };

        /**
         * Event handling callback.
         * Request is assumed to have completed, except in the case of event = 'begin'.
         * @ignore
         */
        var sendEvent = function sendEvent(request, context, status) {

            var data = {};
            data.type = "event";
            data.status = status;
            data.source = context.sourceid;
            // ensure data source is the dom element and not the ID
            // per 14.4.1 of the 2.0 specification.
            if (typeof data.source === 'string') {
                data.source = document.getElementById(data.source);
            }
            if (status !== 'begin') {
                data.responseCode = request.status;
                data.responseXML = request.responseXML;
                data.responseText = request.responseText;
            }

            if (context.onevent) {
                context.onevent.call(null, data);
            }

            for (var i in eventListeners) {
                if (eventListeners.hasOwnProperty(i)) {
                    eventListeners[i].call(null, data);
                }
            }
        };

        // Use module pattern to return the functions we actually expose
        return {
            /**
             * Register a callback for error handling.
             * <p><b>Usage:</b></p>
             * <pre><code>
             * jsf.ajax.addOnError(handleError);
             * ...
             * var handleError = function handleError(data) {
             * ...
             * }
             * </pre></code>
             * <p><b>Implementation Requirements:</b></p>
             * This function must accept a reference to an existing JavaScript function.
             * The JavaScript function reference must be added to a list of callbacks, making it possible
             * to register more than one callback by invoking <code>jsf.ajax.addOnError</code>
             * more than once.  This function must throw an error if the <code>callback</code>
             * argument is not a function.
             *
             * @member jsf.ajax
             * @param callback a reference to a function to call on an error
             */
            addOnError: function addOnError(callback) {
                if (typeof callback === 'function') {
                    errorListeners[errorListeners.length] = callback;
                } else {
                    throw new Error("jsf.ajax.addOnError:  Added a callback that was not a function.");
                }
            },
            /**
             * Register a callback for event handling.
             * <p><b>Usage:</b></p>
             * <pre><code>
             * jsf.ajax.addOnEvent(statusUpdate);
             * ...
             * var statusUpdate = function statusUpdate(data) {
             * ...
             * }
             * </pre></code>
             * <p><b>Implementation Requirements:</b></p>
             * This function must accept a reference to an existing JavaScript function.
             * The JavaScript function reference must be added to a list of callbacks, making it possible
             * to register more than one callback by invoking <code>jsf.ajax.addOnEvent</code>
             * more than once.  This function must throw an error if the <code>callback</code>
             * argument is not a function.
             *
             * @member jsf.ajax
             * @param callback a reference to a function to call on an event
             */
            addOnEvent: function addOnEvent(callback) {
                if (typeof callback === 'function') {
                    eventListeners[eventListeners.length] = callback;
                } else {
                    throw new Error("jsf.ajax.addOnEvent: Added a callback that was not a function");
                }
            },
            /**
             * <p>Send an asynchronous Ajax request to the server.
             * <p><b>Usage:</b></p>
             * <pre><code>
             * Example showing all optional arguments:
             *
             * &lt;commandButton id="button1" value="submit"
             *     onclick="jsf.ajax.request(this,event,
             *       {execute:'button1',render:'status',onevent: handleEvent,onerror: handleError});return false;"/&gt;
             * &lt;/commandButton/&gt;
             * </pre></code>
             * <p><b>Implementation Requirements:</b></p>
             * This function must:
             * <ul>
             * <li>Be used within the context of a <code>form</code>.</li>
             * <li>Capture the element that triggered this Ajax request
             * (from the <code>source</code> argument, also known as the
             * <code>source</code> element.</li>
             * <li>If the <code>source</code> element is <code>null</code> or
             * <code>undefined</code> throw an error.</li>
             * <li>If the <code>source</code> argument is not a <code>string</code> or
             * DOM element object, throw an error.</li>
             * <li>If the <code>source</code> argument is a <code>string</code>, find the
             * DOM element for that <code>string</code> identifier.
             * <li>If the DOM element could not be determined, throw an error.</li>
             * <li>If the <code>onerror</code> and <code>onevent</code> arguments are set,
             * they must be functions, or throw an error.
             * <li>Determine the <code>source</code> element's <code>form</code>
             * element.</li>
             * <li>Get the <code>form</code> view state by calling
             * {@link jsf.getViewState} passing the
             * <code>form</code> element as the argument.</li>
             * <li>Collect post data arguments for the Ajax request.
             * <ul>
             * <li>The following name/value pairs are required post data arguments:
             * <table border="1">
             * <tr>
             * <th>name</th>
             * <th>value</th>
             * </tr>
             * <tr>
             * <td><code>javax.faces.ViewState</code></td>
             * <td><code>Contents of javax.faces.ViewState hidden field.  This is included when
             * {@link jsf.getViewState} is used.</code></td>
             * </tr>
             * <tr>
             * <td><code>javax.faces.partial.ajax</code></td>
             * <td><code>true</code></td>
             * </tr>
             * <tr>
             * <td><code>javax.faces.source</code></td>
             * <td><code>The identifier of the element that triggered this request.</code></td>
             * </tr>
             * </table>
             * </li>
             * </ul>
             * </li>
             * <li>Collect optional post data arguments for the Ajax request.
             * <ul>
             * <li>Determine additional arguments (if any) from the <code>options</code>
             * argument. If <code>options.execute</code> exists:
             * <ul>
             * <li>If the keyword <code>@none</code> is present, do not create and send
             * the post data argument <code>javax.faces.partial.execute</code>.</li>
             * <li>If the keyword <code>@all</code> is present, create the post data argument with
             * the name <code>javax.faces.partial.execute</code> and the value <code>@all</code>.</li>
             * <li>Otherwise, there are specific identifiers that need to be sent.  Create the post
             * data argument with the name <code>javax.faces.partial.execute</code> and the value as a
             * space delimited <code>string</code> of client identifiers.</li>
             * </ul>
             * </li>
             * <li>If <code>options.execute</code> does not exist, create the post data argument with the
             * name <code>javax.faces.partial.execute</code> and the value as the identifier of the
             * element that caused this request.</li>
             * <li>If <code>options.render</code> exists:
             * <ul>
             * <li>If the keyword <code>@none</code> is present, do not create and send
             * the post data argument <code>javax.faces.partial.render</code>.</li>
             * <li>If the keyword <code>@all</code> is present, create the post data argument with
             * the name <code>javax.faces.partial.render</code> and the value <code>@all</code>.</li>
             * <li>Otherwise, there are specific identifiers that need to be sent.  Create the post
             * data argument with the name <code>javax.faces.partial.render</code> and the value as a
             * space delimited <code>string</code> of client identifiers.</li>
             * </ul>
             * <li>If <code>options.render</code> does not exist do not create and send the
             * post data argument <code>javax.faces.partial.render</code>.</li>
             * <li>Determine additional arguments (if any) from the <code>event</code>
             * argument.  The following name/value pairs may be used from the
             * <code>event</code> object:
             * <ul>
             * <li><code>target</code> - the ID of the element that triggered the event.</li>
             * <li><code>captured</code> - the ID of the element that captured the event.</li>
             * <li><code>type</code> - the type of event (ex: onkeypress)</li>
             * <li><code>alt</code> - <code>true</code> if ALT key was pressed.</li>
             * <li><code>ctrl</code> - <code>true</code> if CTRL key was pressed.</li>
             * <li><code>shift</code> - <code>true</code> if SHIFT key was pressed. </li>
             * <li><code>meta</code> - <code>true</code> if META key was pressed. </li>
             * <li><code>right</code> - <code>true</code> if right mouse button
             * was pressed. </li>
             * <li><code>left</code> - <code>true</code> if left mouse button
             * was pressed. </li>
             * <li><code>keycode</code> - the key code.
             * </ul>
             * </li>
             * </ul>
             * </li>
             * <li>Encode the set of post data arguments.</li>
             * <li>Join the encoded view state with the encoded set of post data arguments
             * to form the <code>query string</code> that will be sent to the server.</li>
             * <li>Create a request <code>context</code> object and set the properties:
             * <ul><li><code>source</code> (the source DOM element for this request)</li>
             * <li><code>onerror</code> (the error handler for this request)</li>
             * <li><code>onevent</code> (the event handler for this request)</li></ul>
             * The request context will be used during error/event handling.</li>
             * <li>Send a <code>begin</code> event following the procedure as outlined
             * in the Chapter 13 "Sending Events" section of the spec prose document <a
             *  href="../../javadocs/overview-summary.html#prose_document">linked in the
             *  overview summary</a></li>
             * <li>Set the request header with the name: <code>Faces-Request</code> and the
             * value: <code>partial/ajax</code>.</li>
             * <li>Determine the <code>posting URL</code> as follows: If the hidden field
             * <code>javax.faces.encodedURL</code> is present in the submitting form, use its
             * value as the <code>posting URL</code>.  Otherwise, use the <code>action</code>
             * property of the <code>form</code> element as the <code>URL</code>.</li>
             * <li>Send the request as an <code>asynchronous POST</code> using the
             * <code>posting URL</code> that was determined in the previous step.</li>
             * </ul>
             * Form serialization should occur just before the request is sent to minimize
             * the amount of time between the creation of the serialized form data and the
             * sending of the serialized form data (in the case of long requests in the queue).
             * Before the request is sent it must be put into a queue to ensure requests
             * are sent in the same order as when they were initiated.  The request callback function
             * must examine the queue and determine the next request to be sent.  The behavior of the
             * request callback function must be as follows:
             * <ul>
             * <li>If the request completed successfully invoke {@link jsf.ajax.response}
             * passing the <code>request</code> object.</li>
             * <li>If the request did not complete successfully, notify the client.</li>
             * <li>Regardless of the outcome of the request (success or error) every request in the
             * queue must be handled.  Examine the status of each request in the queue starting from
             * the request that has been in the queue the longest.  If the status of the request is
             * <code>complete</code> (readyState 4), dequeue the request (remove it from the queue).
             * If the request has not been sent (readyState 0), send the request.  Requests that are
             * taken off the queue and sent should not be put back on the queue.</li>
             * </ul>
             *
             * </p>
             *
             * @param source The DOM element that triggered this Ajax request, or an id string of the
             * element to use as the triggering element.
             * @param event The DOM event that triggered this Ajax request.  The
             * <code>event</code> argument is optional.
             * @param options The set of available options that can be sent as
             * request parameters to control client and/or server side
             * request processing. Acceptable name/value pair options are:
             * <table border="1">
             * <tr>
             * <th>name</th>
             * <th>value</th>
             * </tr>
             * <tr>
             * <td><code>execute</code></td>
             * <td><code>space seperated list of client identifiers</code></td>
             * </tr>
             * <tr>
             * <td><code>render</code></td>
             * <td><code>space seperated list of client identifiers</code></td>
             * </tr>
             * <tr>
             * <td><code>onevent</code></td>
             * <td><code>function to callback for event</code></td>
             * </tr>
             * <tr>
             * <td><code>onerror</code></td>
             * <td><code>function to callback for error</code></td>
             * </tr>
             * <tr>
             * <td><code>params</code></td>
             * <td><code>object containing parameters to include in the request</code></td>
             * </tr>
             * </table>
             * The <code>options</code> argument is optional.
             * @member jsf.ajax
             * @function jsf.ajax.request
             * @throws Error if first required argument <code>element</code> is not specified
             */
            request: function request(source, event, options) {

                var element, form;   //  Element variables
                var all, none;

                if (typeof source === 'undefined' || source === null) {
                    throw new Error("jsf.ajax.request: source not set");
                }

                // set up the element based on source
                if (typeof source === 'string') {
                    element = document.getElementById(source);
                } else if (typeof source === 'object') {
                    element = source;
                } else {
                    throw new Error("jsf.request: source must be object or string");
                }
                // attempt to handle case of name unset
                // this might be true in a badly written composite component
                if (!element.name) {
                    element.name = element.id;
                }

                if (typeof(options) === 'undefined' || options === null) {
                    options = {};
                }

                // Error handler for this request
                var onerror = false;

                if (options.onerror && typeof options.onerror === 'function') {
                    onerror = options.onerror;
                } else if (options.onerror && typeof options.onerror !== 'function') {
                    throw new Error("jsf.ajax.request: Added an onerror callback that was not a function");
                }

                // Event handler for this request
                var onevent = false;

                if (options.onevent && typeof options.onevent === 'function') {
                    onevent = options.onevent;
                } else if (options.onevent && typeof options.onevent !== 'function') {
                    throw new Error("jsf.ajax.request: Added an onevent callback that was not a function");
                }

                form = getForm(element);
                if (!form) {
                    throw new Error("jsf.ajax.request: Method must be called within a form");
                }
                var viewState = jsf.getViewState(form);

                // Set up additional arguments to be used in the request..
                // Make sure "javax.faces.source" is set up.
                // If there were "execute" ids specified, make sure we
                // include the identifier of the source element in the
                // "execute" list.  If there were no "execute" ids
                // specified, determine the default.

                var args = {};

                args["javax.faces.source"] = element.id;

                if (event && !!event.type) {
                    args["javax.faces.partial.event"] = event.type;
                }

                // If we have 'execute' identifiers:
                // Handle any keywords that may be present.
                // If @none present anywhere, do not send the
                // "javax.faces.partial.execute" parameter.
                // The 'execute' and 'render' lists must be space
                // delimited.

                if (options.execute) {
                    none = options.execute.search(/@none/);
                    if (none < 0) {
                        all = options.execute.search(/@all/);
                        if (all < 0) {
                            options.execute = options.execute.replace("@this", element.id);
                            options.execute = options.execute.replace("@form", form.id);
                            var temp = options.execute.split(' ');
                            if (!isInArray(temp, element.name)) {
                                options.execute = element.name + " " + options.execute;
                            }
                        } else {
                            options.execute = "@all";
                        }
                        args["javax.faces.partial.execute"] = options.execute;
                    }
                } else {
                    options.execute = element.name + " " + element.id;
                    args["javax.faces.partial.execute"] = options.execute;
                }

                if (options.render) {
                    none = options.render.search(/@none/);
                    if (none < 0) {
                        all = options.render.search(/@all/);
                        if (all < 0) {
                            options.render = options.render.replace("@this", element.id);
                            options.render = options.render.replace("@form", form.id);
                        } else {
                            options.render = "@all";
                        }
                        args["javax.faces.partial.render"] = options.render;
                    }
                }

                // remove non-passthrough options
                delete options.execute;
                delete options.render;
                delete options.onerror;
                delete options.onevent;
                // copy all other options to args
                for (var property in options) {
                    if (options.hasOwnProperty(property)) {
                        args[property] = options[property];
                    }
                }

                args["javax.faces.partial.ajax"] = "true";
                args["method"] = "POST";

                // Determine the posting url

                var encodedUrlField = form.elements["javax.faces.encodedURL"];
                if (typeof encodedUrlField == 'undefined') {
                    args["url"] = form.action;
                } else {
                    args["url"] = encodedUrlField.value;
                }

                var context = {
                    onevent : onevent,
                    onerror : onerror,
                    sourceid : element.id,
                    formid : form.id,
                    render : args["javax.faces.partial.render"]
                }
//                if (form.encoding == "multipart/form-data") {
                if (jsf.util.isFileUpload(form)) {
                    var iframeEngine = new IframeEngine(context);
                    iframeEngine.sendRequest(form, args);
                } else {
                    var ajaxEngine = new AjaxEngine();
                    ajaxEngine.setupArguments(args);
                    ajaxEngine.queryString = viewState;
                    ajaxEngine.context = context;

                    ajaxEngine.sendRequest();
                }

                // null out element variables to protect against IE memory leak
                element = null;
                form = null;

            },
            /**
             * <p>Receive an Ajax response from the server.
             * <p><b>Usage:</b></p>
             * <pre><code>
             * jsf.ajax.response(request, context);
             * </pre></code>
             * <p><b>Implementation Requirements:</b></p>
             * This function must evaluate the markup returned in the
             * <code>request.responseXML</code> object and perform the following action:
             * <ul>
             * <p>If there is no XML response returned, signal an <code>emptyResponse</code>
             * error. If the XML response does not follow the format as outlined
             * in Appendix A of the spec prose document <a
             *  href="../../javadocs/overview-summary.html#prose_document">linked in the
             *  overview summary</a> signal a <code>malformedError</code> error.  Refer to
             * section "Signaling Errors" in Chapter 13 of the spec prose document <a
             *  href="../../javadocs/overview-summary.html#prose_document">linked in the
             *  overview summary</a>.</p>
             * <p>If the response was successfully processed, send a <code>success</code>
             * event as outlined in Chapter 13 "Sending Events" section of the spec prose
             * document <a
             * href="../../javadocs/overview-summary.html#prose_document">linked in the
             * overview summary</a>.</p>
             * <p><i>Update Element Processing</i></p>
             * The <code>update</code> element is used to update a single DOM element.  The
             * "id" attribute of the <code>update</code> element refers to the DOM element that
             * will be updated.  The contents of the <code>CDATA</code> section is the data that
             * will be used when updating the contents of the DOM element as specified by the
             * <code>&lt;update&gt;</code> element identifier.
             * <li>If an <code>update</code> element is found in the response
             * with the identifier <code>javax.faces.ViewRoot</code>:
             * <pre><code>&lt;update id="javax.faces.ViewRoot"&gt;
             *    &lt;![CDATA[...]]&gt;
             * &lt;/update&gt;</code></pre>
             * Update the entire DOM replacing the appropriate <code>head</code> and/or
             * <code>body</code> sections with the content from the response.</li>
             * <li>If an <code>update</code> element is found in the response with the identifier
             * <code>javax.faces.ViewState</code>:
             * <pre><code>&lt;update id="javax.faces.ViewState"&gt;
             *    &lt;![CDATA[...]]&gt;
             * &lt;/update&gt;</code></pre>
             * locate and update the submitting form's <code>javax.faces.ViewState</code> value
             * with the <code>CDATA</code> contents from the response.  Locate and update the
             * <code>javax.faces.ViewState</code> value for all forms specified in the
             * <code>render</code> target list.</li>
             * <li>If an <code>update</code> element is found in the response with the identifier
             * <code>javax.faces.ViewHead</code>:
             * <pre><code>&lt;update id="javax.faces.ViewHead"&gt;
             *    &lt;![CDATA[...]]&gt;
             * &lt;/update&gt;</code></pre>
             * update the document's <code>head</code> section with the <code>CDATA</code>
             * contents from the response.</li>
             * <li>If an <code>update</code> element is found in the response with the identifier
             * <code>javax.faces.ViewBody</code>:
             * <pre><code>&lt;update id="javax.faces.ViewBody"&gt;
             *    &lt;![CDATA[...]]&gt;
             * &lt;/update&gt;</code></pre>
             * update the document's <code>body</code> section with the <code>CDATA</code>
             * contents from the response.</li>
             * <li>For any other <code>&lt;update&gt;</code> element:
             * <pre><code>&lt;update id="update id"&gt;
             *    &lt;![CDATA[...]]&gt;
             * &lt;/update&gt;</code></pre>
             * Find the DOM element with the identifier that matches the
             * <code>&lt;update&gt;</code> element identifier, and replace its contents with
             * the <code>&lt;update&gt;</code> element's <code>CDATA</code> contents.</li>
             * </li>
             * <p><i>Insert Element Processing</i></p>
             * <li>If an <code>&lt;insert&gt;</code> element is found in the response with the
             * attribute <code>before</code>:
             * <pre><code>&lt;insert id="insert id" before="before id"&gt;
             *    &lt;![CDATA[...]]&gt;
             * &lt;/insert&gt;</code></pre>
             * <ul>
             * <li>Extract this <code>&lt;insert&gt;</code> element's <code>CDATA</code> contents
             * from the response.</li>
             * <li>Find the DOM element whose identifier matches <code>before id</code> and insert
             * the <code>&lt;insert&gt;</code> element's <code>CDATA</code> content before
             * the DOM element in the document.</li>
             * </ul>
             * </li>
             * <li>If an <code>&lt;insert&gt;</code> element is found in the response with the
             * attribute <code>after</code>:
             * <pre><code>&lt;insert id="insert id" after="after id"&gt;
             *    &lt;![CDATA[...]]&gt;
             * &lt;/insert&gt;</code></pre>
             * <ul>
             * <li>Extract this <code>&lt;insert&gt;</code> element's <code>CDATA</code> contents
             * from the response.</li>
             * <li>Find the DOM element whose identifier matches <code>after id</code> and insert
             * the <code>&lt;insert&gt;</code> element's <code>CDATA</code> content after
             * the DOM element in the document.</li>
             * </ul>
             * </li>
             * <p><i>Delete Element Processing</i></p>
             * <li>If a <code>&lt;delete&gt;</code> element is found in the response:
             * <pre><code>&lt;delete id="delete id"/&gt;</code></pre>
             * Find the DOM element whose identifier matches <code>delete id</code> and remove it
             * from the DOM.</li>
             * <p><i>Element Attribute Update Processing</i></p>
             * <li>If an <code>&lt;attributes&gt;</code> element is found in the response:
             * <pre><code>&lt;attributes id="id of element with attribute"&gt;
             *    &lt;attribute name="attribute name" value="attribute value"&gt;
             *    ...
             * &lt/attributes&gt;</code></pre>
             * <ul>
             * <li>Find the DOM element that matches the <code>&lt;attributes&gt;</code> identifier.</li>
             * <li>For each nested <code>&lt;attribute&gt;</code> element in <code>&lt;attribute&gt;</code>,
             * update the DOM element attribute value (whose name matches <code>attribute name</code>),
             * with <code>attribute value</code>.</li>
             * </ul>
             * </li>
             * <p><i>JavaScript Processing</i></p>
             * <li>If an <code>&lt;eval&gt;</code> element is found in the response:
             * <pre><code>&lt;eval&gt;
             *    &lt;![CDATA[...JavaScript...]]&gt;
             * &lt;/eval&gt;</code></pre>
             * <ul>
             * <li>Extract this <code>&lt;eval&gt;</code> element's <code>CDATA</code> contents
             * from the response and execute it as if it were JavaScript code.</li>
             * </ul>
             * </li>
             * <p><i>Redirect Processing</i></p>
             * <li>If a <code>&lt;redirect&gt;</code> element is found in the response:
             * <pre><code>&lt;redirect url="redirect url"/&gt;</code></pre>
             * Cause a redirect to the url <code>redirect url</code>.</li>
             * <p><i>Error Processing</i></p>
             * <li>If an <code>&lt;error&gt;</code> element is found in the response:
             * <pre><code>&lt;error&gt;
             *    &lt;error-name&gt;..fully qualified class name string...&lt;error-name&gt;
             *    &lt;error-message&gt;&lt;![CDATA[...]]&gt;&lt;error-message&gt;
             * &lt;/error&gt;</code></pre>
             * Extract this <code>&lt;error&gt;</code> element's <code>error-name</code> contents
             * and the <code>error-message</code> contents. Signal a <code>serverError</code> passing
             * the <code>errorName</code> and <code>errorMessage</code>.  Refer to
             * section "Signaling Errors" in Chapter 13 of the spec prose document <a
             *  href="../../javadocs/overview-summary.html#prose_document">linked in the
             *  overview summary</a>.</li>
             * <p><i>Extensions</i></p>
             * <li>The <code>&lt;extensions&gt;</code> element provides a way for framework
             * implementations to provide their own information.</li>
             * <p><li>The implementation must check if &lt;script&gt; elements in the response can
             * be automatically run, as some browsers support this feature and some do not.
             * If they can not be run, then scripts should be extracted from the response and
             * run separately.</li></p>
             * </ul>
             *
             * </p>
             *
             * @param request The <code>XMLHttpRequest</code> instance that
             * contains the status code and response message from the server.
             *
             * @param context An object containing the request context, including the following properties:
             * the source element, per call onerror callback function, and per call onevent callback function.
             *
             * @throws  Error if request contains no data
             *
             * @function jsf.ajax.response
             */
            response: function response(request, context) {
                if (!request) {
                    throw new Error("jsf.ajax.response: Request parameter is unset");
                }

                // ensure context source is the dom element and not the ID
                // per 14.4.1 of the 2.0 specification.  We're doing it here
                // *before* any errors or events are propagated becasue the
                // DOM element may be removed after the update has been processed.
                if (typeof context.sourceid === 'string') {
                    context.sourceid = document.getElementById(context.sourceid);
                }

                var xml = request.responseXML;
                if (xml === null) {
                    sendError(request, context, "emptyResponse");
                    return;
                }

                if (getParseErrorText(xml) !== PARSED_OK) {
                    sendError(request, context, "malformedXML");
                    return;
                }

                var responseType = xml.getElementsByTagName("partial-response")[0].firstChild;

                if (responseType.nodeName === "error") { // it's an error
                    var errorName = responseType.firstChild.firstChild.nodeValue;
                    var errorMessage = responseType.firstChild.nextSibling.firstChild.nodeValue;
                    sendError(request, context, "serverError", null, errorName, errorMessage);
                    sendEvent(request, context, "success");
                    return;
                }


                if (responseType.nodeName === "redirect") {
                    window.location = responseType.getAttribute("url");
                    return;
                }


                if (responseType.nodeName !== "changes") {
                    sendError(request, context, "malformedXML", "Top level node must be one of: changes, redirect, error, received: " + responseType.nodeName + " instead.");
                    return;
                }


                var changes = responseType.childNodes;

                try {
                    for (var i = 0; i < changes.length; i++) {
                        switch (changes[i].nodeName) {
                            case "update":
                                doUpdate(changes[i], context);
                                break;
                            case "delete":
                                doDelete(changes[i]);
                                break;
                            case "insert":
                                doInsert(changes[i]);
                                break;
                            case "attributes":
                                doAttributes(changes[i]);
                                break;
                            case "eval":
                                doEval(changes[i]);
                                break;
                            case "extension":
                                // no action
                                break;
                            default:
                                sendError(request, context, "malformedXML", "Changes allowed are: update, delete, insert, attributes, eval, extension.  Received " + changes[i].nodeName + " instead.");
                                return;
                        }
                    }
                } catch (ex) {
                    sendError(request, context, "malformedXML", ex.message);
                    return;
                }
                sendEvent(request, context, "success");

            }
        };
    }();

    /**
     *
     * <p>Return the value of <code>Application.getProjectStage()</code> for
     * the currently running application instance.  Calling this method must
     * not cause any network transaction to happen to the server.</p>
     * <p><b>Usage:</b></p>
     * <pre><code>
     * var stage = jsf.getProjectStage();
     * if (stage === ProjectStage.Development) {
     *  ...
     * } else if stage === ProjectStage.Production) {
     *  ...
     * }
     * </code></pre>
     *
     * @returns String <code>String</code> representing the current state of the
     * running application in a typical product development lifecycle.  Refer
     * to <code>javax.faces.application.Application.getProjectStage</code> and
     * <code>javax.faces.application.ProjectStage</code>.
     * @function jsf.getProjectStage
     */
    jsf.getProjectStage = function() {
        // First, return cached value if available
        if (typeof mojarra !== 'undefined' && typeof mojarra.projectStageCache !== 'undefined') {
            return mojarra.projectStageCache;
        }
        var scripts = document.getElementsByTagName("script"); // nodelist of scripts
        var script; // jsf.js script
        var s = 0; // incremental variable for for loop
        var stage; // temp value for stage
        var match; // temp value for match
        while (s < scripts.length) {
            if (typeof scripts[s].src === 'string' && scripts[s].src.match('\/javax\.faces\.resource\/jsf\.js\?.*ln=javax\.faces')) {
                script = scripts[s].src;
                break;
            }
            s++;
        }
        if (typeof script == "string") {
            match = script.match("stage=(.*)");
            if (match) {
                stage = match[1];
            }
        }
        if (typeof stage === 'undefined' || !stage) {
            stage = "Production";
        }

        mojarra = mojarra || {};
        mojarra.projectStageCache = stage;

        return mojarra.projectStageCache;
    };


    /**
     * <p>Collect and encode state for input controls associated
     * with the specified <code>form</code> element.  This will include
     * all input controls of type <code>hidden</code>.</p>
     * <p><b>Usage:</b></p>
     * <pre><code>
     * var state = jsf.getViewState(form);
     * </pre></code>
     *
     * @param form The <code>form</code> element whose contained
     * <code>input</code> controls will be collected and encoded.
     * Only successful controls will be collected and encoded in
     * accordance with: <a href="http://www.w3.org/TR/html401/interact/forms.html#h-17.13.2">
     * Section 17.13.2 of the HTML Specification</a>.
     *
     * @returns String The encoded state for the specified form's input controls.
     * @function jsf.getViewState
     */
    jsf.getViewState = function(form) {
        if (!form) {
            throw new Error("jsf.getViewState:  form must be set");
        }
        var els = form.elements;
        var len = els.length;
        // create an array which we'll use to hold all the intermediate strings
        // this bypasses a problem in IE when repeatedly concatenating very
        // large strings - we'll perform the concatenation once at the end
        var qString = [];
        var addField = function(name, value) {
            var tmpStr = "";
            if (qString.length > 0) {
                tmpStr = "&";
            }
            tmpStr += encodeURIComponent(name) + "=" + encodeURIComponent(value);
            qString.push(tmpStr);
        };
        for (var i = 0; i < len; i++) {
            var el = els[i];
            if (!el.disabled) {
                switch (el.type) {
                    case 'text':
                    case 'password':
                    case 'hidden':
                    case 'textarea':
                        addField(el.name, el.value);
                        break;
                    case 'select-one':
                        if (el.selectedIndex >= 0) {
                            addField(el.name, el.options[el.selectedIndex].value);
                        }
                        break;
                    case 'select-multiple':
                        for (var j = 0; j < el.options.length; j++) {
                            if (el.options[j].selected) {
                                addField(el.name, el.options[j].value);
                            }
                        }
                        break;
                    case 'checkbox':
                    case 'radio':
                        if (el.checked) {
                            addField(el.name, el.value || 'on');
                        }
                        break;
                }
            }
        }
        // concatenate the array
        return qString.join("");
    };

    /**
     * The namespace for JavaServer Faces JavaScript utilities.
     * @name jsf.util
     * @namespace
     */
    jsf.util = {};

    /**
     * <p>A varargs function that invokes an arbitrary number of scripts.
     * If any script in the chain returns false, the chain is short-circuited
     * and subsequent scripts are not invoked.  Any number of scripts may
     * specified after the <code>event</code> argument.</p>
     *
     * @param source The DOM element that triggered this Ajax request, or an
     * id string of the element to use as the triggering element.
     * @param event The DOM event that triggered this Ajax request.  The
     * <code>event</code> argument is optional.
     *
     * @returns boolean <code>false</code> if any scripts in the chain return <code>false</code>,
     *  otherwise returns <code>true</code>
     *
     * @function jsf.util.chain
     */
    jsf.util.chain = function(source, event) {

        if (arguments.length < 3) {
            return true;
        }

        // RELEASE_PENDING rogerk - shouldn't this be getElementById instead of null
        var thisArg = (typeof source === 'object') ? source : null;

        // Call back any scripts that were passed in
        for (var i = 2; i < arguments.length; i++) {

            var f = new Function("event", arguments[i]);
            var returnValue = f.call(thisArg, event);

            if (returnValue === false) {
                return false;
            }
        }
        return true;

    };

    jsf.util.isFileUpload = function (form) {
        for (var i = 0; i < form.elements.length; i++) {
            var el = form.elements[i];
            if ((el.tagName.toLowerCase() == 'input')) {
                if (el.type.toLowerCase() == 'file') {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * <p>An integer specifying the specification version that this file implements.
     * It's format is: rightmost two digits, bug release number, next two digits,
     * minor release number, leftmost digits, major release number.
     * This number may only be incremented by a new release of the specification.</p>
     */
    jsf.specversion = 20000;

    /**
     * <p>An integer specifying the implementation version that this file implements.
     * It's a monotonically increasing number, reset with every increment of
     * <code>jsf.specversion</code>
     * This number is implementation dependent.</p>
     */
    jsf.implversion = 3;


} //end if version detection block
