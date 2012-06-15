/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

function ajaxSubmit(elem) {
    var form = getForm(elem);
    var method = form.method.toLowerCase();
    var url = form.action;

    var xmlhttp = (window.XMLHttpRequest) ?
        new XMLHttpRequest() : // code for IE7+, Firefox, Chrome, Opera, Safari
        new ActiveXObject("Microsoft.XMLHTTP"); // code for IE6, IE5

    xmlhttp.open(method, url, false);
    xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
    xmlhttp.setRequestHeader("X-Requested-By", "GlassFish REST HTML interface");
    if (method == "get") {
        xmlhttp.send();
    } else {
        xmlhttp.send(gatherFormParameters(form));
    }
    // Get the response inside the body tags and replace the document body
    var start = xmlhttp.responseText.indexOf("<body");
    var end = xmlhttp.responseText.indexOf("</body");
    document.body.innerHTML = xmlhttp.responseText.substring(start+6, end);
}

function getForm(elem) {
    while (elem.tagName.toLowerCase() != 'form') {
        elem = elem.parentNode;
    }

    return elem;
}

function gatherFormParameters(form) {
    var result = "";
    var sep = "";
    var elements = form.elements;
    var length = elements.length;
    for (var i = 0; i < length; i++) {
        var element = elements[i];
        var name = element.name;
        var type = element.type.toLowerCase();
        var value = "";
        if (type == 'select') {
            value = element.options[element.selectedIndex];
        } else {
            value = element.value;
        }

        result += sep + encodeURIComponent(name) + "=" + encodeURIComponent(value);
        sep="&";
    }

    return result;
}