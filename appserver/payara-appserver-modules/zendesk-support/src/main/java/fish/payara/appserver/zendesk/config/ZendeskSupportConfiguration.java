/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.appserver.zendesk.config;

import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

/**
 *
 * @author Andrew Pielage
 */
@Configured(name="zendesk-support-configuration")
public interface ZendeskSupportConfiguration extends ConfigBeanProxy, ConfigExtension {
    
    @Attribute(required = true)
    String getEmailAddress();
    public void setEmailAddress(String emailAddress);
}
