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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 *
 * Portions Copyright [2017] Payara Foundation and/or affiliates
 */

package org.glassfish.resources.javamail.beans;

import com.sun.enterprise.deployment.interfaces.MailResourceIntf;
import org.glassfish.resources.api.JavaEEResource;
import org.glassfish.resources.api.JavaEEResourceBase;
import org.glassfish.resourcebase.resources.api.ResourceInfo;

//Renamed from MailResource to avoid confusion with org.glassfish.resources.javamail.config.MailResource
/**
 * Resource info for MailBean.
 * IASRI #4650786
 *
 * @author James Kong
 */
public class MailBean extends JavaEEResourceBase implements MailResourceIntf {

    private String resType_;
    private String factoryClass_;

    private String storeProtocol_;
    private String storeProtocolClass_;
    private String transportProtocol_;
    private String transportProtocolClass_;
    private String mailHost_;
    private String username_;
    private String password_;
    private String mailFrom_;
    private boolean auth_;
    private boolean debug_;

    public MailBean(ResourceInfo resourceInfo) {
        super(resourceInfo);
    }

    @Override
    protected JavaEEResource doClone(ResourceInfo resourceInfo) {
        MailBean clone = new MailBean(resourceInfo);
        clone.setResType(getResType());
        clone.setFactoryClass(getFactoryClass());
        return clone;
    }

    //unused implementation ie., com.sun.enterprise.deployment.MailConfiguration uses this, but is unused in-turn.
    @Override
    public String getName() {
        return getResourceInfo().getName();
    }

    @Override
    public int getType() {
        return JavaEEResource.MAIL_RESOURCE;
    }

    @Override
    public String getResType() {
        return resType_;
    }

    public void setResType(String resType) {
        resType_ = resType;
    }

    @Override
    public String getFactoryClass() {
        return factoryClass_;
    }

    public void setFactoryClass(String factoryClass) {
        factoryClass_ = factoryClass;
    }

    @Override
    public String getStoreProtocol() {
        return storeProtocol_;
    }

    public void setStoreProtocol(String storeProtocol) {
        storeProtocol_ = storeProtocol;
    }

    @Override
    public String getStoreProtocolClass() {
        return storeProtocolClass_;
    }

    public void setStoreProtocolClass(String storeProtocolClass) {
        storeProtocolClass_ = storeProtocolClass;
    }

    @Override
    public String getTransportProtocol() {
        return transportProtocol_;
    }

    public void setTransportProtocol(String transportProtocol) {
        transportProtocol_ = transportProtocol;
    }

    @Override
    public String getTransportProtocolClass() {
        return transportProtocolClass_;
    }

    public void setTransportProtocolClass(String transportProtocolClass) {
        transportProtocolClass_ = transportProtocolClass;
    }

    @Override
    public String getMailHost() {
        return mailHost_;
    }

    public void setMailHost(String mailHost) {
        mailHost_ = mailHost;
    }

    @Override
    public String getUsername() {
        return username_;
    }

    public void setUsername(String username) {
        username_ = username;
    }
    
    @Override
    public String getPassword() {
        return password_;
    }
    
    public void setPassword(String password) {
        password_ = password;
    }
    
    @Override
    public boolean getAuth(){
        return auth_;
    }
    
    public void setAuth(boolean auth){
        auth_ = auth;
    }
    
    @Override
    public String getMailFrom() {
        return mailFrom_;
    }

    public void setMailFrom(String mailFrom) {
        mailFrom_ = mailFrom;
    }

    @Override
    public boolean isDebug() {
        return debug_;
    }

    public void setDebug(boolean debug) {
        debug_ = debug;
    }

    @Override
    public String toString() {
        return "< Mail Resource : " + getResourceInfo() + " , " + getResType() + "... >";
    }
}
