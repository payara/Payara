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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.deploy;

import org.apache.catalina.util.Enumerator;

import javax.servlet.Filter;
import java.io.Serializable;
import java.util.*;

/**
 * Representation of a filter definition for a web application, as represented
 * in a <code>&lt;filter&gt;</code> element in the deployment descriptor.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.3.6.1 $ $Date: 2008/04/17 18:37:10 $
 */

public class FilterDef implements Serializable {

    /**
     * The description of this filter.
     */
    private String description = null;

    /**
     * The display name of this filter.
     */
    private String displayName = null;

    /**
     * The fully qualified name of the Java class that implements this filter.
     */
    private String filterClassName = null;

    /*
     * The class from which this filter will be instantiated
     */
    private Class <? extends Filter> filterClass;

    /*
     * The filter instance
     */
    private transient Filter filter;

    /**
     * The name of this filter, which must be unique among the filters
     * defined for a particular web application.
     */
    private String filterName = null;

    /**
     * The large icon associated with this filter.
     */
    private String largeIcon = null;

    /**
     * The small icon associated with this filter.
     */
    private String smallIcon = null;

    /**
     * The set of initialization parameters for this filter, keyed by
     * parameter name.
     */
    private Map<String, String> parameters = new HashMap<String, String>();

    /**
     * True if this filter supports async operations, false otherwise
     */
    private boolean isAsyncSupported = false;


    // ------------------------------------------------------------- Properties


    public String getDescription() {
        return (this.description);
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public String getDisplayName() {
        return (this.displayName);
    }


    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }


    public String getFilterClassName() {
        return (this.filterClassName);
    }


    public void setFilterClassName(String filterClassName) {
        if (filterClass != null) {
            throw new IllegalStateException("Filter class already set");
        }
        this.filterClassName = filterClassName;
    }


    public Class <? extends Filter> getFilterClass() {
        return filterClass;
    }


    public void setFilterClass(Class <? extends Filter> filterClass) {
        if (filterClassName != null) {
            throw new IllegalStateException("Filter class name already set");
        }
        this.filterClass = filterClass;
        this.filterClassName = filterClass.getName();
    }


    public Filter getFilter() {
        return filter;
    }


    public void setFilter(Filter filter) {
        if (filter == null) {
            throw new NullPointerException("Null Filter instance");
        }
        if (filterClassName != null) {
            throw new IllegalStateException("Filter class name already set");
        }
        this.filter = filter;
        this.filterClassName = filter.getClass().getName();
    }


    public String getFilterName() {
        return (this.filterName);
    }


    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }


    public String getLargeIcon() {
        return (this.largeIcon);
    }


    public void setLargeIcon(String largeIcon) {
        this.largeIcon = largeIcon;
    }


    public String getSmallIcon() {
        return (this.smallIcon);
    }


    public void setSmallIcon(String smallIcon) {
        this.smallIcon = smallIcon;
    }


    /**
     * Configures this filter as either supporting or not supporting
     * asynchronous operations.
     *
     * @param isAsyncSupported true if this filter supports asynchronous
     * operations, false otherwise
     */
    public void setIsAsyncSupported(boolean isAsyncSupported) {
        this.isAsyncSupported = isAsyncSupported;
    }


    /**
     * Checks if this filter has been annotated or flagged in the deployment
     * descriptor as being able to support asynchronous operations.
     *
     * @return true if this filter supports async operations, and false
     * otherwise
     */
    public boolean isAsyncSupported() {
        return isAsyncSupported;
    }


    // --------------------------------------------------------- Public Methods

    /**
     * Adds the initialization parameter with the given name and value
     * on this filter.
     *
     * <p>If an init param with the given name already exists, its value
     * will be overridden.
     *
     * @param name the init parameter name
     * @param value the init parameter value
     */
    public void addInitParameter(String name, String value) {
        setInitParameter(name, value, true);
    }


    /**
     * Sets the init parameter with the given name and value
     * on this filter.
     *
     * @param name the init parameter name
     * @param value the init parameter value
     * @param override true if the given init param is supposed to
     * override an existing init param with the same name, and false
     * otherwise
     *
     * @return true if the init parameter with the given name and value
     * was set, false otherwise
     */
    public boolean setInitParameter(String name, String value, 
                                    boolean override) {
        if (null == name || null == value) {
            throw new IllegalArgumentException(
                "Null filter init parameter name or value");
        }

        synchronized (parameters) {
            if (override || !parameters.containsKey(name)) {
                parameters.put(name, value);
                return true;
            } else {
                return false;
            }
        }
    }


    /**
     * Sets the initialization parameters contained in the given map
     * on this filter.
     *
     * @param initParameters the map with the init params to set
     *
     * @return the (possibly empty) Set of initialization parameter names
     * that are in conflict
     */
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        if (null == initParameters) {
            throw new IllegalArgumentException("Null init parameters");
        }

        synchronized (parameters) {
            Set<String> conflicts = null;
            for (Map.Entry<String, String> e : initParameters.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) {
                    throw new IllegalArgumentException(
                        "Null parameter name or value");
                }
                if (parameters.containsKey(e.getKey())) {
                    if (conflicts == null) {
                        conflicts = new HashSet<String>();    
                    }
                    conflicts.add(e.getKey());
                }
            }

            if (conflicts != null) {
                return conflicts;
            }

            for (Map.Entry<String, String> e : initParameters.entrySet()) {
                setInitParameter(e.getKey(), e.getValue(), true);
            }
   
            return Collections.emptySet();
        }
    }


    public String getInitParameter(String name) {
        synchronized (parameters) {
            return parameters.get(name);
        }
    }        


    public Map<String, String> getInitParameters() {
        synchronized (parameters) {
            return Collections.unmodifiableMap(parameters);
        }
    }


    public Enumeration<String> getInitParameterNames() {
        synchronized (parameters) {
            return new Enumerator<String>(parameters.keySet());
        }
    }


    /**
     * Removes the initialization parameter with the given name.
     *
     * @param name the name of the initialization parameter to be removed
     */
    public void removeInitParameter(String name) {
        synchronized (parameters) {
            parameters.remove(name);
        }
    }


    /**
     * Render a String representation of this object.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder("FilterDef[");
        sb.append("filterName=");
        sb.append(this.filterName);
        sb.append(", filterClassname=");
        sb.append(this.filterClassName);
        sb.append("]");
        return (sb.toString());

    }
}
