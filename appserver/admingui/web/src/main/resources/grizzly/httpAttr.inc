<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2009-2015 Oracle and/or its affiliates. All rights reserved.

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

<!-- grizzly/httpAttr.inc -->


<sun:propertySheetSection id="httpTextField" label="#{pageSession.httpSectionTitle}" >


        <sun:property id="protocolName" rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.protocol.protocolName}"  >
            <sun:staticText id="protocolName" text="#{pageSession.protocolName}" />
        </sun:property>

        <sun:property id="ServerNameProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.serverNameLabel}" helpText="$resource{i18n_web.http.serverNameHelp}" >
            <sun:textField id="ServerName" columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.http.serverName']}" text="#{pageSession.httpMap['serverName']}" />
        </sun:property>

        <sun:property id="DefaultVirtServersProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.defVirtualServerLabel}" helpText="$resource{i18n_web.http.defVirtualServerLabelHelp}">
            <sun:dropDown id="vs" selected="#{pageSession.httpMap['defaultVirtualServer']}" labels="$pageSession{vsList}"  values="$pageSession{vsList}" />
        </sun:property>

        <sun:property id="RedirectPortProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.redirectPortLabel}" helpText="$resource{i18n_web.http.redirectPortLabelHelp}" >
            <sun:textField id="RedirectPort" styleClass="port" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.http.redirectPort']}" text="#{pageSession.httpMap['redirectPort']}" />
        </sun:property>


        <sun:property id="maxC"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.maxConnections}" helpText="$resource{i18n_web.http.maxConnectionsHelp}" >
            <sun:textField id="maxC" styleClass="integer" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.http.maxConnections']}" text="#{pageSession.httpMap['maxConnections']}" />
        </sun:property>


        <sun:property id="TimeoutSeconds"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.TimeoutSeconds}" helpText="$resource{i18n_web.http.TimeoutSecondsHelp}" >
            <sun:textField id="TimeoutSeconds" styleClass="intAllowMinusOne" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.http.TimeoutSeconds']}" text="#{pageSession.httpMap['timeoutSeconds']}" />
            <sun:staticText id="byte" style="padding: 8pt" text="$resource{i18n.common.Seconds}"/>
        </sun:property>

        <sun:property id="DisableUploadTimeout"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.DisableUploadTimeout}" helpText="$resource{i18n_web.http.DisableUploadTimeoutHelp}">
            <sun:checkbox label="$resource{i18n_web.common.enabled}" selected="#{pageSession.httpMap['uploadTimeoutEnabled']}" selectedValue="true" />
        </sun:property>

          <sun:property id="connectionUploadTimeout"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.ConnectionUploadTimeout}" helpText="$resource{i18n_web.http.ConnectionUploadTimeoutHelp}" >
            <sun:textField id="connectionUploadTimeout" styleClass="intAllowMinusOne" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.http.ConnectionUploadTimeout']}" text="#{pageSession.httpMap['connectionUploadTimeoutMillis']}" />
            <sun:staticText id="byte" style="padding: 8pt" text="$resource{i18n.common.Milliseconds}"/>
        </sun:property>

        <sun:property id="RequestTimeoutSeconds"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.RequestTimeoutSeconds}" helpText="$resource{i18n_web.http.RequestTimeoutSecondsHelp}" >
            <sun:textField id="RequestTimeoutSeconds" styleClass="intAllowMinusOne" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.http.RequestTimeoutSeconds']}" text="#{pageSession.httpMap['requestTimeoutSeconds']}" />
            <sun:staticText id="byte" style="padding: 8pt" text="$resource{i18n.common.Seconds}"/>
        </sun:property>

        <sun:property id="sendBsize"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.SendBufferSize}" helpText="$resource{i18n_web.http.SendBufferSizeHelp}" >
            <sun:textField id="sendBsize" styleClass="integer" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.http.SendBufferSize']}" text="#{pageSession.httpMap['sendBufferSizeBytes']}" />
            <sun:staticText id="byte" style="padding: 8pt" text="$resource{i18n.common.Bytes}"/>
        </sun:property>

        <sun:property id="headerBLength"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.HeaderBufferLength}" helpText="$resource{i18n_web.http.HeaderBufferLengthHelp}" >
            <sun:textField id="headerBLength" styleClass="integer" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.http.HeaderBufferLength']}" text="#{pageSession.httpMap['headerBufferLengthBytes']}" />
            <sun:staticText id="byte" style="padding: 8pt" text="$resource{i18n.common.Bytes}"/>
        </sun:property>

        <sun:property id="MaxPostSize"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.MaxPostSize}" helpText="$resource{i18n_web.http.MaxPostSizeHelp}" >
            <sun:textField id="headerBLength" styleClass="intAllowMinusOne" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.http.MaxPostSize']}" text="#{pageSession.httpMap['maxPostSizeBytes']}" />
            <sun:staticText id="byte" style="padding: 8pt" text="$resource{i18n.common.Bytes}"/>
        </sun:property>

        <sun:property id="MaxSavePostSize"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.MaxSavePostSize}" helpText="$resource{i18n_web.http.MaxSavePostSizeHelp}" >
            <sun:textField id="MaxSavePostSize" styleClass="integer" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.http.MaxPostSize']}" text="#{pageSession.httpMap['maxSavePostSizeBytes']}" />
            <sun:staticText id="byte" style="padding: 8pt" text="$resource{i18n.common.Bytes}"/>
        </sun:property>


        <sun:property id="UriEncoding"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.UriEncoding}" helpText="$resource{i18n_web.http.UriEncodingHelp}" >
            <sun:textField id="UriEncoding"  columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.http.UriEncoding']}" text="#{pageSession.httpMap['uriEncoding']}" />
        </sun:property>

        <sun:property id="Version"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.Version}" helpText="$resource{i18n_web.http.VersionHelp}" >
            <sun:textField id="Version"  columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.http.Version']}" text="#{pageSession.httpMap['version']}" />
        </sun:property>

       <sun:property id="Compression"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.Compression}" helpText="$resource{i18n_web.http.CompressionHelp}">
            <sun:dropDown id="Compression" selected="#{pageSession.httpMap['compression']}" labels={"on","off","force"} />
        </sun:property>


        <sun:property id="CompressableMimeType"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.CompressableMimeType}" helpText="$resource{i18n_web.http.CompressableMimeTypeHelp}" >
            <sun:textField id="CompressableMimeType"  columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.http.CompressableMimeType']}" text="#{pageSession.httpMap['compressableMimeType']}" />
        </sun:property>

        <sun:property id="compressableMime"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.CompressionMinSizeBytes}" helpText="$resource{i18n_web.http.CompressionMinSizeBytesHelp}" >
            <sun:textField id="compressableMime"  styleClass="integer" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.http.CompressionMinSizeBytes']}" text="#{pageSession.httpMap['compressionMinSizeBytes']}" />
            <sun:staticText id="byte" style="padding: 8pt" text="$resource{i18n.common.Bytes}"/>
        </sun:property>


        <sun:property id="NoCompressionUserAgents"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.NoCompressionUserAgents}" helpText="$resource{i18n_web.http.NoCompressionUserAgentsHelp}" >
            <sun:textField id="NoCompressionUserAgents" styleClass="integer" columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.http.NoCompressionUserAgents']}" text="#{pageSession.httpMap['noCompressionUserAgents']}" />
        </sun:property>


        <sun:property id="RestrictedUserAgents"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.RestrictedUserAgents}" helpText="$resource{i18n_web.http.RestrictedUserAgentsHelp}" >
            <sun:textField id="RestrictedUserAgents"  columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.http.RestrictedUserAgents']}" text="#{pageSession.httpMap['restrictedUserAgents']}" />
        </sun:property>

        <sun:property id="DefaultResponseType"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.DefaultResponseType}" helpText="$resource{i18n_web.http.DefaultResponseTypeHelp}" >
            <sun:textField id="DefaultResponseType"  columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.http.DefaultResponseType']}" text="#{pageSession.httpMap['defaultResponseType']}" />
        </sun:property>

        <sun:property id="ForcedResponseType"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.ForcedResponseType}" helpText="$resource{i18n_web.http.ForcedResponseTypeHelp}" >
            <sun:textField id="ForcedResponseType"  columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.http.ForcedResponseType']}" text="#{pageSession.httpMap['forcedResponseType']}" />
        </sun:property>

        <sun:property id="adapter"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.Adapter}" helpText="$resource{i18n_web.http.AdapterHelp}" >
            <sun:textField id="adapter"   columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.http.Adapter']}" text="#{pageSession.httpMap['adapter']}" />
        </sun:property>


        <sun:property id="Comet"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.CometSupport}" helpText="$resource{i18n_web.http.CometSupportHelp}">
            <sun:checkbox id="cometEnabled" label="$resource{i18n_web.common.enabled}" selected="#{pageSession.httpMap['cometSupportEnabled']}" selectedValue="true" />
        </sun:property>

        <sun:property id="DnsLookup"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.DnsLookup}" helpText="$resource{i18n_web.http.DnsLookupHelp}">
            <sun:checkbox id="dnsLookupEnabled" label="$resource{i18n_web.common.enabled}" selected="#{pageSession.httpMap['dnsLookupEnabled']}" selectedValue="true" />
        </sun:property>

        <sun:property id="RcmSupport"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.RcmSupport}" helpText="$resource{i18n_web.http.RcmSupportHelp}">
            <sun:checkbox id="rcmSupportEnabled" label="$resource{i18n_web.common.enabled}" selected="#{pageSession.httpMap['rcmSupportEnabled']}" selectedValue="true" />
        </sun:property>

        <sun:property id="Trace"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.Trace}" helpText="$resource{i18n_web.http.TraceHelp}">
            <sun:checkbox id="traceEnabled" label="$resource{i18n_web.common.enabled}" selected="#{pageSession.httpMap['traceEnabled']}" selectedValue="true" />
        </sun:property>

        <sun:property id="AuthPassThrough"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.AuthPassThrough}" helpText="$resource{i18n_web.http.AuthPassThroughHelp}">
            <sun:checkbox id="authPassThroughEnabled" label="$resource{i18n_web.common.enabled}" selected="#{pageSession.httpMap['authPassThroughEnabled']}" selectedValue="true" />
        </sun:property>

        <sun:property id="chunking"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.Chunking}" helpText="$resource{i18n_web.http.ChunkingHelp}" >
            <sun:checkbox id="chunkingEnabled" label="$resource{i18n_web.common.enabled}" selected="#{pageSession.httpMap['chunkingEnabled']}" selectedValue="true" />
        </sun:property>

        <sun:property id="XpoweredBy"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.XpoweredBy}" helpText="$resource{i18n_web.http.XpoweredByHelp}" >
            <sun:checkbox id="xpoweredByEnabled" selected="#{pageSession.httpMap['xpoweredBy']}" selectedValue="true" />
        </sun:property>

        <sun:property id="encodedSlash"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.EncodedSlash}" helpText="$resource{i18n_web.http.EncodedSlashHelp}" >
            <sun:checkbox id="encodedSlashEnabled" label="$resource{i18n_web.common.enabled}" selected="#{pageSession.httpMap['encodedSlashEnabled']}" selectedValue="true" />
        </sun:property>

        <sun:property id="websockets"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.WebsocketsSupport}" helpText="$resource{i18n_web.http.WebsocketsSupportHelp}" >
            <sun:checkbox id="websocketsEnabled" label="$resource{i18n_web.common.enabled}" selected="#{pageSession.httpMap['websocketsSupportEnabled']}" selectedValue="true" />
        </sun:property>
"<br /><br />
</sun:propertySheetSection>
