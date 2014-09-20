/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.base;

import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;

import org.glassfish.admin.amx.annotation.ManagedOperation;
import org.glassfish.admin.amx.annotation.ManagedAttribute;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;

/**
Interface for a sample MBean , used as target for sample and test code.
Various Attributes of varying types are made available for testing.
 */
@Taxonomy(stability = Stability.NOT_AN_INTERFACE)
@AMXMBeanMetadata(singleton = true, globalSingleton = true, leaf = true, immutableMBeanInfo=false)
public interface Sample extends AMXProxy
{
    /**
    The type of Notification emitted by emitNotification().
     */
    public static final String SAMPLE_NOTIFICATION_TYPE = "Sample";

    /**
    The key to access user data within the Map obtained from Notification.getUserData().
     */
    public static final String USER_DATA_KEY = "UserData";

    /**
    Emit 'numNotifs' notifications of type
    SAMPLE_NOTIFICATION_TYPE at the specified interval.

    @param data arbitrary data which will be placed into the Notification's UserData field.
    @param numNotifs number of Notifications to issue >= 1
    @param intervalMillis interval at which Notifications should be issued >= 0
     */
    @ManagedOperation(impact = MBeanOperationInfo.ACTION)
    public void emitNotifications(final Object data, final int numNotifs, final long intervalMillis);

    /**
    Add a new Attribute. After this, the MBeanInfo will contain an MBeanAttributeInfo
    for this Attribute.

    @param name
    @param value
     */
    @ManagedOperation(impact = MBeanOperationInfo.ACTION)
    public void addAttribute(final String name, final Object value);

    /**
    Remove an Attribute. After this, the MBeanInfo will no longer
    contain an MBeanAttributeInfo for this Attribute.
     */
    @ManagedOperation(impact = MBeanOperationInfo.ACTION)
    public void removeAttribute(final String name);

    /**
    For testing bandwidth...
     */
    @ManagedOperation(impact = MBeanOperationInfo.ACTION)
    public void uploadBytes(final byte[] bytes);

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    public byte[] downloadBytes(final int numBytes);

    /** explicity getter using an array, must work through proxy code */
    @ManagedAttribute
    public ObjectName[] getAllAMX();
    
    /** Attribute whose values will have a variety of types that should pass the AMXValidtor */
    @ManagedAttribute
    public Object[] getAllSortsOfStuff();
}










