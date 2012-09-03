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
package org.glassfish.admin.amx.impl.mbean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.util.StringUtil;

final class ParentChildren implements Comparable<ParentChildren> {

    final AMXProxy mParent;
    final List<ParentChildren> mChildren;

    public ParentChildren(final AMXProxy parent, final List<ParentChildren> children) {
        mParent = parent;
        mChildren = children;
    }

    public void sortChildren() {
        Collections.sort(mChildren);
    }
    
    @Override
    public boolean equals(final Object rhs) {
        return rhs instanceof ParentChildren ? compareTo((ParentChildren)rhs) == 0 : false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + (this.mParent != null ? this.mParent.hashCode() : 0);
        hash = 83 * hash + (this.mChildren != null ? this.mChildren.hashCode() : 0);
        return hash;
    }

    @Override
    public int compareTo(final ParentChildren rhs) {
        int cmp = mParent.type().compareTo(rhs.mParent.type());
        if (cmp == 0) {
            cmp = mParent.nameProp().compareTo(rhs.mParent.nameProp());
        }

        if (cmp == 0) {
            cmp = mChildren.size() - rhs.mChildren.size();
        }

        return cmp;
    }

    public AMXProxy parent() {
        return mParent;
    }

    public List<ParentChildren> children() {
        return mChildren;
    }

    public List<String> toLines(final boolean details) {
        sortChildren();
        final List<String> lines = new ArrayList<String>();

        lines.add(descriptionFor(mParent));

        for (final ParentChildren child : mChildren) {
            final List<String> moreLines = indentAll(child.toLines(details));
            lines.addAll(moreLines);
        }
        return lines;
    }

    public List<AMXProxy> asList() {
        final List<AMXProxy> items = new ArrayList<AMXProxy>();

        items.add(mParent);
        for (final ParentChildren child : mChildren) {
            items.addAll(child.asList());
        }
        return items;
    }

    public static ParentChildren hierarchy(final AMXProxy top) {
        // make a list of all children, grouping by type
        final List<AMXProxy> children = new ArrayList<AMXProxy>();
        final Map<String, Map<String, AMXProxy>> childrenMaps = top.childrenMaps();
        for (final Map<String, AMXProxy> childrenOfType : childrenMaps.values()) {
            for (final AMXProxy amx : childrenOfType.values()) {
                children.add(amx);
            }
        }

        final List<ParentChildren> pcList = new ArrayList<ParentChildren>();
        for (final AMXProxy child : children) {
            final ParentChildren pc = hierarchy(child);
            pcList.add(pc);
        }

        final ParentChildren result = new ParentChildren(top, pcList);
        result.sortChildren();
        return result;
    }

    public static String descriptionFor(final AMXProxy proxy) {
        String desc = proxy.type();
        final String name = proxy.nameProp();
        if (name != null) {
            desc = desc + "=" + name;
        }

        return desc;
    }

    private static List<String> indentAll(final List<String> lines) {
        final List<String> linesIndented = new ArrayList<String>();
        final String INDENT = "   ";
        for (final String line : lines) {
            linesIndented.add(INDENT + line);
        }
        return linesIndented;
    }

    public static String getHierarchyString(final AMXProxy top) {
        final ParentChildren pc = hierarchy(top);

        return StringUtil.toLines(pc.toLines(true));
    }
}




































