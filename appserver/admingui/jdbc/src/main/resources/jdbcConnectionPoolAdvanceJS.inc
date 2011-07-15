<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.

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
    <script type="text/javascript">
        function disableTableField(selStr) {
            var val = document.getElementById("$pageSession{valMethod}").value;
            if (val == 'table'){
                var isConnValidation = document.getElementById("$pageSession{conValidationId}").checked;
                if (isConnValidation) {
                    disableRadioButtons(false);
                    var dropDownVal = document.getElementById("$pageSession{dropdownTextId}").value;
                    if (dropDownVal == '') {
                        enableRadioButtonFields('text');
                    } else {
                        if(dropDownVal == selStr) {
                            document.getElementById('$pageSession{textId}').value='';
                            enableRadioButtonFields('dropdown');
                        }
                        else if('$pageSession{myOption}' == 'dropdown') {
                            enableRadioButtonFields('dropdown');
                        } else {
                            enableRadioButtonFields('text');
                        }
                    }
                    disableComponent('$pageSession{validCNTextField}', 'text');
                } else {
                    disableComponent('$pageSession{dropdownTextId}', 'select');
                    disableTextComponent('$pageSession{textId}');
                    disableComponent('$pageSession{validCNTextField}', 'text');
                    disableRadioButtons(true);
                }
            }else
            if (val == 'custom-validation') {
                disableComponent('$pageSession{dropdownTextId}', 'select');
                disableTextComponent('$pageSession{textId}');
                disableRadioButtons(true);
                enableComponent('$pageSession{validCNTextField}', 'text');
            }else{
                disableComponent('$pageSession{dropdownTextId}', 'select');
                disableTextComponent('$pageSession{textId}');
                disableComponent('$pageSession{validCNTextField}', 'text');
                disableRadioButtons(true);
            }
        }
        function disableTextComponent(textId) {
            var textVal = document.getElementById(textId).value;
            disableComponent('$pageSession{textId}', 'text');
            document.getElementById('$pageSession{textId}').value=textVal;
        }
        function getValidationTableInfo(map, selStr) {
            var b= map.toString();
            var methodVal = document.getElementById("$pageSession{valMethod}").value;
            if (methodVal == 'table'){
                var isConnValidation = document.getElementById("$pageSession{conValidationId}").checked;
                if (isConnValidation) {
                    admingui.ajax.invoke("gf.restRequest", {
                        endpoint:"#{sessionScope.REST_URL}/resources/get-validation-table-names",
                        attrs:b,
                        method:"get",
                        result:""}, function(result) {
                        processValidationTableNamesResult(result, selStr);
                    }, 3, false);
                }
            }
        }

        function processValidationTableNamesResult(result, selStr) {
            if(result.result.data != undefined){
                var exitCode = result.result.data.exit_code.toString();
                if (exitCode == "SUCCESS") {
                    var res = result.result.data.extraProperties.toString();                 
                    if (res != null) {
                        var searchStr = "validationTableNames=";
                        var index = res.indexOf(searchStr);
                        if (index != -1) {
                            var tableNamesStr = res.substring(index + searchStr.length + 1, (res.length) - 2);
                            setTableOptions(tableNamesStr, selStr);
                        }
                    }
                }
            }
        }
        function setTableOptions(tableNamesStr, selStr) {
            var strs = tableNamesStr.split(", ");
            for (var i = 0; i < strs.length; i++){
                var optn;
                optn = document.createElement("option");
                optn.value = optn.text = strs[i];
                if(selStr == strs[i]) {
                    optn.selected = true;
                }
                document.getElementById("$pageSession{dropdownTextId}").options.add(optn);
            }            
        }
    </script>
</f:verbatim>   
