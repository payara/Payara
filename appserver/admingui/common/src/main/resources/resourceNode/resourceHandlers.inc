<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.

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

<!-- resourceNode/resourceHandlers.inc -->

<!-- Input : tmpRows     - required
     Output: listOfRows
-->
<handler id="gfr.getResourcesTableData">
    gf.listClusters(clusters="#{pageSession.clusterList}");
    gf.listInstances(optionKeys={"standaloneonly"} optionValues={"true"}, instances="#{pageSession.standaloneList}" );
    listAdd(list="#{pageSession.standaloneList}", value="server" index="0");
    setPageSessionAttribute(key="targetList" value={});
    gf.listCombine(list="#{pageSession.targetList}",  list2="#{pageSession.clusterList}");
    gf.listCombine(list="#{pageSession.targetList}",  list2="#{pageSession.standaloneList}");

    setAttribute(key="one" value="$int{1}");
    setAttribute(key="zero" value="$int{0}");
    setAttribute(key="targetSize" value="#{pageSession.targetList.size()}");

    setPageSessionAttribute(key="listOfRows" value={});
    setAttribute(key="emptyStr" value="");

    foreach (var="oneRow" list="#{pageSession.tmpRows}") {
        setAttribute(key="resourceTargets" value={});
        setAttribute(key="enabledTargetsCount" value="$int{0}");
        setAttribute(key="enableURL" value="");
        createMap(result=>$attribute{targetsMap});
        setAttribute(key="targetUrls" value={});

        foreach (var="inst" list="#{pageSession.standaloneList}") {
            //getResourceRefTargets
            setAttribute(key="instUrl" value="#{sessionScope.REST_URL}/servers/server/#{inst}");
            gf.getChildrenNamesList(endpoint="#{instUrl}/resource-ref"
                                    result=>$attribute{serverRefs});
            gf.containedIn(list="#{serverRefs}" testStr="#{oneRow['name']}" contain=>$attribute{isTarget});
            if (#{isTarget}) {
                listAdd(list="#{resourceTargets}" value="#{oneRow['name']}");
                mapPut(map="#{targetsMap}" key="#{instUrl}" value="#{inst}");
                listAdd(list="#{targetUrls}" value="#{instUrl}");
                gf.getEntityAttrs(endpoint="#{instUrl}/resource-ref/#{oneRow['encodedName']}" valueMap="#{requestScope.refAttrs}");
                if ("#{refAttrs.enabled} = true") {
                    inc(number="#{enabledTargetsCount}" value="#{enabledTargetsCount}");
                }
            }
        }
        foreach (var="cluster" list="#{pageSession.clusterList}") {
            setAttribute(key="clusterUrl" value="#{sessionScope.REST_URL}/clusters/cluster/#{cluster}");
            gf.getChildrenNamesList(endpoint="#{clusterUrl}/resource-ref"
                                    result=>$attribute{clusterRefs});
            gf.containedIn(list="#{clusterRefs}" testStr="#{oneRow['name']}" contain=>$attribute{isTarget});
            if (#{isTarget}) {
                listAdd(list="#{resourceTargets}" value="#{oneRow['name']}");
                mapPut(map="#{targetsMap}" key="#{clusterUrl}" value="#{cluster}");
                listAdd(list="#{targetUrls}" value="#{clusterUrl}");
                gf.getEntityAttrs(endpoint="#{clusterUrl}/resource-ref/#{oneRow['encodedName']}" valueMap="#{requestScope.refAttrs}");
                if ("#{refAttrs.enabled} = true") {
                    inc(number="#{enabledTargetsCount}" value="#{enabledTargetsCount}");
                }
            }
        }
        setAttribute(key="resourceTargetSize" value="#{resourceTargets.size()}");
        //get Target Enable info
        if (#{requestScope.targetSize}=#{requestScope.one}) {
            setPageSessionAttribute(key="onlyDASExist" value="#{true}");
            if (#{resourceTargetSize} = #{zero}) {
               setAttribute(key="enabledVal" value="#{oneRow['enabled']}");
            }
            setAttribute(key="enabledVal" value="#{oneRow['enabled']}");
            //if the resource itself is disabled, only happen when user uses CLI to set the resource to false, instead of settng resource-ref to false,
            //it will be disabled, we don't even need to check the resource-ref status
            if ("#{requestScope.enabledVal} = true"){
                if (#{resourceTargetSize} > #{zero}) {
                    gf.getEntityAttrs(endpoint="#{sessionScope.REST_URL}/servers/server/server/resource-ref/#{oneRow['encodedName']}" valueMap="#{requestScope.refAttrs}");
                    setAttribute(key="enabledVal" value="#{refAttrs.enabled}");
                }
            }
            if ("#{enabledVal}=true") {
                setAttribute(key="enableURL" value="/resource/images/enabled.png");
            }
            if ("#{enabledVal}=false") {
                setAttribute(key="enableURL" value="/resource/images/disabled.png");
            }
        }
        if (#{requestScope.targetSize} > #{requestScope.one}){
            //"resourceTargetSize=0" means that there is no target associated with the resource.
            setPageSessionAttribute(key="onlyDASExist" value="#{false}");
            if (#{resourceTargetSize} = #{zero}) {
                setAttribute(key="enableURL" value="$resource{i18n.deploy.noTarget}");
            }
            if ("#{enableURL} = #{emptyStr}") {
                setAttribute(key="enableURL" value="$resource{i18n.deploy.someEnabled,$attribute{enabledTargetsCount},$attribute{resourceTargetSize}}");
            }
        }
        mapPut(map="#{oneRow}" key="targetUrls" value="#{targetUrls}");
        mapPut(map="#{oneRow}" key="targetsMap" value="#{targetsMap}");
        mapPut(map="#{oneRow}" key="enableURL" value="#{enableURL}");

        listAdd(list="#{pageSession.listOfRows}" value="#{oneRow}");
    }
</handler>

<!-- Input : selectedRows     - required
-->
<handler id="gfr.enableSelectedResources">
    // enable the resources for all targets
    foreach (var="row" list="#{pageSession.selectedRows}") {
        gf.createAttributeMap(keys={"enabled"} values={"true"} map="#{requestScope.attrMap}");
        urlencode(value="#{row['name']}" result="#{requestScope.encodedResName}");
        setAttribute(key="targetUrlsList" value="#{row['targetUrls']}");
        foreach (var="targetUrl" list="#{requestScope.targetUrlsList}") {
            gf.restRequest(
                endpoint="#{requestScope.targetUrl}/resource-ref/#{requestScope.encodedResName}"
                attrs="#{requestScope.attrMap}"
                method="POST"
                result="#{requestScope.foo}");
        }
    }
</handler>

<!-- Input : selectedRows     - required
-->
<handler id="gfr.disableSelectedResources">
    // disable the resource for all targets
    foreach (var="row" list="#{pageSession.selectedRows}") {
        gf.createAttributeMap(keys={"enabled"} values={"false"} map="#{requestScope.attrMap}");
        urlencode(value="#{row['name']}" result="#{requestScope.encodedResName}");
        setAttribute(key="targetUrlsList" value="#{row['targetUrls']}");
        foreach (var="targetUrl" list="#{requestScope.targetUrlsList}") {
            gf.restRequest(
                endpoint="#{requestScope.targetUrl}/resource-ref/#{requestScope.encodedResName}"
                attrs="#{requestScope.attrMap}"
                method="POST"
                result="#{requestScope.foo}");
        }
    }
</handler>

<!-- Input : selectedRows     - required
-->
<handler id="gfr.deleteSelectedResources">
    foreach (var="row" list="#{pageSession.selectedRows}") {
        urlencode(value="#{row['name']}" result="#{requestScope.encodedResName}");
        gf.onlyDASExist(onlyDAS="#{pageSession.onlyDASExist}");
        //if(!#{pageSession.onlyDASExist}){
            setAttribute(key="targetUrlsList" value="#{row['targetUrls']}");
            foreach (var="targetUrl" list="#{requestScope.targetUrlsList}") {
                createMap(result="#{requestScope.attrMap}");
                mapGet(Map="#{row['targetsMap']}" Key="#{requestScope.targetUrl}" Value="#{requestScope.targetVal}");
                mapPut(map="#{requestScope.attrMap}" key="target" value="#{targetVal}");
                gf.restRequest(
                    endpoint="#{requestScope.targetUrl}/resource-ref/#{requestScope.encodedResName}"
                    attrs="#{requestScope.attrMap}"
                    method="DELETE"
                    result="#{requestScope.foo}");
            }
        //}
        createMap(result="#{requestScope.attrMap}");
        if ("#{pageSession.childType}=admin-object-resource") {
            mapPut(map="#{requestScope.attrMap}" key="target" value="server-config");
        }
        if ("!(#{pageSession.childType}=admin-object-resource)") {
            mapPut(map="#{requestScope.attrMap}" key="target" value="domain");
        }
        gf.restRequest(
            endpoint="#{pageSession.parentUrl}/#{pageSession.childType}/#{requestScope.encodedResName}"
            attrs="#{requestScope.attrMap}"
            method="DELETE"
            result="#{requestScope.foo}");
    }
</handler>


<handler id="gfr.getJmsDestResources" >
        createMap(result="#{requestScope.resMap}");
        mapPut(map="#{requestScope.resMap}"  key="target" value="domain");
        mapPut(map="#{requestScope.resMap}"  key="resType" value="javax.jms.Queue");
        gf.restRequest(endpoint="#{sessionScope.REST_URL}/resources/list-jms-resources" attrs="#{requestScope.resMap}" method="get" result="#{requestScope.queue}");
        mapPut(map="#{requestScope.resMap}"  key="resType" value="javax.jms.Topic");
        gf.restRequest(endpoint="#{sessionScope.REST_URL}/resources/list-jms-resources" attrs="#{requestScope.resMap}" method="get" result="#{requestScope.topic}");
        gf.listCombine(list="#{requestScope.queue.data.extraProperties.jmsResources}" list2="#{requestScope.topic.data.extraProperties.jmsResources}" result="#{requestScope.children}");
</handler>   

<handler id="gfr.getJmsConnectionFactories" >
        createMap(result="#{requestScope.rMap}");
        mapPut(map="#{requestScope.rMap}"  key="target" value="domain");
        mapPut(map="#{requestScope.rMap}"  key="resType" value="javax.jms.ConnectionFactory");
        gf.restRequest(endpoint="#{sessionScope.REST_URL}/resources/list-jms-resources" attrs="#{requestScope.rMap}" method="get" result="#{requestScope.c1}");

        mapPut(map="#{requestScope.rMap}"  key="resType" value="javax.jms.TopicConnectionFactory");
        gf.restRequest(endpoint="#{sessionScope.REST_URL}/resources/list-jms-resources" attrs="#{requestScope.rMap}" method="get" result="#{requestScope.c2}");

        mapPut(map="#{requestScope.rMap}"  key="resType" value="javax.jms.QueueConnectionFactory");
        gf.restRequest(endpoint="#{sessionScope.REST_URL}/resources/list-jms-resources" attrs="#{requestScope.rMap}" method="get" result="#{requestScope.c3}");

        gf.listCombine(list="#{requestScope.c1.data.extraProperties.jmsResources}" list2="#{requestScope.c2.data.extraProperties.jmsResources}" result="#{requestScope.tmplist}");
        gf.listCombine(list="#{requestScope.tmplist}" list2="#{requestScope.c3.data.extraProperties.jmsResources}" result="#{requestScope.children}");
 
</handler>   
