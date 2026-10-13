/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config;

import org.jvnet.hk2.annotations.Contract;

/**
 * Denotes the <code>type</code> of the data a particular config
 * element (attribute, element) should have. This interface should be 
 * implemented whenever a need arises to check if
 * an abstract data type can be represented as a given <code> String </code>.
 * The implementations of a DataType are mapped by their <code> names </code> elsewhere.
 * Implementations should provide functional implementation of the #validate method
 * and must have a public parameterless constructor (except possibly for primitives).
 * 
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 * @see PrimitiveDataType
 * @see WriteableView
 * @since hk2 0.3.10
 */
@Contract
public interface DataType {

    /** Checks if given value can be had by the abstract data type represented
     *  by this implementation.
     * @param value String representing the value for this DataType
     * @throws org.jvnet.hk2.config.ValidationException if given String does
     * not represent this data type.
     */
    public void validate(String value) throws ValidationException;
}
