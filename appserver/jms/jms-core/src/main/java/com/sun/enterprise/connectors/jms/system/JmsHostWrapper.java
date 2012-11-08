/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package com.sun.enterprise.connectors.jms.system;

import java.beans.PropertyVetoException;

import com.sun.enterprise.connectors.jms.config.JmsHost;
import org.jvnet.hk2.config.types.Property;
import java.util.List;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.TransactionFailure;
/** A wrapper class for the JmsHost serverbean since you cannot clone JmsHost
 * elements without being a part of a transaction
 * This class is primaritly used to create copies of the default jmshost element
 * during auto-clustering
*/

public class JmsHostWrapper implements JmsHost {
    private String name;
    public String getName(){
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(String value) throws PropertyVetoException
    {
        name = value;
    }

    /**
     * Gets the value of the host property.
     *
     * @return possible object is
     *         {@link String }
     */
    private String host;
    public String getHost(){
        return host;
    }

    /**
     * Sets the value of the host property.
     *
     * ip V6 or V4 address or hostname
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setHost(String value) throws PropertyVetoException{
        host = value;
    }

    /**
     * Gets the value of the port property.
     *
     * Port number used by the JMS service
     *
     * @return possible object is
     *         {@link String }
     */
    private String port = "7676";
    public String getPort()
    {
        return port;
    }

    /**
     * Sets the value of the port property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setPort(String value) throws PropertyVetoException{
        //only set the value if it is not null. Incase it is null, use the default value
        if (value != null)
            port = value;
    }

    /**
     * Gets the value of lazyInit property
     *
     * if false, this listener is started during server startup
     *
     * @return true or false
     */
    private String lazyInit;
    public String getLazyInit(){
        return lazyInit;
    }

    /**
     * Sets the value of lazyInit property
     *
     * Specify is this listener should be started as part of server startup or not
     *
     * @param value true if the listener is to be started lazily; false otherwise
     */
    public void setLazyInit(String value){
        lazyInit = value;
    }

    /**
     * Gets the value of the adminUserName property.
     *
     * Specifies the admin username
     *
     * @return possible object is
     *         {@link String }
     */

    private String adminUserName;
    public String getAdminUserName(){
        return adminUserName;
    }

    /**
     * Sets the value of the adminUserName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setAdminUserName(String value) throws PropertyVetoException
    {
        adminUserName = value;
    }

    /**
     * Gets the value of the adminPassword property.
     *
     * Attribute specifies the admin password
     *
     * @return possible object is
     *         {@link String }
     */
     private String adminPassword;
    public String getAdminPassword(){
           return  adminPassword;
    }

    public void setAdminPassword(String value) throws PropertyVetoException
    {
        adminPassword = value;
    }

    public List<Property> getProperty()
    {
          return null;
    }
    public ConfigBeanProxy deepCopy(ConfigBeanProxy proxy)
    {

        JmsHostWrapper clone = new JmsHostWrapper();
        if(! (proxy instanceof JmsHost)) return null;

        JmsHost host = (JmsHost)proxy;
        try{
        clone.setHost(host.getHost());
        clone.setPort(host.getPort());
        clone.setName(host.getName());
        clone.setAdminPassword(host.getAdminPassword());
        clone.setAdminUserName(host.getAdminUserName());
        }catch (PropertyVetoException e)
        {
            //todo : handle this exception
        }

        return null;
    }
    /* The following methods are dummy methods required as part of the interface implentation.*/

    public <T extends ConfigBeanProxy> T createChild(Class<T> type) throws TransactionFailure
    {
        return null;
    }
    public ConfigBeanProxy getParent()
    {
        return null;
    }
    public <T extends ConfigBeanProxy> T getParent(Class<T> type){
        return null;
    }
    public void injectedInto(Object target)    {
        //do nothing
    }
    public String getPropertyValue (String str, String str2){
        return null;
    }
    public String getPropertyValue (String str){
        return null;
    }
     public Property getProperty (String str){
        return null;
    }
}
