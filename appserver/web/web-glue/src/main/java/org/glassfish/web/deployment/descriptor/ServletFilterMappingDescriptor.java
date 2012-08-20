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

package org.glassfish.web.deployment.descriptor;

import com.sun.enterprise.deployment.web.ServletFilterMapping;
import org.glassfish.deployment.common.Descriptor;

import java.util.*;
import javax.servlet.DispatcherType;

/**
 * Deployment object representing the servlet filter mapping spec
 * @author Martin D. Flynn
 */
public final class ServletFilterMappingDescriptor
    extends Descriptor
    implements com.sun.enterprise.deployment.web.ServletFilterMapping
{
    
    private EnumSet<DispatcherType> dispatchers;
    private List<String> servletNames;
    private List<String> urlPatterns;

    /** generic constructor */
    public ServletFilterMappingDescriptor() {
	super(""/*name*/, ""/*description*/);
    }

    /** copy constructor */
    public ServletFilterMappingDescriptor(ServletFilterMappingDescriptor other) {
        super(other);
        dispatchers = (other.dispatchers != null)?
            EnumSet.copyOf(other.dispatchers) : null;
    }

    public void addServletName(String servletName) {
        getServletNames().add(servletName);
    }

    public List<String> getServletNames() {
        if (servletNames == null) {
            servletNames = new LinkedList<String>();
        }
        return servletNames;
    }

    public void addURLPattern(String urlPattern) {
        getUrlPatterns().add(urlPattern);
    }

    public List<String> getUrlPatterns() {
        if (urlPatterns == null) {
            urlPatterns = new LinkedList<String>();
        }
        return urlPatterns;
    }

    public void addDispatcher(String dispatcher) {
        if (dispatchers == null) {
            dispatchers = EnumSet.noneOf(DispatcherType.class);
        }
        dispatchers.add(Enum.valueOf(DispatcherType.class, dispatcher));
    }
    
    public void removeDispatcher(String dispatcher) {
        if (dispatchers == null) {
            return;
        }
        dispatchers.remove(Enum.valueOf(DispatcherType.class, dispatcher));
    }

    public Set<DispatcherType> getDispatchers() {
        if (dispatchers == null) {
            dispatchers = EnumSet.noneOf(DispatcherType.class);
        }
        return dispatchers;
    }


    /** compare equals */
    public boolean equals(Object obj) {
        if (obj instanceof ServletFilterMapping) {
            ServletFilterMapping o = (ServletFilterMapping) obj;
            Set<DispatcherType> otherDispatchers = o.getDispatchers();
            boolean sameDispatchers =
                ( (dispatchers == null &&
                        (otherDispatchers == null || otherDispatchers.size() == 0)) ||
                    (dispatchers != null && dispatchers.equals(otherDispatchers)) );
            if ( this.getName().equals(o.getName())
                    && this.getServletNames().equals(o.getServletNames())
                    && this.getUrlPatterns().equals(o.getUrlPatterns())
                    && sameDispatchers ) {
                return true;
            }
        }

        return false;
    }

    public int hashCode() {
        int result = 17;
        result = 37*result + getName().hashCode();
        result = 37*result + getServletNames().hashCode();
        result = 37*result + getUrlPatterns().hashCode();
        return result;
    }
}
