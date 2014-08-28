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

<!-- resourceNode/resourceEditTabs.inc -->

<sun:tabSet id="resEditTabs" rendered="$pageSession{showTargetSection}" immediate="$boolean{true}" selected="#{sessionScope.resEditTabs}">
    <!beforeCreate
        //To set the showTargetSection        
#include "/common/shared/targetsList.inc"
        setPageSessionAttribute(key="rest-api" value="true");
        if(#{pageSession.isAppScopedRes}){
            setPageSessionAttribute(key="showTargetSection" value="#{false}");
            setPageSessionAttribute(key="useString" value="#{false}");
            setPageSessionAttribute(key="useCheckBox" value="#{false}");
            setPageSessionAttribute(key="has2Objects" value="#{false}");
            setPageSessionAttribute(key="showDefaultButton" value="#{false}" );
        }
        if(!#{pageSession.isAppScopedRes}){
            if(#{pageSession.showTargetSection}) {
                setPageSessionAttribute(key="useString" value="#{true}");
                setPageSessionAttribute(key="useCheckBox" value="#{false}");
                setPageSessionAttribute(key="has2Objects" value="#{false}");
                setPageSessionAttribute(key="showDefaultButton" value="#{true}" );
            }
            if(!#{pageSession.showTargetSection}) {
                setPageSessionAttribute(key="useString" value="#{false}");
                setPageSessionAttribute(key="useCheckBox" value="#{true}");
                setPageSessionAttribute(key="parentUrl2", value="#{sessionScope.REST_URL}/servers/server/server");
                setPageSessionAttribute(key="childType2", value="resource-ref");
                if ("#{pageSession.resChildType} = connector-resource") {
                    setPageSessionAttribute(key="resourceRefUrl", value="#{pageSession.parentUrl2}/#{pageSession.childType2}/#{pageSession.encodedName}");
                    gf.checkIfEndPointExist(endpoint="#{pageSession.resourceRefUrl}" exists="#{requestScope.hasAssociatedTarget}");
                    if (#{hasAssociatedTarget}) {
                        gf.getEntityAttrs(endpoint="#{pageSession.resourceRefUrl}",  valueMap="#{pageSession.resourceRef}");
                        // if the resource itself is set to disabled, the real status should be disabled.
                        if( "#{pageSession.valueMap.enabled} = false"){
                            mapPut(map="#{pageSession.resourceRef}", key="enabled", value="#{pageSession.valueMap.enabled}");
                        }
                    }
                    if (!#{hasAssociatedTarget}) {
                        createMap(result="#{pageSession.resourceRef}");
                        gf.getEntityAttrs(endpoint="#{pageSession.parentUrl}/#{pageSession.resChildType}/#{pageSession.encodedName}",  valueMap="#{requestScope.resValueMap}");
                        mapPut(map="#{pageSession.resourceRef}", key="enabled", value="#{resValueMap.enabled}");
                    }
                }
                if ("!(#{pageSession.resChildType} = connector-resource)") {
                    setPageSessionAttribute(key="has2Objects" value="#{true}");
                    setPageSessionAttribute(key="showDefaultButton" value="#{true}" );
                    setPageSessionAttribute(key="selfUrl2", value="#{pageSession.parentUrl2}/#{pageSession.childType2}/#{pageSession.encodedName}");
                    gf.checkIfEndPointExist(endpoint="#{pageSession.selfUrl2}" exists="#{requestScope.hasAssociatedTarget}");
                    if (#{hasAssociatedTarget}) {
                        gf.getEntityAttrs(endpoint="#{pageSession.selfUrl2}", valueMap="#{pageSession.valueMap2}");
                        // if the resource itself is set to disabled, the real status should show disabled.
                        if( "#{pageSession.valueMap.enabled} = false"){
                            mapPut(map="#{pageSession.valueMap2}", key="enabled", value="#{pageSession.valueMap.enabled}");
                        }
                    }
                    if (!#{hasAssociatedTarget}) {
                        createMap(result="#{pageSession.valueMap2}");
                        mapPut(map="#{pageSession.valueMap2}", key="enabled", value="#{pageSession.valueMap.enabled}");
                    }
                    if(#{reload}) {
                        mapPut(map="#{pageSession.valueMap2}", key="enabled", value="#{status}");
                    }
                }
            }
        }
    />
    <sun:tab id="general" immediate="$boolean{true}" text="$resource{i18n.common.General}" >
        <!command
            setSessionAttribute(key="resEditTabs" value="general");
            gf.redirect(page="#{generalPage}");
        />
    </sun:tab>
    <sun:tab id="targetTab" immediate="$boolean{true}" text="$resource{i18n.common.TargetTab}" >
        <!command
            setSessionAttribute(key="resEditTabs" value="targetTab");
            gf.redirect(page="#{targetPage}&generalPage=#{generalPage}");
        />
    </sun:tab>
</sun:tabSet>
