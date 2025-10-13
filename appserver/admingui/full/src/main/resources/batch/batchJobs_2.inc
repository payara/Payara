<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://github.com/payara/Payara/blob/master/LICENSE.txt
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

#include "/common/shared/alertMsg_1.inc"
             <sun:title id="propertyContentPage" title="$resource{i18nf.batch.jobListingTitle}" helpText="$resource{i18nf.batch.jobListingTitleHelp}" />
#include "/common/shared/nameSection.inc"
        <sun:table id="configs" title="#{tableTitle}"
            visible="#{pageSession.showJobExecutions}"
            deselectMultipleButton="$boolean{false}"
            selectMultipleButton="$boolean{false}"
            paginationControls="$boolean{false}"
            paginateButton="$boolean{false}">
            <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{tableId});
            />

   <sun:tableRowGroup id="rowGroup1"  data={"$attribute{listOfRows}"} sourceVar="td">
     <!afterCreate
         getClientId(component="$this{component}" clientId=>$page{tableRowGroupId});
        />

    <sun:tableColumn headerText="$resource{i18nf.batch.executionIdCol}" sort="executionId" rowHeader="$boolean{true}" id="col1">
        <sun:hyperlink id="link" text="#{td.value.executionId}" url="#{pageSession.editLink}&executionId=#{td.value.executionId}" />
    </sun:tableColumn>

    <sun:tableColumn headerText="$resource{i18nf.batch.jobNameCol}"  sort="jobName"  rowHeader="$boolean{false}" id="jobname">
        <staticText id="max" value="#{td.value.jobName}" />
    </sun:tableColumn>

    <sun:tableColumn headerText="$resource{i18nf.batch.batchStatusCol}"  sort="batchStatus"  rowHeader="$boolean{false}" id="batchStatus">
        <staticText id="max" value="#{td.value.batchStatus}" />
    </sun:tableColumn>

    <sun:tableColumn headerText="$resource{i18nf.batch.exitStatusCol}"  sort="exitStatus"  rowHeader="$boolean{false}" id="exitStatus">
        <staticText id="max" value="#{td.value.exitStatus}" />
    </sun:tableColumn>

    <sun:tableColumn headerText="$resource{i18nf.batch.instanceIdCol}"  sort="instanceId"  rowHeader="$boolean{false}" id="instanceId">
        <staticText id="max" value="#{td.value.instanceId}" />
    </sun:tableColumn>

    <sun:tableColumn headerText="$resource{i18nf.batch.startTimeCol}"  sort="startTime"  rowHeader="$boolean{false}" id="startTime">
        <staticText id="st" value="#{requestScope.st}" >
        <!beforeEncode
            gf.convertDateTime(dateTime="#{td.value.startTime}"  result="#{requestScope.st}");
        />
        </staticText>
    </sun:tableColumn>

    <sun:tableColumn headerText="$resource{i18nf.batch.endTimeCol}"  sort="endTime"  rowHeader="$boolean{false}" id="endTime">
        <staticText id="endTime" value="#{requestScope.endTime}" >
            <!beforeEncode
                gf.convertDateTime(dateTime="#{td.value.endTime}"  result="#{requestScope.endTime}");
            />
        </staticText>
    </sun:tableColumn>
  </sun:tableRowGroup>
</sun:table>

    <sun:panelGroup id="paginationButtons" rendered="#{pageSession.showJobExecutions}" >
         <sun:button id="previousJobsButton"  
                         disabled="#{pageSession.disablePreviousButton}"
                         text="$resource{i18nf.batch.pagination.perviousButton}" 
                         onClick="submitAndDisable(this, '$resource{i18n.button.Processing}'); return false;" >
                <!command 
                    py.subtractFromOffSetValue(offset="#{pageSession.offsetValue}" result="offsetNewValue");
                    if(#{pageSession.isCluster}=true){
                        gf.redirect(page="#{request.contextPath}/full/batch/batchJobsCluster.jsf?target=#{pageSession.encodedTarget}&offsetValue=#{requestScope.offsetNewValue}&isCluster=true&tabSetName=#{pageSession.encodedTabSetName}");
                    }

                    if(#{pageSession.isCluster}=false){
                        if(#{pageSession.target}=server){
                            gf.redirect(page="#{request.contextPath}/full/batch/batchJobsServer.jsf?target=server&offsetValue=#{requestScope.offsetNewValue}&isCluster=false&tabSetName=#{pageSession.encodedTabSetName}");
                        }
                        if("!(#{pageSession.target}=server)"){
                             gf.redirect(page="#{request.contextPath}/full/batch/batchJobsStandalone.jsf?target=#{pageSession.encodedTarget}&offsetValue=#{requestScope.offsetNewValue}&isCluster=false&tabSetName=#{pageSession.encodedTabSetName}");
                        }
                    }                  
                />
         </sun:button>
     <sun:button id="nextJobsButton"  
                     disabled="#{pageSession.disableNextButton}"
                     text="$resource{i18nf.batch.pagination.nextButton}"
                     onClick="submitAndDisable(this, '$resource{i18n.button.Processing}'); return false;" >
                <!command        
                    py.addToOffSetValue(offset="#{pageSession.offsetValue}" result="offsetNewValue");
                    if(#{pageSession.isCluster}=true){
                        gf.redirect(page="#{request.contextPath}/full/batch/batchJobsCluster.jsf?target=#{pageSession.encodedTarget}&offsetValue=#{requestScope.offsetNewValue}&isCluster=true&tabSetName=#{pageSession.encodedTabSetName}");
                    }

                    if(#{pageSession.isCluster}=false){
                        if(#{pageSession.target}=server){
                            gf.redirect(page="#{request.contextPath}/full/batch/batchJobsServer.jsf?target=server&offsetValue=#{requestScope.offsetNewValue}&isCluster=false&tabSetName=#{pageSession.encodedTabSetName}");
                        }
                        if("!(#{pageSession.target}=server)"){
                             gf.redirect(page="#{request.contextPath}/full/batch/batchJobsStandalone.jsf?target=#{pageSession.encodedTarget}&offsetValue=#{requestScope.offsetNewValue}&isCluster=false&tabSetName=#{pageSession.encodedTabSetName}");
                        }
                    }
                />
        </sun:button>
            <sun:property id="pageNumberProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="Page">
            <sun:textField id="pageNumber" columns="$int{4}" maxLength="5" text="#{pageSession.pageNumber}" /> 
        </sun:property>

     <sun:button id="goButton"  
                         text="$resource{i18nf.batch.pagination.goButton}" 
                         onClick="submitAndDisable(this, '$resource{i18n.button.Processing}'); return false;" >
                <!command 
                     if(#{pageSession.pageNumber}>#{pageSession.pageCount}){
                               setPageSessionAttribute(key="pageNumber" value="#{pageSession.pageCount}");
                      }
                    py.getSpecifiedPageNumber(pageNumber="#{pageSession.pageNumber}" result="offsetNewValue");
                    if(#{pageSession.isCluster}=true){
                        gf.redirect(page="#{request.contextPath}/full/batch/batchJobsCluster.jsf?target=#{pageSession.encodedTarget}&offsetValue=#{requestScope.offsetNewValue}&isCluster=true&tabSetName=#{pageSession.encodedTabSetName}");
                    }

                    if(#{pageSession.isCluster}=false){
                        if(#{pageSession.target}=server){
                            gf.redirect(page="#{request.contextPath}/full/batch/batchJobsServer.jsf?target=server&offsetValue=#{requestScope.offsetNewValue}&isCluster=false&tabSetName=#{pageSession.encodedTabSetName}");
                        }
                        if("!(#{pageSession.target}=server)"){
                             gf.redirect(page="#{request.contextPath}/full/batch/batchJobsStandalone.jsf?target=#{pageSession.encodedTarget}&offsetValue=#{requestScope.offsetNewValue}&isCluster=false&tabSetName=#{pageSession.encodedTabSetName}");
                        }
                    }      
                />
     </sun:button>
      <sun:property id="totalNumberOfPages" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="of #{pageSession.pageCount}">
      </sun:property>         
    </sun:panelGroup>

    <sun:propertySheet id="infoSheet">
        <sun:propertySheetSection id="infoSheetSection">
        <sun:property id="noJobsProp" rendered="#{pageSession.displayNoJobsMessage}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" >
            <sun:staticText id="noJobs"  text="$resource{i18nf.batch.pagination.noJobsToDisplayMessage}" />
        </sun:property>
        </sun:propertySheetSection>
    </sun:propertySheet>
    <sun:hidden id="helpKey" value="$resource{help_full.batchJobs}" />
  </sun:form>
 </define>
</composition>
