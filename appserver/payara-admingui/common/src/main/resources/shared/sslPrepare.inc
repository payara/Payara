<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.

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

<!-- sslPrepare.inc  -->

gf.checkIfEndPointExist(endpoint="#{pageSession.selfUrl}" exists="#{requestScope.exist}")
if (#{requestScope.exist}){
    setPageSessionAttribute(key="edit" value="#{true}" )
    gf.getEntityAttrs(endpoint="#{pageSession.selfUrl}.json" valueMap="#{pageSession.valueMap}");
    convertToDifferentCiphersGroup(ciphers="#{pageSession.valueMap['ssl3TlsCiphers']}"
        CommonCiphersList="#{pageSession.selectedCommon}"
        EphemeralCiphersList="#{pageSession.selectedEphemeral}"
        OtherCiphersList="#{pageSession.selectedOther}"
        EccCiphersList="#{pageSession.selectedEcc}")

}
if (! #{requestScope.exist}){
    setPageSessionAttribute(key="edit" value="#{false}" )
    <!-- this <ssl> does not exists, we cannot find a way to get the default value, so hard coded here for now.
         TODO: REST need to provide an endpoint to get the default value.
      -->
    createMap(result="#{pageSession.valueMap}");
    mapPut(map="#{pageSession.valueMap}" key="ssl3Enabled" value="true");
    mapPut(map="#{pageSession.valueMap}" key="tlsEnabled" value="true");
    mapPut(map="#{pageSession.valueMap}" key="trustMaxCertLength" value="5");

    convertToDifferentCiphersGroup(ciphers="#{pageSession.valueMap['ssl3TlsCiphers']}"
        CommonCiphersList="#{pageSession.selectedCommon}"
        EphemeralCiphersList="#{pageSession.selectedEphemeral}"
        OtherCiphersList="#{pageSession.selectedOther}"
        EccCiphersList="#{pageSession.selectedEcc}")
}

//set the following for including buttons.inc
setPageSessionAttribute(key="convertToFalseList" value={"ssl3Enabled" "tlsEnabled" "clientAuthEnabled" })
setPageSessionAttribute(key="skipAttrsList", value={"sslInactivityTimeout"});
setPageSessionAttribute(key="showDefaultButton" value="#{false}" )
setPageSessionAttribute(key="hasPropertySheet" value="#{true}" )
setPageSessionAttribute(key="hasPropertyTable" value="#{false}" )
