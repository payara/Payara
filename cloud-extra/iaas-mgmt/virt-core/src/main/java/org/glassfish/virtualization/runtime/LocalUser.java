/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.virtualization.runtime;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.virtualization.config.VirtUser;
import org.glassfish.virtualization.spi.OsInterface;
import javax.inject.Inject;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * represents this user running this process
 */
public class LocalUser implements VirtUser {

    @Inject
    OsInterface os;

    String authMethod=""; // not sure we will ever need another value.

    public static LocalUser myself(ServiceLocator injector) {
    	LocalUser retVal = new LocalUser();
    	injector.inject(retVal);
        return retVal;
    }

    private LocalUser() {
    }

    @Override
    public String getName() {
        return System.getProperty("user.name");
    }

    @Override
    public void setName(String name) {
        throw new RuntimeException("Cannot change local user name !");
    }

    @Override
    public String getUserId() {
        return os.userId();
    }

    @Override
    public void setUserId(String id) {
        throw new RuntimeException("Cannot change local user id !");
    }

    @Override
    public String getGroupId() {
        return os.groupId();
    }

    @Override
    public void setGroupId(String id) {
        throw new RuntimeException("Cannot change local serverPool id !");
    }

    @Override
    public String getPassword() {
        throw new RuntimeException("Cannot get local user password !");
    }

    @Override
    public void setPassword(String id) {
        throw new RuntimeException("Cannot change local user password !");
    }

    @Override
    public String getAuthMethod() {
        return authMethod;
    }

    @Override
    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    @Override
    public ConfigBeanProxy getParent() {
        return null;
    }

    @Override
    public <T extends ConfigBeanProxy> T getParent(Class<T> type) {
        return null;
    }

    @Override
    public <T extends ConfigBeanProxy> T createChild(Class<T> type) throws TransactionFailure {
        return null;
    }

    @Override
    public ConfigBeanProxy deepCopy(ConfigBeanProxy parent) throws TransactionFailure {
        return null;
    }
}
