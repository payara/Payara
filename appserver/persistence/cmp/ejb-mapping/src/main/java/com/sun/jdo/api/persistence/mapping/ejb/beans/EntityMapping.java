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

public class EntityMapping extends org.netbeans.modules.schema2beans.BaseBean
{

	static Vector comparators = new Vector();
	private static final org.netbeans.modules.schema2beans.Version runtimeVersion = new org.netbeans.modules.schema2beans.Version(5, 0, 0);

	static public final String EJB_NAME = "EjbName";	// NOI18N
	static public final String TABLE_NAME = "TableName";	// NOI18N
	static public final String CMP_FIELD_MAPPING = "CmpFieldMapping";	// NOI18N
	static public final String CMR_FIELD_MAPPING = "CmrFieldMapping";	// NOI18N
	static public final String SECONDARY_TABLE = "SecondaryTable";	// NOI18N
	static public final String CONSISTENCY = "Consistency";	// NOI18N

	public EntityMapping() {
		this(Common.USE_DEFAULT_VALUES);
	}

	public EntityMapping(int options)
	{
		super(comparators, runtimeVersion);
		// Properties (see root bean comments for the bean graph)
		initPropertyTables(6);
		this.createProperty("ejb-name", 	// NOI18N
			EJB_NAME, 
			Common.TYPE_1 | Common.TYPE_STRING | Common.TYPE_KEY, 
			String.class);
		this.createProperty("table-name", 	// NOI18N
			TABLE_NAME, 
			Common.TYPE_1 | Common.TYPE_STRING | Common.TYPE_KEY, 
			String.class);
		this.createProperty("cmp-field-mapping", 	// NOI18N
			CMP_FIELD_MAPPING, 
			Common.TYPE_1_N | Common.TYPE_BEAN | Common.TYPE_KEY, 
			CmpFieldMapping.class);
		this.createProperty("cmr-field-mapping", 	// NOI18N
			CMR_FIELD_MAPPING, 
			Common.TYPE_0_N | Common.TYPE_BEAN | Common.TYPE_KEY, 
			CmrFieldMapping.class);
		this.createProperty("secondary-table", 	// NOI18N
			SECONDARY_TABLE, 
			Common.TYPE_0_N | Common.TYPE_BEAN | Common.TYPE_KEY, 
			SecondaryTable.class);
		this.createProperty("consistency", 	// NOI18N
			CONSISTENCY, 
			Common.TYPE_0_1 | Common.TYPE_BEAN | Common.TYPE_KEY, 
			Consistency.class);
		this.initialize(options);
	}

	// Setting the default values of the properties
	void initialize(int options) {

	}

	// This attribute is mandatory
	public void setEjbName(String value) {
		this.setValue(EJB_NAME, value);
	}

	//
	public String getEjbName() {
		return (String)this.getValue(EJB_NAME);
	}

	// This attribute is mandatory
	public void setTableName(String value) {
		this.setValue(TABLE_NAME, value);
	}

	//
	public String getTableName() {
		return (String)this.getValue(TABLE_NAME);
	}

	// This attribute is an array containing at least one element
	public void setCmpFieldMapping(int index, CmpFieldMapping value) {
		this.setValue(CMP_FIELD_MAPPING, index, value);
	}

	//
	public CmpFieldMapping getCmpFieldMapping(int index) {
		return (CmpFieldMapping)this.getValue(CMP_FIELD_MAPPING, index);
	}

	// Return the number of properties
	public int sizeCmpFieldMapping() {
		return this.size(CMP_FIELD_MAPPING);
	}

	// This attribute is an array containing at least one element
	public void setCmpFieldMapping(CmpFieldMapping[] value) {
		this.setValue(CMP_FIELD_MAPPING, value);
	}

	//
	public CmpFieldMapping[] getCmpFieldMapping() {
		return (CmpFieldMapping[])this.getValues(CMP_FIELD_MAPPING);
	}

	// Add a new element returning its index in the list
	public int addCmpFieldMapping(com.sun.jdo.api.persistence.mapping.ejb.beans.CmpFieldMapping value) {
		int positionOfNewItem = this.addValue(CMP_FIELD_MAPPING, value);
		return positionOfNewItem;
	}

	//
	// Remove an element using its reference
	// Returns the index the element had in the list
	//
	public int removeCmpFieldMapping(com.sun.jdo.api.persistence.mapping.ejb.beans.CmpFieldMapping value) {
		return this.removeValue(CMP_FIELD_MAPPING, value);
	}

	// This attribute is an array, possibly empty
	public void setCmrFieldMapping(int index, CmrFieldMapping value) {
		this.setValue(CMR_FIELD_MAPPING, index, value);
	}

	//
	public CmrFieldMapping getCmrFieldMapping(int index) {
		return (CmrFieldMapping)this.getValue(CMR_FIELD_MAPPING, index);
	}

	// Return the number of properties
	public int sizeCmrFieldMapping() {
		return this.size(CMR_FIELD_MAPPING);
	}

	// This attribute is an array, possibly empty
	public void setCmrFieldMapping(CmrFieldMapping[] value) {
		this.setValue(CMR_FIELD_MAPPING, value);
	}

	//
	public CmrFieldMapping[] getCmrFieldMapping() {
		return (CmrFieldMapping[])this.getValues(CMR_FIELD_MAPPING);
	}

	// Add a new element returning its index in the list
	public int addCmrFieldMapping(com.sun.jdo.api.persistence.mapping.ejb.beans.CmrFieldMapping value) {
		int positionOfNewItem = this.addValue(CMR_FIELD_MAPPING, value);
		return positionOfNewItem;
	}

	//
	// Remove an element using its reference
	// Returns the index the element had in the list
	//
	public int removeCmrFieldMapping(com.sun.jdo.api.persistence.mapping.ejb.beans.CmrFieldMapping value) {
		return this.removeValue(CMR_FIELD_MAPPING, value);
	}

	// This attribute is an array, possibly empty
	public void setSecondaryTable(int index, SecondaryTable value) {
		this.setValue(SECONDARY_TABLE, index, value);
	}

	//
	public SecondaryTable getSecondaryTable(int index) {
		return (SecondaryTable)this.getValue(SECONDARY_TABLE, index);
	}

	// Return the number of properties
	public int sizeSecondaryTable() {
		return this.size(SECONDARY_TABLE);
	}

	// This attribute is an array, possibly empty
	public void setSecondaryTable(SecondaryTable[] value) {
		this.setValue(SECONDARY_TABLE, value);
	}

	//
	public SecondaryTable[] getSecondaryTable() {
		return (SecondaryTable[])this.getValues(SECONDARY_TABLE);
	}

	// Add a new element returning its index in the list
	public int addSecondaryTable(com.sun.jdo.api.persistence.mapping.ejb.beans.SecondaryTable value) {
		int positionOfNewItem = this.addValue(SECONDARY_TABLE, value);
		return positionOfNewItem;
	}

	//
	// Remove an element using its reference
	// Returns the index the element had in the list
	//
	public int removeSecondaryTable(com.sun.jdo.api.persistence.mapping.ejb.beans.SecondaryTable value) {
		return this.removeValue(SECONDARY_TABLE, value);
	}

	// This attribute is optional
	public void setConsistency(Consistency value) {
		this.setValue(CONSISTENCY, value);
	}

	//
	public Consistency getConsistency() {
		return (Consistency)this.getValue(CONSISTENCY);
	}

	/**
	 * Create a new bean using it's default constructor.
	 * This does not add it to any bean graph.
	 */
	public CmpFieldMapping newCmpFieldMapping() {
		return new CmpFieldMapping();
	}

	/**
	 * Create a new bean using it's default constructor.
	 * This does not add it to any bean graph.
	 */
	public CmrFieldMapping newCmrFieldMapping() {
		return new CmrFieldMapping();
	}

	/**
	 * Create a new bean using it's default constructor.
	 * This does not add it to any bean graph.
	 */
	public SecondaryTable newSecondaryTable() {
		return new SecondaryTable();
	}

	/**
	 * Create a new bean using it's default constructor.
	 * This does not add it to any bean graph.
	 */
	public Consistency newConsistency() {
		return new Consistency();
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
		// Validating property ejbName
		if (getEjbName() == null) {
			throw new org.netbeans.modules.schema2beans.ValidateException("getEjbName() == null", org.netbeans.modules.schema2beans.ValidateException.FailureType.NULL_VALUE, "ejbName", this);	// NOI18N
		}
		// Validating property tableName
		if (getTableName() == null) {
			throw new org.netbeans.modules.schema2beans.ValidateException("getTableName() == null", org.netbeans.modules.schema2beans.ValidateException.FailureType.NULL_VALUE, "tableName", this);	// NOI18N
		}
		// Validating property cmpFieldMapping
		if (sizeCmpFieldMapping() == 0) {
			throw new org.netbeans.modules.schema2beans.ValidateException("sizeCmpFieldMapping() == 0", org.netbeans.modules.schema2beans.ValidateException.FailureType.NULL_VALUE, "cmpFieldMapping", this);	// NOI18N
		}
		for (int _index = 0; _index < sizeCmpFieldMapping(); ++_index) {
			com.sun.jdo.api.persistence.mapping.ejb.beans.CmpFieldMapping element = getCmpFieldMapping(_index);
			if (element != null) {
				element.validate();
			}
		}
		// Validating property cmrFieldMapping
		for (int _index = 0; _index < sizeCmrFieldMapping(); ++_index) {
			com.sun.jdo.api.persistence.mapping.ejb.beans.CmrFieldMapping element = getCmrFieldMapping(_index);
			if (element != null) {
				element.validate();
			}
		}
		// Validating property secondaryTable
		for (int _index = 0; _index < sizeSecondaryTable(); ++_index) {
			com.sun.jdo.api.persistence.mapping.ejb.beans.SecondaryTable element = getSecondaryTable(_index);
			if (element != null) {
				element.validate();
			}
		}
		// Validating property consistency
		if (getConsistency() != null) {
			getConsistency().validate();
		}
	}

	// Dump the content of this bean returning it as a String
	public void dump(StringBuffer str, String indent){
		String s;
		Object o;
		org.netbeans.modules.schema2beans.BaseBean n;
		str.append(indent);
		str.append("EjbName");	// NOI18N
		str.append(indent+"\t");	// NOI18N
		str.append("<");	// NOI18N
		o = this.getEjbName();
		str.append((o==null?"null":o.toString().trim()));	// NOI18N
		str.append(">\n");	// NOI18N
		this.dumpAttributes(EJB_NAME, 0, str, indent);

		str.append(indent);
		str.append("TableName");	// NOI18N
		str.append(indent+"\t");	// NOI18N
		str.append("<");	// NOI18N
		o = this.getTableName();
		str.append((o==null?"null":o.toString().trim()));	// NOI18N
		str.append(">\n");	// NOI18N
		this.dumpAttributes(TABLE_NAME, 0, str, indent);

		str.append(indent);
		str.append("CmpFieldMapping["+this.sizeCmpFieldMapping()+"]");	// NOI18N
		for(int i=0; i<this.sizeCmpFieldMapping(); i++)
		{
			str.append(indent+"\t");
			str.append("#"+i+":");
			n = (org.netbeans.modules.schema2beans.BaseBean) this.getCmpFieldMapping(i);
			if (n != null)
				n.dump(str, indent + "\t");	// NOI18N
			else
				str.append(indent+"\tnull");	// NOI18N
			this.dumpAttributes(CMP_FIELD_MAPPING, i, str, indent);
		}

		str.append(indent);
		str.append("CmrFieldMapping["+this.sizeCmrFieldMapping()+"]");	// NOI18N
		for(int i=0; i<this.sizeCmrFieldMapping(); i++)
		{
			str.append(indent+"\t");
			str.append("#"+i+":");
			n = (org.netbeans.modules.schema2beans.BaseBean) this.getCmrFieldMapping(i);
			if (n != null)
				n.dump(str, indent + "\t");	// NOI18N
			else
				str.append(indent+"\tnull");	// NOI18N
			this.dumpAttributes(CMR_FIELD_MAPPING, i, str, indent);
		}

		str.append(indent);
		str.append("SecondaryTable["+this.sizeSecondaryTable()+"]");	// NOI18N
		for(int i=0; i<this.sizeSecondaryTable(); i++)
		{
			str.append(indent+"\t");
			str.append("#"+i+":");
			n = (org.netbeans.modules.schema2beans.BaseBean) this.getSecondaryTable(i);
			if (n != null)
				n.dump(str, indent + "\t");	// NOI18N
			else
				str.append(indent+"\tnull");	// NOI18N
			this.dumpAttributes(SECONDARY_TABLE, i, str, indent);
		}

		str.append(indent);
		str.append("Consistency");	// NOI18N
		n = (org.netbeans.modules.schema2beans.BaseBean) this.getConsistency();
		if (n != null)
			n.dump(str, indent + "\t");	// NOI18N
		else
			str.append(indent+"\tnull");	// NOI18N
		this.dumpAttributes(CONSISTENCY, 0, str, indent);

	}
	public String dumpBeanNode(){
		StringBuffer str = new StringBuffer();
		str.append("EntityMapping\n");	// NOI18N
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
