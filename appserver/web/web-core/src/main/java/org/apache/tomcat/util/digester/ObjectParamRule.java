/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.tomcat.util.digester;

import org.xml.sax.Attributes;

/**
 * <p>Rule implementation that saves a parameter for use by a surrounding
 * <code>CallMethodRule<code>.</p>
 *
 * <p>This parameter may be:
 * <ul>
 * <li>an arbitrary Object defined programatically, assigned when the element pattern associated with the Rule is matched
 * See {@link #ObjectParamRule(int paramIndex, Object param)}
 * <li>an arbitrary Object defined programatically, assigned if the element pattern AND specified attribute name are matched
 * See {@link #ObjectParamRule(int paramIndex, String attributeName, Object param)}
 * </ul>
 * </p>
 *
 * @since 1.4
 */

public class ObjectParamRule extends Rule {
    // ----------------------------------------------------------- Constructors
    /**
     * Construct a "call parameter" rule that will save the given Object as
     * the parameter value.
     *
     * @param paramIndex The zero-relative parameter number
     * @param param the parameter to pass along
     */
    public ObjectParamRule(int paramIndex, Object param) {
        this(paramIndex, null, param);
    }


    /**
     * Construct a "call parameter" rule that will save the given Object as
     * the parameter value, provided that the specified attribute exists.
     *
     * @param paramIndex The zero-relative parameter number
     * @param attributeName The name of the attribute to match
     * @param param the parameter to pass along
     */
    public ObjectParamRule(int paramIndex, String attributeName, Object param) {
        this.paramIndex = paramIndex;
        this.attributeName = attributeName;
        this.param = param;
    }


    // ----------------------------------------------------- Instance Variables

    /**
     * The attribute which we are attempting to match
     */
    protected String attributeName = null;

    /**
     * The zero-relative index of the parameter we are saving.
     */
    protected int paramIndex = 0;

    /**
     * The parameter we wish to pass to the method call
     */
    protected Object param = null;


    // --------------------------------------------------------- Public Methods

    /**
     * Process the start of this element.
     *
     * @param attributes The attribute list for this element
     */
    public void begin(String namespace, String name,
                      Attributes attributes) throws Exception {
        Object anAttribute = null;
        Object parameters[] = (Object[]) digester.peekParams();

        if (attributeName != null) {
            anAttribute = attributes.getValue(attributeName);
            if(anAttribute != null) {
                parameters[paramIndex] = param;
            }
            // note -- if attributeName != null and anAttribute == null, this rule
            // will pass null as its parameter!
        }else{
            parameters[paramIndex] = param;
        }
    }

    /**
     * Render a printable version of this Rule.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("ObjectParamRule[");
        sb.append("paramIndex=");
        sb.append(paramIndex);
        sb.append(", attributeName=");
        sb.append(attributeName);
        sb.append(", param=");
        sb.append(param);
        sb.append("]");
        return (sb.toString());
    }
}
