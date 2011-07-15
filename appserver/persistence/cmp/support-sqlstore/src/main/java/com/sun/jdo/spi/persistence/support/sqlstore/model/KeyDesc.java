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
 */

/*
 * KeyDesc.java
 *
 * Created on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.model;

import org.netbeans.modules.dbschema.ColumnElement;
import java.util.ArrayList;

/**
 * This class is used to encapsulate an association between
 * key fields and key columns.
 */
public class KeyDesc {

    /** Array of LocalFieldDesc */
    private ArrayList fields;

    /** Array of ColumnElements */
    private ArrayList columns;

    /** Initialize the columns ArrayList. */
    void addColumns(ArrayList columns) {
        this.columns = columns;
    }

    /** Add a field to the KeyDesc.
     *  @param f - FieldDesc to be added
     */
    void addField(FieldDesc f) {
        if (fields == null)
            fields = new ArrayList();

        fields.add(f);
    }

    /** Add a column to this KeyDesc.
     *  @param c - ColumnElement to be added
     */
    void addColumn(ColumnElement c) {
        if (columns == null)
            columns = new ArrayList();

        columns.add(c);
    }

    /** Return all key columns.
     *  @return an ArrayList of ColumnElements
     */
    public ArrayList getColumns() {
        return columns;
    }

    /** Return all key fields.
     *  @return an ArrayList of FieldDescs
     */
    public ArrayList getFields() {
        return fields;
    }
}




