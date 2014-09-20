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
package com.sun.enterprise.admin.servermgmt.domain;

import com.sun.enterprise.admin.servermgmt.stringsubs.StringSubstitutor;
import com.sun.enterprise.admin.servermgmt.template.TemplateInfoHolder;
import com.sun.enterprise.admin.servermgmt.xml.templateinfo.TemplateInfo;

public class DomainTemplate {

    private StringSubstitutor _stringSubstitutor;
    private String _location;
    private TemplateInfoHolder _templateInfoHolder;

    public DomainTemplate(TemplateInfoHolder tInfoHolder, StringSubstitutor substitutor, String location) {
        _templateInfoHolder = tInfoHolder;
        _stringSubstitutor = substitutor;
        _location = location;
    }

    /**
     * Checks if the template has string-subs.
     *
     * @return <code>true</code> If template has stringsubs.xml file.
     */
    public boolean hasStringsubs() {
        return _stringSubstitutor != null;
    }

    /**
     * Get's the information used to perform string subs on the template.
     *
     * @return The encapsulated string-subs
     */
    public StringSubstitutor getStringSubs() {
        return _stringSubstitutor;
    }

    /**
     * Get's the location of the actual template.
     *
     * @return Template location.
     */
    public String getLocation() {
        return _location;
    }

    public TemplateInfo getInfo() {
        return _templateInfoHolder.getTemplateInfo();
    }
}
