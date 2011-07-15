<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.

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

<sun:masthead id="Masthead" productImageURL="#{request.contextPath}/resource/community-theme/images/masthead-product_name_open.png" 
              style="border-width: 0px"
              productImageDescription="$resource{theme.productName}"
              productImageWidth="327"
              productImageHeight="23"
              userInfo="#{userName}"  userInfoLabel="$resource{i18n.masthead.userInfoLabel}"
              roleInfo="#{domainName}" roleInfoLabel="$resource{i18n.masthead.domainInfoLabel}"
              serverInfo="#{hostName}" serverInfoLabel="$resource{i18n.masthead.serverInfoLabel}"
              dateTime="$boolean{false}" notificationMsg="$attribute{null}">



    <!facet consoleLink>
        <sun:hyperlink id="homeLink" toolTip="$resource{i18n.homeLinkTooltip}" target="_top" text="$resource{i18n.masthead.Home}" url="#{request.contextPath}/common/index.jsf" />
    </facet>
    <!facet versionLink>
        <sun:hyperlink id="versionLink" toolTip="$resource{i18n.versionTooltip}" text="$resource{i18n.masthead.Version}"
            onClick="javascript: var versionWin = window.open('#{request.contextPath}/common/version.jsf','VersionWindow','scrollbars,resizable,width=800,height=740,top='+((screen.height - (screen.height/1.618)) - (500/2))+',left='+((screen.width-650)/2) ); versionWin.focus(); return false;" onKeyPress="javascript: return true;" />
    </facet>
    <!facet helpLink>
        <sun:button id="helpLink" rendered="true" toolTip="$resource{i18n.helpWindowTooltip}" target="help_window" text="$resource{i18n.masthead.Help}" onClick="admingui.help.launchHelp(this); return false;" />
    </facet>
    <!facet logoutLink>
        <sun:button id="logoutLink" rendered="#{showLogoutButton}" toolTip="$resource{i18n.logoutTooltip}" target="_top" text="$resource{i18n.masthead.Logout}"
            onClick="if (!confirm('$resource{i18n.msg.JS.confirmLogout}')) return false;" onKeyPress="javascript: return true;" url="/" >
                <!command
                    logout();
                    redirect("#{request.contextPath}/common/index.jsf");
                />
        </sun:button>
    </facet>
    <!facet statusArea>
        <sun:panelGroup id="statusPanel" separator="">
            "<span style="margin: 5px 5px 0px 5px">
                <sun:image id="alertImage" rendered="#{sessionScope.restartRequired}" alt="$resource{i18n.masthead.restartRequired}" icon="ALERT_WARNING_MEDIUM"/>
                <sun:hyperlink id="restartLink" rendered="#{sessionScope.restartRequired}"
                               style="color:white; margin-left: 5px;"
                               text="$resource{i18n.masthead.restartRequired}"
                               toolTip="$resource{i18n.masthead.restartStatusMsg}"
                               onMouseOver="window.status='$resource{i18n.masthead.restartStatusMsg}'; return true"
                               onMouseOut="window.status=''; return true"
                               onBlur="window.status=''; return true"
                               onFocus="window.status='$resource{i18n.masthead.restartStatusMsg}'; return true"
                               url="#{request.contextPath}/common/appServer/restart.jsf">
                </sun:hyperlink>
            "</span>
            <!afterCreate
                includeIntegrations(type="org.glassfish.admingui:mastheadStatusArea" root="$this{component}");
            />
        </sun:panelGroup>
    </facet>
</sun:masthead>
