
<sun:tabSet id="microprofileTabs" immediate="$boolean{true}" selected="#{sessionScope.microprofileTabs}">
    <sun:tab id="microprofileConfigTab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.configTab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.configTab} Tab" >
        <!command
        setSessionAttribute(key="microprofileTabs" value="microprofileConfigTab");
        gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/configConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
    <sun:tab id="microprofileFaultToleranceTab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.faultToleranceTab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.faultToleranceTab} Tab" >
        <!command
        setSessionAttribute(key="microprofileTabs" value="microprofileFaultToleranceTab");
        gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/faultToleranceConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
   <sun:tab id="microprofileHealthCheckTab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.healthCheckTab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.healthCheckTab} Tab" >
        <!command
        setSessionAttribute(key="microprofileTabs" value="microprofileHealthCheckTab");
        gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/healthCheckConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
   <sun:tab id="microprofileMetricsTab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.metricsTab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.metricsTab} Tab" >
        <!command
        setSessionAttribute(key="microprofileTabs" value="microprofileMetricsTab");
        gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/metricsConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
   <sun:tab id="microprofileOpenAPITab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.openAPITab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.openAPITab} Tab" >
        <!command
        setSessionAttribute(key="microprofileTabs" value="microprofileOpenAPITab");
        gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/openAPIConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
   <sun:tab id="microprofileOpenTracingTab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.openTracingTab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.openTracingTab} Tab" >
        <!command
        setSessionAttribute(key="microprofileTabs" value="microprofileOpenTracingTab");
        gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/openTracingConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
</sun:tabSet>