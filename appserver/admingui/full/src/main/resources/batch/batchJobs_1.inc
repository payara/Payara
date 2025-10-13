<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://github.com/payara/Payara/blob/main/LICENSE.txt
    See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.

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

<!initPage
    setResourceBundle(key="i18nf" bundle="org.glassfish.full.admingui.Strings");
    setResourceBundle(key="i18nc" bundle="org.glassfish.common.admingui.Strings");
    setResourceBundle(key="i18ncs" bundle="org.glassfish.cluster.admingui.Strings")
	setResourceBundle(key="help_full" bundle="org.glassfish.full.admingui.Helplinks");
/>
<!composition template="/templates/default.layout"  guiTitle="$resource{i18nf.batch.jobListingTitle}"  >

<!define name="content">
    <event>
    <!beforeCreate
#include "/full/batch/batchRequestParam.inc"
        setSessionAttribute(key="#{pageSession.tabSetName}" value="batchJobs");
        createMap(result="#{requestScope.attrsMap}");
        py.getPageNumber(offset="#{pageSession.offsetValue}" result="#{pageSession.pageNumber}");
        mapPut(map="#{requestScope.attrsMap}"  key="target" value="#{pageSession.encodedTarget}");
        mapPut(map="#{requestScope.attrsMap}"  key="output" value="executionid,jobname,batchstatus,exitstatus,instanceid,starttime,endtime");
        mapPut(map="#{requestScope.attrsMap}"  key="offset" value="#{pageSession.offsetValue}");
        mapPut(map="#{requestScope.attrsMap}"  key="limit" value="$int{20}");
        gf.restRequest(endpoint="#{sessionScope.REST_URL}/list-batch-jobs" attrs="#{requestScope.attrsMap}" method="GET" result="#{requestScope.resp}");
        setAttribute(key="listOfRows", value="#{requestScope.resp.data.extraProperties.listBatchJobs}");
        setPageSessionAttribute(key="size" value="#{requestScope.listOfRows.size()}");
        setPageSessionAttribute(key="valueMap", value="#{requestScope.resp.data.extraProperties.listJobsCount}");
        py.getPageCount(jobCount="#{pageSession.valueMap['allJobsCount']}" result="#{pageSession.pageCount}");
        setPageSessionAttribute(key="showJobExecutions" value="#{true}");
        setPageSessionAttribute(key="displayNoJobsMessage" value="#{false}");

        if (#{pageSession.size}<$int{1}){
            setPageSessionAttribute(key="showJobExecutions" value="#{false}");
            setPageSessionAttribute(key="displayNoJobsMessage" value="#{true}");
        }

        if (#{pageSession.pageNumber}<$int{1}){
            setPageSessionAttribute(key="pageNumber" value="$int{1}");
        }
 
        if(#{pageSession.pageNumber}=#{pageSession.pageCount}){
           setPageSessionAttribute(key="disableNextButton" value="#{true}");
        }
        
        if(#{pageSession.pageNumber}=$int{1}){
           setPageSessionAttribute(key="disablePreviousButton" value="#{true}");
        } 

        setPageSessionAttribute(key="tableTitle" value="$resource{i18nf.batch.batchJobsTableTitle}");
        setPageSessionAttribute(key="editLink" value="#{request.contextPath}/full/batch/batchJobExecution.jsf?target=#{pageSession.target}&isCluster=#{pageSession.isCluster}&tabSetName=#{pageSession.encodedTabSetName}");
    />
    </event>
#include"/batch/assets/js/batch.js"
        <sun:form id="propertyForm">
