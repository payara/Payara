<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.

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

<f:verbatim>
<div style="height: 435px;background-image: url(#{request.contextPath}/resource/community-theme/images/login-backimage-open.png);
    background-repeat:no-repeat;background-position:left top; width: 720px; margin: auto;">
    <div style="width: 460px; padding-top: 160px; margin-left: 310px;">
</f:verbatim>
        <sun:image url="#{request.contextPath}/resource/community-theme/images/login-product_name_open.png" alt="$resource{theme.productName}" height="42" width="329" >
        <!beforeCreate
                setResourceBundle(key="theme" bundle="org.glassfish.admingui.community-theme.Strings");
            />
        </sun:image>
<f:verbatim>
        <form method="POST" class="form" name="loginform" action="j_security_check">
        <table role="presentation">
        <tr>
            <td><label for="Login.username" style="font-weight: bold;">$resource{i18n.common.username}</label></td>
            <td><input type="text" name="j_username" id="Login.username" tabindex="1" value="#{byPassLogin ? pageSession.loginUser : ''}"></td>
        </tr>
        <tr>
            <td><label for="Login.password" style="font-weight: bold;">$resource{i18n.common.password}</label>
            <td><input type="password" name="j_password" id="Login.password" tabindex="2">
        <tr>
            <td colspan="2" align="center">
                <input type="submit" class="Btn1"
                    value="$resource{i18n.loginPageTitle}"
                    title="#{loginButtonTooltip}" tabindex="3"
                    onmouseover="javascript: if (this.disabled==0) this.className='Btn1Hov'"
                    onmouseout="javascript: if (this.disabled==0) this.className='Btn1'"
                    onblur="javascript: if (this.disabled==0) this.className='Btn1'"
                    onfocus="javascript: if (this.disabled==0) this.className='Btn1Hov'"
                    name="loginButton" id="loginButton">
        	    <input type="hidden" name="loginButton.DisabledHiddenField" value="true" />
    	    </td>
	    </tr>
	    </table>
        </form>
    </div>
</div>
</f:verbatim>
