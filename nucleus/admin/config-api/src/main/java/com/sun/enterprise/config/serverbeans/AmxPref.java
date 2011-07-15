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

package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.types.PropertyBag;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;


/**
 *  Preferences for AMX MBean behavior.
 */
@Configured
@Deprecated
public interface AmxPref extends ConfigBeanProxy, Injectable, PropertyBag  {
    /** Possible value for {@link #getValidationLevel} */
    public static final String VALIDATION_LEVEL_OFF    = "off";
    /** Possible value for {@link #getValidationLevel} */
    public static final String VALIDATION_LEVEL_FULL   = "full";
    
    /**
        Validation level for AMX MBeans (validated when an MBean is registered).
      */
    @Attribute(defaultValue=VALIDATION_LEVEL_FULL)
    public String getValidationLevel();
    public void setValidationLevel(String level);
    
    /** Whether to unregister AMX MBeans not compliant to the AMX specification */
    @Attribute(defaultValue="false", dataType=Boolean.class)
    public String getUnregisterNonCompliant();
    public void setUnregisterNonCompliant(String unregister);
    
    /** lazy-loads AMX by default */
    public static final boolean AUTO_START_DEFAULT = false;
    
    /** Whether to automatically start AMX */
    @Attribute(defaultValue="" + AUTO_START_DEFAULT, dataType=Boolean.class)
    public String getAutoStart();
    public void setAutoStart(String autoStart);
    
    /** Whether to log registration and unregistration of AMX MBeans */
    @Attribute(defaultValue="false", dataType=Boolean.class)
    public String getEmitRegisrationStatus();
    public void setEmitRegisrationStatus(String emit);
    
    
    /** Whether inaccessible attributes should be logged */
    @Attribute(defaultValue="true", dataType=Boolean.class)
    public String getLogInaccessibleAttributes();
    public void setLogInaccessibleAttributes(String flag);

}
