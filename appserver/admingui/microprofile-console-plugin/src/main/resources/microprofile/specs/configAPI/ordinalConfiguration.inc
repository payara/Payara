    <sun:title id="propertyContentPage" title="$resource{i18n_microprofile.microprofile.specs.configuration.config.ordinal.pageTitle}" 
               helpText="$resource{i18n_microprofile.microprofile.specs.configuration.config.ordinal.pageTitleHelpText}" >
        <!facet pageButtonsTop>
        <sun:panelGroup id="topButtons">
            <sun:button id="saveButton"  text="$resource{i18n.button.Save}"
                        onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) 
                        submitAndDisable(this, '$resource{i18n.button.Processing}'); return false;" >
                      <!command
                        prepareSuccessfulMsg();
                        gf.restRequest(endpoint="#{pageSession.MICROPROFILE_CONFIG_URL}/set-config-ordinal" 
                        method="POST" attrs="#{pageSession.valueMap}");
                    /> 
            </sun:button>
        </sun:panelGroup>
        </facet>
    </sun:title>
   <sun:propertySheet id="propertySheet">
#include "/common/shared/configNameSection.inc"
  <sun:propertySheetSection id="ordinalProps">
        <sun:property id="ordinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18n_microprofile.config.ordinal.configuration.ordinal}"  
                      helpText="$resource{i18n_microprofile.config.ordinal.configuration.ordinalHelp}">
              <sun:textField id="endPoint" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.valueMap['ordinal']}"/>
        </sun:property>
        <sun:property id="sourceProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18n_microprofile.config.ordinal.configuration.source}"  
                      helpText="$resource{i18n_microprofile.config.ordinal.configuration.sourceHelp}">
            <sun:dropDown id="sourceNames" selected="#{pageSession.valueMap['source']}" 
                          labels={"application", "cluster", "config", "domain", "jndi", "module", "password", "secret", "server"} />
        </sun:property>
    </sun:propertySheetSection>
   </sun:propertySheet>