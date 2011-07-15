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
 * EJBInfoHelper.java
 *
 * Created on October 15, 2004, 12:51 PM
 */

package com.sun.jdo.api.persistence.mapping.ejb;

import java.util.Collection;

import org.netbeans.modules.dbschema.SchemaElement;

import com.sun.jdo.api.persistence.model.Model;

/** This is an interface which represents information found in the 
 * ejb-jar.xml descriptor and provides a variety of other information
 * and helper objects needed to do mapping and generating of ejb related 
 * things in persistence.
 *
 * @author Rochelle Raccah
 */
public interface EJBInfoHelper
{
	/** Gets the name of the ejb jar.
	 * @return the name of the ejb jar
	 */
	public String getEjbJarDisplayName ();

	/** Gets a collection of names of schemas defined in this
	 * ejb jar.
	 * @return a collection schema names
	 */
	public Collection getAvailableSchemaNames ();

	/** Gets the name to use for schema generation.  An example might be
	 * a combo of app name, module name, etc.
	 * @return the name to use for schema generation
	 */
	public String getSchemaNameToGenerate ();

	/** Gets the schema with the specified name, loading it if necessary.
	 * @param schemaName the name of the schema to be loaded
	 * @return the schema object
	 */
	public SchemaElement getSchema (String schemaName);

	/** Gets a collection of names of cmp entity beans defined in this
	 * ejb jar.
	 * @return a collection cmp ejb names
	 */
	public Collection getEjbNames ();

	/** Gets a collection of names of cmp fields and cmr fields defined in 
	 * this ejb jar for the specified ejb.
	 * @param ejbName the name of the ejb for which a list of fields 
	 * will be created
	 * @return a collection cmp and cmr field names
	 */
	public Collection getFieldsForEjb (String ejbName);

	/** Gets a collection of names of cmr fields defined in 
	 * this ejb jar for the specified ejb.
	 * @param ejbName the name of the ejb for which a list of cmr fields 
	 * will be created
	 * @return a collection cmr field names
	 */
	public Collection getRelationshipsForEjb (String ejbName);

	/** Gets the class loader which corresponds to this ejb jar.
	 * Implementations can return <code>null</code> if this is not 
	 * relevant.
	 * @return the class loader which corresponds to this ejb jar
	 */
	public ClassLoader getClassLoader ();

	/** Gets the AbstractNameMapper object to use for this helper.
	 * @return the name mapper object
	 */
	public AbstractNameMapper getNameMapper ();

	/** Creates and returns an instance of the AbstractNameMapper object to 
	 * use for generation of unique names with this helper.  Unique names 
	 * usually means that the mapper doesn't use the same jdo and ejb names.
	 * Note that this method is a factory-like method which creates a new 
	 * instance so the caller can make modifications to it as necessary.
	 * @return the name mapper object
	 */
	public AbstractNameMapper createUniqueNameMapper ();

	/** Creates and returns an instance of the ConversionHelper object to 
	 * use for this helper.  Note that this method is a factory-like method
	 * which creates a new instance so the caller can make modifications to 
	 * it as necessary.
	 * @return the conversion helper object
	 */
	public ConversionHelper createConversionHelper ();

	/** Gets the Model object to use for this helper.
	 * @return the model object
	 */
	public Model getModel ();
}
