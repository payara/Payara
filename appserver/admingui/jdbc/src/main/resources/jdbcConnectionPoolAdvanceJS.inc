<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.

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
        function disableTableField(selStr, selValidCNVal) {
            var val = document.getElementById("$pageSession{valMethod}").value;
            var isConnValidation = document.getElementById("$pageSession{conValidationId}").checked;
            if (isConnValidation && val == 'table'){
                disableAllJdbcRadioButtons(false, 'table');
                var index = findSelectOptionIndex(document.getElementById("$pageSession{tableDropdownTextId}"), selStr);
                var optionsExists = (document.getElementById("$pageSession{tableDropdownTextId}").options.length > 0);
                var isSelStrExists = (selStr != null) && (selStr != '') && (isWhitespace(selStr) == false);
                if (index == undefined || index == -1) {
                    if (isSelStrExists == true) {
                        enableJdbcRadioButtons('text', 'table');
                    }
                    else {
                        if (optionsExists == true) {
                            enableJdbcRadioButtons('dropdown', 'table');
                        } else {
                            enableJdbcRadioButtons('text', 'table');
                        }
                    }
                } else {
                    if(index >= 0) {
                        document.getElementById('$pageSession{tableTextId}').value='';
                        enableJdbcRadioButtons('dropdown', 'table');
                    }
                    else if('$pageSession{myTableOption}' == 'dropdown') {
                        enableJdbcRadioButtons('dropdown', 'table');
                    } else {
                        enableJdbcRadioButtons('text', 'table');
                    }
                }
                enableBtnComponent('$pageSession{tableNamesButtonId}');
                disableComponent('$pageSession{validCNDropdownTextId}', 'select');
                disableTextComponent('$pageSession{validCNTextId}');
                disableAllJdbcRadioButtons(true, 'validCN');
            }else if (isConnValidation && val == 'custom-validation') {
                disableAllJdbcRadioButtons(false, 'validCN');
                disableBtnComponent('$pageSession{tableNamesButtonId}');
                var dropDownVal = document.getElementById("$pageSession{validCNDropdownTextId}").value;
                if (dropDownVal == '') {
                    enableJdbcRadioButtons('text', 'validCN');
                } else {
                    if(dropDownVal == selValidCNVal) {
                        document.getElementById('$pageSession{validCNTextId}').value='';
                        enableJdbcRadioButtons('dropdown', 'validCN');
                    }
                    else if('$pageSession{myValidCNOption}' == 'dropdown') {
                        enableJdbcRadioButtons('dropdown', 'validCN');
                    } else {
                        enableJdbcRadioButtons('text', 'validCN');
                    }
                }
                disableComponent('$pageSession{tableDropdownTextId}', 'select');
                disableTextComponent('$pageSession{tableTextId}');
                disableAllJdbcRadioButtons(true, 'table');
            }else{
                disableComponent('$pageSession{tableDropdownTextId}', 'select');
                disableBtnComponent('$pageSession{tableNamesButtonId}');
                disableTextComponent('$pageSession{tableTextId}');
                disableAllJdbcRadioButtons(true, 'table');
                disableComponent('$pageSession{validCNDropdownTextId}', 'select');
                disableTextComponent('$pageSession{validCNTextId}');
                disableAllJdbcRadioButtons(true, 'validCN');
            }
        }
        function disableTextComponent(textId) {
            var textVal = document.getElementById(textId).value;
            disableComponent(textId, 'text');
            document.getElementById(textId).value=textVal;
        }
        function getDropdownInfo(tableAttrs, validCNAttrs, tableVal, validCNVal) {
            var isConnValidation = document.getElementById("$pageSession{conValidationId}").checked;
            var methodVal = document.getElementById("$pageSession{valMethod}").value;
            if (isConnValidation && methodVal == 'custom-validation'){
                var c= validCNAttrs.toString();
                admingui.ajax.invoke("gf.restRequest", {
                    endpoint:"#{sessionScope.REST_URL}/resources/get-validation-class-names",
                    attrs:c,
                    method:"get",
                    result:""}, function(result) {
                    processResult(result, validCNVal, "validationClassNames=", "$pageSession{validCNDropdownTextId}");
                }, 3, false);
            }
        }

        function getValidationTableNames(tableAttrs, tableVal) {
            var b= tableAttrs.toString();
            var size = document.getElementById("$pageSession{tableDropdownTextId}").options.length;
            if (size < 1) {
                admingui.ajax.invoke("gf.restRequest", {
                    endpoint:"#{sessionScope.REST_URL}/resources/get-validation-table-names",
                    attrs:b,
                    method:"get",
                    result:""}, function(result) {
                    processResult(result, tableVal, "validationTableNames=", "$pageSession{tableDropdownTextId}");
                }, 3, false);
            }
        }

        function processResult(result, selStr, searchStr, dropdownId) {
            if(result.result.data != undefined){
                var exitCode = result.result.data.exit_code.toString();
                if (exitCode == "SUCCESS") {
                    var res = result.result.data.extraProperties.toString();                 
                    if (res != null) {
                        var index = res.indexOf(searchStr);
                        if (index != -1) {
                            var lastIndex = res.indexOf("]", index + searchStr.length + 1);
                            var tableNamesStr = res.substring(index + searchStr.length + 1, lastIndex);
                            setDropdownOptions(tableNamesStr, selStr, dropdownId);
                        }
                    }
                }
            }
        }
        function setDropdownOptions(tableNamesStr, selStr, dropdownId) {
            var strs = tableNamesStr.split(", ");
            for (var i = 0; i < strs.length; i++){
                var optn;
                optn = document.createElement("option");
                optn.value = optn.text = strs[i];
                if(selStr == strs[i]) {
                    optn.selected = true;
                }
                document.getElementById(dropdownId).options.add(optn);
            }            
        }
        function disableAllJdbcRadioButtons(opt, type) {
            if (type=='table') {
                disableTwoRadioButtons(opt, '$pageSession{tablePropId}', '$pageSession{tableOptionId}');
            } else {
                disableTwoRadioButtons(opt, '$pageSession{validCNPropId}', '$pageSession{validCNOptionId}');
            }
        }
        function enableJdbcRadioButtons(opt, type) {
            if (type=='table') {
                enableRadioButtons(opt, '$pageSession{tableTextId}', '$pageSession{tableDropdownTextId}', '$pageSession{tablePropId}', '$pageSession{tableOptionId}');
            } else {
                enableRadioButtons(opt, '$pageSession{validCNTextId}', '$pageSession{validCNDropdownTextId}', '$pageSession{validCNPropId}', '$pageSession{validCNOptionId}');
            }
        }
        function enableRadioButtons(opt, textId, dropdownId, propId, optionId){
            if(opt == 'dropdown') {
                var textVal = document.getElementById(textId).value;
                disableComponent(textId, 'text');
                document.getElementById(textId).value=textVal;
                enableComponent(dropdownId, 'select');
                #{themeJavascript.JS_PREFIX}.radiobutton.setChecked(propId+':optB', false);
                #{themeJavascript.JS_PREFIX}.radiobutton.setChecked(propId+':optA', true);
                document.getElementById(optionId).value='dropdown';
            }else{
                enableComponent(textId, 'text');
                disableComponent(dropdownId, 'select');
                getTextElement(textId).focus();
                #{themeJavascript.JS_PREFIX}.radiobutton.setChecked(propId+':optB', true);
                #{themeJavascript.JS_PREFIX}.radiobutton.setChecked(propId+':optA', false);
                document.getElementById(optionId).value='text';
            }         
      }
      function disableTwoRadioButtons(opt, propId, optionId){
          #{themeJavascript.JS_PREFIX}.radiobutton.setDisabled(propId+':optA', opt);
          #{themeJavascript.JS_PREFIX}.radiobutton.setDisabled(propId+':optB', opt);
          document.getElementById(optionId).value='';
      }
    </script>
</f:verbatim>   
