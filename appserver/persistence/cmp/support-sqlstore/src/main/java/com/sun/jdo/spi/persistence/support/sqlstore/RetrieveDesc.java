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
 * RetrieveDesc.java
 *
 * Created on March 3, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore;


/**
 * <P>This interface represents a retrieve descriptor used by an application
 * to retrieve container-managed entity beans from a persistent store. It
 * allows you specify which persistent fields an application wants to retrieve.
 * In addition, it allows an application to specify sophisticated constraints
 * on its object retrieval.
 */
public interface RetrieveDesc extends ActionDesc {

    /**
    * The addResult method is used to specify which fields should be
    * returned in a persistent object. If the field requested is a
    * reference to another persistent object then a RetrieveDesc may be
    * provided which describes which fields of the referenced object
    * should be returned and, optionally, constraints on it.
    * The parameter <code>projection</code> specifies, if the field
    * specified by <code>name</code> should be projected.
    *
    * @param name The name of the field to return.
    * @param foreignConstraint
    * RetrieveDesc describing fields and constraints for a referenced object.
    * @param projection Specifies, if this is a projection.
    */
   public void addResult(String name, RetrieveDesc foreignConstraint, boolean projection);

    /**
     * The addResult method can be used to specify <it>global</it>
     * query attributes that don't end up in the where clause.
     * Aggregate functions and the distinct op code are examples for
     * those query options. The result type defines the object to be
     * returned by an aggregate query. In case of distinct the result
     * type should be FieldTypeEnumeration.NOT_ENUMERATED. The method
     * might be called twice, in case of a JDOQL query having both an
     * aggregate and distinct:
     * query.setResult("avg (distinct salary)");
     * ->
     * retrieveDesc.addResult(OP_AVG, FieldTypeEnumeration.DOUBLE);
     * retrieveDesc.addResult(OP_DISTINCT, FieldTypeEnumeration.NOT_ENUMERATED);
     * retrieveDesc.addResult("salary", null, true);
     *
     * @param opCode The operation code.
     * @param resultType The object type returned by aggregate queries.
     * @see com.sun.jdo.spi.persistence.utility.FieldTypeEnumeration
     */
    public void addResult(int opCode, int resultType);

    /**
     * <P>Adds a constraint on the persistent field specified by
     * <code>name</code>. The valid values for <code>operation
     * </code> are defined in <b>ActionDesc</b>. The parameter
     * <code>value</code> specifies the constraint value.
     * <P>By default, multiple constraints are implicitly ANDed together.
     * If the applications want to OR together the constraints,
     * it can explicitly add <b>OP_OR</b> constraints. For example,
     * to OR together two constraints, an application can do the following:
     * <PRE>
     * addConstraint("field1", ActionDesc.OP_EQ, "field1Value");
     * addConstraint("field2", ActionDesc.OP_EQ, "field2Value");
     * addConstraint(null, ActionDesc.OP_OR, null);
     * </PRE>
     * <P>The important thing to note about the above example is
     * that the constraints are processed in postfix order, so
     * the above example should be read as
     * <PRE>
     * (field1 == "field1Value") OR (field2 == "field2Value")
     * </PRE>
     */
    public void addConstraint(String name, int operation, Object value);

    /**
     * <P>Adds a constraint on the foreign field specified by
     * <code>name</code>. This method is used to specify a relationship
     * navigation on field <code>name</code> to the class represented by
     * the retrieve descriptor <code>foreignConstraint</code>.
     * If <code>name</code> is null, an unrelated constraint is added.
     * A constraint is unrelated, if there is neither a foreign field
     * nor a local field connecting to the retrieve descriptor
     * <code>foreignConstraint</code>.
     */
    void addConstraint(String name, RetrieveDesc foreignConstraint);

    /**
     * <P>Adds a constraint on the field specified by <code>name</code>.
     * This method is useful e.g. for comparisons of local fields with field of a related object:
     *   emp.addConstraint("lastName", ActionDesc.OP_EQ, mgr, lastName");
     * compares the employee's lastName field with the lastName field of the related manager.
     */
    void addConstraint(String name, int operator, RetrieveDesc foreignConstraint, String foreignFieldName);

    /**
     * Sets a navigational id on the retrieve descriptor.
     * This id will be used to discriminate different retrieve descriptors which
     * use the same navigational field.
     */
    void setNavigationalId(Object navigationalId);

    /** Sets the prefetchEnabled option.
     *
     * The prefetchEnabled option specifies whether prefetch of relationship
     * fields should be enabled for this retrieve descriptor. The prefetch
     * is enabled by default if such fields are part of DFG. A user needs
     * to explicitely disable prefetch for any particular query if the related
     * instances will not be used in this transaction.
     *
     * @param prefetchEnabled the setting of the prefetchEnabled option.
     */
    void setPrefetchEnabled(boolean prefetchEnabled);
}
