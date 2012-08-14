/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.module.maven.commandsecurityplugin;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author tjquinn
 */
public class CommandAuthorizationInfo {
    private final static String LINE_SEP = System.getProperty("line.separator");
    
    final AtomicBoolean hasRestAnno = new AtomicBoolean(false);
    final AtomicBoolean hasCommandLevelAccessRequiredAnno = new AtomicBoolean(false);
    final AtomicBoolean hasFieldLevelAccessRequiredAnno = new AtomicBoolean(false);
    final AtomicBoolean isAccessCheckProvider = new AtomicBoolean(false);
    final AtomicBoolean isLocal = new AtomicBoolean(false);
    
    private List<RestEndpointInfo> endpoints = new ArrayList<RestEndpointInfo>();
    
    private CommandAuthorizationInfo parent = null;
    
    private String genericMethodListActual = "";
    private String genericMethodName = "";
    private String genericParentConfigured = "";
    private String fullPath = "";
    private String genericAction = "";
    
    private final List<ResourceAction> resourceActionPairs = new ArrayList<ResourceAction>();
    
    public void addRestEndpoint(final RestEndpointInfo endpoint) {
        endpoints.add(endpoint);
    }
    
    public void addResourceAction(final String resource, final String action,
            final String origin) {
        resourceActionPairs.add(new ResourceAction(resource, action, origin));
    }
    
    public List<ResourceAction> resourceActionPairs() {
        return resourceActionPairs;
    }
    
    public String genericSubpath(final String separator) {
        if (genericMethodListActual == null || genericMethodListActual.isEmpty()) {
            return "";
        }
        return fullPath;
    }
    
    public String genericSubpathPerAction(final String separator) {
        String subpath = genericSubpath(separator);
        if (genericAction.equals("create")) {
            subpath = subpath.substring(0, subpath.lastIndexOf('/'));
        } else if (genericAction.equals("list")) {
            subpath = subpath.substring(0, subpath.lastIndexOf('/'));
        }
        return subpath;
    }
    
    public String adjustedGenericAction() {
        if (genericAction.equals("create")) {
            return "update";
        } else if (genericAction.equals("list")) {
            return "read";
        }
        return genericAction;
    }
    
    public String genericAction() {
        return genericAction;
    }
    
    public void setGeneric(final String methodListActual, 
            final String methodName, 
            final String fullPath,
            final String action) {
        this.genericMethodListActual = methodListActual;
        this.genericMethodName = methodName;
//        this.genericParentConfigured = parentConfigured;
        this.fullPath = fullPath;
        this.genericAction = action;
    }
    
    public List<RestEndpointInfo> restEndpoints() {
        return endpoints;
    }
    
    public void setParent(final CommandAuthorizationInfo parent) {
        this.parent = parent;
    }
    
    public void setLocal(final boolean local) {
        isLocal.set(local);
    }
    
    public boolean isLocalDeep() {
        return isLocal.get() || (parent != null ? parent.isLocalDeep() : false);
    }
    
    boolean isOK() {
        return hasRestAnno.get() || hasCommandLevelAccessRequiredAnno.get() || hasFieldLevelAccessRequiredAnno.get() || isAccessCheckProvider.get();
    }
    
    boolean isOKDeep() {
        return isOK() || (parent != null ? parent.isOKDeep() : false);
    }
    
    boolean isAccessCheckProvider() {
        return isAccessCheckProvider.get();
    }
    
    private String name;
    private String className;
    private List<Param> params = new ArrayList<Param>();

    void setName(final String name) {
        this.name = name;
    }

    void addParam(final Param p) {
        params.add(p);
    }

    void setClassName(final String className) {
        this.className = className;
    }
    
    List<Param> params() {
        return params;
    }
    
    String name() {
        return name;
    }
    
    String className() {
        return className;
    }

    @Override
    public String toString() {
        return toString("", true);
    }

    
    public String toString(final String indent, final boolean isFull) {
        final StringBuffer sb = new StringBuffer(/* indent + */ name + " (" + (fullPath != null && ! fullPath.isEmpty() ? "[" + adjustedGenericAction() + "] " + genericSubpathPerAction("/") : className) + ")");
        if (isFull) {
            sb.append(LINE_SEP);
            final Deque<CommandAuthorizationInfo> levelsToProcess = new LinkedList<CommandAuthorizationInfo>();
            
            CommandAuthorizationInfo info = this;
            while (info != null) {
                levelsToProcess.addFirst(info);
                info = info.parent;
            }
            for (CommandAuthorizationInfo level : levelsToProcess) {
                for (Param p : level.params()) {
                    sb.append(indent).append("  ").append(p);
                }
            }
            for (RestEndpointInfo i : restEndpoints()) {
                if (i.useForAuthorization()) {
                    sb.append(LINE_SEP).append(indent).append("  ").append(i.toString());
                }
            }
                
        }
        return sb.append(LINE_SEP).toString();
    }
    
    static class Param {
        private String name;
        private String type;
        private Map<String,Object> values = new HashMap<String,Object>();
        
        Param(final String name, final String type) {
            this.name = name;
            this.type = type;
        }
        
        void setName(final String name) {
            this.name = name;
        }
        
        void setType (final String type) {
            this.type = type;
        }
        
        void addValue(final String name, final Object value) {
            values.put(name, value);
        }
        
        Map<String,Object> values() {
            return values;
        }
        
        boolean isOptional() {
            return booleanValue("optional");
        }
        
        boolean isPrimary() {
            return booleanValue("primary");
        }
        
        private boolean booleanValue(final String key) {
            boolean result = false;
            final Object v = values.get(key);
            if (v != null) {
                if (v instanceof Boolean) {
                    result = ((Boolean) v).booleanValue();
                }
            }
            return result;
        }
        
        String type() {
            return type;
        }
        
        @Override
        public String toString() {
            return (isOptional() ? "[" : "") + (isPrimary() ? "**" : "--") + name + friendlyType() + (isOptional() ? "]" : "");
        }
        
        private String friendlyType() {
            return (type.isEmpty() ? "" : " (" + type + ")");
        }
    }
    
    static class ResourceAction {
        String resource;
        String action;
        String origin;
        
        ResourceAction(final String resource, final String action, final String origin) {
            this.resource = resource;
            this.action = action;
            this.origin = origin;
        }
    }
}
