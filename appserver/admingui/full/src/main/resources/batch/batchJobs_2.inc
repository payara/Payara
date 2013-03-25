<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.

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

#include "/common/shared/alertMsg_1.inc"
             <sun:title id="propertyContentPage" title="$resource{i18nf.batch.jobListingTitle}" helpText="$resource{i18nf.batch.jobListingTitleHelp}" />
#include "/common/shared/nameSection.inc"
        <sun:table id="configs" title="#{tableTitle}"
            deselectMultipleButton="$boolean{false}"
            selectMultipleButton="$boolean{false}"
            paginationControls="$boolean{true}"
            paginateButton="$boolean{true}">
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

<sun:hidden id="helpKey" value="$resource{help_full.batchJobs}" />
            </sun:form>
</define>
</composition>
