<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
    or packager/legal/LICENSE.txt.  See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at packager/legal/LICENSE.txt.

    GPL Classpath Exception:
    Oracle designates this particular file as subject to the "Classpath"
    exception as provided by Oracle in the GPL Version 2 section of the License
    file that accompanied this code.

    Modifications:
    If applicable, add the following below the License Header, with the fields
    enclosed by brackets [] replaced by your own identifying information:
    "Portions Copyright [year] [name of copyright owner]"

    Contributor(s):
    If you wish your version of this file to be governed by only the CDDL or
    only the GPL Version 2, indicate your decision by adding "[Contributor]
    elects to include this software in this distribution under the [CDDL or GPL
    Version 2] license."  If you don't indicate a single choice of license, a
    recipient has the option to distribute your version of this file under
    either the CDDL, the GPL Version 2 or to extend the choice of license to
    its licensees as provided above.  However, if you add GPL Version 2 code
    and therefore, elected the GPL Version 2 license, then the option applies
    only if the new code is made subject to such option by the copyright
    holder.

-->

<!-- grizzly/listenerTabs.inc -->

<sun:tabSet id="listenerTabs" immediate="$boolean{true}" selected="#{sessionScope.listenerTabs}">
    <sun:tab id="generalTab" immediate="$boolean{true}" text="$resource{i18n_web.grizzly.networkListener.listenerTab}" toolTip="$resource{i18n_web.grizzly.networkListener.listenerTab} Tab" >
        <!command
        setSessionAttribute(key="listenerTabs" value="generalTab");
        gf.redirect(page="#{request.contextPath}/web/grizzly/networkListenerEdit.jsf?configName=#{configName}&name=#{pageSession.encodedListenerName}&cancelTo=#{pageSession.cancelTo}");
        />
    </sun:tab>
    <sun:tab id="sslTab" rendered="#{showSSLTab}" immediate="$boolean{true}" text="$resource{i18n_web.grizzly.sslTab}" toolTip="$resource{i18n_web.grizzly.sslTab} Tab" >
        <!command
        setSessionAttribute(key="listenerTabs" value="sslTab");
        gf.redirect(page="#{request.contextPath}/web/grizzly/listenerSSLEdit.jsf?configName=#{configName}&name=#{pageSession.encodedProtocolName}&listenerName=#{pageSession.encodedListenerName}&cancelTo=#{pageSession.cancelTo}");
        />
    </sun:tab>
    <sun:tab id="httpTab" rendered="#{showHttpTab}"  immediate="$boolean{true}" text="$resource{i18n_web.grizzly.httpTab}" toolTip="$resource{i18n_web.grizzly.httpTab} Tab" >
        <!command
            setSessionAttribute(key="listenerTabs" value="httpTab");
            gf.redirect(page="#{request.contextPath}/web/grizzly/listenerHttpEdit.jsf?configName=#{pageSession.configName}&name=#{pageSession.encodedProtocolName}&listenerName=#{pageSession.encodedListenerName}&cancelTo=#{pageSession.cancelTo}");
        />
    </sun:tab>
    <sun:tab id="fileCacheTab" rendered="#{showFileCacheTab}" immediate="$boolean{true}" text="$resource{i18n_web.grizzly.fileCacheTab}" toolTip="$resource{i18n_web.grizzly.fileCacheTab} Tab" >
        <!command
        setSessionAttribute(key="listenerTabs" value="fileCacheTab");
        gf.redirect(page="#{request.contextPath}/web/grizzly/listenerFileCache.jsf?configName=#{pageSession.configName}&name=#{pageSession.encodedProtocolName}&listenerName=#{pageSession.encodedListenerName}&cancelTo=#{pageSession.cancelTo}");
        />
        
    </sun:tab>

</sun:tabSet>
