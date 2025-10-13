<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.

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

    Portions Copyright [2020] [Payara Foundation and/or its affiliates]
-->
<f:verbatim>
<td class="LogCntTd" style="background-image: url(#{request.contextPath}/resource/community-theme/images/backimage.png);background-repeat:no-repeat;background-position:left top;" height="435" align="center" valign="middle">
    <!--<td class="LogCntTd" style="background-repeat:no-repeat;background-position:left top;" height="435" align="center" valign="middle">-->
    <table border="0" background="/resource/images/dot.gif" cellpadding="0" cellspacing="0" width="100%" title="">
        <tr>
            <td width="150"><img src="/resource/images/dot.gif" width="150" height="245" alt="" /></td>
            <td width="415" valign="top" align="left">&nbsp;

                <table border="0" cellspacing="0" cellpadding="0">
                    <tr>
                        <td>&nbsp;</td>
                    </tr>
                    <tr>
                        <td><div class="logLbl"><span class="LblLev2Txt">
</f:verbatim>
                                    <sun:staticText text="$resource{i18n.restart.RestartHeading}  "  />
<f:verbatim>
                    </span></div></td>
                    </tr>
                    <tr>
                        <td><div class="logLblLst"><span class="LblLev2Txt">
                </span></div></td>
                    </tr>
                    <tr>

                        <td><div class="logLbl"><span class="LblLev2Txt">
</f:verbatim>
                                    <sun:staticText text="#{pageSession.url2}" >
                        <!beforeCreate
                            //we have to get the scheme and port number from domain.xml instead of from #{request} because user may have changed this.
                            gf.getEntityAttrs(endpoint="#{sessionScope.REST_URL}/configs/config/server-config/network-config/network-listeners/network-listener/admin-listener", valueMap="#{requestScope.portMap}");
                            gf.restRequest(endpoint="#{sessionScope.REST_URL}/configs/config/server-config/network-config/network-listeners/network-listener/admin-listener/find-http-protocol"
                                   method="GET"
                                   result="#{requestScope.output}" );
                            gf.getEntityAttrs(endpoint="#{sessionScope.REST_URL}/configs/config/server-config/network-config/protocols/protocol/#{requestScope.output.data.properties.protocol}", valueMap="#{requestScope.schemeMap}");
                            setAttribute(key="scheme" value="http");
                            if(#{requestScope.schemeMap.securityEnabled}){
                                setAttribute(key="scheme", value="https");
                            }
                            setAttribute(key="url" value="#{requestScope.scheme}://#{request.serverName}:#{requestScope.portMap.port}");
                            setPageSessionAttribute(key="url2", value="$resource{i18n.restart.RestartContinue, #{requestScope.url}}")
                        />
                    </sun:staticText>
<f:verbatim>
                    </span></div></td>
                    </tr>
                </table>
            </td>
            <td>&nbsp;</td>
        </tr>
    </table>
</td>
</f:verbatim>