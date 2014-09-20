<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.

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

<!-- peTree.jsf 

JVM             common  100
Logger Setting  common  150

Web Container    web  200
EJB Container    ejb-lite  250
JMS             jms-plugin  300
Security       common  350
Transaction    jts  400
HTTP Srvice    web  450
Virtual Server web  500
Network Config web  550
ORB            corba  600
Thread Pools   web  650
Admin Service  common 700
Connector Service   750
monitoring     web  800

    // Parent == "tree" priorities:
    200: pluginTreeNodeServer.jsf
    300: pluginTreeNodeApps.jsf
    400: pluginTreeNodeResources.jsf
    500: pluginTreeNodeClusters.jsf
    530: pluginTreeNodeInstances.jsf
    560: pluginTreeNodeNodes.jsf
    700: pluginTreeNodeConfigurations.jsf
    800: update center
-->

// NOTE: Do not include anything outside the sun:form component... it will
// NOTE: not get used on tree refresh.

  <sun:form id="treeForm">
    <!beforeCreate
      gf.listConfigs(configs="#{pageSession.configsList}");
      setAttribute(key="configName" value="#{pageSession.configsList[0]}");
      gf.encodeId(id="${configName}" result="#{requestScope.configNameId}");
    />
    <h:commandButton id="update" style="display: none;">
        <!command
            setResourceBundle(key="i18n" bundle="org.glassfish.admingui.core.Strings");

            // Must use entire treeForm so that all IntegrationPoints are considered
            getUIComponent(clientId="treeForm", component="#{requestScope.oldComp}");
            setAttribute(key="parent" value="#{requestScope.oldComp.parent}");

            // Replace it with something temporary to hold position
            // Needed, or it it will just find the old one vs. create it
            createComponent(type="staticText" component=>$attribute{temp});
            replaceUIComponent(old="$attribute{oldComp}" new="$attribute{temp}");

            // Rengenerate entire tree (must create everything to be complete)
            getLayoutComponent(
                viewId="/common/peTree.inc"
                clientId="treeForm"
                component=>$attribute{desc});
            buildUIComponentTree(layoutElement="$attribute{desc}" parent="#{parent}" result=>$attribute{newComp});
            replaceUIComponent(old="${temp}" new="$attribute{newComp}");
        />
    </h:commandButton>
    <sun:tree id="tree" 
              text="$resource{i18n.tree.commonTasks}"
              url="/common/commonTask.jsf"
              imageURL="/resource/images/common_tasks.gif"
              clientSide="#{true}">
              <!facet content>
                // Doing this avoids the JS which highlights the entire tree.
                <sun:hyperlink id="ct" url="/common/commonTask.jsf" text="$resource{i18n.tree.commonTasks}" />
              </facet>
          <!afterCreate
              getClientId(component="$this{component}" clientId=>$page{treeId});
          />
    </sun:tree>

    <event>
        <!afterCreate
            includeIntegrations(type="org.glassfish.admingui:navNode" root="#{pageSession.treeId}");
            foreach(var="configName" list="#{pageSession.configsList}") {
                if ("!(${configName}=#{pageSession.configsList[0]})") {
                    gf.encodeId(id="${configName}" result="#{requestScope.configNameId}");
                    buildUIComponentTree(layoutElement="$attribute{configTreeNodeLE}" parent="$attribute{configsTreeNode}");
                    includeIntegrations(type="org.glassfish.admingui:navNode" root="$attribute{configsTreeNode}");
                }
            }
        />
    </event>

<!-- We only want to set the width for IE browser only. -->
<f:verbatim>
<script type="text/javascript">
function setTreeWidth(treeId) {
    var version = navigator.appVersion;
    var ix = version.indexOf('MSIE');
    if (ix == -1){
        return;
    }
    var foo = document.getElementById(treeId);
    foo.style.width="40em";
}
</script>
</f:verbatim>
  </sun:form>
