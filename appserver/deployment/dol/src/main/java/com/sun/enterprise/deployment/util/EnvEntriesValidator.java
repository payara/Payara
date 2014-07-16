/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.ResourceEnvReferenceDescriptor;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.web.EnvironmentEntry;

public class EnvEntriesValidator {

  private static final String JAVA_COLON = "java:";

  private static final String JAVA_COMP_ENV_STRING = "java:comp/env/";

  private static final String JAVA_COMP_PREFIX = "java:comp/";

  private static final String JAVA_MODULE_PREFIX = "java:module/";

  private static final String JAVA_APP_PREFIX = "java:app/";

  private Map<String, Map> componentNamespaces;

  private Map<String, Map> appNamespaces;

  private Map<AppModuleKey, Map> moduleNamespaces;

  private Map globalNameSpace;

  public EnvEntriesValidator() {
    componentNamespaces = new HashMap<String, Map>();
    appNamespaces = new HashMap<String, Map>();
    moduleNamespaces = new HashMap<AppModuleKey, Map>();
    globalNameSpace = new HashMap();
  }

  public void validateEnvEntries(JndiNameEnvironment env) {
    if (env instanceof WebBundleDescriptor) {
      Enumeration<EnvironmentEntry> envEntries = ((WebBundleDescriptor) env)
          .getEnvironmentEntries();
      validateSimpleEnvEntries(env, envEntries);
    } else {
      Set<EnvironmentProperty> envProperties = env.getEnvironmentProperties();
      validateSimpleEnvEntries(env, envProperties);
    }
    Set<EjbReference> ejbReferences = env.getEjbReferenceDescriptors();
    validateEjbReferences(env, ejbReferences);
    Set<ResourceReferenceDescriptor> resRefs = env
        .getResourceReferenceDescriptors();
    validateResRefs(env, resRefs);
    Set<ResourceEnvReferenceDescriptor> resEnvRefs = env
        .getResourceEnvReferenceDescriptors();
    validateResEnvRefs(env, resEnvRefs);
  }

  private void validateSimpleEnvEntries(JndiNameEnvironment env,
      Set<EnvironmentProperty> envEntries) {
    for (EnvironmentProperty environmentProperty : envEntries) {
      SimpleEnvEntry simpleEnvEntry = new SimpleEnvEntry(environmentProperty);
      validateEnvEntry(env, simpleEnvEntry, simpleEnvEntry.getName());
    }
  }

  private void validateSimpleEnvEntries(JndiNameEnvironment env,
      Enumeration<EnvironmentEntry> envEntries) {
    while (envEntries.hasMoreElements()) {
      EnvironmentEntry envEntry = envEntries.nextElement();
      SimpleEnvEntry simpleEnvEntry = new SimpleEnvEntry(envEntry);
      validateEnvEntry(env, simpleEnvEntry, simpleEnvEntry.getName());
    }
  }

  private void validateEjbReferences(JndiNameEnvironment env,
      Set<EjbReference> ejbRefes) {
    for (EjbReference ejbRef : ejbRefes) {
      validateEnvEntry(env, ejbRef, ejbRef.getName());
    }
  }

  private void validateResRefs(JndiNameEnvironment env,
      Set<ResourceReferenceDescriptor> resRefs) {
    for (ResourceReferenceDescriptor resRef : resRefs) {
      validateEnvEntry(env, resRef, resRef.getName());
    }
  }

  private void validateResEnvRefs(JndiNameEnvironment env,
      Set<ResourceEnvReferenceDescriptor> resEnvRefs) {
    for (ResourceEnvReferenceDescriptor resEnvRef : resEnvRefs) {
      validateEnvEntry(env, resEnvRef, resEnvRef.getName());
    }
  }

  private void validateEnvEntry(JndiNameEnvironment env, Object curEntry,
      String name) {
    String logicalJndiName = getLogicalJNDIName(name, env);
    Map namespace = getNamespace(logicalJndiName, env);
    Object preObject = namespace.get(logicalJndiName);

    if (preObject != null) {
      if (preObject instanceof SimpleEnvEntry
          && curEntry instanceof SimpleEnvEntry) {
        SimpleEnvEntry preEnvEntry = (SimpleEnvEntry) preObject;
        SimpleEnvEntry curEnvEntry = (SimpleEnvEntry) curEntry;
        if (areConflicting(preEnvEntry.getType(), curEnvEntry.getType())
            || areConflicting(preEnvEntry.getValue(), curEnvEntry.getValue())) {
          throwConflictException(name, namespace.toString());
        }
      } else if (preObject instanceof EjbReference
          && curEntry instanceof EjbReference) {
        EjbReference preRef = (EjbReference) preObject;
        EjbReference curRef = (EjbReference) curEntry;
        if (areConflicting(preRef.getType(), curRef.getType())
            || areConflicting(preRef.getEjbHomeInterface(),
                curRef.getEjbHomeInterface())
            || areConflicting(preRef.getEjbInterface(),
                curRef.getEjbInterface())
            // link name is optional. compare only when they are both not null.
            || ((preRef.getLinkName() != null && curRef.getLinkName() != null && !preRef.getLinkName().equals(curRef.getLinkName())))
            || (preRef.isLocal() != curRef.isLocal())
            || areConflicting(preRef.getLookupName(), curRef.getLookupName())) {
          throwConflictException(name, namespace.toString());
        }
      } else if (preObject instanceof ResourceReferenceDescriptor
          && curEntry instanceof ResourceReferenceDescriptor) {
        ResourceReferenceDescriptor preRef = (ResourceReferenceDescriptor) preObject;
        ResourceReferenceDescriptor curRef = (ResourceReferenceDescriptor) curEntry;
        if (areConflicting(preRef.getType(), curRef.getType())
            || areConflicting(preRef.getAuthorization(),
                curRef.getAuthorization())
            || areConflicting(preRef.getSharingScope(),
                curRef.getSharingScope())
            || areConflicting(preRef.getMappedName(), curRef.getMappedName())
            || areConflicting(preRef.getLookupName(), curRef.getLookupName())) {
          throwConflictException(name, namespace.toString());
        }
      } else if (preObject instanceof ResourceEnvReferenceDescriptor
          && curEntry instanceof ResourceEnvReferenceDescriptor) {
        ResourceEnvReferenceDescriptor preRef = (ResourceEnvReferenceDescriptor) preObject;
        ResourceEnvReferenceDescriptor curRef = (ResourceEnvReferenceDescriptor) curEntry;
        if (areConflicting(preRef.getType(), curRef.getType())
            || areConflicting(preRef.getRefType(), curRef.getRefType())
            || areConflicting(preRef.getMappedName(), curRef.getMappedName())
            || areConflicting(preRef.getLookupName(), curRef.getLookupName())) {
          throwConflictException(name, namespace.toString());
        }
      } else {
        throwConflictException(name, namespace.toString());
      }
    } else {
      namespace.put(logicalJndiName, curEntry);
    }

  }

  private Map getNamespace(String logicalJndiName, JndiNameEnvironment env) {
    String appName = DOLUtils.getApplicationName(env);
    Map namespace = null;
    if (logicalJndiName.startsWith(JAVA_COMP_PREFIX)) {
      String componentId = DOLUtils.getComponentEnvId(env);
      namespace = (Map) componentNamespaces.get(componentId);
      if (namespace == null) {
        namespace = new HashMap<String, Map>();
        componentNamespaces.put(componentId, namespace);
      }
    } else if (logicalJndiName.startsWith(JAVA_MODULE_PREFIX)) {
      String moduleName = DOLUtils.getModuleName(env);
      AppModuleKey appModuleKey = new AppModuleKey(appName, moduleName);
      namespace = moduleNamespaces.get(appModuleKey);
      if (namespace == null) {
        namespace = new HashMap<AppModuleKey, Map>();
        moduleNamespaces.put(appModuleKey, namespace);
      }

    } else if (logicalJndiName.startsWith(JAVA_APP_PREFIX)) {

      namespace = appNamespaces.get(appName);
      if (namespace == null) {
        namespace = new HashMap<String, Map>();
        appNamespaces.put(appName, namespace);
      }
    } else {
      // java:global
      namespace = globalNameSpace;
    }
    return namespace;
  }

  private void throwConflictException(String jndiName, String namespace) {
    throw new IllegalStateException("Naming binding already exists for "
        + jndiName + " in namespace " + namespace);
  }

  private boolean areConflicting(String s1, String s2) {
    boolean conflict = false;
    if ((s1 != null && !s1.equals(s2)) || (s2 != null && !s2.equals(s1))) {
      conflict = true;
    }
    return conflict;
  }

  private static class AppModuleKey {
    private String app;

    private String module;

    public AppModuleKey(String appName, String moduleName) {
      app = appName;
      module = moduleName;
    }

    public boolean equals(Object o) {
      boolean equal = false;
      if ((o != null) && (o instanceof AppModuleKey)) {
        AppModuleKey other = (AppModuleKey) o;
        if (app.equals(other.app) && module.equals(other.module)) {
          equal = true;
        }
      }
      return equal;
    }

    public int hashCode() {
      return app.hashCode();
    }

    public String toString() {
      return "appName = " + app + " , module = " + module;
    }
  }

  private static class SimpleEnvEntry extends EnvironmentProperty {
    SimpleEnvEntry(EnvironmentEntry envEntry) {
      super(envEntry.getName(), envEntry.getValue(), envEntry.getDescription(),
          envEntry.getType());
    }
  }

  /**
   * If no java: prefix is specified, default to component scope.
   */
  private String rawNameToLogicalJndiName(String rawName) {
    return (rawName.startsWith(JAVA_COLON)) ? rawName : JAVA_COMP_ENV_STRING
        + rawName;
  }

  /**
   * convert name from java:comp/xxx to java:module/xxx
   */
  private String logicalCompJndiNameToModule(String logicalCompName) {
    String tail = logicalCompName.substring(JAVA_COMP_PREFIX.length());
    return JAVA_MODULE_PREFIX + tail;
  }

  private String getLogicalJNDIName(String name, JndiNameEnvironment env) {
    String logicalJndiName = rawNameToLogicalJndiName(name);
    boolean treatComponentAsModule = DOLUtils.getTreatComponentAsModule(env);
    if (treatComponentAsModule && logicalJndiName.startsWith(JAVA_COMP_PREFIX)) {
      logicalJndiName = logicalCompJndiNameToModule(logicalJndiName);
    }
    return logicalJndiName;
  }

}
