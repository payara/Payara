/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdo.api.persistence.mapping.ejb.beans;

import org.w3c.dom.*;
import org.netbeans.modules.schema2beans.*;
import java.beans.*;
import java.util.*;

// BEGIN_NOI18N

public class CmpFieldMapping extends org.netbeans.modules.schema2beans.BaseBean
	 implements com.sun.jdo.api.persistence.mapping.ejb.HasFetchedWith
{

	static Vector comparators = new Vector();
	private static final org.netbeans.modules.schema2beans.Version runtimeVersion = new org.netbeans.modules.schema2beans.Version(5, 0, 0);

	static public final String FIELD_NAME = "FieldName";	// NOI18N
	static public final String COLUMN_NAME = "ColumnName";	// NOI18N
	static public final String READ_ONLY = "ReadOnly";	// NOI18N
	static public final String FETCHED_WITH = "FetchedWith";	// NOI18N

	public CmpFieldMapping() {
		this(Common.USE_DEFAULT_VALUES);
	}

	public CmpFieldMapping(int options)
	{
		super(comparators, runtimeVersion);
		// Properties (see root bean comments for the bean graph)
		initPropertyTables(4);
		this.createProperty("field-name", 	// NOI18N
			FIELD_NAME, 
			Common.TYPE_1 | Common.TYPE_STRING | Common.TYPE_KEY, 
			String.class);
		this.createProperty("column-name", 	// NOI18N
			COLUMN_NAME, 
			Common.TYPE_1_N | Common.TYPE_STRING | Common.TYPE_KEY, 
			String.class);
		this.createProperty("read-only", 	// NOI18N
			READ_ONLY, 
			Common.TYPE_0_1 | Common.TYPE_BOOLEAN | Common.TYPE_KEY, 
			Boolean.class);
		this.createProperty("fetched-with", 	// NOI18N
			FETCHED_WITH, 
			Common.TYPE_0_1 | Common.TYPE_BEAN | Common.TYPE_KEY, 
			FetchedWith.class);
		this.initialize(options);
	}

	// Setting the default values of the properties
	void initialize(int options) {

	}

	// This attribute is mandatory
	public void setFieldName(String value) {
		this.setValue(FIELD_NAME, value);
	}

	//
	public String getFieldName() {
		return (String)this.getValue(FIELD_NAME);
	}

	// This attribute is an array containing at least one element
	public void setColumnName(int index, String value) {
		this.setValue(COLUMN_NAME, index, value);
	}

	//
	public String getColumnName(int index) {
		return (String)this.getValue(COLUMN_NAME, index);
	}

	// Return the number of properties
	public int sizeColumnName() {
		return this.size(COLUMN_NAME);
	}

	// This attribute is an array containing at least one element
	public void setColumnName(String[] value) {
		this.setValue(COLUMN_NAME, value);
	}

	//
	public String[] getColumnName() {
		return (String[])this.getValues(COLUMN_NAME);
	}

	// Add a new element returning its index in the list
	public int addColumnName(String value) {
		int positionOfNewItem = this.addValue(COLUMN_NAME, value);
		return positionOfNewItem;
	}

	//
	// Remove an element using its reference
	// Returns the index the element had in the list
	//
	public int removeColumnName(String value) {
		return this.removeValue(COLUMN_NAME, value);
	}

	// This attribute is optional
	public void setReadOnly(boolean value) {
		this.setValue(READ_ONLY, (value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
	}

	//
	public boolean isReadOnly() {
		Boolean ret = (Boolean)this.getValue(READ_ONLY);
		if (ret == null)
			ret = (Boolean)Common.defaultScalarValue(Common.TYPE_BOOLEAN);
		return ((java.lang.Boolean)ret).booleanValue();
	}

	// This attribute is optional
	public void setFetchedWith(FetchedWith value) {
		this.setValue(FETCHED_WITH, value);
	}

	//
	public FetchedWith getFetchedWith() {
		return (FetchedWith)this.getValue(FETCHED_WITH);
	}

	/**
	 * Create a new bean using it's default constructor.
	 * This does not add it to any bean graph.
	 */
	public FetchedWith newFetchedWith() {
		return new FetchedWith();
	}

	//
	public static void addComparator(org.netbeans.modules.schema2beans.BeanComparator c) {
		comparators.add(c);
	}

	//
	public static void removeComparator(org.netbeans.modules.schema2beans.BeanComparator c) {
		comparators.remove(c);
	}
	public void validate() throws org.netbeans.modules.schema2beans.ValidateException {
		boolean restrictionFailure = false;
		boolean restrictionPassed = false;
		// Validating property fieldName
		if (getFieldName() == null) {
			throw new org.netbeans.modules.schema2beans.ValidateException("getFieldName() == null", org.netbeans.modules.schema2beans.ValidateException.FailureType.NULL_VALUE, "fieldName", this);	// NOI18N
		}
		// Validating property columnName
		if (sizeColumnName() == 0) {
			throw new org.netbeans.modules.schema2beans.ValidateException("sizeColumnName() == 0", org.netbeans.modules.schema2beans.ValidateException.FailureType.NULL_VALUE, "columnName", this);	// NOI18N
		}
		// Validating property readOnly
		// Validating property fetchedWith
		if (getFetchedWith() != null) {
			getFetchedWith().validate();
		}
	}

	// Dump the content of this bean returning it as a String
	public void dump(StringBuffer str, String indent){
		String s;
		Object o;
		org.netbeans.modules.schema2beans.BaseBean n;
		str.append(indent);
		str.append("FieldName");	// NOI18N
		str.append(indent+"\t");	// NOI18N
		str.append("<");	// NOI18N
		o = this.getFieldName();
		str.append((o==null?"null":o.toString().trim()));	// NOI18N
		str.append(">\n");	// NOI18N
		this.dumpAttributes(FIELD_NAME, 0, str, indent);

		str.append(indent);
		str.append("ColumnName["+this.sizeColumnName()+"]");	// NOI18N
		for(int i=0; i<this.sizeColumnName(); i++)
		{
			str.append(indent+"\t");
			str.append("#"+i+":");
			str.append(indent+"\t");	// NOI18N
			str.append("<");	// NOI18N
			o = this.getColumnName(i);
			str.append((o==null?"null":o.toString().trim()));	// NOI18N
			str.append(">\n");	// NOI18N
			this.dumpAttributes(COLUMN_NAME, i, str, indent);
		}

		str.append(indent);
		str.append("ReadOnly");	// NOI18N
		str.append(indent+"\t");	// NOI18N
		str.append((this.isReadOnly()?"true":"false"));
		this.dumpAttributes(READ_ONLY, 0, str, indent);

		str.append(indent);
		str.append("FetchedWith");	// NOI18N
		n = (org.netbeans.modules.schema2beans.BaseBean) this.getFetchedWith();
		if (n != null)
			n.dump(str, indent + "\t");	// NOI18N
		else
			str.append(indent+"\tnull");	// NOI18N
		this.dumpAttributes(FETCHED_WITH, 0, str, indent);

	}
	public String dumpBeanNode(){
		StringBuffer str = new StringBuffer();
		str.append("CmpFieldMapping\n");	// NOI18N
		this.dump(str, "\n  ");	// NOI18N
		return str.toString();
	}}

// END_NOI18N


/*
		The following schema file has been used for generation:

<!--
  XML DTD for Sun ONE Application Server specific Object Relational Mapping 
  with Container Managed Persistence.
-->

<!--

This sun-cmp-mapping_1_2.dtd has a workaround for an unfiled schema2beans bug
which prevents us from having the DTD specify the sub-elements in column-pair as
it really should be.  This issue is fixed in schema2beans shipped with NB > 3.5,
but we are currently using schema2beans from NB 3.5, and so must use this
workaround.

Because of the workaround, the file here differs from the official one in
appserv-commons/lib/dtds (which also has previous versions of sun-cmp-mapping 
dtds) in the definition of the column pair element.  This difference is so 
that schema2beans can produce usable beans.  The official dtd has:

    <!ELEMENT column-pair (column-name, column-name) >

and the one in here has:

    <!ELEMENT column-pair (column-name+) >

-->

<!-- This file maps at least one set of beans to tables and columns in a 
     specific db schema
-->
<!ELEMENT sun-cmp-mappings ( sun-cmp-mapping+ ) >

<!-- At least one bean is mapped to database columns in the named schema -->
<!ELEMENT sun-cmp-mapping ( schema, entity-mapping+) >

<!-- A cmp bean has a name, a primary table, one or more fields, zero or 
     more relationships, and zero or more secondary tables, plus flags for 
     consistency checking.
 
     If the consistency checking flag element is not present, then none 
     is assumed 
--> 
<!ELEMENT entity-mapping (ejb-name, table-name, cmp-field-mapping+, 
        cmr-field-mapping*, secondary-table*, consistency?)>

<!ELEMENT consistency (none | check-modified-at-commit | lock-when-loaded |
        check-all-at-commit | (lock-when-modified, check-all-at-commit?) |
        check-version-of-accessed-instances) >

<!ELEMENT read-only EMPTY>

<!-- A cmp-field-mapping has a field, one or more columns that it maps to.  
     The column can be from a bean's primary table or any defined secondary 
     table.  If a field is mapped to multiple columns, the column listed first
     is used as the SOURCE for getting the value from the database.  The 
     columns are updated in their order.  A field may also be marked as 
     read-only.  It may also participate in a hierarchial or independent 
     fetch group. If the fetched-with element is not present, the value,
          <fetched-with><none/></fetched-with>
     is assumed.
-->
<!ELEMENT cmp-field-mapping (field-name, column-name+, read-only?, 
        fetched-with?) >
            
<!-- The java identifier of a field. Must match the value of the field-name 
     sub-element of the cmp-field that is being mapped. 
-->
<!ELEMENT field-name (#PCDATA) >

<!-- The java identifier of a field.  Must match the value of the 
     cmr-field-name sub-element of the cmr-field tat is being mapped. 
-->
<!ELEMENT cmr-field-name (#PCDATA) >

<!-- The ejb-name from the standard EJB-jar DTD--> 
<!ELEMENT ejb-name (#PCDATA) >

<!-- The COLUMN name of a column from the primary table, or the table 
     qualified name (TABLE.COLUMN) of a column from a secondary or related 
     table
--> 
<!ELEMENT column-name (#PCDATA) >

<!-- Holds the fetch group configuration for fields and relationships -->
<!ELEMENT fetched-with (default | level | named-group | none) >

<!-- Sub element of fetched-with. Implies that a field belongs to the default 
     hierarchical fetch group. -->
<!ELEMENT default EMPTY>

<!-- A hierarchial fetch group.  The value of this element must be an integer.
     Fields and relationships that belong to a hierachial fetch group of equal
     (or lesser) value are fetched at the same time. The value of level must
     be greater than zero.
-->
<!ELEMENT level (#PCDATA) >

<!-- The name of an independent fetch group.  All the fields and relationships 
  that are part of a named-group are fetched at the same time-->
<!ELEMENT named-group (#PCDATA) >

<!-- The name of a database table -->
<!ELEMENT table-name (#PCDATA) >

<!-- a bean's secondary tables -->
<!ELEMENT secondary-table (table-name, column-pair+) >

<!-- the pair of columns -->
<!ELEMENT column-pair (column-name+) >

<!-- cmr-field mapping.  A cmr field has a name and one or more column 
     pairs that define the relationship. The relationship can also 
     participate in a fetch group.
     
     If the fetched-with element is not present, the value,
          <fetched-with><none/></fetched-with>
     is assumed. 
-->
<!ELEMENT cmr-field-mapping (cmr-field-name, column-pair+, fetched-with? ) >

<!-- The path name to the schema file--> 
<!ELEMENT schema (#PCDATA) >

<!-- flag elements for consistency levels -->

<!-- note: none is also a sub-element of the fetched-with tag -->
<!ELEMENT none EMPTY >
<!ELEMENT check-modified-at-commit EMPTY >
<!ELEMENT check-all-at-commit EMPTY>
<!ELEMENT lock-when-modified EMPTY>
<!ELEMENT lock-when-loaded EMPTY >
<!ELEMENT check-version-of-accessed-instances (column-name+) >

*/
