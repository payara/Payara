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

<!-- grizzly/transportsTable.inc -->

<sun:table id="configs" title="#{tableTitle}">

#include "/common/shared/listTableButtons.inc"
    
   <sun:tableRowGroup id="rowGroup1" selected="#{td.value.selected}" data={"$attribute{listOfRows}"} sourceVar="td">
     <!afterCreate
         getClientId(component="$this{component}" clientId=>$page{tableRowGroupId});
        />
    <sun:tableColumn headerText="$resource{i18n.common.SelectHeader}" selectId="select" rowHeader="$boolean{false}" id="col0">
            <sun:checkbox id="select" selected="#{td.value.selected}" selectedValue="$boolean{true}" />
    </sun:tableColumn>    

    <sun:tableColumn headerText="$resource{i18n_web.grizzly.transport.nameCol}" sort="name"  rowHeader="$boolean{true}" id="col1">
        <sun:hyperlink id="link" text="#{td.value.name}" 
            url="#{pageSession.editLink}&name=#{td.value.name}" />
    </sun:tableColumn>

    <sun:tableColumn headerText="$resource{i18n_web.grizzly.transport.byteBufferCol}" sort="byteBuffer"   rowHeader="$boolean{true}" id="col3">
        <staticText id="byteBuffer" value="#{td.value.byteBuffer}" />
    </sun:tableColumn>

    <sun:tableColumn headerText="$resource{i18n_web.grizzly.transport.classnameCol}"  sort="classname" rowHeader="$boolean{true}" id="pro">
        <staticText id="classname" value="#{td.value.classname}" />
    </sun:tableColumn>

  </sun:tableRowGroup>
  
</sun:table>
