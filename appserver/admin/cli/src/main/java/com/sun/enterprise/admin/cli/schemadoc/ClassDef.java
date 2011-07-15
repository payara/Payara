/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli.schemadoc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.List;

import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.Attribute;
import org.glassfish.api.admin.config.PropertyDesc;

/**
 * Contains metadata information about a class
 */
public class ClassDef {
    private final String def;
    private List<String> interfaces;
    private Set<ClassDef> subclasses = new HashSet<ClassDef>();
    private Map<String, String> types = new HashMap<String, String>();
    private Map<String, Attribute> attributes = new TreeMap<String, Attribute>();
    private boolean deprecated;
    private Set<PropertyDesc> properties = new TreeSet<PropertyDesc>(new Comparator<PropertyDesc>() {
        @Override
        public int compare(PropertyDesc left, PropertyDesc right) {
            return left.name().compareTo(right.name());
        }
    });

    public ClassDef(String def, List<String> interfaces) {
        this.def = def;
        this.interfaces = interfaces;
    }

    public String getDef() {
        return def;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ClassDef classDef = (ClassDef) o;
        if (def != null ? !def.equals(classDef.def) : classDef.def != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return def != null ? def.hashCode() : 0;
    }

    public void addSubclass(ClassDef classDef) {
        subclasses.add(classDef);
    }

    public Set<ClassDef> getSubclasses() {
        return subclasses;
    }

    public void addAggregatedType(String name, String type) {
        types.put(name, type);
    }

    public Map<String, String> getAggregatedTypes() {
        return types;
    }
    
    @Override
    public String toString() {
        return def;
    }

    public Map<String, Attribute> getAttributes() {
        return attributes;
    }

    public void addAttribute(String name, Attribute annotation) {
        attributes.put(Dom.convertName(name), annotation);
    }

    public void removeAttribute(String name) {
        attributes.remove(Dom.convertName(name));
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public Set<PropertyDesc> getProperties() {
        return properties;
    }

    public void addProperty(PropertyDesc prop) {
        properties.add(prop);
    }

    public String getXmlName() {
        return Dom.convertName(def.substring(def.lastIndexOf(".")+1));
    }
}
