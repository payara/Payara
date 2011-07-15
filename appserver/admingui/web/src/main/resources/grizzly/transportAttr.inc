<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.

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

<!-- grizzly/transportAttr.inc -->

<sun:propertySheet id="propertySheet">
#include "/common/shared/configNameSection.inc"
    <sun:propertySheetSection id="propertSectionTextField">

    <sun:property id="name"  rendered="#{edit}"   labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.transportName}">
        <sun:staticText id="name" text="#{pageSession.Name}" />
    </sun:property>

    <sun:property id="IdTextProp"  rendered="#{!edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.transportName}">
        <sun:textField id="IdText" styleClass="required" text="#{pageSession.valueMap['name']}" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.transport.transportName']}" required="#{true}" />
    </sun:property>

    <sun:property id="Classname"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.Classname}"  helpText="$resource{i18n_web.transport.ClassnameHelp}">
        <sun:textField id="Classname" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.transport.Classname']}" text="#{pageSession.valueMap['classname']}" />
    </sun:property>


    <sun:property id="SelectionKeyHandler"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.SelectionKeyHandler}"  helpText="$resource{i18n_web.transport.SelectionKeyHandlerHelp}">
        <sun:textField id="SelectionKeyHandler"  columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.transport.SelectionKeyHandler']}" text="#{pageSession.valueMap['selectionKeyHandler']}" />
    </sun:property>

    <sun:property id="ByteBufferType"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.ByteBufferType}"  helpText="$resource{i18n_web.transport.ByteBufferTypeHelp}">
        <sun:dropDown id="ByteBufferType" selected="#{pageSession.valueMap['byteBufferType']}" labels={"HEAP", "DIRECT"}  />
    </sun:property>

    <sun:property id="BufferSizeBytes"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.BufferSizeBytes}"  helpText="$resource{i18n_web.transport.BufferSizeBytesHelp}">
        <sun:textField id="BufferSizeBytes" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.transport.BufferSizeBytes']}" styleClass="integer" text="#{pageSession.valueMap['bufferSizeBytes']}" />
        <sun:staticText id="bytes" style="padding: 8pt" text="$resource{i18n.common.Bytes}"/>
    </sun:property>

    <sun:property id="AcceptorThreads"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.AcceptorThreads}" helpText="$resource{i18n_web.transport.AcceptorThreadsHelp}">
        <sun:textField id="AcceptorThreads" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.transport.AcceptorThreads']}" styleClass="intAllowMinusOne" text="#{pageSession.valueMap['acceptorThreads']}" />
    </sun:property>

    <sun:property id="MaxConnectionsCount"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.MaxConnectionsCount}"  helpText="$resource{i18n_web.transport.MaxConnectionsCountHelp}">
        <sun:textField id="MaxConnectionsCount" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.transport.MaxConnectionsCount']}" styleClass="integer" text="#{pageSession.valueMap['maxConnectionsCount']}" />
    </sun:property>

    <sun:property id="IdleKeyTimeoutSeconds"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.IdleKeyTimeoutSeconds}"  helpText="$resource{i18n_web.transport.IdleKeyTimeoutSecondsHelp}">
        <sun:textField id="IdleKeyTimeoutSeconds" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.transport.IdleKeyTimeoutSeconds']}" styleClass="integer" text="#{pageSession.valueMap['idleKeyTimeoutSeconds']}" />
        <sun:staticText id="sec" style="padding: 8pt" text="$resource{i18n.common.Seconds}"/>
    </sun:property>

    <sun:property id="ReadTimeoutMillis"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.ReadTimeoutMillis}" helpText="$resource{i18n_web.transport.ReadTimeoutMillisHelp}">
        <sun:textField id="ReadTimeoutMillis" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.transport.ReadTimeoutMillis']}" styleClass="integer" text="#{pageSession.valueMap['readTimeoutMillis']}" />
        <sun:staticText id="sec" style="padding: 8pt" text="$resource{i18n.common.Milliseconds}"/>
    </sun:property>

    <sun:property id="SelectorPollTimeoutMillis"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.SelectorPollTimeoutMillis}" helpText="$resource{i18n_web.transport.SelectorPollTimeoutMillisHelp}">
        <sun:textField id="SelectorPollTimeoutMillis" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.transport.SelectorPollTimeoutMillis']}" styleClass="integer" text="#{pageSession.valueMap['selectorPollTimeoutMillis']}" />
        <sun:staticText id="sec" style="padding: 8pt" text="$resource{i18n.common.Milliseconds}"/>
    </sun:property>


    <sun:property id="WriteTimeoutMillis"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.WriteTimeoutMillis}"  helpText="$resource{i18n_web.transport.WriteTimeoutMillisHelp}">
        <sun:textField id="WriteTimeoutMillis" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.transport.WriteTimeoutMillis']}" styleClass="integer" text="#{pageSession.valueMap['writeTimeoutMillis']}" />
        <sun:staticText id="sec" style="padding: 8pt" text="$resource{i18n.common.Milliseconds}"/>
    </sun:property>

    <sun:property id="DisplayConfiguration"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.DisplayConfiguration}"  helpText="$resource{i18n_web.transport.DisplayConfigurationHelp}" >
        <sun:checkbox  selected="#{pageSession.valueMap['displayConfiguration']}" selectedValue="true" />
    </sun:property>

    <sun:property id="EnableSnoop"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.EnableSnoop}"  helpText="$resource{i18n_web.transport.EnableSnoopHelp}" >
        <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['enableSnoop']}" selectedValue="true" />
    </sun:property>

     <sun:property id="TcpNoDelay"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.transport.TcpNoDelay}" helpText="$resource{i18n_web.transport.TcpNoDelayHelp}">
        <sun:checkbox  selected="#{pageSession.valueMap['tcpNoDelay']}" selectedValue="true" />
    </sun:property>

    </sun:propertySheetSection>
</sun:propertySheet>
	
