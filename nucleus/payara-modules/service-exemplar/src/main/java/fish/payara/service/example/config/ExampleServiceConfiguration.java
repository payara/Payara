/*
 * Copyright (c) 2016 Payara Foundation. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
  */
package fish.payara.service.example.config;

import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

/**
 * This class is an example configuration class used to configure the service
 * ConfigExtension means this class hooks itself under the config tab for a 
 * specific configuration in the domain.xml
 * @author steve
 */
@Configured(name="example-service-configuration") // name is optional
public interface ExampleServiceConfiguration  extends ConfigBeanProxy, ConfigExtension {
    
    // says this value will be injected from an XML attribute for the example-service tag
    // Bean validation annotations can also be added
    @Attribute(required = false, defaultValue = "Hello World")
    String getMessage();
    public void setMessage(String message);
    
}
