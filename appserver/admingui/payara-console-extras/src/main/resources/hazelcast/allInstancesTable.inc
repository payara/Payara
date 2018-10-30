<!--
  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

  Copyright (c) [2016-2018] Payara Foundation and/or its affiliates. 
  All rights reserved.

  The contents of this file are subject to the terms of either the GNU
  General Public License Version 2 only ("GPL") or the Common Development
  and Distribution License("CDDL") (collectively, the "License").  You
  may not use this file except in compliance with the License.  You can
  obtain a copy of the License at
  https://github.com/payara/Payara/blob/master/LICENSE.txt
  See the License for the specific
  language governing permissions and limitations under the License.

  When distributing the software, include this License Header Notice in each
  file and include the License file at glassfish/legal/LICENSE.txt.

  GPL Classpath Exception:
  The Payara Foundation designates this particular file as subject to the 
  "Classpath" exception as provided by the Payara Foundation in the GPL 
  Version 2 section of the License file that accompanied this code.

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
<sun:table id="allInstancesTable" title="$resource{i18nhc.hazelcast.allInstancesTableTitle}" style="padding: 10pt"
           rendered="#{pageSession.hazelcastConfiguration['enabled'] == true}">
<sun:tableRowGroup id="allInstancesRowGroup" data={"$attribute{allInstances}"} sourceVar="td" 
                   emptyDataMsg="$resource{i18nhc.hazelcast.allInstancesTable.emptyDataMsg}">
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.allInstancesTable.instanceName}" sort="instanceName" rowHeader="$boolean{true}" id="col1">
        <staticText id="instanceName" value="#{td.value.instanceName}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.allInstancesTable.instanceType}" sort="instanceType" rowHeader="$boolean{false}" id="col2">
        <staticText id="instanceType" value="#{td.value.instanceType}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.allInstancesTable.instanceGroup}" sort="instanceGroup" rowHeader="$boolean{false}" id="col3">
        <staticText id="instanceGroup" value="#{td.value.instanceGroup}" />
    </sun:tableColumn>
   <sun:tableColumn headerText="$resource{i18nhc.hazelcast.allInstancesTable.hostName}" sort="hostName" rowHeader="$boolean{true}" id="col4">
        <staticText id="hostName" value="#{td.value.hostName}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.allInstancesTable.httpPorts}" sort="httpPorts" rowHeader="$boolean{false}" id="col5">
        <staticText id="httpPorts" value="#{td.value.httpPorts}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.allInstancesTable.httpsPorts}" sort="httpsPorts" rowHeader="$boolean{false}" id="col6">
        <staticText id="httpsPorts" value="#{td.value.httpsPorts}" />
    </sun:tableColumn>result
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.allInstancesTable.adminPort}" sort="adminPort" rowHeader="$boolean{false}" id="col7">
        <staticText id="adminPort" value="#{td.value.adminPort}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.allInstancesTable.hazelcastPort}" sort="hazelcastPort" rowHeader="$boolean{true}" id="col8">
        <staticText id="hazelcastPort" value="#{td.value.hazelcastPort}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.allInstancesTable.liteMember}" sort="liteMember" rowHeader="$boolean{false}" id="col9">
        <staticText id="liteMember" value="#{td.value.liteMember}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.allInstancesTable.applications}" sort="applications" rowHeader="$boolean{false}" id="col10">
        <staticText id="applications" value="#{td.value.applications}" />
    </sun:tableColumn>
</sun:tableRowGroup>
</sun:table>