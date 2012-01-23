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
function getDisplay(elem) {
    return document.getElementById(elem).style.display;
}

function setDisplay(elem, value) {
    if (typeof(elem) == 'string') {
        elem = document.getElementById(elem);
    }
    return elem.style.display = value;
}

function updateUI() {
    try {
        var jmsBasicConfig = document.getElementById('propertyForm:propertySheet:propertySectionTextField:jmsConfigTypeProp:optBasic').checked;
        var jmsCustomConfig = document.getElementById('propertyForm:propertySheet:propertySectionTextField:jmsConfigTypeProp:optCustom').checked;
    } catch (e) {
        jmsBasicConfig = false;
        jmsCustomConfig = true;
    }
    if (!(jmsBasicConfig || jmsCustomConfig)) {
        document.getElementById('propertyForm:propertySheet:propertySectionTextField:jmsConfigTypeProp:optBasic').checked = true;
        jmsBasicConfig = true;
    }
    updateJmsPropertySheet(!jmsBasicConfig);
}
    
function updateJmsPropertySheet(customConfig) {
    var jmsTypeSheet = 'propertyForm:jmsTypePropertySheet';
    var jmsPropsheet = 'propertyForm:jmsPropertySheet';
    if (!customConfig) {
        setDisplay(jmsTypeSheet, 'none');
        setDisplay(jmsPropsheet, 'none');
        return;
    }
    
    var baseId = jmsPropsheet + ':configureJmsClusterSection';
    var configStoreType = document.getElementById(baseId+':ConfigStoreTypeProp:configStoreType').value;
    var messageStoreType = document.getElementById(baseId+':MessageStoreTypeProp:messageStoreType').value;
    var pwdSel = document.getElementById(baseId+':PswdSelProp:pwdSel').value;
    
    var conventional = document.getElementById(baseId + ':ClusterTypeProp:optConventional').checked;
    var enhanced = document.getElementById(baseId + ':ClusterTypeProp:optEnhanced').checked;
    
    var embedded = document.getElementById(jmsTypeSheet + ':jmsTypeSection:jmsTypeProp:optEmbedded').checked;
    var local = document.getElementById(jmsTypeSheet + ':jmsTypeSection:jmsTypeProp:optLocal').checked;
    var remote = document.getElementById(jmsTypeSheet + ':jmsTypeSection:jmsTypeProp:optRemote').checked;

    setDisplay(jmsTypeSheet, 'block');
    setDisplay(jmsPropsheet, 'block');

    // Update hidden field for type
    document.getElementById(jmsTypeSheet + ':jmsTypeSection:jmsTypeProp:jmsType').value =
        (embedded ? "EMBEDDED" : (local ? "LOCAL" : "REMOTE"));
    
    if (remote) {
        setDisplay(jmsPropsheet, 'none');
    } else {     
        setDisplay(jmsPropsheet, 'block');
        
        if (embedded) {
            setDisplay(baseId + ':ClusterTypeProp:optEnhanced_span', 'none');
            document.getElementById(baseId + ':ClusterTypeProp:optConventional').checked = true;
            conventional = true;
            enhanced = false;
        } else {
            setDisplay(baseId + ':ClusterTypeProp:optEnhanced_span', 'block');
        }
        document.getElementById(baseId+':ClusterTypeProp:clusterType').value = (conventional ? "conventional" : "enhanced");
       
        if (enhanced) {
            setDisplay(baseId+':ConfigStoreTypeProp', 'none');
            setDisplay(baseId+':MessageStoreTypeProp', 'none');
            var elems = getByClass("__database");
            for (var i=0; i < elems.length; i++) {
                setDisplay(elems[i], 'table-row');
            }
            if (embedded) {
                document.getElementById(jmsTypeSheet + ':jmsTypeSection:jmsTypeProp:optLocal').checked = true;
                document.getElementById(jmsTypeSheet + ':jmsTypeSection:jmsTypeProp:jmsType').value = 'LOCAL';
                local = true;
                embedded = false;
            }
            fixPasswordFields(baseId, pwdSel);
        }
        
        if (conventional) {
            setDisplay(baseId+':ConfigStoreTypeProp', 'table-row');
            setDisplay(baseId+':MessageStoreTypeProp', 'table-row');

            if ((messageStoreType == 'file') && (configStoreType == 'masterbroker')) { //} && (getDisplay(baseId+':MessageStoreTypeProp') != 'none')) {
                var elems = getByClass("__database");
                for (var i=0; i < elems.length; i++) {
                    setDisplay(elems[i], 'none');
                }
            } else {
                var elems = getByClass("__database");
                for (var i=0; i < elems.length; i++) {
                    setDisplay(elems[i], 'table-row');
                }
        
                fixPasswordFields(baseId, pwdSel);
            }
        }
    }
}

function fixPasswordFields(baseId, pwdSel) {
    if (pwdSel == 'password') {
        setDisplay(baseId+':PswdTextProp', 'table-row');
        setDisplay(baseId+':PswdAliasProp', 'none');
    } else {
        setDisplay(baseId+':PswdTextProp', 'none');
        setDisplay(baseId+':PswdAliasProp', 'table-row');
    }
}

function getByClass (className, parent) {
    parent || (parent=document);
    var descendants=parent.getElementsByTagName('*'), i=-1, e, result=[];
    while (e = descendants[++i]) {
        ((' '+(e['class']||e.className)+' ').indexOf(' '+className+' ') > -1) && result.push(e);
    }
    return result;
}
