<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.

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

 function initClassname(option, createFlag) {
    var selectedOption = getSelectedValueFromForm(document.forms['form1'], 'classnameOption');
    enableClassnameFields(option, createFlag);
    if (option == 'input'){ 
        #{themeJavascript.JS_PREFIX}.radiobutton.setChecked('#{rbPropId}:optB', true);  
    }else{  
        #{themeJavascript.JS_PREFIX}.radiobutton.setChecked('#{rbPropId}:optA', true);  
    }  
  }

 function enableClassnameFields(opt, createFlag){
    if(opt == 'input') {
	enableComponent('#{classnameTextId}', 'text');
        if (createFlag){
            getTextElement('#{classnameTextId}').focus();
        }
	#{themeJavascript.JS_PREFIX}.dropDown.setDisabled('#{classnameDropdownId}', true); 
        showDisplay('none'); 
        document.getElementById('form1:option').value='input'; 
    }else{
        //We need to set a timeout to delay the call to getTextElement inside disable component.
        //otherwise getTextElement will always return null, causing JS error.
	window.setTimeout("disableComponent('#{classnameTextId}', 'text')", 1); 
	#{themeJavascript.JS_PREFIX}.dropDown.setDisabled('#{classnameDropdownId}', false); 
        showDisplay(''); 
        document.getElementById('form1:option').value='predefine'; 
    }
 }

function checkClassname() {
    var opt = getSelectedValueFromForm(document.forms['form1'], 'classnameOption');
    if (opt == 'predefine') {
	type = getSelectElement('#{classnameDropdownId}').value;
	if (type.match('FileRealm') != null) {
	    return (checkRequired('form1:fileSection:jaax:jaax', '#{reqMsg}')  &&
		    checkRequired('form1:fileSection:keyFile:keyFile',  '#{reqMsg}')  )
	} else if (type.match('LDAPRealm') != null) {
	    return ( checkRequired(id="form1:ldapSection:jaax:jaax" , '#{reqMsg}') &&
		checkRequired(id="form1:ldapSection:directory:directory" , '#{reqMsg}')  &&
		checkRequired(id="form1:ldapSection:baseDn:baseDn" , '#{reqMsg}'))
	} else if (type.match('SolarisRealm') != null) {
	    return checkRequired(id="form1:solarisSection:jaax:jaax" , '#{reqMsg}') ;
	} else if (type.match('JDBCRealm') != null) {
	    return checkRequired(id="form1:jdbcSection:jaax:jaax" , '#{reqMsg}' ) &&
		checkRequired(id="form1:jdbcSection:jndi:jndi" , '#{reqMsg}' )  &&
		checkRequired(id="form1:jdbcSection:userTable:userTable" , '#{reqMsg}' )  &&
		checkRequired(id="form1:jdbcSection:userName:userName", '#{reqMsg}' )  &&
		checkRequired(id="form1:jdbcSection:pswd:pswd" , '#{reqMsg}' )  &&
		checkRequired(id="form1:jdbcSection:groupTable:groupTable" , '#{reqMsg}' )  &&
		checkRequired(id="form1:jdbcSection:groupName:groupName" , '#{reqMsg}' ) &&
                checkRequired(id="form1:jdbcSection:pswdEnc:pswdEnc" , '#{reqMsg}' );
	} else if (type.match('PamRealm') != null) {
	    return checkRequired(id="form1:pamSection:jaax:jaax" , '#{reqMsg}') ;
	} else if (type == '') {
	    return showAlert('$resource{i18n.msg.JS.realm.needClassName}');
	}
	// No special options to check for this type... continue
	return true;
    } else if (opt == 'input' ) {
	cla = getTextElement('#{classnameTextId}').value;
	if (checkForSelectedValue('#{classnameTextId}')) {
	    return true;
	} else {
	    return showAlert('$resource{i18n.msg.JS.realm.needClassName}');
	}
    }
}

function showDisplay(testtype) {
    document.getElementById('#{fileSectionId}').style.display = 'none'; 
    document.getElementById('#{ldapSectionId}').style.display = 'none'; 
    document.getElementById('#{jdbcSectionId}').style.display = 'none'; 
    document.getElementById('#{certSectionId}').style.display = 'none'; 
    document.getElementById('#{solarisSectionId}').style.display = 'none';
    document.getElementById('#{pamSectionId}').style.display = 'none';
    
    if (testtype.match('none')){  
        return;  
    }  
    type = getSelectElement('#{classnameDropdownId}').value;
    rtype = type.match('FileRealm');  
    if (rtype != null){  
        document.getElementById('#{fileSectionId}').style.display = 'block';   
        return;  
    }  
    rtype = type.match('LDAPRealm');  
    if (rtype != null){  
        document.getElementById('#{ldapSectionId}').style.display = 'block';   
        return;  
    }  
    rtype = type.match('SolarisRealm');  
    if (rtype != null){  
        document.getElementById('#{solarisSectionId}').style.display = 'block';   
        return;  
    }  
    rtype = type.match('JDBCRealm');  
    if (rtype != null){  
        document.getElementById('#{jdbcSectionId}').style.display = 'block';   
        return;  
    }  
    rtype = type.match('CertificateRealm');  
    if (rtype != null){  
        document.getElementById('#{certSectionId}').style.display = 'block';   
        return;  
    }
    rtype = type.match('PamRealm');
    if (rtype != null){
        document.getElementById('#{pamSectionId}').style.display = 'block';
        return;
    }
}    

</script>
</f:verbatim>

