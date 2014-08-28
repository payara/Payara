<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.

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

<sun:button id="saveButton" rendered="#{edit}" text="$resource{i18n.button.Save} "
    onClick="if (checkSSHPswd('#{pageSession.typeId}','#{pageSession.nodeHostId}',
        '#{pageSession.psSelectId}', '#{pageSession.ps1Id}', 
        '#{pageSession.setupSshId}', '#{pageSession.setupPswdId}',
        '#{pageSession.winPsSelectId}', '#{pageSession.winPs1Id}' ))
    {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;"
>
    <!command
        setAttribute(key="includeInstallDir" value="false");
        if ("#{pageSession.valueMap['type']}=CONFIG"){
            setAttribute(key="attrsList" value={"nodehost", "nodedir" });
            setAttribute(key="endpoint" value="nodes/node/#{pageSession.nodeName}/update-node-config");
            gfr.prepareNodeCmd();
            mapPut(map="#{pageSession.attrsMap}", key="installdir" value="#{pageSession.valueMap['installdirConfig']}");
        }
        if ("#{pageSession.valueMap['type']}=SSH"){
            gfr.fixPswd();
            if (!#{pageSession.valueMap['force']}){
                mapPut(map="#{pageSession.valueMap}"  key="force" value="false");
            }
            setAttribute(key="attrsList" value={"nodehost", "nodedir", "installdir", "sshport", "sshuser", "sshpassword" "sshkeyfile", "force"});
            setAttribute(key="endpoint" value="nodes/node/#{pageSession.nodeName}/update-node-ssh");
            gfr.prepareNodeCmd();
            //Run setup-ssh
            if ("#{pageSession.valueMap['setupSsh']}=true"){
                createMap(result="#{pageSession.sMap}");
                if ("#{pageSession.valueMap['generatekey']}"){
                    mapPut(map="#{pageSession.sMap}"  key="generatekey" value="true");
                }
                if ("#{pageSession.setupPswd}"){
                    mapPut(map="#{pageSession.sMap}", key="sshpassword" value="#{pageSession.setupPswd}");
                }
                mapPut(map="#{pageSession.sMap}", key="hosts" value="#{pageSession.valueMap['nodehost']}");
                foreach(var="sattr" list={"nodehost", "sshport", "sshuser",  "sshkeyfile" "sshpublickeyfile" } ){
                    mapGet(Map="#{pageSession.valueMap}" Key="#{requestScope.sattr}" Value="#{requestScope.sval}");
                    if ("#{requestScope.sval}"){
                        mapPut(map="#{pageSession.sMap}", key="#{requestScope.sattr}" value="#{requestScope.sval}");
                    }
                }
                gf.restRequest( endpoint="#{sessionScope.REST_URL}/nodes/setup-ssh" attrs="#{pageSession.sMap}" method="POST" )
            }
        }
        if ("#{pageSession.valueMap['type']}=DCOM"){
            gfr.fixWinPswd();
            setAttribute(key="attrsList" value={"nodehost", "nodedir", "installdir", "windowsdomain", "windowsuser", "windowspassword"});
            setAttribute(key="endpoint" value="nodes/node/#{pageSession.nodeName}/update-node-dcom");
            if (!#{pageSession.valueMap['winForce']}){
                mapPut(map="#{pageSession.valueMap}"  key="winForce" value="false");
            }
            gfr.prepareNodeCmd();
            mapPut(map="#{pageSession.attrsMap}"  key="force" value="#{pageSession.valueMap['winForce']}");

        }
        gf.restRequest(endpoint="#{sessionScope.REST_URL}/#{requestScope.endpoint}" method="POST" attrs="#{pageSession.attrsMap}")
        prepareSuccessfulMsg();
        gf.redirect(page="#{request.contextPath}/cluster/#{selfPage}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
    />
</sun:button>

<sun:button id="newButton" rendered="#{!edit}" text="$resource{i18n.button.OK}"
    onClick="if (checkSSHPswd('#{pageSession.typeId}', '#{pageSession.nodeHostId}',
        '#{pageSession.psSelectId}', '#{pageSession.ps1Id}',
        '#{pageSession.setupSshId}', '#{pageSession.setupPswdId}',
        '#{pageSession.winPsSelectId}', '#{pageSession.winPs1Id}'))
    {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;"
>
<!command
    setAttribute(key="nameToCheck" value="#{pageSession.valueMap['id']}");
    gfj.checkNameExist();
    setAttribute(key="includeInstallDir" value="true");
    if ("#{pageSession.valueMap['type']}=CONFIG"){
        setAttribute(key="attrsList" value={"id", "nodehost", "nodedir"});
        setAttribute(key="endpoint" value="nodes/create-node-config");
        gfr.prepareNodeCmd();
        mapPut(map="#{pageSession.attrsMap}", key="installdir" value="#{pageSession.valueMap['installdirConfig']}");
    }
    if ("#{pageSession.valueMap['type']}=SSH"){
        gfr.fixPswd();
        setAttribute(key="attrsList" value={"id", "nodehost", "nodedir", "installdir", "sshport", "sshuser", "sshpassword" "sshkeyfile", "force", "install"});
        setAttribute(key="endpoint" value="nodes/create-node-ssh");
        if (!#{pageSession.valueMap['force']}){
            mapPut(map="#{pageSession.valueMap}"  key="force" value="false");
        }
        gfr.prepareNodeCmd();

        //Run setup-ssh
        if ("#{pageSession.valueMap['setupSsh']}=true"){
            createMap(result="#{pageSession.sMap}");
            if ("#{pageSession.valueMap['generatekey']}"){
                mapPut(map="#{pageSession.sMap}"  key="generatekey" value="true");
            }
            if ("#{pageSession.setupPswd}"){
                mapPut(map="#{pageSession.sMap}", key="sshpassword" value="#{pageSession.setupPswd}");
            }
            mapPut(map="#{pageSession.sMap}", key="hosts" value="#{pageSession.valueMap['nodehost']}");
            foreach(var="sattr" list={"nodehost", "sshport", "sshuser",  "sshkeyfile" "sshpublickeyfile" } ){
                mapGet(Map="#{pageSession.valueMap}" Key="#{requestScope.sattr}" Value="#{requestScope.sval}");
                if ("#{requestScope.sval}"){
                    mapPut(map="#{pageSession.sMap}", key="#{requestScope.sattr}" value="#{requestScope.sval}");
                }
            }
            gf.restRequest( endpoint="#{sessionScope.REST_URL}/nodes/setup-ssh" attrs="#{pageSession.sMap}" method="POST" )
        }
    }
    if ("#{pageSession.valueMap['type']}=DCOM"){
        gfr.fixWinPswd();
        //call validate-dcom first
        if ("#{pageSession.valueMap['validateDcom']}"){
            createMap(result="#{pageSession.dMap}");
            mapPut(map="#{pageSession.dMap}", key="host" value="#{pageSession.valueMap['nodehost']}");
            foreach(var="dattr" list={"windowsuser", "windowsdomain", "windowspassword" "remotetestdir" } ){
                mapGet(Map="#{pageSession.valueMap}" Key="#{requestScope.dattr}" Value="#{requestScope.dval}");
                if ("#{requestScope.dval}"){
                    mapPut(map="#{pageSession.dMap}", key="#{requestScope.dattr}" value="#{requestScope.dval}");
                }
            }
            gf.restRequest( endpoint="#{sessionScope.REST_URL}/nodes/validate-dcom" attrs="#{pageSession.dMap}" method="GET" )
        }
        setAttribute(key="attrsList" value={"id", "nodehost", "nodedir", "installdir", "windowsuser", "windowsdomain", "windowspassword", "install" });
        setAttribute(key="endpoint" value="nodes/create-node-dcom");
        if (!#{pageSession.valueMap['winForce']}){
            mapPut(map="#{pageSession.valueMap}"  key="winForce" value="false");
        }
        gfr.prepareNodeCmd();
        mapPut(map="#{pageSession.attrsMap}"  key="force" value="#{pageSession.valueMap['winForce']}");
    }
    gf.restRequest( endpoint="#{sessionScope.REST_URL}/#{requestScope.endpoint}" attrs="#{pageSession.attrsMap}" method="POST" )
    gf.redirect(page="#{request.contextPath}/cluster/node/nodes.jsf?alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
 />
</sun:button>

<sun:button id="cancelButton" immediate="#{true}" text="$resource{i18n.button.Cancel}" primary="#{false}" >
    <!command
        gf.redirect(page="#{request.contextPath}/cluster/node/nodes.jsf");
    />
</sun:button>
