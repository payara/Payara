<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.

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

<handler id="gf.configureJmsCluster">
    createMap(result="#{requestScope.jmsAttrs}");
    //println("***** #{pageSession}\n\n\n\n\n\n");
    mapPut(map="#{requestScope.jmsAttrs}" key="clustertype" value="$pageSession{clusterType}");
    mapPut(map="#{requestScope.jmsAttrs}" key="property" value="$pageSession{properties}");
    if ('$pageSession{clusterType}=conventional') {
        mapPut(map="#{requestScope.jmsAttrs}" key="configStoreType" value="$pageSession{configStoreType}");
        mapPut(map="#{requestScope.jmsAttrs}" key="messageStoreType" value="$pageSession{messageStoreType}");
    }

    // Save JMS integration type
    createMap(result="#{requestScope.jmsSvcAttrs}");

    mapPut(map="#{requestScope.jmsSvcAttrs}" key="type" value="#{empty pageSession.jmsType ? pageSession.jmsTypeOptionGrp : pageSession.jmsType}");
    gf.restRequest(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.config}/jms-service",
        method="POST", attrs="#{requestScope.jmsSvcAttrs}", result="#{pageSession.resultMap}");
    //println("\n\n\n\n\n**************1needsDB = #{needsDB}\n\n\n\n\n\n\n\n\n");
        
    setAttribute(key="needsDB", value="#{false}");
    
    //println("\n\n\n\n\n**************2needsDB = #{needsDB}\n\n\n\n\n\n\n\n\n");

    if ('($pageSession{clusterType}=enhanced)') {
        setAttribute(key="needsDB" value="#{true}");
    }
    //println("\n\n\n\n\n**************3needsDB = #{needsDB}\n\n\n\n\n\n\n\n\n");
    if ('($pageSession{messageStoreType}=jdbc)') {
        setAttribute(key="needsDB" value="#{true}");
    }
    //println("\n\n\n\n\n**************4needsDB = #{needsDB}\n\n\n\n\n\n\n\n\n");
    if ('($pageSession{configStoreType}=shareddb)') {
        setAttribute(key="needsDB" value="#{true}");
    }
    //println("\n\n\n\n\n**************5needsDB = #{needsDB}\n\n\n\n\n\n\n\n\n");
    if ('(#{needsDB})') {
        if (#{dbVendor}) {
            mapPut(map="#{requestScope.jmsAttrs}" key="dbVendor" value="$pageSession{dbVendor}");
        }
        if (#{dbUser}) {
            mapPut(map="#{requestScope.jmsAttrs}" key="dbUser" value="$pageSession{dbUser}");
        }
        if (#{dbUrl}) {
            mapPut(map="#{requestScope.jmsAttrs}" key="dbUrl" value="$pageSession{dbUrl}");
        }
        if ('$pageSession{authType}=password') {
            mapPut(map="#{requestScope.jmsAttrs}" key="jmsdbpassword" value="#{pageSession.pswdText}")
        }
        if ('$pageSession{authType}=alias') {
            gf.convertToAlias(in="#{pageSession.pswdAlias}" out="#{requestScope.tmpv}");
            mapPut(map="#{requestScope.jmsAttrs}" key="jmsdbpassword" value="#{requestScope.tmpv}")
        }
    }

    if ('($pageSession{jmsType}=LOCAL) || ($pageSession{jmsType}=EMBEDDED)') {  
        gf.restRequest(endpoint="#{sessionScope.REST_URL}/clusters/cluster/#{pageSession.name}/configure-jms-cluster",
            attrs="#{requestScope.jmsAttrs}",
            method="post",
            result="#{pageSession.resultMap}",
            throwException="#{false}");

        if ("#{pageSession.resultMap.data.exit_code}=FAILURE") {
            if (#{pageSession.deleteClusterOnFailure}) {
                prepareAlertMsg(summary="$resource{i18ncs.cluster.jms.ConfigureJmsClusterFailure}", 
                    detail="#{pageSession.resultMap.data.message}"
                    type="error");
                gf.restRequest(endpoint="#{sessionScope.REST_URL}/clusters/cluster/#{pageSession.name}", method="delete");
                gf.redirect(page="#{pageSession.selfPage}?alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
            }
            if (!#{pageSession.deleteClusterOnFailure}) {
                prepareAlertMsg(summary="#{pageSession.resultMap.data.message}", type="error");
                gf.redirect(page="#{pageSession.selfPage}?alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
            }
        }
    }
</handler>
