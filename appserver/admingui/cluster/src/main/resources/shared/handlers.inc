<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.

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

<!--
     This handler check if there is a cluster or standalone instance, node or config  with the name already existed.
     Input : #{requestScope.nameToCheck}     - required
     Output: #{pageSession.nameAlreadyExists}
-->
<handler id="gfj.checkNameExist">
    urlencode(value="#{requestScope.nameToCheck}" result="#{requestScope.encodedNameToCheck}");
    gf.checkIfEndPointExist(endpoint="#{sessionScope.REST_URL}/clusters/cluster/#{requestScope.encodedNameToCheck}", exists="#{requestScope.exx}");
    if (#{requestScope.exx}){
        setAttribute(key="nameAlreadyExists" value="#{true}");
    }
    if ("!#{requestScope.exx}"){
        gf.checkIfEndPointExist(endpoint="#{sessionScope.REST_URL}/servers/server/#{requestScope.encodedNameToCheck}", exists="#{requestScope.exx}");
    }
    if (#{requestScope.exx}){
        setAttribute(key="nameAlreadyExists" value="#{true}");
    }
    if ("!#{requestScope.exx}"){
        gf.checkIfEndPointExist(endpoint="#{sessionScope.REST_URL}/configs/config/#{requestScope.encodedNameToCheck}", exists="#{requestScope.exx}");
    }
    if (#{requestScope.exx}){
        setAttribute(key="nameAlreadyExists" value="#{true}");
    }
    if ("!#{requestScope.exx}"){
        gf.checkIfEndPointExist(endpoint="#{sessionScope.REST_URL}/nodes/node/#{requestScope.encodedNameToCheck}", exists="#{requestScope.nameAlreadyExists}");
    }

    if (#{requestScope.nameAlreadyExists}){
        setAttribute(key="errormsg" value="$resource{i18ncs.msg.error.nameExists, #{requestScope.encodedNameToCheck}}")
        gf.handleError(detail="#{requestScope.errormsg}");
    }

</handler>



<!--
 This handler calls list-instances and return a map that has the instance name as the key and the status as value.
 Input : #{requestScope.listInstanceAttrMap}     - required
 Output: #{pageSession.instanceStatusMap}
-->
<handler id="gfr.getInstancesStatus">
    gf.restRequest( endpoint="#{sessionScope.REST_URL}/list-instances"
        attrs="#{requestScope.listInstanceAttrMap}"
        method="GET"
        result="#{pageSession.props}");
    setAttribute(key="insList" value="#{pageSession.props.data.extraProperties.instanceList}" );
    //println("======= insList = #{requestScope.insList}");
    createMap(result="#{pageSession.instanceStatusMap}");
    // if( "(#{requestScope.insList} = #{null})" ){
        //println("============= no instances found");
    //}
    if( "!(#{requestScope.insList} = #{null})" ){
        foreach(var="oneIns" list="#{requestScope.insList}"){
            mapPut(map="#{pageSession.instanceStatusMap}"  key="#{requestScope.oneIns.name}" value="#{requestScope.oneIns.status}");
        }
    }
</handler>


<!--
 This handler gets the running status of an instance.  The String returned is the running status in word followed by icon.
 Input : #{pageSession.instanceName}     - required
 Output: #{pageSession.statusString}
-->
<handler id="gfr.getStatusString" >
    createMap(result="#{requestScope.listInstanceAttrMap}");
    mapPut(map="#{requestScope.listInstanceAttrMap}" key="whichtarget" value="#{pageSession.instanceName}")
    gfr.getInstancesStatus();
    //println("-------- instanceStatusMap = #{pageSession.instanceStatusMap}");
    mapGet(Map="#{pageSession.instanceStatusMap}" Key="#{pageSession.instanceName}" Value="#{pageSession.status}")
    setPageSessionAttribute(key="statusString" value="$resource{i18nc.status.$pageSession{status}} $resource{i18nc.status.image.$pageSession{status}} ");
    setPageSessionAttribute(key="statusImage" value="$resource{i18nc.status.image.$pageSession{status}} ");

</handler>


    <!--
    This handler gets the JDPA debug information.
    Input: #{pageSession.instanceName}
    Output" #{pageSession.instanceDebugInfo}
    -->
    <handler id="gfr.getInstanceDebugInfo" >
        setAttribute(key="saveType", value="#{requestScope.alertType}");
        setAttribute(key="saveSummary", value="#{requestScope.alertSummary}");
        setAttribute(key="saveDetail", value="#{requestScope.alertDetail}");
        createMap(result="#{requestScope._debugAttrMap}")
        mapPut(map="#{requestScope._debugAttrMap}" key="target" value="#{pageSession.instanceName}")
        gf.restRequest(endpoint="#{sessionScope.REST_URL}/get-runtime-info" attrs="#{requestScope._debugAttrMap}"
            method="GET" result="#{requestScope.debugResult}");
        setAttribute(key="debugF" value="#{requestScope.debugResult.data.subReports.get(0).properties.debug}");
        setPageSessionAttribute(key="instanceDebugInfo" value="$resource{i18n.inst.notEnabled}");
        if ("#{requestScope.debugF}=true"){
            setAttribute(key="debugPort" value="#{requestScope.debugResult.data.subReports.get(0).properties.debugPort}");
            setPageSessionAttribute(key="instanceDebugInfo" value="$resource{i18n.inst.debugEnabled} ${debugPort}");
        }
        setAttribute(key="alertType", value="#{requestScope.saveType}");
        setAttribute(key="alertSummary", value="#{requestScope.saveSummary}");
        setAttribute(key="alertDetail", value="#{requestScope.saveDetail}");
    </handler>