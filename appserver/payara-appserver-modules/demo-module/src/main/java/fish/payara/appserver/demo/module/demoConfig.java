/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.appserver.demo.module;

import javax.validation.constraints.Pattern;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

/**
 *
 * @author srai
 */
/*This covers how to add configuration data to a service.By adding configuration
 data I mean adding the ability for the service to store data about itself and 
 persist these to the Payara "domain.xml"*/
//Add @Configured annotation to define the interface as a configuration element
@Configured
public interface demoConfig extends ConfigBeanProxy, ConfigExtension {

    @Attribute
    /*@ to specify a regular expression to validate the attribute aganist. If a 
     parameter is passed that doesn't meet this expression. It will be rejected.*/
    @Pattern(regexp = "[a-zA-z]+")// Names can only contain letters
    public String getFristname();
    public void setFristname(String value);

    @Attribute(defaultValue = "Payara")
    @Pattern(regexp = "[a-zA-z]+")
    public String getSurname();
    public void setSurname(String value);
}
//With this done we need to edit of "asadmin" command to reflect these changes
