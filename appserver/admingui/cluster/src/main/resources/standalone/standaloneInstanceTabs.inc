<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.

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

<!-- standalone/standaloneInstanceTabs.inc -->

<sun:tabSet id="standaloneInstanceTabs" immediate="$boolean{true}" selected="#{sessionScope.standaloneInstanceTabs}">

    <sun:tab id="general" immediate="$boolean{true}" text="$resource{i18nc.GeneralTab}" >
        <!command
            setSessionAttribute(key="standaloneInstanceTabs" value="general");
            gf.redirect(page="#{request.contextPath}/cluster/standalone/standaloneInstanceGeneral.jsf?instanceName=#{pageSession.encodedInstanceName}");
        />       
    </sun:tab>

   <sun:tab id="standaloneApps" immediate="$boolean{true}" text="$resource{i18ncs.cluster.ApplicationsTab}">
	<!command
            setSessionAttribute(key="standaloneInstanceTabs" value="standaloneApps");
            gf.redirect(page="#{request.contextPath}/cluster/standalone/standaloneApplications.jsf?instanceName=#{pageSession.encodedInstanceName}");
        />
    </sun:tab>

    <sun:tab id="resources" immediate="$boolean{true}" text="$resource{i18nc.resources}" >
        <!command
            setSessionAttribute(key="standaloneInstanceTabs" value="resources");
            gf.redirect(page="#{request.contextPath}/cluster/standalone/standaloneInstanceResources.jsf?instanceName=#{pageSession.encodedInstanceName}");
        />
    </sun:tab>

    <sun:tab id="standaloneProp" immediate="$boolean{true}" text="$resource{i18nc.PropertiesTab}">
        <!command
            setSessionAttribute(key="standaloneInstanceTabs" value="configProps");
            gf.redirect(page="#{request.contextPath}/cluster/standalone/standaloneInstanceConfigProperties.jsf?instanceName=#{pageSession.encodedInstanceName}");
        />
        <sun:tab id="configProps" immediate="$boolean{true}" text="$resource{i18n.inst.tab.ConfigProperties}" >
            <!command
                setSessionAttribute(key="standaloneInstanceTabs" value="configProps");
                gf.redirect(page="#{request.contextPath}/cluster/standalone/standaloneInstanceConfigProperties.jsf?instanceName=#{pageSession.encodedInstanceName}");
            />
	</sun:tab>
        <sun:tab id="instanceProps" immediate="$boolean{true}" text="$resource{i18n.inst.tab.InstanceProperties}" >
	    <!command
                setSessionAttribute(key="standaloneInstanceTabs" value="instanceProps");
                gf.redirect(page="#{request.contextPath}/cluster/standalone/standaloneInstanceProperties.jsf?instanceName=#{pageSession.encodedInstanceName}");
	    />
	</sun:tab>
        <sun:tab id="microProfileProps" immediate="$boolean{true}" text="$resource{i18n.inst.tab.MicroProfileProperties}" >
	    <!command
                setSessionAttribute(key="standaloneInstanceTabs" value="microProfileProps");
                gf.redirect(page="#{request.contextPath}/cluster/standalone/standaloneMicroProfileProperties.jsf?instanceName=#{pageSession.encodedInstanceName}");
	    />
	</sun:tab>
    </sun:tab>
</sun:tabSet>
<event>
    <!afterCreate
        includeIntegrations(type="org.glassfish.admingui:standaloneInstTab");
    />
</event>
