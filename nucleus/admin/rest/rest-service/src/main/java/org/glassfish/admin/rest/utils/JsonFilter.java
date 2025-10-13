/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright [2017-2021] Payara Foundation and/or affiliates
 */
package org.glassfish.admin.rest.utils;

import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.Set;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * @author tmoreau
 */
public class JsonFilter {

    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(JsonFilter.class);

    private boolean defaultInclude;
    private List<Filter> filters = new ArrayList<Filter>();

    public JsonFilter() {
        this.defaultInclude = true;
    }

    public JsonFilter(Locale locale, String include, String exclude) throws Exception {
        this(locale, include, exclude, "name");
    }

    public JsonFilter(Locale locale, String include, String exclude, String identityAttr) throws Exception {
        if (include != null) {
            if (exclude != null) {
                String msg =
                    localStrings.getLocalString(
                        "filter.includeAndExcludeFieldsSpecified",
                        "__excludeFields cannot be specified when __includeFields is specified.");
                throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(msg).build());
            } else {
                addFilter(new IdentityFilter(identityAttr)).addFilter(new IncludeFilter(locale, include));
                this.defaultInclude = false;
            }
        } else {
            if (exclude != null) {
                addFilter(new IdentityFilter(identityAttr)).addFilter(new ExcludeFilter(locale, exclude));
                this.defaultInclude = true;
            } else {
                this.defaultInclude = true;
            }
        }
    }

    public JsonFilter addFilter(Filter filter) {
        this.filters.add(filter);
        return this;
    }

    protected enum Result { EXCLUDE, INCLUDE, DEFERR }

    public static interface Filter {
        Result filter(String attr);
    }

    public static class IdentityFilter implements Filter {
        private String identityAttr;
        public IdentityFilter(String identityAttr) {
            this.identityAttr = identityAttr;
        }
        @Override
        public Result filter(String attr) {
            if (identityAttr.equals(attr)) {
                return Result.INCLUDE;
            }
            return Result.DEFERR;
        }
    }

    public abstract static class AttrsFilter implements Filter {
        private Set<String> attrs = new HashSet<String>();
        private Set<String> parentAttrs = new HashSet<String>();

        protected AttrsFilter(Locale locale, String attrsString, boolean includeParents) throws Exception {
            for (String attrString : attrsString.split(",")) {
                attrString = attrString.trim();
                if (attrs.contains(attrString) || parentAttrs.contains(attrString)) {
                    throwOverlappingFieldsException(attrsString);
                }
                attrs.add(attrString);
                // loop through this attr's parents
                // temporarily collect parents even if we don't want to use them later
                // so that we can detect overlaps between the attrs and the parents now
                String parent = "";
                boolean first = true;
                for (String comp : attrString.split("\\.")) {
                    // Split the guts of the loop into a function to shut up findbugs complaints
                    // about accumulating a string in a loop with '+'
                    parent = processParentComponent(attrsString, attrString, parent, comp.trim(), first);
                    first = false;
                }
            }
            if (!includeParents) {
                parentAttrs.clear();
            }
        }

        private String processParentComponent(String attrsString, String attrString, String parent, String comp, boolean first) throws Exception {
            StringBuilder sb = new StringBuilder();
            sb.append(parent);
            if (!first) {
                sb.append(".");
            }
            sb.append(comp);
            parent = sb.toString();
            if (!parent.equals(attrString)) { // only look at my parents
                if (attrs.contains(parent)) {
                    throwOverlappingFieldsException(attrsString);
                }
                parentAttrs.add(parent);
            }
            return parent;
        }

        private void throwOverlappingFieldsException(String attrs) throws Exception {
            String msg = 
                localStrings.getLocalString(
                    "filter.overLappingFieldsSpecified",
                    "The field names must not overlap or be specified more than once: {0}", attrs);
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(msg).build());
        }

        @Override
        public Result filter(String attr) {
            // See if we know about this exact attr
            if (attrs.contains(attr) || parentAttrs.contains(attr)) {
                return foundResult();
            }
            // See if this attr is a child of one of the exact attrs (v.s. one of the implied parents)
            // e.g. if including A and A.B, then A.C should not match but A.B.C should match
            for (String a : attrs) {
                if (attr.startsWith(a + ".")) {
                    return foundResult();
                }
            }
            return notFoundResult();
        }

        protected abstract Result foundResult();
        protected abstract Result notFoundResult();
    }

    public static class IncludeFilter extends AttrsFilter {
        public IncludeFilter(Locale locale, String attrsString) throws Exception { super(locale, attrsString, true); }
        protected Result foundResult() { return Result.INCLUDE; }
        protected Result notFoundResult() { return Result.DEFERR; }
    }

    public static class ExcludeFilter extends AttrsFilter {
        public ExcludeFilter(Locale locale, String attrsString) throws Exception { super(locale, attrsString, false); }
        protected Result foundResult() { return Result.EXCLUDE; }
        protected Result notFoundResult() { return Result.DEFERR; }
    }

    public static class IncludeExceptFilter extends AttrsFilter {
        public IncludeExceptFilter(Locale locale, String attrsString) throws Exception { super(locale, attrsString, false); }
        protected Result foundResult() { return Result.DEFERR; }
        protected Result notFoundResult() { return Result.INCLUDE; }
    }

    public static class ExcludeExceptFilter extends AttrsFilter {
        public ExcludeExceptFilter(Locale locale, String attrsString) throws Exception { super(locale, attrsString, true); }
        protected Result foundResult() { return Result.DEFERR; }
        protected Result notFoundResult() { return Result.EXCLUDE; }
    }

    public JsonObject trim(JsonObject j) {
        newScope().trimJsonObject(j);
        return j;
    }

    public Scope newScope() {
        return new Scope();
    }

    public class Scope
    {
        private Stack<String> scopeStack = null;

        private Scope() {
            if (!filters.isEmpty()) {
                scopeStack = new Stack<String>();
            }
        }

        public JsonObject trim(JsonObject j) {
            (new Scope()).trimJsonObject(j);
            return j;
        }

        private void trimJsonObject(JsonObject j) {
            for (String property : getPropertyNames(j)) {
                if (!include(property)) {
                    j.remove(property);
                } else {
                    try {
                        Object o = j.get(property);
                        if (o instanceof JsonObject) {
                            JsonObject next = (JsonObject)o;
                            beginObjectAttr(property);
                            try {
                                trimJsonObject(next);
                            } finally {
                                endObjectAttr();
                            }
                        } else if (o instanceof JsonArray) {
                            JsonArray ar = (JsonArray)o;
                            beginArrayAttr(property);
                            try {
                                trimJsonArray(ar);
                            } finally {
                                endArrayAttr();
                            }
                        } else {
                            // scalar - we're done recursing
                        }
                    } catch (JsonException e) { /* impossible since we're iterating over the known keys */ }
                }
            }
        }

        // Can't iterate and remove properties at the same time so make a list of the properties
        private List<String> getPropertyNames(JsonObject j) {
            List<String> rtn = new ArrayList<String>();
            for (String property: j.keySet()){
                rtn.add(property);
            }
            
            return rtn;
        }

        private void trimJsonArray(JsonArray ar) {
            for (int i = 0; i < ar.size(); i++) {
                try {
                    Object o = ar.get(i);
                    if (o instanceof JsonObject) {
                        trimJsonObject((JsonObject)o);
                    } else if (o instanceof JsonArray) { // I don't think our models support arrays of arrays, but I might be wrong
                        trimJsonArray((JsonArray)o);
                    } else {
                        // scalar - we're done recursing
                    }
                } catch (JsonException e) { /* impossible since we're iterating over the known elements */ }
            }
        }

        public boolean includeAny(String[] properties) {
            for (String property : properties) {
                if (include(property)) {
                    return true;
                }
            }
            return false;
        }

        public boolean include(String property) {
            if (this.scopeStack != null) {
                String attr = (scopeStack.isEmpty()) ? property : scopeStack.peek() + "." + property;
                for (Filter filter : filters) {
                    Result r = filter.filter(attr);
                    if (r == Result.INCLUDE) {
                        return true;
                    }
                    if (r == Result.EXCLUDE) {
                        return false;
                    }
                }
            }
            return defaultInclude;
        }

        public void beginObjectAttr(String name) {
            beginAttr(name);
        }

        public void endObjectAttr() {
            endAttr();
        }

        public void beginArrayAttr(String name) {
            beginAttr(name);
        }

        public void endArrayAttr() {
            endAttr();
        }

        private void beginAttr(String name) {
            if (this.scopeStack != null) {
                String scope = (scopeStack.isEmpty()) ? name : scopeStack.peek() + "." + name;
                scopeStack.push(scope);
            }
        }

        private void endAttr() {
            if (this.scopeStack != null) {
                scopeStack.pop();
            }
        }
    }
}


