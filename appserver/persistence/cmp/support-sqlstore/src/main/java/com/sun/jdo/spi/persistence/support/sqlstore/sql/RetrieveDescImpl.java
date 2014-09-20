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
 * RetrieveDescImpl.java
 *
 * Created on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.sql;

import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.spi.persistence.support.sqlstore.ActionDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.RetrieveDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.SQLStoreManager;
import com.sun.jdo.spi.persistence.support.sqlstore.model.ClassDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.LocalFieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.concurrency.Concurrency;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.constraint.Constraint;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.constraint.ConstraintFieldName;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.generator.SelectQueryPlan;
import com.sun.jdo.spi.persistence.utility.FieldTypeEnumeration;
import com.sun.jdo.spi.persistence.utility.ParameterInfo;
import org.glassfish.persistence.common.I18NHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ResourceBundle;

/**
 */
public class RetrieveDescImpl extends Object implements RetrieveDesc, Cloneable {

    private static final int OPINFO_FIELD_DISALLOWED =  0x01; // 1

    private static final int OPINFO_FIELD_REQUIRED =    0x02; // 2

    private static final int OPINFO_VAL_DISALLOWED =    0x04; // 4

    private static final int OPINFO_VAL_REQUIRED =      0x08; // 8

    private static final int OPINFO_VAL_IS_PCOUNT =     0x10; // 16

    private static final int OPINFO_ILLEGAL =           0x20; // 32

    private static final int OPINFO_NO_OPERATION =      0x40; // 64

    /** Indicates, that the query projects on this RD. */
    public static final int OPT_PROJECTION =    0x1; // 1

    /** Indicates, that a distinct query should be run. */
    public static final int OPT_DISTINCT =      0x2; // 2

    /** Indicates, that the selected rows should be locked for update. */
    public static final int OPT_FOR_UPDATE =    0x4; // 4

    /** Indicates, that an avg aggregate query should be run. */
    public static final int OPT_AVG =           0x08; // 8

    /** Indicates, that a min aggregate query should be run. */
    public static final int OPT_MIN =           0x10; // 16

    /** Indicates, that a max aggregate query should be run. */
    public static final int OPT_MAX =           0x20; // 32

    /** Indicates, that a sum aggregate query should be run. */
    public static final int OPT_SUM =           0x40; // 64

    /** Indicates, that a count aggregate query should be run. */
    public static final int OPT_COUNT =         0x80; // 128

    /** Special treatment for count on persistent capable objects. */
    public static final int OPT_COUNT_PC =      0x100; // 256

    /** Sum of all aggregate options. */
    public static final int OPT_AGGREGATE = OPT_AVG + OPT_MIN + OPT_MAX +
            OPT_SUM + OPT_COUNT + OPT_COUNT_PC;

    /** Sum of all aggregate options excluding count on persistent capable objects. */
    public static final int OPT_AGGREGATE_NON_COUNT_PC = OPT_AGGREGATE - OPT_COUNT_PC;

    /** Indicates, that fetch group fields should be added. */
    public static final int OPT_ADD_FETCHGROUPS = 0x200; // 512

    /**
     * Indicates, that only key fields should be added. When this option is set,
     * it modifies meaning of OPT_ADD_FETCHGROUPS. It is assumed that
     * only key fieldes from the fetch group will be added.
     */
    public static final int OPT_ADD_KEYS_ONLY = 0x400; // 1024

    /**
     * Indicates, that even if relationship fields are in DFG, they should not
     * be prefetched. The runtime will attempt to fetch relationhip fields only
     * when OPT_ADD_FETCH_GROUPS is set.
     * @see #OPT_ADD_FETCHGROUPS
     * @see #OPT_ADD_KEYS_ONLY
     */
    public static final int OPT_DISABLE_RELATIONSHIP_PREFETCH =    0x800; // 2048

    /** Indicates special treatment for version consistency verifications. */
    public static final int OPT_VERIFY =       0x1000; // 4024

    /** Array of ConstraintFieldName. */
    private ArrayList fields;

    /** The constraint stack */
    private Constraint constraint;

    /** Bitmask of options for this instance as specified by the OPT_* constants */
    private int options;

    /** Candidate class of this instance */
    private Class pcClass;

    /** Config for candidate class of this instance */
    private ClassDesc config;

    /** SelectQueryPlan for this instance */
    private SelectQueryPlan plan;

    /**
     * Discriminates different retrieve descriptors which use the same
     * navigational field.
     */
    private Object navigationalId;

    /** Result type for an aggregate query. */
    private int aggregateResultType = FieldTypeEnumeration.NOT_ENUMERATED;

    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
            RetrieveDescImpl.class.getClassLoader());

    public RetrieveDescImpl(Class pcClass, ClassDesc config) {
        super();

        this.pcClass = pcClass;
        this.config = config;
        fields = new ArrayList();
        constraint = new Constraint();
    }

    /**
     * The addResult method is used to specify which fields should be
     * returned in a persistent object. If the field requested is a
     * reference to another persistent object then a RetrieveDesc may be
     * provided which describes which fields of the referenced object
     * should be returned and, optionally, constraints on it.
     * If the parameter <code>projection</code> is true, the field
     * specified by <code>name</code> should be projected.
     *
     * @param name The name of the field to return.
     * @param foreignConstraint
     * RetrieveDesc describing fields and constraints for a referenced object.
     * @param projection Specifies, if this is a projection.
     */
    public void addResult(String name,
                          RetrieveDesc foreignConstraint,
                          boolean projection) {
        ConstraintFieldName cfName = new ConstraintFieldName(name, foreignConstraint);

        if (projection) {
            if ((options & OPT_PROJECTION) > 0) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "sqlstore.retrievedesc.toomanyprojections")); // NOI18N
            }

            // For projections on foreign fields, mark the foreign constraint.
            // For local fields, set the property on the field constraint.
            if (foreignConstraint != null) {
                ((RetrieveDescImpl) foreignConstraint).options |= OPT_PROJECTION;
            } else {
                cfName.setProjection();

                // Set this property if you want to have DFG fields added for
                // projections on local fields.
                //      options = options | OPT_PROJECTION;
            }
        }
        fields.add(cfName);
    }

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
     * @param aggregateResultType The object type returned by aggregate queries.
     * @see com.sun.jdo.spi.persistence.utility.FieldTypeEnumeration
     */
    public void addResult(int opCode, int aggregateResultType) {
        switch (opCode) {
            case ActionDesc.OP_DISTINCT:
                options = options | OPT_DISTINCT;
                break;
            case ActionDesc.OP_AVG:
                options = options | OPT_AVG;
                break;
            case ActionDesc.OP_MIN:
                options = options | OPT_MIN;
                break;
            case ActionDesc.OP_MAX:
                options = options | OPT_MAX;
                break;
            case ActionDesc.OP_SUM:
                options = options | OPT_SUM;
                break;
            case ActionDesc.OP_COUNT:
                options = options | OPT_COUNT;
                break;
            case ActionDesc.OP_COUNT_PC:
                options = options | OPT_COUNT_PC;
                break;
            default:
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.constraint.illegalop", "" + opCode)); // NOI18N
        }

        if (aggregateResultType != FieldTypeEnumeration.NOT_ENUMERATED) {
            if (this.aggregateResultType == FieldTypeEnumeration.NOT_ENUMERATED) {
                this.aggregateResultType = aggregateResultType;
            } else {
                // aggregate result type has already been set
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "sqlstore.retrievedesc.toomanyresulttypes")); // NOI18N
            }
        }
    }

    /**
     * Add a field specified by <code>name</code> to the list of fields to be prefetched.
     * @param name Name of the field to be prefetched.
     * @param foreignConstraint This parameter is null if the field is a local field.
     * If the field is a foreign field, this parameter should be not null and must refer
     * to an instance of RetrieveDesc correpsonding to the config of the foreign field.
     */
    public void addPrefetchedField(String name, RetrieveDesc foreignConstraint) {
        fields.add(new ConstraintFieldName(name, foreignConstraint, true));
    }

    /**
     * {@inheritDoc}
     */
    public void setPrefetchEnabled(boolean prefetchEnabled) {
        if (!prefetchEnabled) {
            options |= OPT_DISABLE_RELATIONSHIP_PREFETCH;
        } else {
            // options has the flag OPT_DISABLE_RELATIONSHIP_PREFETCH unset by default
        }
    }

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
    public void addConstraint(String name,
                              RetrieveDesc foreignConstraint) {
        if (name == null) {
            constraint.addField(null, foreignConstraint);
        } else  {
            constraint.addForeignField(name, foreignConstraint);
        }
    }

    /**
     * <P>Adds a constraint on the relationship field specified by
     * <code>name</code>.
     * This method is useful e.g. for comparisons of local fields with field of a related object:
     *   emp.addConstraint("lastName", ActionDesc.OP_EQ, mgr, lastName");
     * compares the employee's lastName field with the lastName field of the related manager.
     */
    public void addConstraint(String name, int operation,
                              RetrieveDesc foreignConstraint, String foreignName) {
        constraint.addField(foreignName, foreignConstraint);
        constraint.addField(name, null);
        constraint.addOperation(operation);
    }

    /**
     * The addConstraint method is used to limit the values of fields for
     * objects being selected.
     * addConstraint pushes the value, name, and operation onto the
     * Constraint stack.  The constraints are anded together by default.
     * Arbitrarily complex relationships on the Constraint stack are supported.
     * The Constraint stack can be manipulated directly if necessary.
     *
     * @param name
     * 	The name parameter specifies the field whose value
     * 	should be limited.
     * @param operation
     * 	The operation parameter specifies the relationship the field
     * 	should bear to the value.  Values for operation are defined in
     * 	the ActionDesc interface.
     * @param value
     * 	The value parameter usually specifies the value to which the
     * 	field should be limited, however it is sometimes used to
     * 	hold a parameter count as for the OP_IN operation.
     */
    public void addConstraint(String name,
                              int operation,
                              Object value) {
        int info = getOperationInfo(operation);

        if ((info & OPINFO_ILLEGAL) > 0) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.illegalop", "" + operation)); // NOI18N
        } else if ((info & OPINFO_FIELD_REQUIRED) > 0 && name == null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.fieldrequired", "" + operation)); // NOI18N
        } else if ((info & OPINFO_FIELD_DISALLOWED) > 0 && name != null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.fielddisallowed", "" + operation)); // NOI18N
        } else if ((info & OPINFO_VAL_REQUIRED) > 0 && value == null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.valrequired", "" + operation)); // NOI18N
        } else if ((info & OPINFO_VAL_DISALLOWED) > 0 && value != null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.valdisallowed", "" + operation)); // NOI18N
        }
        if ((info & OPINFO_VAL_IS_PCOUNT) > 0) {
            if (name != null) {
                constraint.addField(name, null);
            }
            addValueConstraint(name, value);
        } else {
            switch (operation) {
                case RetrieveDescImpl.OP_PARAMETER:
                    addParameterConstraint(value);
                    break;
                case ActionDesc.OP_IN:
                case ActionDesc.OP_NOTIN:
                    constraint.addConstraintFieldSubQuery(name,(ActionDesc) value);
                    break;
                default:
                    if (value != null) {
                        addValueConstraint(name, value);
                    }
                    if (name != null) {
                        constraint.addField(name, null);
                    }
                    break;
            }
        }
        if ((info & OPINFO_NO_OPERATION) == 0) {
            constraint.addOperation(operation);
        }
    }

    /**
     * Add Constraints corresponding to given <code>fields</code>.
     * The constraints are added as Parameter Constraints.
     * index of the parameter starts at given <code>startIndex</code>
     * @param fields fields for which constraints are to be added.
     * @param startIndex starting Index for the parameter.
     */
    public void addParameterConstraints(LocalFieldDesc[] fields, int startIndex) {
        for (int i = 0; i < fields.length; i++) {
            LocalFieldDesc field = fields[i];
            addParameterConstraint(field, i + startIndex);
        }
    }

    /**
     * Add ParameterConstraint corresponding to given <code>field</code>
     * at given  <code>index</code>.
     * @param field The field for which constraints are to be added.
     * @param index Index at which the ParameterConstraint is to be inserted.
     */
    public void addParameterConstraint(LocalFieldDesc field, int index) {
        // Generate parameter info for this parameter.
        String fieldName = field.getName();
        int type = field.getEnumType();

        addConstraint(null, ActionDesc.OP_PARAMETER,new ParameterInfo(index, type, fieldName));
        addConstraint(fieldName, ActionDesc.OP_FIELD, null);
        addConstraint(null, ActionDesc.OP_EQ, null);
    }

    /**
     * Adds information about <code>value</code> on the constraint stack.
     * @param name Name of the field to which the specified value is bound.
     * The query compiler can detect the correct value for name only in
     * case of simple expressions in the filter. A simple expression is
     * <em>fieldName op value</em>. If the compiler is not able to detect
     * correct value for name it will pass null.
     * @param value value to which the field's value is constrained.
     */
    private void addValueConstraint(String name, Object value) {
        constraint.addValue(value, getLocalFieldDesc(name));
    }

    /**
     * Adds information about parameter on the constraint stack.
     * @param value Instance of
     * <code>com.sun.jdo.spi.persistence.utility.ParameterInfo</code>.
     * Contains index, type and name of the field to which
     * this parameter is bound. The field name is used when binding
     * the parameter to the sql statement.
     * name can be null for complex expressions in a filter as described in
     * addValueConstraint.
     *  @see #addValueConstraint
     */
    private void addParameterConstraint(Object value) {
        if (value instanceof ParameterInfo) {
            ParameterInfo parameterInfo = (ParameterInfo)value;
            constraint.addParamIndex(parameterInfo.getIndex(), parameterInfo.getType(),
                    getLocalFieldDesc(parameterInfo.getAssociatedField()));
        } else {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.illegalParameterInfo")); // NOI18N
        }
    }

    /**
     * Returns the local field descriptor for the field <code>name</code>.
     * Delegates to <code>ClassDesc#getLocalFieldDesc(String)<code>.
     *
     * @param name Field name.
     * @return null if the <code>name</code> is null.
     * LocalFieldDesc for the field <code>name</code> otherwise.
     */
    private LocalFieldDesc getLocalFieldDesc(String name) {
        return name == null ? null : config.getLocalFieldDesc(name);
    }

    /**
     * 	The getOperationInfo method returns information about the operation
     * 	requested.  The following constants define different properties of
     * 	an operation and can be or'd together:
     *   OPINFO_FIELD_DISALLOWED A field parameter cannot be specified
     *                           with this operation
     *   OPINFO_FIELD_REQUIRED   A field parameter must be specified
     *                           with this operation
     *   OPINFO_VAL_DISALLOWED	 A value parameter cannot be specified
     *                           with this operation
     *   OPINFO_VAL_REQUIRED     A value parameter must be specified
     *                           with this operation
     *   OPINFO_NULL             This operation is a "null" type operation
     *                           (i.e. "is null", or "is not null")
     *   OPINFO_ILLEGAL          This operation is illegal
     *   OPINFO_NO_OPERATION     This isn't an operation at all,
     *                           it is some other specifier.
     *   OPINFO_VAL_IS_PCOUNT    This operation has a value which is the
     *                           parameter count of following parameter
     *                           nodes
     *
     * @param operation
     *   The operation parameter specifies the operation for which
     *   information is desired.
    */
    private static int getOperationInfo(int operation) {
        int info;
        switch (operation) {
            case ActionDesc.OP_ABS:
                info = OPINFO_VAL_DISALLOWED;
                break;
            case ActionDesc.OP_ADD:
                info = 0;
                break;
            case ActionDesc.OP_AND:
                info = 0;
                break;
            case ActionDesc.OP_FIELD:
                info = OPINFO_FIELD_REQUIRED | OPINFO_VAL_DISALLOWED | OPINFO_NO_OPERATION;
                break;
            case ActionDesc.OP_BETWEEN:
                info = 0;
                break;
            case ActionDesc.OP_DIV:
                info = 0;
                break;
            case ActionDesc.OP_EQ:
                info = 0;
                break;
            case ActionDesc.OP_NE:
                info = 0;
                break;
            case ActionDesc.OP_EQUIJOIN:
                info = 0;
                break;
            case ActionDesc.OP_NOT:
                info = OPINFO_FIELD_DISALLOWED | OPINFO_VAL_DISALLOWED;
                break;
            case ActionDesc.OP_GE:
                info = 0;
                break;
            case ActionDesc.OP_GT:
                info = 0;
                break;
            case ActionDesc.OP_IN:
            case ActionDesc.OP_NOTIN:
                info = OPINFO_FIELD_REQUIRED | OPINFO_VAL_REQUIRED;
                break;
            case ActionDesc.OP_LE:
                info = 0;
                break;
            case ActionDesc.OP_LEFTJOIN:
                info = 0;
                break;
            case ActionDesc.OP_LENGTH:
                info = OPINFO_VAL_DISALLOWED;
                break;
            case ActionDesc.OP_LIKE:
                info = 0;
                break;
            case ActionDesc.OP_LIKE_ESCAPE:
                info = 0;
                break;
            case ActionDesc.OP_LT:
                info = 0;
                break;
            case ActionDesc.OP_LTRIM:
                info = OPINFO_VAL_DISALLOWED;
                break;
            case ActionDesc.OP_MUL:
                info = 0;
                break;
            case ActionDesc.OP_OR:
                info = 0;
                break;
            case ActionDesc.OP_ORDERBY:
                info = 0;
                break;
            case ActionDesc.OP_ORDERBY_DESC:
                info = 0;
                break;
            case ActionDesc.OP_PARAMETER_COUNT:
                info = OPINFO_FIELD_DISALLOWED | OPINFO_VAL_REQUIRED | OPINFO_VAL_IS_PCOUNT | OPINFO_NO_OPERATION;
                break;
            case ActionDesc.OP_RIGHTJOIN:
                info = 0;
                break;
            case ActionDesc.OP_RTRIM:
                info = OPINFO_VAL_DISALLOWED;
                break;
            case ActionDesc.OP_SQRT:
                info = OPINFO_VAL_DISALLOWED;
                break;
            case ActionDesc.OP_SUB:
                info = 0;
                break;
            case ActionDesc.OP_SUBSTRING:
                info = 0;
                break;
            case ActionDesc.OP_POSITION:
                info = 0;
                break;
            case ActionDesc.OP_POSITION_START:
                info = 0;
                break;
            case ActionDesc.OP_MAYBE_NULL:
                info = OPINFO_FIELD_REQUIRED;
                break;
            case ActionDesc.OP_CONCAT:
                info = 0;
                break;
            case ActionDesc.OP_VALUE:
                info = OPINFO_FIELD_DISALLOWED | OPINFO_VAL_REQUIRED | OPINFO_NO_OPERATION;
                break;
            case ActionDesc.OP_PARAMETER:
                info = OPINFO_FIELD_DISALLOWED | OPINFO_VAL_REQUIRED | OPINFO_NO_OPERATION;
                break;
            case OP_NULL:
            case OP_NOTNULL:
                info = 0;
                break;
            case ActionDesc.OP_MOD:
                info = 0;
                break;
            default:
                info = OPINFO_ILLEGAL;
                break;
        }
        return info;
    }

    /**
     * Builds the internal query plan and initializes the select statements.
     * Projections on collection fields will not be resolved until the actual
     * retrieval in {@link SQLStoreManager#retrieve(
     * com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManager,
     *       RetrieveDesc, com.sun.jdo.spi.persistence.support.sqlstore.ValueFetcher)}.
     */
    public synchronized SelectQueryPlan buildQueryPlan(SQLStoreManager store,
                                                       Concurrency concurrency) {

        if (plan == null) {

            handleProjection();

            plan = SelectQueryPlan.newInstance(this, store, concurrency);

            plan.build();

            // Generate the text for the select statements.
            ArrayList statements = plan.getStatements();

            // Sanity check.
            if (statements.size() > 1) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "sqlstore.retrievedesc.stmntsnotjoined")); // NOI18N
            }
        }
        return plan;
    }

    /**
     * Sets the fetch group options for all retrieve descriptors in the
     * retrieve descriptor hierarchy. This is important for projection and
     * aggregate queries, as we don't want to select additional fields
     * from the data store. This is also important for queries where relationship
     * prefetch is involved, as we want to propagate user's choice to disable
     * relationship prefetch for a finder to all the retrieve descriptors in
     * the projection tree.
     * Also sets the projection on the candidate  class in case of queries without
     * user projection.
     */
    private void handleProjection() {
        // Prepare all the query options that need to be distributed to all the
        // RetrieveDescs involved in the projection tree.
        final int queryOptions = options & (OPT_AGGREGATE | OPT_DISABLE_RELATIONSHIP_PREFETCH);
        RetrieveDescImpl projectedDesc = distributeQueryOptions(
                queryOptions, aggregateResultType);

        if (projectedDesc == null) {
            // Set the default projection on the candidate retrieve descriptor.
            options |= OPT_PROJECTION;
            // Prepare fetch group options again after default projection has been set.
            setFetchGroupOptions(queryOptions);
        }
    }

    /**
     * Marks each descriptor with the options <code>queryOptions</code> and
     * finds the projection in the retrieve descriptor hierarchy.
     * The query options have to be known for each descriptor, because we don't want to
     * generate outer joins for OPT_COUNT_PC and we want to propagate users
     * choice to disable prefetch for a finder to projected RetrieveDesc.
     * This method relies on the fact, that each query can have only
     * <b>one</b> projection.
     *
     * @param queryOptions The options that need to be set on all
     *    retrieve descriptors. This helps identify aggregate queries, see
     *    {@link SelectQueryPlan#processJoins()}.
     *    This also helps propagate user's choice to disable prefetch to the
     *    projected RetrieveDesc, see
     *    {@link SelectQueryPlan#addFetchGroup(int, java.util.ArrayList, java.util.ArrayList)}.
     * @param aggregateResultType The aggregate result type has to
     *    be set on the projected retrieve descriptor.
     * @return The projected retrieve descriptor, or null there is
     *    no projection.
     */
    private RetrieveDescImpl distributeQueryOptions(int queryOptions,
                                                    int aggregateResultType) {
        RetrieveDescImpl projectedDesc = null;

        if ((options & OPT_PROJECTION) > 0) {
            // This is a foreign field projection.
            // Set the fetch group properties.
            setFetchGroupOptions(queryOptions);
            this.aggregateResultType = aggregateResultType;
            projectedDesc = this;
        }

        // Distribute the aggregate option to all retrieve descriptors.
        options |= queryOptions;

        // Loop through all dependent retrieve descriptors in the field list.
        for (int i = 0; i < fields.size(); i++) {
            ConstraintFieldName cfn = (ConstraintFieldName) fields.get(i);

            if (cfn.isProjection()) {
                // This is a local field projection.
                // No fetch groups are added.
                this.aggregateResultType = aggregateResultType;
                projectedDesc = this;
            } else if (cfn.desc != null) {
                projectedDesc = ((RetrieveDescImpl) cfn.desc).distributeQueryOptions(
                        queryOptions, aggregateResultType);
            }
        }

        return projectedDesc;
    }

    /**
     * Sets the fetch group policy for the projected retrieve descriptor.
     * The policy is based on the following rules:
     *
     * <ul>
     * <li>Fetchgroups are added for a projection w/o aggregates.</li>
     * <li>Only keys are added for count(*) queries.</li>
     * </ul>
     *
     * @param queryOptions The quey options that needs to be set for ********
     *    <code>queryOptions</code> Aggregate queries impose
     *    special restrictions on which fields to be selected. All aggregate
     *    queries except count(*) operate on exactly one field. Count(*)
     *    queries operate on persistence capable objects.
     * @see SelectQueryPlan#processFetchGroups(ArrayList, ArrayList)
     */
    private void setFetchGroupOptions(int queryOptions) {

        if ((queryOptions & OPT_AGGREGATE_NON_COUNT_PC) == 0) {
            // Don't add fetch groups except for projections
            // w/o aggregates or counts on persistence capable objects.
            options |= OPT_ADD_FETCHGROUPS;

            if (queryOptions == OPT_COUNT_PC)  {
                options |= OPT_ADD_KEYS_ONLY;
            }
        }
    }

    /**
     * Sets a navigational id on the retrieve descriptor. This id
     * will be used to discriminate different retrieve descriptors which
     * use the same navigational field. If not set, the field name is used.
     *
     * @param navigationalId Tag to discriminate different retrieve
     * descriptors that use the same navigational field.
     */
    public void setNavigationalId(Object navigationalId) {
        this.navigationalId = navigationalId;
    }

    /**
     * Returns the navigational id of this retrieve descriptor. This id
     * will be used to discriminate different retrieve descriptors which
     * use the same navigational field. If not set, the field name is used.
     *
     * @return Tag to discriminate different retrieve descriptors that
     * use the same navigational field.
     */
    public Object getNavigationalId() {
        return navigationalId;
    }

    /**
     * Sets option <code>option</code>. Only used to mark this
     * retrieve descriptor as internal. All valid options are defined
     * in this class.
     *
     * @param option Option being set.
     */
    public void setOption(int option) {
        this.options |= option;
    }

    /**
     * Returns the result type for aggregate queries.
     */
    public int getAggregateResultType() {
        return aggregateResultType;
    }

    /**
     * Returns the options of this retrieve descriptor.
     *
     * @return The options of this retrieve descriptor.
     */
    public int getOptions() {
        return options;
    }

    public SelectQueryPlan getPlan() {
        return plan;
    }

    public ClassDesc getConfig() {
        return config;
    }

    public void setPlan(SelectQueryPlan plan) {
        this.plan = plan;
    }

    public Class getPersistenceCapableClass() {
        return pcClass;
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public Iterator getFields() {
        return fields.iterator();
    }

    public Object clone() {
        try {
            RetrieveDescImpl clone = (RetrieveDescImpl) super.clone();
            clone.fields = new ArrayList();
            clone.constraint = new Constraint();

            return clone;
        } catch (CloneNotSupportedException e) {
            //
            // shouldn't happen.
            //
            return null;
        }
    }

}
