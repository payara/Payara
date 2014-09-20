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

<!-- jdbc/poolPropertyEdit.inc -->

<!-- PropertySheet .... -->
<sun:propertySheet id="sheet" requiredFields="true">
<sun:propertySheetSection id="generalSheet" label="$resource{i18njdbc.jdbcPool.general}">
    <sun:property id="name"   labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.poolName}">
        <sun:staticText id="name" text="#{pageSession.Name}"/>
    </sun:property>

    <sun:property id="resTypeEditProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.resType}" helpText="$resource{i18njdbc.jdbcPool.resTypeHelp}" >
        <sun:dropDown id="resTypeVal" selected="#{pageSession.valueMap.resType}"
            labels={"" "javax.sql.DataSource"  "javax.sql.XADataSource"  "javax.sql.ConnectionPoolDataSource" "java.sql.Driver"}
            onChange="javascript: disableFields();" >
            <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{resTypeVal});
            />
        </sun:dropDown>
    </sun:property>

    <sun:property id="dSProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.datasource}" helpText="$resource{i18njdbc.jdbcPool.datasourceHelp}">
        <sun:textField id="dsTextField" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.datasource']}" text="#{pageSession.valueMap.datasourceClassname}"  >
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{dsTextField});
        />
        </sun:textField>
    </sun:property>

    <sun:property id="dDSProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.driver}" helpText="$resource{i18njdbc.jdbcPool.driverHelp}">
        <sun:textField id="ddsTextField"  columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.driver']}" text="#{pageSession.valueMap.driverClassname}"  >
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{ddsTextField});
        />
        </sun:textField>
    </sun:property>
    <sun:property id="pingProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.ping}" helpText="$resource{i18njdbc.jdbcPool.pingHelp}">
        <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap.ping}" selectedValue="true" />
   </sun:property>
   <sun:property id="deploymentOrder" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.resource.deploymentOrder}"  helpText="$resource{i18n.common.resource.deploymentOrderHelp}" >
       <sun:textField id="deploymentOrder" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.common.deploymentOrder']}" text="#{pageSession.valueMap['deploymentOrder']}" />
   </sun:property>
    <sun:property id="descProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18n.common.description}" >
        <sun:textField id="desc" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.common.description']}" text="#{pageSession.valueMap.description}" />
    </sun:property>
    "<br /><br />
</sun:propertySheetSection>

			<!-- -------------------- Pool Settings ------------ -->
<sun:propertySheetSection id="poolSettingsPropertySheet" label="$resource{i18njdbc.jdbcPool.poolSettings}">
    <sun:property id="initSizeProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.initPoolSize}" helpText="$resource{i18njdbc.jdbcPool.initPoolSizeHelp}">
        <sun:textField id="ds" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.initPoolSize']}" text="#{pageSession.valueMap.steadyPoolSize}" />
        <sun:staticText id="con1" style="padding: 8pt" text="$resource{i18n.common.Connections}"/> 
    </sun:property>

    <sun:property id="maxProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.maxPoolSize}" helpText="$resource{i18njdbc.jdbcPool.maxPoolSizeHelp}">
        <sun:textField id="ds2" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.maxPoolSize']}" text="#{pageSession.valueMap.maxPoolSize}" />
        <sun:staticText id="con2" style="padding: 8pt" text="$resource{i18n.common.Connections}"/>
    </sun:property>

    <sun:property id="resizeProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.poolResize}" helpText="$resource{i18njdbc.jdbcPool.poolResizeHelp}">
        <sun:textField id="ds3" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.poolResize']}" text="#{pageSession.valueMap.poolResizeQuantity}" />
        <sun:staticText id="con3" style="padding: 8pt" text="$resource{i18n.common.Connections}"/>
    </sun:property>

    <sun:property id="idleProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.idleTimeout}" helpText="$resource{i18njdbc.jdbcPool.idleTimeoutHelp}">
        <sun:textField id="ds" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.idleTimeout']}" text="#{pageSession.valueMap.idleTimeoutInSeconds}" />
        <sun:staticText id="sec" style="padding: 8pt" text="$resource{i18n.common.Seconds}"/>
    </sun:property>

    <sun:property id="maxWaitProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.maxWait}" helpText="$resource{i18njdbc.jdbcPool.maxWaitHelp}">
        <sun:textField id="ds" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.maxWait']}"  text="#{pageSession.valueMap.maxWaitTimeInMillis}" />
        <sun:staticText id="mil" style="padding: 8pt" text="$resource{i18n.common.Milliseconds}"/>
    </sun:property>

    "<br /><br />
</sun:propertySheetSection>

			
    <!---------------------- Transaction Isolation ------------ -->

<sun:propertySheetSection id="transactionPropertySheet" label="$resource{i18njdbc.jdbcPool.transIsolationSection}">

    <sun:property id="nonTransProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.nonTransConnection}" helpText="$resource{i18njdbc.jdbcPool.nonTransConnectionHelp}">
        <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{valueMap.nonTransactionalConnections}" selectedValue="true" />
   </sun:property>

    <sun:property id="transProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.transIsolation}" helpText="$resource{i18njdbc.jdbcPool.transIsolationHelp}" >
        <sun:dropDown id="transIso" selected="#{valueMap.transactionIsolationLevel}"
            labels={"" "read-uncommitted" "read-committed" "repeatable-read" "serializable" }
            
            />
    </sun:property>

    <sun:property id="levelProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.isoLevel}" helpText="$resource{i18njdbc.jdbcPool.isoLevelHelp}">
        <sun:checkbox label="$resource{i18njdbc.jdbcPool.Guaranteed}" selected="#{valueMap.isIsolationLevelGuaranteed}" selectedValue="true" />
   </sun:property>
   "<br /><br />
</sun:propertySheetSection>
</sun:propertySheet>
