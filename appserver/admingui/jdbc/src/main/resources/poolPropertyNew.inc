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

<!-- jdbc/poolPropertyNew.inc -->

<!-- PropertySheet .... -->
<sun:propertySheet id="sheet" requiredFields="true">

<sun:propertySheetSection id="generalSheet" label="$resource{i18njdbc.jdbcPool.general}">
    <sun:property id="jndiProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.poolName}">
        <sun:staticText escape="true" id="Jndi" text="#{wizardPoolExtra.name}" />
    </sun:property>

    <sun:property id="resTypeWizProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18n.common.resType}"  >
        <sun:staticText escape="true" id="resType" text="#{wizardPoolExtra.resType}"  />
    </sun:property>

    <sun:property id="dbProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.dbVendor}"  >
        <sun:staticText escape="true" id="db" text="#{wizardPoolExtra.DBVendor}"/>
    </sun:property>

    <sun:property id="dsProp" labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.datasource}" helpText="$resource{i18njdbc.jdbcPool.datasourceHelpEnter}">
        <sun:dropDown  id="datasource" selected="#{wizardPoolExtra.datasourceClassname}" labels="$attribute{DSList}"  disabled="#{!wizardPoolExtra.dsClassname}" submitForm="#{true}" immediate="#{true}">
            <!command
                    setAttribute(key="click" value="$this{component}");
                    mapPut(map="#{wizardPoolExtra}" key="datasourceClassname" value="#{click.selected}");
                    gf.updateJdbcConnectionPoolPropertiesTable();
                    gf.navigate(page="#{selfPage}");
                />
        </sun:dropDown>
        "<br/>
        <sun:textField id="datasourceField"  columns="$int{60}" maxLength="#{sessionScope.fieldLengths['jdbcPool.datasource']}" text="#{wizardPoolExtra.DatasourceClassnameField}"  disabled="#{!wizardPoolExtra.dsClassname}" >
            <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{datasourceClassNameFieldId});
            />
        </sun:textField>
    </sun:property>
    <sun:property id="ddsProp" labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.driver}" helpText="$resource{i18njdbc.jdbcPool.driverHelpEnter}">
        <sun:dropDown id="ddatasource"  selected="#{wizardPoolExtra.driverClassname}" labels="$attribute{DList}"  disabled="#{wizardPoolExtra.dsClassname}" />
        "<br/>
        <sun:textField id="ddatasourceField"  columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.driver']}" text="#{wizardPoolExtra.DriverClassnameField}"  disabled="#{wizardPoolExtra.dsClassname}" >
            <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{driverClassNameFieldId});
            />
        </sun:textField>
    </sun:property>

    <sun:property id="pingProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.ping}" helpText="$resource{i18njdbc.jdbcPool.pingHelp}">
        <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{sessionScope.wizardMap.ping}" selectedValue="true" />
   </sun:property>
    <sun:property id="descProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18n.common.description}" >
        <sun:textField id="desc" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.common.description']}" text="#{sessionScope.wizardMap.Description}" />
    </sun:property>
    "<br /><br />
</sun:propertySheetSection>

			<!-- -------------------- Pool Settings ------------ -->
<sun:propertySheetSection id="poolSettingsPropertySheet" label="$resource{i18njdbc.jdbcPool.poolSettings}">
    <sun:property id="initSizeProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.initPoolSize}" helpText="$resource{i18njdbc.jdbcPool.initPoolSizeHelp}">
        <sun:textField id="ds" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.initPoolSize']}" text="#{sessionScope.wizardMap.steadyPoolSize}" />
        <sun:staticText id="con1" style="padding: 8pt" text="$resource{i18n.common.Connections}"/> 
    </sun:property>

    <sun:property id="maxProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.maxPoolSize}" helpText="$resource{i18njdbc.jdbcPool.maxPoolSizeHelp}">
        <sun:textField id="ds2" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.maxPoolSize']}" text="#{sessionScope.wizardMap.maxPoolSize}" />
        <sun:staticText id="con2" style="padding: 8pt" text="$resource{i18n.common.Connections}"/>
    </sun:property>

    <sun:property id="resizeProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.poolResize}" helpText="$resource{i18njdbc.jdbcPool.poolResizeHelp}">
        <sun:textField id="ds3" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.poolResize']}" text="#{sessionScope.wizardMap.poolResizeQuantity}" />
        <sun:staticText id="con3" style="padding: 8pt" text="$resource{i18n.common.Connections}"/>
    </sun:property>

    <sun:property id="idleProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.idleTimeout}" helpText="$resource{i18njdbc.jdbcPool.idleTimeoutHelp}">
        <sun:textField id="ds" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.idleTimeout']}" text="#{sessionScope.wizardMap.idleTimeoutInSeconds}" />
        <sun:staticText id="sec" style="padding: 8pt" text="$resource{i18n.common.Seconds}"/>
    </sun:property>

    <sun:property id="maxWaitProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.maxWait}" helpText="$resource{i18njdbc.jdbcPool.maxWaitHelp}">
        <sun:textField id="ds" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.jdbcPool.maxWait']}" text="#{sessionScope.wizardMap.maxWaitTimeInMillis}" />
        <sun:staticText id="mil" style="padding: 8pt" text="$resource{i18n.common.Milliseconds}"/>
    </sun:property>

    "<br /><br />
</sun:propertySheetSection>

			

<!-- -------------------- Transaction Isolation ------------ -->

<sun:propertySheetSection id="transactionPropertySheet" label="$resource{i18njdbc.jdbcPool.transIsolationSection}">

    <sun:property id="nonTransProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.nonTransConnection}" helpText="$resource{i18njdbc.jdbcPool.nonTransConnectionHelp}">
        <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{sessionScope.wizardMap.nonTransactionalConnections}" selectedValue="true" />
   </sun:property>

    <sun:property id="transProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.transIsolation}" helpText="$resource{i18njdbc.jdbcPool.transIsolationHelp}" >
        <sun:dropDown id="transIso" selected="#{sessionScope.wizardMap.transactionIsolationLevel}" labels={"" "read-uncommitted" "read-committed" "repeatable-read" "serializable" } />
    </sun:property>

    <sun:property id="levelProp"  labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njdbc.jdbcPool.isoLevel}" helpText="$resource{i18njdbc.jdbcPool.isoLevelHelp}">
        <sun:checkbox label="$resource{i18njdbc.jdbcPool.Guaranteed}" selected="#{sessionScope.wizardMap.isIsolationLevelGuaranteed}" selectedValue="true" />
   </sun:property>
   "<br /><br />
</sun:propertySheetSection>
</sun:propertySheet>

