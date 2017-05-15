/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.micro.event;

import fish.payara.micro.data.InstanceDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Properties;
import java.util.Set;

/**
 * Interface that classes must implement to be used as Clustered Events
 * 
 * @author Steve Millidge
 */
public interface PayaraClusteredCDIEvent extends Serializable {

    /**
     * Return the Instance Descriptor of the sender
     * @return 
     */
    InstanceDescriptor getId();

    /**
     * Returns the Instance Descriptor of the Sender
     * @return 
     */
    InstanceDescriptor getInstanceDescriptor();

    /**
     * Returns the Payload of the object. This should be deserialized before
     * being returned
     * @return
     * @throws IOException If a problem occurs during Deserialization
     * @throws ClassNotFoundException If a problem occurs during Deserialization
     */
    Serializable getPayload() throws IOException, ClassNotFoundException;

    /**
     * Returns the set of properties in the event
     * @return 
     */
    Properties getProperties();

    /**
     * Returns the property value with the specified name
     * @param name Name of the property to return
     * @return 
     */
    String getProperty(String name);

    /**
     * Returns the property value with the specified name
     * @param name Name of the property to return
     * @param defaultValue Value to be returned if the property is not present
     * @return 
     */
    String getProperty(String name, String defaultValue);

    /**
     * Returns true if the event was sent from the same JVM
     * @return 
     */
    boolean isLoopBack();

    /**
     * Set the Intstance Descriptor
     * @param id 
     */
    void setId(InstanceDescriptor id);

    /**
     * Set to true if the event should be sent to listeners in the same JVM
     * @param loopBack 
     */
    void setLoopBack(boolean loopBack);

    /**
     * Sets a property in the event
     * @param name Name of the property
     * @param value THe value of the property
     */
    void setProperty(String name, String value);
    
    /**
     * Returns the set of qualifiers on the event
     * @return 
     */
    public Set<Annotation> getQualifiers();
    
    /**
     * Adds the set of qualifiers to the event
     * @param qualifiers 
     */
    public void addQualifiers(Set<Annotation> qualifiers);
}
