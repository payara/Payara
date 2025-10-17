<!--

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://github.com/payara/Payara/blob/main/LICENSE.txt
 See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 -->
<sun:tabSet id="hazelcastTabs" immediate="true" selected="#{sessionScope.hazelcastTabs}" >            
    <sun:tab id="hazelcastMembers" immediate="$boolean{true}" text="$resource{i18nhc.hazelcast.membersTab}" toolTip="$resource{i18nhc.hazelcast.membersTabTip}" >
        <!command
        setSessionAttribute(key="hazelcastTabs" value="hazelcastMembers");
        gf.redirect(page="#{request.contextPath}/payaraExtras/hazelcast/hazelcastMembersDomain.jsf?tabSetName=hazelcastTabs");
        />
    </sun:tab>

    <sun:tab id="hazelcastConfig" immediate="$boolean{true}" text="$resource{i18nhc.hazelcast.configuration}" toolTip="$resource{i18nhc.hazelcast.configurationTip}" >
        <!command
        setSessionAttribute(key="hazelcastTabs" value="hazelcastConfig");
        gf.redirect(page="#{request.contextPath}/payaraExtras/hazelcast/hazelcast.jsf?tabSetName=hazelcastTabs");
        />
    </sun:tab>
</sun:tabSet>
