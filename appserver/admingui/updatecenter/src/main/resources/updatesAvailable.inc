<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.

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

<sun:panelGroup id="ucPanel" separator="">
    <sun:imageHyperlink id="imgHlink" url="#{request.contextPath}/updateCenter/update.jsf"
	    style="text-decoration: none"   
	    alt="$resource{i18n.tree.updateCenter} #{sessionScope._updateCountMsg}"
	    toolTip="$resource{i18n.tree.updateCenter} #{sessionScope._updateCountMsg}"
	    imageURL="#{request.contextPath}/resource/updateCenter/images/updateCenter.png" 
	    text="#{sessionScope._updateCountMsg}"
	    style="color:white; margin-left: 5px; text-decoration: none;"
    >
    <ui:event type="beforeCreate">
        setSessionAttribute(key="_hideUpdateMsg" value="#{true}");
        if ("#{sessionScope._updateCountCalculated}=#{null}") {
            getUpdateComponentCount();
            setSessionAttribute(key="_updateCountCalculated" value="true");
            setSessionAttribute(key="_currentUpdateCountMsg" value="#{sessionScope._updateCountMsg}");
        }
    </ui:event>
    </sun:imageHyperlink>
</sun:panelGroup>
