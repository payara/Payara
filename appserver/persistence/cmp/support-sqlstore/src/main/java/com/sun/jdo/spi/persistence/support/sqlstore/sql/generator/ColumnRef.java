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
 * ColumnRef.java
 *
 * Create on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.sql.generator;

import org.netbeans.modules.dbschema.ColumnElement;
import com.sun.jdo.api.persistence.support.FieldMapping;

/**
 */
public class ColumnRef extends Object implements FieldMapping {
	//
	// actual ColumnElement from the dbmodel
	//
    private ColumnElement columnElement;

	//
	// the table this column belongs to
	//
    private QueryTable table;

	//
	// input value for this column for update statements.
        // This field contains LocalFieldDesc for the corresponding field when
        // an UpdateStatement using batching uses this field.
	//
    private Object value;

	//
	// the position of this column in the SQL statement
	//
	private int index;

	//
	// the name of this column
	//
	private String name;

	public ColumnRef(ColumnElement columnElement,
					 QueryTable table) {
		this.columnElement = columnElement;
		name = columnElement.getName().getName();
		this.table = table;
	}

	public ColumnRef(ColumnElement columnElement,
					 Object value) {
		this.columnElement = columnElement;
		name = columnElement.getName().getName();
		this.value = value;
	}

	/** Return the actual ColumnElement associated with this column.
     *  @return the ColumnElement associated with this
	 */
	public ColumnElement getColumnElement() {
		return columnElement;
	}

	/** Return the position of this column in the SQL statement.
	 *  @return the position of this column in the SQL statement
	 */
	public int getIndex() {
		return index;
	}

	/** Set the position of this column in the SQL statement.
	 *  @param value - the new position
     */
	public void setIndex(int value) {
		this.index = value;
	}

	/** Return the input value for this column.
     *  @return the input value for this column
	 */
	public Object getValue() {
		return value;
	}

	/** Return the QueryTable associated with this column. 
	 *  @return the QueryTable associated with this column. 
	 */
	public QueryTable getQueryTable() {
		return table;
	}

	/** Return the name of this column.
     *  @return the name of this column.
	 */
	public String getName() {
		return name;
	}

     //---- implementing FieldMapping ------------------------------//
     /**
      * This method return int corresponding to java.sql.Types.
      */
     public int getColumnType() {
         return columnElement.getType();
     }

     /**
      * This method return the name of the column.
      */
     public String getColumnName() {
         return name;
     }

     /**
      * This method return the length of the column and -1 if unknown.
      */
     public int getColumnLength() {
         Integer len = columnElement.getLength();
         return (len != null) ? len.intValue(): -1;
     }

     //---- end of implementing FieldMapping -----------------------//
}
