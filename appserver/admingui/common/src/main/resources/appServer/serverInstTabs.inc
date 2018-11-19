<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.

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

    Portions Copyright [2018] [Payara Foundation and/or its affiliates]

-->

<!-- appServer/serverInstTabs.jsf -->

<sun:tabSet id="serverInstTabs" immediate="true" selected="#{sessionScope.serverInstTabs}">
    <sun:tab id="general" immediate="true" text="$resource{i18n.common.General}" toolTip="$resource{i18n.common.GeneralTab}" >
        <!command
            setSessionAttribute(key="serverInstTabs" value="general");
            gf.redirect(page="#{request.contextPath}/common/appServer/serverInstGeneralPe.jsf");
        />
    </sun:tab>
    <sun:tab id="resources" immediate="true" text="$resource{i18nc.resources}" toolTip="$resource{i18nc.resourcesTab}">
        <!command
            setSessionAttribute(key="serverInstTabs" value="resources");
            gf.redirect(page="#{request.contextPath}/common/appServer/serverInstResources.jsf?instanceName=server");
        />
    </sun:tab>

    <sun:tab id="serverInstProps" immediate="$boolean{true}" text="$resource{i18nc.PropertiesTab}">
        <!command
            setSessionAttribute(key="serverInstTabs" value="configProps");
            gf.redirect(page="#{request.contextPath}/common/appServer/instanceConfigProperties.jsf?instanceName=server");
        />
        <sun:tab id="configProps" immediate="$boolean{true}" text="$resource{i18nc.instanceProperties.SystemPropertiesTitle}" >
            <!command
                setSessionAttribute(key="serverInstTabs" value="configProps");
                gf.redirect(page="#{request.contextPath}/common/appServer/instanceConfigProperties.jsf?instanceName=server");
            />
	</sun:tab>
        <sun:tab id="instanceProps" immediate="$boolean{true}" text="$resource{i18nc.instanceProperties.PropertiesTitle}" >
	    <!command
                setSessionAttribute(key="serverInsTabs" value="instanceProps");
                gf.redirect(page="#{request.contextPath}/common/appServer/instanceProperties.jsf?instanceName=server");
	    />
	</sun:tab>
        <sun:tab id="microProfileProps" immediate="$boolean{true}" text="$resource{i18nc.instanceProperties.MicroProfilePropertiesTitle}" >
	    <!command
                setSessionAttribute(key="serverInsTabs" value="microProfileProps");
                gf.redirect(page="#{request.contextPath}/common/appServer/instanceMicroProfileProperties.jsf?instanceName=server");
	    />
	</sun:tab>
    </sun:tab>

</sun:tabSet>
<event>
    <!afterCreate
        includeIntegrations(type="org.glassfish.admingui:serverInstTab");
    />
</event>
