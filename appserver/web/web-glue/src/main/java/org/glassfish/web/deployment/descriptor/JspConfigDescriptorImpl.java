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
 */
// Portions Copyright [2019-2021] Payara Foundation and/or affiliates

package org.glassfish.web.deployment.descriptor;

import org.glassfish.deployment.common.Descriptor;

import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.descriptor.JspPropertyGroupDescriptor;
import jakarta.servlet.descriptor.TaglibDescriptor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * This is a descriptor for the taglib config used in a web application.
 */
public class JspConfigDescriptorImpl extends Descriptor
        implements JspConfigDescriptor {

    private Set<TaglibDescriptor> taglibs = null;
    private Vector<JspPropertyGroupDescriptor> jspGroups = null;

    public void add(JspConfigDescriptorImpl jspConfigDesc) {
        if (jspConfigDesc.taglibs != null) {
            getTaglibs().addAll(jspConfigDesc.taglibs);
        }

        if (jspConfigDesc.jspGroups != null) {
            getJspPropertyGroups().addAll(jspConfigDesc.jspGroups);
        }
    }

    /**
     * return the set of tag lib elements
     */
    public Set<TaglibDescriptor> getTaglibs() {
        if (taglibs == null) {
            taglibs = new HashSet<TaglibDescriptor>();
        }
        return taglibs;
    }

    /**
     * add a tag lib element to the set.
     */
    public void addTagLib(TagLibConfigurationDescriptor desc) {
        getTaglibs().add(desc);
    }

    /**
     * remove a tag lib element from the set.
     */
    public void removeTagLib(TagLibConfigurationDescriptor desc) {
        getTaglibs().remove(desc);
    }

    /**
     * return Collection of jsp-group elements
     */
    public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups() {
        if (jspGroups == null) {
            jspGroups = new Vector<JspPropertyGroupDescriptor>();
        }
        return jspGroups;
    }

    /**
     * add a jsp group element to the set.
     */
    public void addJspGroup(JspGroupDescriptor desc) {
        getJspPropertyGroups().add(desc);
    }

    /**
     * remove a jsp group element from the set.
     */
    public void removeJspGroup(JspGroupDescriptor desc) {
        getJspPropertyGroups().remove(desc);
    }

    /**
     * @return a string describing the values I hold
     */
    public void print(StringBuilder toStringBuilder) {
        toStringBuilder.append("\nTagLibs : ").append(taglibs).append(
            " jsp groups:").append(jspGroups);
    }
}
