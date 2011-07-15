/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.tomcat.util.modeler;


import javax.management.Descriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * <p>Convenience base class for <code>AttributeInfo</code>,
 * <code>ConstructorInfo</code>, and <code>OperationInfo</code> classes
 * that will be used to collect configuration information for the
 * <code>ModelMBean</code> beans exposed for management.</p>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.5 $ $Date: 2003/07/20 07:35:13 $
 */

public class FeatureInfo implements Serializable {
    static final long serialVersionUID = -911529176124712296L;
    protected String description = null;
    protected List<FieldInfo> fields = new ArrayList<FieldInfo>();
    protected String name = null;

    // ------------------------------------------------------------- Properties


    /**
     * The human-readable description of this feature.
     */
    public String getDescription() {
        return (this.description);
    }

    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * The field information for this feature.
     */
    public List<FieldInfo> getFields() {
        return (fields);
    }


    /**
     * The name of this feature, which must be unique among features in the
     * same collection.
     */
    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * <p>Add a new field to the fields associated with the
     * Descriptor that will be created from this metadata.</p>
     *
     * @param field The field to be added
     */
    public void addField(FieldInfo field) {
        fields.add(field);
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * <p>Add the name/value fields that have been stored into the
     * specified <code>Descriptor</code> instance.</p>
     *
     * @param descriptor The <code>Descriptor</code> to add fields to
     */
    protected void addFields(Descriptor descriptor) {

        Iterator<FieldInfo> items = getFields().iterator();
        while (items.hasNext()) {
            FieldInfo item = items.next();
            descriptor.setField(item.getName(), item.getValue());
        }

    }


}
