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

 /**
 *	This generated bean class SunCmpMappings matches the schema element 'sun-cmp-mappings'.
 *
 *	Generated on Mon Dec 22 15:39:25 PST 2008
 *
 *	This class matches the root element of the DTD,
 *	and is the root of the following bean graph:
 *
 *	sunCmpMappings <sun-cmp-mappings> : SunCmpMappings
 *		sunCmpMapping <sun-cmp-mapping> : SunCmpMapping[1,n]
 *			schema <schema> : String
 *			entityMapping <entity-mapping> : EntityMapping[1,n]
 *				ejbName <ejb-name> : String
 *				tableName <table-name> : String
 *				cmpFieldMapping <cmp-field-mapping> : CmpFieldMapping[1,n]
 *					fieldName <field-name> : String
 *					columnName <column-name> : String[1,n]
 *					readOnly <read-only> : boolean[0,1]
 *						EMPTY : String
 *					fetchedWith <fetched-with> : FetchedWith[0,1]
 *						| default <default> : boolean
 *						| 	EMPTY : String
 *						| level <level> : int
 *						| namedGroup <named-group> : String
 *						| none <none> : boolean
 *						| 	EMPTY : String
 *				cmrFieldMapping <cmr-field-mapping> : CmrFieldMapping[0,n]
 *					cmrFieldName <cmr-field-name> : String
 *					columnPair <column-pair> : ColumnPair[1,n]
 *						columnName <column-name> : String[1,n]
 *					fetchedWith <fetched-with> : FetchedWith[0,1]
 *						| default <default> : boolean
 *						| 	EMPTY : String
 *						| level <level> : int
 *						| namedGroup <named-group> : String
 *						| none <none> : boolean
 *						| 	EMPTY : String
 *				secondaryTable <secondary-table> : SecondaryTable[0,n]
 *					tableName <table-name> : String
 *					columnPair <column-pair> : ColumnPair[1,n]
 *						columnName <column-name> : String[1,n]
 *				consistency <consistency> : Consistency[0,1]
 *					| none <none> : boolean
 *					| 	EMPTY : String
 *					| checkModifiedAtCommit <check-modified-at-commit> : boolean
 *					| 	EMPTY : String
 *					| lockWhenLoaded <lock-when-loaded> : boolean
 *					| 	EMPTY : String
 *					| checkAllAtCommit <check-all-at-commit> : boolean
 *					| 	EMPTY : String
 *					| lockWhenModified <lock-when-modified> : boolean
 *					| 	EMPTY : String
 *					| checkAllAtCommit2 <check-all-at-commit> : boolean[0,1]
 *					| 	EMPTY : String
 *					| checkVersionOfAccessedInstances <check-version-of-accessed-instances> : CheckVersionOfAccessedInstances
 *					| 	columnName <column-name> : String[1,n]
 *
 * @Generated
 */

package com.sun.jdo.api.persistence.mapping.ejb.beans;

import org.w3c.dom.*;
import org.netbeans.modules.schema2beans.*;
import java.beans.*;
import java.util.*;
import java.io.*;

// BEGIN_NOI18N

public class SunCmpMappings extends org.netbeans.modules.schema2beans.BaseBean
{

	static Vector comparators = new Vector();
	private static final org.netbeans.modules.schema2beans.Version runtimeVersion = new org.netbeans.modules.schema2beans.Version(5, 0, 0);

	static public final String SUN_CMP_MAPPING = "SunCmpMapping";	// NOI18N

	public SunCmpMappings() throws org.netbeans.modules.schema2beans.Schema2BeansException {
		this(null, Common.USE_DEFAULT_VALUES);
	}

	public SunCmpMappings(org.w3c.dom.Node doc, int options) throws org.netbeans.modules.schema2beans.Schema2BeansException {
		this(Common.NO_DEFAULT_VALUES);
		initFromNode(doc, options);
	}
	protected void initFromNode(org.w3c.dom.Node doc, int options) throws Schema2BeansException
	{
		if (doc == null)
		{
			doc = GraphManager.createRootElementNode("sun-cmp-mappings");	// NOI18N
			if (doc == null)
				throw new Schema2BeansException(Common.getMessage(
					"CantCreateDOMRoot_msg", "sun-cmp-mappings"));
		}
		Node n = GraphManager.getElementNode("sun-cmp-mappings", doc);	// NOI18N
		if (n == null)
			throw new Schema2BeansException(Common.getMessage(
				"DocRootNotInDOMGraph_msg", "sun-cmp-mappings", doc.getFirstChild().getNodeName()));

		this.graphManager.setXmlDocument(doc);

		// Entry point of the createBeans() recursive calls
		this.createBean(n, this.graphManager());
		this.initialize(options);
	}
	public SunCmpMappings(int options)
	{
		super(comparators, runtimeVersion);
		initOptions(options);
	}
	protected void initOptions(int options)
	{
		// The graph manager is allocated in the bean root
		this.graphManager = new GraphManager(this);
		this.createRoot("sun-cmp-mappings", "SunCmpMappings",	// NOI18N
			Common.TYPE_1 | Common.TYPE_BEAN, SunCmpMappings.class);

		// Properties (see root bean comments for the bean graph)
		initPropertyTables(1);
		this.createProperty("sun-cmp-mapping", 	// NOI18N
			SUN_CMP_MAPPING, 
			Common.TYPE_1_N | Common.TYPE_BEAN | Common.TYPE_KEY, 
			SunCmpMapping.class);
		this.initialize(options);
	}

	// Setting the default values of the properties
	void initialize(int options) {

	}

	// This attribute is an array containing at least one element
	public void setSunCmpMapping(int index, SunCmpMapping value) {
		this.setValue(SUN_CMP_MAPPING, index, value);
	}

	//
	public SunCmpMapping getSunCmpMapping(int index) {
		return (SunCmpMapping)this.getValue(SUN_CMP_MAPPING, index);
	}

	// Return the number of properties
	public int sizeSunCmpMapping() {
		return this.size(SUN_CMP_MAPPING);
	}

	// This attribute is an array containing at least one element
	public void setSunCmpMapping(SunCmpMapping[] value) {
		this.setValue(SUN_CMP_MAPPING, value);
	}

	//
	public SunCmpMapping[] getSunCmpMapping() {
		return (SunCmpMapping[])this.getValues(SUN_CMP_MAPPING);
	}

	// Add a new element returning its index in the list
	public int addSunCmpMapping(com.sun.jdo.api.persistence.mapping.ejb.beans.SunCmpMapping value) {
		int positionOfNewItem = this.addValue(SUN_CMP_MAPPING, value);
		return positionOfNewItem;
	}

	//
	// Remove an element using its reference
	// Returns the index the element had in the list
	//
	public int removeSunCmpMapping(com.sun.jdo.api.persistence.mapping.ejb.beans.SunCmpMapping value) {
		return this.removeValue(SUN_CMP_MAPPING, value);
	}

	/**
	 * Create a new bean using it's default constructor.
	 * This does not add it to any bean graph.
	 */
	public SunCmpMapping newSunCmpMapping() {
		return new SunCmpMapping();
	}

	//
	public static void addComparator(org.netbeans.modules.schema2beans.BeanComparator c) {
		comparators.add(c);
	}

	//
	public static void removeComparator(org.netbeans.modules.schema2beans.BeanComparator c) {
		comparators.remove(c);
	}
	//
	// This method returns the root of the bean graph
	// Each call creates a new bean graph from the specified DOM graph
	//
	public static SunCmpMappings createGraph(org.w3c.dom.Node doc) throws org.netbeans.modules.schema2beans.Schema2BeansException {
		return new SunCmpMappings(doc, Common.NO_DEFAULT_VALUES);
	}

	public static SunCmpMappings createGraph(java.io.File f) throws org.netbeans.modules.schema2beans.Schema2BeansException, java.io.IOException {
		java.io.InputStream in = new java.io.FileInputStream(f);
		try {
			return createGraph(in, false);
		} finally {
			in.close();
		}
	}

	public static SunCmpMappings createGraph(java.io.InputStream in) throws org.netbeans.modules.schema2beans.Schema2BeansException {
		return createGraph(in, false);
	}

	public static SunCmpMappings createGraph(java.io.InputStream in, boolean validate) throws org.netbeans.modules.schema2beans.Schema2BeansException {
		Document doc = GraphManager.createXmlDocument(in, validate);
		return createGraph(doc);
	}

	//
	// This method returns the root for a new empty bean graph
	//
	public static SunCmpMappings createGraph() {
		try {
			return new SunCmpMappings();
		}
		catch (Schema2BeansException e) {
			throw new RuntimeException(e);
		}
	}

	public void validate() throws org.netbeans.modules.schema2beans.ValidateException {
		boolean restrictionFailure = false;
		boolean restrictionPassed = false;
		// Validating property sunCmpMapping
		if (sizeSunCmpMapping() == 0) {
			throw new org.netbeans.modules.schema2beans.ValidateException("sizeSunCmpMapping() == 0", org.netbeans.modules.schema2beans.ValidateException.FailureType.NULL_VALUE, "sunCmpMapping", this);	// NOI18N
		}
		for (int _index = 0; _index < sizeSunCmpMapping(); ++_index) {
			com.sun.jdo.api.persistence.mapping.ejb.beans.SunCmpMapping element = getSunCmpMapping(_index);
			if (element != null) {
				element.validate();
			}
		}
	}

	// Special serializer: output XML as serialization
	private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		write(baos);
		String str = baos.toString();;
		// System.out.println("str='"+str+"'");
		out.writeUTF(str);
	}
	// Special deserializer: read XML as deserialization
	private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException{
		try{
			init(comparators, runtimeVersion);
			String strDocument = in.readUTF();
			// System.out.println("strDocument='"+strDocument+"'");
			ByteArrayInputStream bais = new ByteArrayInputStream(strDocument.getBytes());
			Document doc = GraphManager.createXmlDocument(bais, false);
			initOptions(Common.NO_DEFAULT_VALUES);
			initFromNode(doc, Common.NO_DEFAULT_VALUES);
		}
		catch (Schema2BeansException e) {
			throw new RuntimeException(e);
		}
	}

	public void _setSchemaLocation(String location) {
		if (beanProp().getAttrProp("xsi:schemaLocation", true) == null) {
			createAttribute("xmlns:xsi", "xmlns:xsi", AttrProp.CDATA | AttrProp.IMPLIED, null, "http://www.w3.org/2001/XMLSchema-instance");
			setAttributeValue("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			createAttribute("xsi:schemaLocation", "xsi:schemaLocation", AttrProp.CDATA | AttrProp.IMPLIED, null, location);
		}
		setAttributeValue("xsi:schemaLocation", location);
	}

	public String _getSchemaLocation() {
		if (beanProp().getAttrProp("xsi:schemaLocation", true) == null) {
			createAttribute("xmlns:xsi", "xmlns:xsi", AttrProp.CDATA | AttrProp.IMPLIED, null, "http://www.w3.org/2001/XMLSchema-instance");
			setAttributeValue("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			createAttribute("xsi:schemaLocation", "xsi:schemaLocation", AttrProp.CDATA | AttrProp.IMPLIED, null, null);
		}
		return getAttributeValue("xsi:schemaLocation");
	}

	// Dump the content of this bean returning it as a String
	public void dump(StringBuffer str, String indent){
		String s;
		Object o;
		org.netbeans.modules.schema2beans.BaseBean n;
		str.append(indent);
		str.append("SunCmpMapping["+this.sizeSunCmpMapping()+"]");	// NOI18N
		for(int i=0; i<this.sizeSunCmpMapping(); i++)
		{
			str.append(indent+"\t");
			str.append("#"+i+":");
			n = (org.netbeans.modules.schema2beans.BaseBean) this.getSunCmpMapping(i);
			if (n != null)
				n.dump(str, indent + "\t");	// NOI18N
			else
				str.append(indent+"\tnull");	// NOI18N
			this.dumpAttributes(SUN_CMP_MAPPING, i, str, indent);
		}

	}
	public String dumpBeanNode(){
		StringBuffer str = new StringBuffer();
		str.append("SunCmpMappings\n");	// NOI18N
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
