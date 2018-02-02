/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config;

import java.beans.PropertyVetoException;
import java.beans.PropertyChangeEvent;

/**
 * Interceptor interface to be notified of read/write operations on a ConfigBean.
 * Interceptor can be configured using the optional T interface.
 *
 * @Author Jerome Dochez
 */
public interface ConfigBeanInterceptor<T> {

    /**
     * Interceptor can usually be configured, allowing for customizing how
     * the interceptor should behave or do.
     *
     * @return interface implementing the configuration capability of this
     * interceptor
     */
    public T getConfiguration();

    /**
     * Notification that an attribute is about to be changed
     *
     * @param evt information about the forthcoming change
     * @throws PropertyVetoException if the change is unacceptable
     */
    public void beforeChange(PropertyChangeEvent evt) throws PropertyVetoException;

    /**
     * Notification that an attribute has changed
     *
     * @param evt information about the change
     * @param timestamp time of the change
     */
    public void afterChange(PropertyChangeEvent evt, long timestamp);

    /**
     * Notification of an attribute read
     *
     * @param source  object owning the attribute
     * @param xmlName name of the attribute
     * @param value value of the attribute
     */
    public void readValue(ConfigBean source, String xmlName, Object value);
}
