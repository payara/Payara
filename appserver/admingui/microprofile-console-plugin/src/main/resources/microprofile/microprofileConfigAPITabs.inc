
<sun:tabSet id="microprofileConfigTabs" immediate="$boolean{true}" selected="#{sessionScope.microprofileConfigTabs}">
    <sun:tab id="microProfileConfigOrdinalTab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.configOrdinalTab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.configOrdinalTab} Tab" >
        <!command
          setSessionAttribute(key="microprofileConfigTabs" value="microProfileConfigOrdinalTab");
          gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/configConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
    <sun:tab id="microProfileConfigPropertyTab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.configPropertyTab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.configPropertyTab} Tab" >
        <!command
          setSessionAttribute(key="microprofileConfigTabs" value="microProfileConfigPropertyTab");
          gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/configAPI/propertyConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
    <sun:tab id="microProfileConfigSecretsDirectoryTab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.configSecretsDirectoryTab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.configSecretsDirectoryTab} Tab" >
        <!command
          setSessionAttribute(key="microprofileConfigTabs" value="microProfileConfigSecretsDirectoryTab");
          gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/configAPI/secretsDirectoryConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
</sun:tabSet>