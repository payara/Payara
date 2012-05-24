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

package org.glassfish.ejb.deployment.descriptor.runtime;

import org.glassfish.deployment.common.Descriptor;


/** 
 * This class contains information about the persistent state
 * (abstract persistence schema)
 * for EJB2.0 CMP EntityBeans .
 *
 * @author Prashant Jamkhedkar
 */

public class IASPersistenceManagerDescriptor extends Descriptor {
    
    public static final String PM_IDENTIFIER_DEFAULT = "SunOne"; // NOI18N
    public static final String PM_VERSION_DEFAULT = "1.0"; // NOI18N
    public static final String PM_CONFIG_DEFAULT = "myconfig.config";
    public static final String PM_CLASS_GENERATOR_DEFAULT = "com.sun.jdo.spi.persistence.support.ejb.ejbc.JDOCodeGenerator"; // NOI18N
    public static final String PM_CLASS_GENERATOR_DEFAULT_OLD = "com.iplanet.ias.persistence.internal.ejb.ejbc.JDOCodeGenerator"; //NOI18N
    public static final String PM_MAPPING_FACTORY_DEFAULT = "com.sun.ffj.MyFactory"; // NOI18N
    private String pm_identifier = null;
    private String pm_version = null;
    private String pm_config = null;
    private String pm_class_generator = null;
    private String pm_mapping_factory = null;

    public IASPersistenceManagerDescriptor() {
       pm_identifier = PM_IDENTIFIER_DEFAULT;
       pm_version = PM_VERSION_DEFAULT;
       pm_config = PM_CONFIG_DEFAULT;
       pm_class_generator = PM_CLASS_GENERATOR_DEFAULT;
       pm_mapping_factory = PM_MAPPING_FACTORY_DEFAULT;
    }

    /** 
     * The copy constructor.
     */
    public IASPersistenceManagerDescriptor(String id, String ver, String conf, String generator, String factory) {
       pm_identifier = id;
       pm_version = ver;
       pm_config = conf;
       pm_class_generator = generator;
       pm_mapping_factory = factory;
    }

    public String getPersistenceManagerIdentifier() {
      return pm_identifier;
    }
    
    public void setPersistenceManagerIdentifier(String pm_identifier) {
        if (pm_identifier == null) {
            this.pm_identifier = PM_IDENTIFIER_DEFAULT;
        } else {
            this.pm_identifier = pm_identifier;
        }
    }
    
    public String getPersistenceManagerVersion() {
      return pm_version;
    }
    
    public void setPersistenceManagerVersion(String pm_version) {
        if (pm_version == null) {
            this.pm_version = PM_VERSION_DEFAULT;
        } else {
            this.pm_version = pm_version;
        }
    }
    
    public String getPersistenceManagerConfig () {
      return pm_config;
    }
    
    public void setPersistenceManagerConfig(String pm_config) {
        if (pm_config == null) {
            this.pm_config = PM_CONFIG_DEFAULT;
        } else {
            this.pm_config = pm_config;
        }
    }    
    
    public String getPersistenceManagerClassGenerator() {
      return pm_class_generator;
    }
    
    public void setPersistenceManagerClassGenerator(String pm_class_generator) {
        if (pm_class_generator == null) {
            this.pm_class_generator = PM_CLASS_GENERATOR_DEFAULT;
        } else {
            this.pm_class_generator = pm_class_generator;
        }
    }
    
    public String getPersistenceManagerMappingFactory() {
      return pm_mapping_factory;
    }
    
    public void setPersistenceManagerMappingFactory(String pm_mapping_factory) {
        if (pm_mapping_factory == null) {
            this.pm_mapping_factory = PM_MAPPING_FACTORY_DEFAULT;
        } else {
            this.pm_mapping_factory = pm_mapping_factory;
        }
    }

    /**     
     * Called from EjbCMPEntityDescriptor
     * when some classes in this object are updated.
     */         
    public boolean classesChanged() {
        return false;
    }

}
