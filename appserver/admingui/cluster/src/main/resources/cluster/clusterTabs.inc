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

<!-- cluster/clusterTabs.jsf -->

<sun:tabSet id="clusterTabs" immediate="$boolean{true}" selected="#{sessionScope.clusterTabs}">
    <!beforeCreate
        setResourceBundle(key="i18nc" bundle="org.glassfish.common.admingui.Strings");
        setResourceBundle(key="i18ncs" bundle="org.glassfish.cluster.admingui.Strings");
    />
    <sun:tab id="general" immediate="$boolean{true}" text="$resource{i18ncs.cluster.GeneralTab}" >
        <!command
            setSessionAttribute(key="clusterTabs" value="general");
            gf.redirect(page="#{request.contextPath}/cluster/cluster/clusterGeneral.jsf?clusterName=#{pageSession.encodedClusterName}");
        />       
    </sun:tab>

    <sun:tab id="clusterApp" immediate="$boolean{true}" text="$resource{i18ncs.cluster.ApplicationsTab}">
        <!command
            setSessionAttribute(key="clusterTabs" value="clusterApp");
            gf.redirect(page="#{request.contextPath}/cluster/cluster/clusterApplications.jsf?clusterName=#{encodedClusterName}");
        />
    </sun:tab>

    <sun:tab id="clusterInst" immediate="$boolean{true}" text="$resource{i18ncs.cluster.InstancesTab}">
        <!command
            setSessionAttribute(key="clusterTabs" value="clusterInst");
            gf.redirect(page="#{request.contextPath}/cluster/cluster/clusterInstances.jsf?clusterName=#{pageSession.encodedClusterName}");
        />
    </sun:tab>

     <sun:tab id="clusterResources" immediate="$boolean{true}" text="$resource{i18ncs.cluster.ResourcesTab}">
        <!command
            setSessionAttribute(key="clusterTabs" value="clusterResources");
            gf.redirect(page="#{request.contextPath}/cluster/cluster/clusterResources.jsf?clusterName=#{encodedClusterName}");
        />
    </sun:tab>

        <!-- Should be from the jms plugin
    <sun:tab id="clusterPhysicalDest" immediate="$boolean{true}" text="$resource{i18ncs.cluster.PhysicalDestTab}">
        <!command
            setSessionAttribute(key="clusterTabs" value="clusterPhysicalDest");
            redirect(page="../cluster/clusterPhysicalDest.jsf?name=#{encodedClusterName}");
        />
    </sun:tab>
        -->

    <sun:tab id="clusterProps" immediate="$boolean{true}" text="$resource{i18ncs.cluster.PropertiesTab}">
        <!command
            setSessionAttribute(key="clusterTabs" value="clusterSystemProps");
            gf.redirect(page="#{request.contextPath}/cluster/cluster/clusterSystemProperties.jsf?clusterName=#{encodedClusterName}");
        />
        <sun:tab id="clusterSystemProps" immediate="$boolean{true}" text="$resource{i18ncs.cluster.SystemPropertiesTab}">
            <!command
                setSessionAttribute(key="clusterTabs" value="clusterSystemProps");
                gf.redirect(page="#{request.contextPath}/cluster/cluster/clusterSystemProperties.jsf?clusterName=#{encodedClusterName}");
            />
        </sun:tab>
        <sun:tab id="clusterInstanceProps" immediate="$boolean{true}" text="$resource{i18ncs.cluster.ClusterProperties}">
            <!command
                setSessionAttribute(key="clusterTabs" value="clusterInstanceProps");
                gf.redirect(page="#{request.contextPath}/cluster/cluster/clusterProperties.jsf?clusterName=#{encodedClusterName}");
            />
        </sun:tab>    
    </sun:tab>
</sun:tabSet>
<event>
    <!afterCreate
        includeIntegrations(type="org.glassfish.admingui:clusterTab");
    />
</event>
