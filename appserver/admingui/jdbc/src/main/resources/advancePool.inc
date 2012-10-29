<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.

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

<!-- jdbc/advancePool.inc -->

<!-- PropertySheet .... -->
<sun:propertySheet id="propertySheet">			 
        <sun:propertySheetSection id="generalPropertySheet">
            <sun:property id="jndiProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.poolName}">
                <sun:staticText id="Jndi" text="#{pageSession.Name}"/>
            </sun:property>

        <sun:property id="p2"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.statementTimeoutInSeconds}" helpText="$resource{i18njdbc.jdbcPool.statementTimeoutInSecondsHelp}">
            <sun:textField id="va" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.statementTimeoutInSeconds']}" text="#{pageSession.valueMap['statementTimeoutInSeconds']}" >
            <!afterCreate
                 getClientId(component="$this{component}" clientId=>$page{timeoutId});
            />
            </sun:textField>
            <sun:staticText id="vasec" text="$resource{i18n.common.Seconds}"/>
        </sun:property>
        <sun:property id="cacheSizeProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.statementCacheSize}" helpText="$resource{i18njdbc.jdbcPool.statementCacheSizeHelp}">
            <sun:textField id="cachesize" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.statementCacheSize']}" text="#{pageSession.valueMap['statementCacheSize']}" >

            </sun:textField>
        </sun:property>
        <sun:property id="initSqlProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.initSql}" helpText="$resource{i18njdbc.jdbcPool.initSqlHelp}">
            <sun:textField id="initsql" columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.initSql']}" text="#{pageSession.valueMap['initSql']}" >

            </sun:textField>
        </sun:property>
        <sun:property id="sqlTLProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.sqlTL}"  helpText="$resource{i18njdbc.jdbcPool.sqlTLHelp}">
            <sun:textField id="sqlTL" columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.sqlTL']}" text="#{pageSession.valueMap['sqlTraceListeners']}" >

            </sun:textField>
        </sun:property>
         <sun:property id="p1"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.wrapJdbcObjects}" helpText="$resource{i18njdbc.jdbcPool.wrapJdbcObjectsHelp}">
            <sun:checkbox  selected="#{pageSession.valueMap['wrapJdbcObjects']}" label="$resource{i18n.common.Enabled}" selectedValue="true" />
       </sun:property>
       <sun:property id="poolingProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.pooling}" helpText="$resource{i18njdbc.jdbcPool.poolingHelp}">
            <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['pooling']}" selectedValue="true" />
       </sun:property>
</sun:propertySheetSection>
<sun:propertySheetSection id="connectionPropertySheet" label="$resource{i18njdbc.jdbcPool.connectionSettings}">

    <sun:property id="p1"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.validateAtmostOnceInSeconds}" helpText="$resource{i18njdbc.jdbcPool.validateAtmostOnceInSecondsHelp}">
        <sun:textField id="va" styleClass="integer" columns="$int{30}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.validateAtmostOnceInSeconds']}" text="#{pageSession.valueMap['validateAtmostOncePeriodInSeconds']}" /> 
        <sun:staticText id="vasec" text="$resource{i18n.common.Seconds}"/>
    </sun:property>

    <sun:property id="p2"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.connectionLeakTimeoutInSeconds}" helpText="$resource{i18njdbc.jdbcPool.connectionLeakTimeoutInSecondsHelp}">
        <sun:textField id="p2" styleClass="integer" columns="$int{30}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.connectionLeakTimeoutInSeconds']}" text="#{pageSession.valueMap['connectionLeakTimeoutInSeconds']}"/> 
        <sun:staticText id="vasec" text="$resource{i18n.common.Seconds}"/>
   </sun:property>   

    <sun:property id="p3"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.connectionLeakReclaim}" helpText="$resource{i18njdbc.jdbcPool.connectionLeakReclaimHelp}">
        <sun:checkbox  selected="#{pageSession.valueMap['connectionLeakReclaim']}" label=" " selectedValue="true" />
    </sun:property>

    <sun:property id="statementLeakTimeoutInSeconds"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.statementLeakTimeoutInSeconds}" helpText="$resource{i18njdbc.jdbcPool.statementLeakTimeoutInSecondsHelp}">
        <sun:textField id="statementLeakTimeoutInSeconds" styleClass="integer" columns="$int{30}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.statementTimeoutInSeconds']}" text="#{pageSession.valueMap['statementLeakTimeoutInSeconds']}"/>
        <sun:staticText id="vasec" text="$resource{i18n.common.Seconds}"/>
   </sun:property>

    <sun:property id="statementLeakReclaim"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.statementLeakReclaim}" helpText="$resource{i18njdbc.jdbcPool.statementLeakReclaimHelp}">
        <sun:checkbox  selected="#{pageSession.valueMap['statementLeakReclaim']}" label=" " selectedValue="true" />
    </sun:property>

    <sun:property id="p4"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.connectionCreationRetryAttempts}" helpText="$resource{i18njdbc.jdbcPool.connectionCreationRetryAttemptsHelp}">
        <sun:textField id="p4" styleClass="integer" columns="$int{30}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.connectionCreationRetryAttempts']}" text="#{pageSession.valueMap['connectionCreationRetryAttempts']}"/> 
   </sun:property>   

    <sun:property id="p5"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.connectionCreationRetryIntervalInSeconds}" helpText="$resource{i18njdbc.jdbcPool.connectionCreationRetryIntervalInSecondsHelp}">
        <sun:textField id="p5" styleClass="integer" columns="$int{30}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.connectionCreationRetryIntervalInSeconds']}" text="#{pageSession.valueMap['connectionCreationRetryIntervalInSeconds']}"/> 
        <sun:staticText id="p51" text="$resource{i18n.common.Seconds}"/>
   </sun:property>

    <sun:property id="p7"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.lazyConnectionAssociation}" helpText="$resource{i18njdbc.jdbcPool.lazyConnectionAssociationHelp}">
        <sun:checkbox id="associate"  selected="#{pageSession.valueMap['lazyConnectionAssociation']}" label="$resource{i18n.common.Enabled}" onClick="enableDisableLazyConnection('#{associateId}');" selectedValue="true" >  
           <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{associateId});
            />
        </sun:checkbox>
   </sun:property>

    <sun:property id="p6"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.lazyConnectionEnlistment}" helpText="$resource{i18njdbc.jdbcPool.lazyConnectionEnlistmentHelp}">
        <sun:checkbox id="enlist"  selected="#{pageSession.valueMap['lazyConnectionEnlistment']}" label="$resource{i18n.common.Enabled}"  selectedValue="true">  
            <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{enlistId});
            />
        </sun:checkbox>
    </sun:property>

    <sun:property id="p8"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.associationWithThread}" helpText="$resource{i18njdbc.jdbcPool.associationWithThreadHelp}">
        <sun:checkbox  selected="#{pageSession.valueMap['associateWithThread']}" label="$resource{i18n.common.Enabled}" selectedValue="true"/>  
   </sun:property>
   <sun:property id="p9"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.matchConnections}" helpText="$resource{i18njdbc.jdbcPool.matchConnectionsHelp}">
        <sun:checkbox  selected="#{pageSession.valueMap['matchConnections']}" label="$resource{i18n.common.Enabled}" selectedValue="true"/>  
   </sun:property>

    <sun:property id="p10"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.maxConnectionUsageCount}" helpText="$resource{i18njdbc.jdbcPool.maxConnectionUsageCountHelp}">
        <sun:textField id="p5" styleClass="integer" columns="$int{30}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.maxConnectionUsageCount']}" text="#{pageSession.valueMap['maxConnectionUsageCount']}"/> 
   </sun:property>

</sun:propertySheetSection>
<!-- -------------------- Connection Validation ------------ -->

    <sun:propertySheetSection id="connectionValidationPropertySheet" label="$resource{i18njdbc.jdbcPool.Validation}">

        <sun:property id="conValidationProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.conValidation}" helpText="$resource{i18njdbc.jdbcPool.conValidationHelp}">
            <sun:checkbox label="$resource{i18n.common.Required}" selected="#{pageSession.valueMap.isConnectionValidationRequired}" selectedValue="true" onChange="javascript: getDropdownInfo('#{pageSession.attrMap}', '#{pageSession.validCNAttrMap}', '#{pageSession.valueMap.validationTableName}', '#{pageSession.valueMap.validationClassname}'); disableTableField('#{pageSession.valueMap.validationTableName}','#{pageSession.valueMap.validationClassname}');" >
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{conValidationId});
                />
            </sun:checkbox>
       </sun:property>

        <sun:property id="methodProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.valMethod}" >
            <sun:dropDown id="valMethod" selected="#{pageSession.valueMap.connectionValidationMethod}"
                labels={"auto-commit" "meta-data"  "custom-validation" "table"} onChange="javascript: getDropdownInfo('#{pageSession.attrMap}', '#{pageSession.validCNAttrMap}', '#{pageSession.valueMap.validationTableName}', '#{pageSession.valueMap.validationClassname}'); disableTableField('#{pageSession.valueMap.validationTableName}', '#{pageSession.valueMap.validationClassname}');">
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{valMethod});
                />
            </sun:dropDown>
        </sun:property>

        <sun:property id="tableProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.tableName}" helpText="$resource{i18njdbc.jdbcPool.tableNameHelp}">
            <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{tablePropId});
            />
            <sun:hidden id="option" value="#{pageSession.myTableOption}">
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{tableOptionId});
                />
            </sun:hidden>
            <sun:radioButton id="optA"  name="dropDownOption" onClick="javascript: enableJdbcRadioButtons('dropdown', 'table');" />
            <sun:dropDown id="tableTextField"  labels="$pageSession{tableNameList}"  values="$pageSession{tableNameList}" selected="#{pageSession.tableDropDownValue}" >
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{tableDropdownTextId});
                />                
            </sun:dropDown>
            <sun:button id="tableNamesButton" primary="#{false}" text="$resource{i18njdbc.jdbcPool.populatetableNames}" onClick="javascript: getValidationTableNames('#{pageSession.attrMap}', '#{pageSession.valueMap.validationTableName}'); disableTableField('#{pageSession.valueMap.validationTableName}', '#{pageSession.valueMap.validationClassname}');return false;">
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{tableNamesButtonId});
                />
            </sun:button>
            "<br />
            <sun:radioButton id="optB"  name="textOption" onClick="javascript: enableJdbcRadioButtons('text', 'table');" />
            <sun:textField id="tableText" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.tableText']}" text="#{pageSession.tableTextValue}" >
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{tableTextId});
                />
            </sun:textField>
        </sun:property>
        <sun:property id="validCNProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.valCN}" helpText="$resource{i18njdbc.jdbcPool.valCNHelp}">
            <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{validCNPropId});
            />
            <sun:hidden id="option" value="#{pageSession.myValidCNOption}">
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{validCNOptionId});
                />
            </sun:hidden>
            <sun:radioButton id="optA"  name="dropDownOption" onClick="javascript: enableJdbcRadioButtons('dropdown', 'validCN');" />
            <sun:dropDown id="validCNTextField"  labels="$pageSession{classNameList}"  values="$pageSession{classNameList}" selected="#{pageSession.validCNDropDownValue}" >
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{validCNDropdownTextId});
                />
            </sun:dropDown>
            "<br />
            <sun:radioButton id="optB"  name="textOption" onClick="javascript: enableJdbcRadioButtons('text', 'validCN');" />
            <sun:textField id="validCNText" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.validationClassNameText']}" text="#{pageSession.validCNTextValue}" >
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{validCNTextId});
                />
            </sun:textField>
        </sun:property>

        <sun:property id="failProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.failOnAny}" helpText="$resource{i18njdbc.jdbcPool.failOnAnyHelp}">
            <sun:checkbox label="$resource{i18njdbc.jdbcPool.closeAll}" selected="#{pageSession.valueMap.failAllConnections}" selectedValue="true" />
       </sun:property>

        <sun:property id="allowProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.allowCaller}" helpText="$resource{i18njdbc.jdbcPool.allowCallerHelp}">
            <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap.allowNonComponentCallers}" selectedValue="true" />
       </sun:property>

      "<br /><br />
    </sun:propertySheetSection>
</sun:propertySheet>
