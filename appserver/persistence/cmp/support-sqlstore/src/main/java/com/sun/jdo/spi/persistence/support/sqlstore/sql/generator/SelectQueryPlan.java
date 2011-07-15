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
 * SelectQueryPlan.java
 *
 * Created on October 3, 2001
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.sql.generator;

import org.netbeans.modules.dbschema.ColumnElement;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.api.persistence.support.JDOUserException;
import com.sun.jdo.spi.persistence.support.sqlstore.ActionDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperSQLStore;
import com.sun.jdo.spi.persistence.support.sqlstore.RetrieveDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.SQLStoreManager;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManager;
import com.sun.jdo.spi.persistence.support.sqlstore.model.*;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.ResultDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.RetrieveDescImpl;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.concurrency.Concurrency;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.constraint.*;
import com.sun.jdo.spi.persistence.utility.FieldTypeEnumeration;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;

import java.util.*;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * This class prepares the generation of select statements,
 * by joining the constaint stacks of all retrieve descriptors
 * into one stack.
 */
public class SelectQueryPlan extends QueryPlan {

    /** This plan is joined with the parent plan. Used only for dependent plan. */
    private static final int ST_JOINED = 0x2;

    /** This plans's constraints are already processed. */
    public static final int ST_C_BUILT = 0x4;

    /** This plans's order by constraints are already processed. */
    public static final int ST_OC_BUILT = 0x10;

    /**
     * Pointer to the retrieve descriptor's constraint stack.
     * NOTE: The retrieve descriptor's stack will be modified
     * building the query plan!
     */
    protected Constraint constraint;

    /** Bitmask constaining OPT_* Constants defined in {@link RetrieveDescImpl } */
    public int options;

    /** Iterator for the retrieve descriptor's list of fields to be retrieved. */
    private Iterator fieldIterator;

    /**
     * Aggregate result type from the retrieve descriptor as defined by
     * {@link FieldTypeEnumeration}
     */
    private int aggregateResultType;

    /**
     * List of SelectQueryPlan. After this plan is completely built, this field
     * contains all the foreign plans that could not be joined with this plan.
     */
    private ArrayList foreignPlans;

    /**
     * This foreign field joins this plan to the parent plan. The field is from
     * config of the parent plan. Used only for dependent plan.
     */
    protected ForeignFieldDesc parentField;

    /**
     * This plan corresponds to prefetched values for <code>parentField</code>.
     * Used only for dependent plan.
     */
    private boolean prefetched;

    private Concurrency concurrency;

    /** BitSet containing the hierarchical fetch groups to be retrieved for this plan. */
    private BitSet hierarchicalGroupMask;

    /** BitSet containing the independent fetch groups to be retrieved for this plan. */
    private BitSet independentGroupMask;

    /** BitSet containing the fields to be retrieved for this plan */
    private BitSet fieldMask;

    private Map foreignConstraintPlans;

    private ResultDesc resultDesc;

    /**
     * Takes care of adding an "And" constraint for unbound constraints, e.g.
     * "empid == department.deptid". As the foreign constraint stack is empty,
     * we would not add the necessary "And" constraint otherwise.
     */
    // See navigation033 for an example
    private boolean appendAndOp;

    /** The logger. */
    private final static Logger logger = LogHelperSQLStore.getLogger();

    /** Name of the MULTILEVEL_PREFETCH property. */
    public static final String MULTILEVEL_PREFETCH_PROPERTY =
        "com.sun.jdo.spi.persistence.support.sqlstore.MULTILEVEL_PREFETCH"; // NOI18N

    /**
     * Property to switch on multilevel prefetch. Note, that the default
     * is false, meaning that multilevel prefetch is disabled by default.
     */
    private static final boolean MULTILEVEL_PREFETCH = Boolean.valueOf(
        System.getProperty(MULTILEVEL_PREFETCH_PROPERTY, "false")).booleanValue(); // NOI18N
    
    /**
     * Creates a new instance of SelectQueryPlan depending on the retrieve
     * descriptor options.
     * @param desc The retrieve descriptor
     * @param store The store
     * @param concurrency The concurrency for the plan.
     * @return An instance of SelectQueryPlan depending on the retrieve
     * descriptor options.
     */
    public static SelectQueryPlan newInstance(RetrieveDescImpl desc,
                                              SQLStoreManager store,
                                              Concurrency concurrency) {
        SelectQueryPlan plan = null;

        if ( (desc.getOptions() & RetrieveDescImpl.OPT_VERIFY) > 0) {
            plan = new VerificationSelectPlan(desc, store);
        } else {
            plan = new SelectQueryPlan(desc, store, concurrency);
        }

        return plan;
    }

    /**
     * Creates a new SelectQueryPlan.
     *
     * @param desc Retrieve descriptor holding the query information
     *    from the query compiler. This information includes selected
     *    fields and the query constraints. <code>desc</code> must be an
     *    instance of RetrieveDescImpl.
     * @param store Store manager executing the query.
     * @param concurrency Query concurrency.
     */
    public SelectQueryPlan(ActionDesc desc,
                           SQLStoreManager store,
                           Concurrency concurrency) {

        super(desc, store);
        action = ACT_SELECT;

        // Initialize internal fields.
        fieldMask = new BitSet();
        hierarchicalGroupMask = new BitSet();
        independentGroupMask = new BitSet();
        this.concurrency = concurrency;
        //excludeSubclasses = true;

        if (desc instanceof RetrieveDescImpl) {
            RetrieveDescImpl retrieveDesc = (RetrieveDescImpl) desc;
            retrieveDesc.setPlan(this);

            // Get the information from the retrieve descriptor.
            constraint = retrieveDesc.getConstraint();
            options = retrieveDesc.getOptions();
            fieldIterator = retrieveDesc.getFields();
            aggregateResultType = retrieveDesc.getAggregateResultType();
        } else {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                "core.generic.notinstanceof", desc.getClass().getName(), "RetrieveDescImpl")); // NOI18N
        }
    }

    public Constraint getConstraint() {
        return constraint;
    }

    private void setFieldMask(int index) {
        if (index < 0) {
            index = config.fields.size() - index;
        }

        fieldMask.set(index);
    }

    private boolean getFieldMask(int index) {
        if (index < 0) {
            index = config.fields.size() - index;
        }

        return fieldMask.get(index);
    }

    /**
     * The addTable method is used to add tables correponding to a field to the plan.
     * No columns corresponding the field are added to the plan.
     *
     * @param fieldDesc The field for which we need to add table
     */
    protected void addTable(LocalFieldDesc fieldDesc) {
        addColumn(fieldDesc, false, false);
    }

    /**
     * The addColumn method is used to specify a field for which data needs
     * to be retrieved and therefore for which we need to select a column.
     *
     * @param fieldDesc The field for which we need to retrieve data and therefore
     * for which we need to select a column.
     */
    protected void addColumn(LocalFieldDesc fieldDesc) {
        addColumn(fieldDesc, true, false);
    }

    /**
     * The addColumn method is used to specify a field for which data needs to
     * be retrieved and therefore for which we need  to select a column.
     *
     * @param fieldDesc The field for which we need to retrieve data and therefore
     * for which we need to select a column.
     * @param add Specifies if the field will be added to {@link ResultDesc}.
     * @param projection Pass the projection information for this field
     * to {@link ResultDesc}.
     */
    private void addColumn(LocalFieldDesc fieldDesc, boolean add, boolean projection) {

        // We first search to see if any of the tables to which the requested
        // field is mapped is being used by this query plan. Initially
        // there is a select statement for each table so we just append this
        // request as a column to the found statement. If none of the tables
        // are being used in this query plan then we create a new statement.
        //
        for (Iterator iter = fieldDesc.getColumnElements(); iter.hasNext(); ) {
            ColumnElement columnElement = (ColumnElement) iter.next();
            QueryTable table = findQueryTable(columnElement.getDeclaringTable());

            if (table == null)
                table = addQueryTable(columnElement.getDeclaringTable(), null);

            SelectStatement statement = (SelectStatement) getStatement(table);

            if (statement == null)
                statement = (SelectStatement) addStatement(table);

            if (add) {
                ColumnRef columnRef = statement.addColumn(columnElement, table);
                // initialize the resultDesc
                if (resultDesc == null) {
                    resultDesc = new ResultDesc(config, aggregateResultType);
                }
                resultDesc.addField(fieldDesc, columnRef, projection);
            }
        }
    }

    /**
     * Create and build the query plans for foreign fields that have been
     * added by {@link RetrieveDesc#addResult(String, RetrieveDesc, boolean)}.
     * If there is no projection, add the foreign key fields to the list
     * of fields to be selected.
     *
     * @param foreignFields List of local fields.
     * @param localFields List of fields to be selected.
     * @see RetrieveDescImpl
     */
    private void processForeignFields(ArrayList foreignFields,
                                      ArrayList localFields) {
        if (foreignFields.size() == 0) {
            return;
        }

        boolean debug = logger.isLoggable(Logger.FINEST);

        if (debug) {
            logger.finest("sqlstore.sql.generator.selectqueryplan.processforeignfield", // NOI18N
                          config.getPersistenceCapableClass().getName());
        }

        foreignPlans = new ArrayList();

        for (int i = 0; i < foreignFields.size(); i++) {
            processForeignField((ConstraintFieldName) foreignFields.get(i), localFields);
        }

        if (debug) {
            logger.finest("sqlstore.sql.generator.selectqueryplan.processforeignfield.exit"); // NOI18N
        }
    }

    /**
     * Process the projected foreign field at index <code>index</code>.
     * Initializes and builds a new SelectQueryPlan for this field and
     * adds selected columns to the result descriptor. If the field is
     * navigated only, just adds the statement and table alias.
     *
     * @param cfn
     * @param localFields
     */
    private void processForeignField(ConstraintFieldName cfn,
                                     ArrayList localFields) {

        SelectQueryPlan fp = new SelectQueryPlan(cfn.desc, store, concurrency);

        fp.prefetched = cfn.isPrefetched();
        if (fp.prefetched) {
            // Add fetch groups to the foreign plan if we are prefetching.
            fp.options |= RetrieveDescImpl.OPT_ADD_FETCHGROUPS;
        }

        fp.processParentField(config, cfn.name);
        fp.build();

        // For navigational queries, add in any additional local fields which
        // may be needed by this foreign field (typically the foreign keys).
        // For external (user) queries, we just make sure we include the
        // corresponding table into the table list.
        for (int i = 0; i < fp.parentField.localFields.size(); i++) {
            LocalFieldDesc la = (LocalFieldDesc) fp.parentField.localFields.get(i);

            if (!getFieldMask(la.absoluteID)) {
                if ((options & RetrieveDescImpl.OPT_ADD_FETCHGROUPS) > 0) {
                    // Add the field to localFields only if this plan corresponds
                    // to a candidate.
                    localFields.add(la);
                } else {
                    // This plan is participating in a projection.
                    // Add the table and a corresponding statement to the plan.
                    addTable(la);
                }
            }
        }
        foreignPlans.add(fp);
    }

    private boolean getGroupMask(int groupID) {
        if (groupID >= FieldDesc.GROUP_DEFAULT)
            return hierarchicalGroupMask.get(groupID);
        else if (groupID < FieldDesc.GROUP_NONE)
            return independentGroupMask.get(-(groupID + 1));

        return true;
    }

    private void setGroupMask(int groupID) {
        if (groupID >= FieldDesc.GROUP_DEFAULT)
            hierarchicalGroupMask.set(groupID);
        else if (groupID < FieldDesc.GROUP_NONE)
            independentGroupMask.set(-(groupID + 1));
    }

    /**
     * Add the fields from the fetch group specified by <code>groupID</code>
     * to the list of selected fields. The decision which fields are
     * added is based on the following rules:
     *
     * <ol>
     * <li>Local fields are added for all the queries and user
     *     projections, that aren't aggregates.</li>
     * <li>Only key fields are added for aggregate queries counting persistence capable objects.</li>
     * <li>Foreign fields are added only for the projected retrieve descriptor and for
     *     queries that do not have relationsip prefetch disabled. <li>
     * </ol>
     *
     * @param groupID Fetch group id.
     * @param localFields List of fields to be selected.
     * @param foreignFields List of foreign fields connecting to foreign plans.
     */
    private void addFetchGroup(int groupID,
                               ArrayList localFields,
                               ArrayList foreignFields) {
        // We should enter this method only if OPT_ADD_FETCHGROUPS is set.
        assert (options & RetrieveDescImpl.OPT_ADD_FETCHGROUPS) > 0;

        ArrayList group = config.getFetchGroup(groupID);
        setGroupMask(groupID);

        if (group != null) {
            for (int i = 0; i < group.size() ; i++) {
                FieldDesc f = (FieldDesc) group.get(i);

                if (!getFieldMask(f.absoluteID)) {
                    final boolean isLocalField = f instanceof LocalFieldDesc;
                    // Prevent testing field again.
                    setFieldMask(f.absoluteID);

                    if (isLocalField) {
                        if ((options & RetrieveDescImpl.OPT_ADD_KEYS_ONLY) == 0 || f.isKeyField()) {
                            // pk fields are added before any other fields already
                            // present in localFields. This is because pk fields
                            // are the first to be read when resultset is processed.
                            // Please see IN=8852 for more details.
                            // All other fields are appended to the list.
                            int indexToInsert = ( f.isKeyField() ? 0 : localFields.size() );
                            localFields.add(indexToInsert, f);
                        }
                    } else {
                        // Add foreign fields only if this plan corresponds to the
                        // projected RD and relationship prefetch is not explicitly
                        // disabled by the user.
                        if ( ((options & RetrieveDescImpl.OPT_PROJECTION) > 0 || MULTILEVEL_PREFETCH) &&
                              (options & RetrieveDescImpl.OPT_ADD_KEYS_ONLY) == 0 &&
                              (options & RetrieveDescImpl.OPT_DISABLE_RELATIONSHIP_PREFETCH) == 0 ) {
                            // Add current field to foreignFields as ConstraintFieldName
                            ForeignFieldDesc ff = (ForeignFieldDesc) f;
                            RetrieveDescImpl desc = (RetrieveDescImpl)
                                    store.getRetrieveDesc(ff.foreignConfig.getPersistenceCapableClass());
                            foreignFields.add(new ConstraintFieldName(ff.getName(), desc, true));
                        }
                    }
                }
            }
        }
    }

    /**
     * Add the fields from the fetch group specified by <code>groupID</code>
     * to the list of selected fields. For hierarchical groups, we add all the
     * groups from GROUP_DEFAULT up to <code>groupID</code>. For independent
     * groups, we only add the default and the group indicated by
     * <code>groupID</code>.
     *
     * @param groupID Fetch group id.
     * @param localFields List of fields to be selected.
     * @param foreignFields List of foreign fields.
     */
    private void addFetchGroups(int groupID,
                                ArrayList localFields,
                                ArrayList foreignFields) {

        if (groupID >= FieldDesc.GROUP_DEFAULT) {
            //Hierachical fetch group
            for (int i = FieldDesc.GROUP_DEFAULT; i <= groupID; i++) {
                if (!getGroupMask(i)) {
                    addFetchGroup(i, localFields, foreignFields);
                }
            }
        } else if (groupID < FieldDesc.GROUP_NONE) {
            if (!getGroupMask(FieldDesc.GROUP_DEFAULT)) {
                //Independent fetch group
                addFetchGroup(FieldDesc.GROUP_DEFAULT, localFields, foreignFields);
            }

            if (!getGroupMask(groupID)) {
                addFetchGroup(groupID, localFields, foreignFields);
            }
        }
    }

    /**
     * Add the fetch group fields to the list of selected fields.
     * The decision if fetch groups are added is based on the following rules:
     *
     * <ul>
     * <li>Always add fetch groups for internal queries.</li>
     * <li>For external queries, add fetch groups to the projected retrieve
     *    descriptor as marked in RetrieveDescImpl#setFetchGroupOptions(int)</li>
     * </ul>
     *
     * @param localFields List of fields to be selected.
     * @param foreignFields List of foreign fields.
     * @see RetrieveDescImpl#setFetchGroupOptions(int)
     */
     private void processFetchGroups(ArrayList localFields, ArrayList foreignFields) {

        if ((options & RetrieveDescImpl.OPT_ADD_FETCHGROUPS) > 0) {
            int requestedItems = localFields.size() + foreignFields.size();

            // Add the default fetch group.
            if (!getGroupMask(FieldDesc.GROUP_DEFAULT)) {
                addFetchGroups(FieldDesc.GROUP_DEFAULT, localFields, foreignFields);
            }

            if (requestedItems > 0) {
                for (int i = 0; i < localFields.size(); i++) {
                    FieldDesc f = (FieldDesc) localFields.get(i);

                    setFieldMask(f.absoluteID);

                    if (f.fetchGroup != FieldDesc.GROUP_NONE) {
                        if (!getGroupMask(f.fetchGroup)) {
                            addFetchGroups(f.fetchGroup, localFields, foreignFields);
                        }
                    }
                }

                for (int i = 0; i < foreignFields.size(); i++) {
                    ConstraintFieldName cfn = (ConstraintFieldName) foreignFields.get(i);
                    FieldDesc f = config.getField(cfn.name);

                    setFieldMask(f.absoluteID);

                    if (f.fetchGroup != FieldDesc.GROUP_NONE) {
                        if (!getGroupMask(f.fetchGroup)) {
                            addFetchGroups(f.fetchGroup, localFields, foreignFields);
                        }
                    }
                }
            }
        }
    }

    /**
     * Add all requested local fields to {@link ResultDesc}.
     *
     * @param localFields List of local fields to be selected.
     * @param projectionField The projected field.
     */
    private void processLocalFields(ArrayList localFields, LocalFieldDesc projectionField) {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (debug) {
            logger.finest("sqlstore.sql.generator.selectqueryplan.processlocalfield", // NOI18N
                          config.getPersistenceCapableClass().getName());
        }

        for (int i = 0; i < localFields.size(); i++) {
            LocalFieldDesc lf = (LocalFieldDesc) localFields.get(i);
            addColumn(lf, true, (projectionField == lf));
        }

        if (debug) {
            logger.finest("sqlstore.sql.generator.selectqueryplan.processlocalfield.exit"); // NOI18N
        }
    }

    private void joinSecondaryTableStatement(SelectStatement statement,
                                             SelectStatement secondaryTableStatement) {
        statement.copyColumns(secondaryTableStatement);

        QueryTable secondaryTable = (QueryTable) secondaryTableStatement.getQueryTables().get(0);
        ReferenceKeyDesc key = secondaryTable.getTableDesc().getPrimaryTableKey();

        addJoinConstraint(this, this,
                key.getReferencedKey().getColumns(),
                key.getReferencingKey().getColumns(), ActionDesc.OP_LEFTJOIN);

        secondaryTableStatement.markJoined();
    }

    private void processRelatedStatements(SelectStatement statement) {
        ArrayList secondaryTableStatements = statement.getSecondaryTableStatements();

        if (secondaryTableStatements != null) {
            for (int i = 0; i < secondaryTableStatements.size(); i++) {
                SelectStatement secondaryTableStatement = (SelectStatement) secondaryTableStatements.get(i);

                if (!secondaryTableStatement.isJoined()) {
                    processRelatedStatements(secondaryTableStatement);
                    joinSecondaryTableStatement(statement, secondaryTableStatement);
                }
            }

            secondaryTableStatements.clear();
        }
    }

    protected void processStatements() {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (debug) {
             Object[] items = new Object[] {config.getPersistenceCapableClass().getName(),
                                            new Integer(statements.size())};
             logger.finest("sqlstore.sql.generator.selectqueryplan.processstmts",items); // NOI18N
        }

        if (concurrency != null) {
            concurrency.select(this);
        }

        int size = statements.size();

        if (size > 1) {
            super.processStatements();

            for (int i = 0; i < size; i++) {
                SelectStatement s = (SelectStatement) statements.get(i);

                if (!s.isJoined())
                    processRelatedStatements(s);
            }

            // Remove all the statements that have been joined.
            for (int i = 0; i < statements.size(); i++) {
                SelectStatement s = (SelectStatement) statements.get(i);

                if (s.isJoined()) {
                    statements.remove(i);
                    i--;
                    continue;
                }
            }
        }

        if (debug) {
            logger.finest("sqlstore.sql.generator.selectqueryplan.processstmts.exit"); // NOI18N
        }
    }


    /**
     * Asociates every local constraint on the stack with it's original plan
     * and include any table that hasn't been added to the table list of the
     * corresponding original plan.
     *
     * @see ConstraintFieldName#originalPlan
     */
    private void processLocalConstraints() {
        List stack = constraint.getConstraints();

        for (int i = 0; i < stack.size(); i++) {
            ConstraintNode node = (ConstraintNode) stack.get(i);

            if (node instanceof ConstraintFieldName) {
                ConstraintFieldName fieldNode = (ConstraintFieldName) node;

                if (fieldNode.originalPlan == null) {
                    // The field has not been processed before
                    SelectQueryPlan thePlan = null;

                    if (fieldNode.desc == null) {
                        thePlan = this;
                    } else {
                        // If the field belongs to a different RetrieveDesc, we need
                        // to use the query plan associated with that RetrieveDesc.
                        RetrieveDescImpl rd = (RetrieveDescImpl) fieldNode.desc;
                        thePlan = newForeignConstraintPlan(rd, fieldNode.name);
                    }

                    fieldNode.originalPlan = thePlan;

                    // The name field is null for unrelated constraints.
                    if (fieldNode.name != null) {
                        FieldDesc field = thePlan.config.getField(fieldNode.name);

                        if (field instanceof LocalFieldDesc) {
                            // Adds the statement and table for the field.
                            // This is only required to process plans corresponding
                            // to query filters containing unbound variables
                            // e.g. setFilter("empid == d.deptid") on an employee query.

                            thePlan.addTable((LocalFieldDesc) field);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the plan for {@link RetrieveDesc} <code>rd</code>. If there is no
     * plan associated with <code>rd</code>, a new plan is created.
     * If <code>fieldName</code> is not null, the returned plan will be matched
     * with the first plan that was registered for a navigation on the field
     * or the plan's navigational id. The navigational id is used to discriminate
     * several navigations on the same field.
     *
     * @param rd Foreign retrieve descriptor.
     * @param fieldName Parent field name.
     * @return The plan for {@link RetrieveDesc} <code>rd</code>.
     * @see RetrieveDescImpl
     */
    private SelectQueryPlan newForeignConstraintPlan(RetrieveDescImpl rd,
                                                     String fieldName) {

        SelectQueryPlan fcp = rd.getPlan();

        if (fcp == null) {
            fcp = new SelectQueryPlan(rd, store, null);
        }
        // If fieldName is null, it means that we don't know what the relationship
        // field name is yet and this query plan is a place holder.
        if (fieldName == null) {
            return fcp;
        }

        if (foreignConstraintPlans == null) {
            foreignConstraintPlans = new HashMap();
        }

        SelectQueryPlan masterPlan = null;

        Object tag = (rd.getNavigationalId() != null) ? rd.getNavigationalId() : fieldName;

        if ((masterPlan = (SelectQueryPlan) foreignConstraintPlans.get(tag)) != null) {
            // Share the tables with the master plan.
            fcp.tables = masterPlan.tables;
            fcp.foreignConstraintPlans = masterPlan.foreignConstraintPlans;
        } else {
            foreignConstraintPlans.put(tag, fcp);
            fcp.foreignConstraintPlans = new HashMap();
        }

        return fcp;
    }

    /**
     * Adds a subquery constraint for a correlated exists query to the
     * constraint stack. Also add the table alias from the subquery
     * to the local table aliases.
     *
     * @param ff The relationship field for the subquery
     * @param operation {@link ActionDesc#OP_NOTEXISTS}
     * or {@link ActionDesc#OP_EXISTS}.
     */
    private void addCorrelatedExistsQuery(ForeignFieldDesc ff, int operation) {

        Class classType = (ff.cardinalityUPB > 1) ? ff.getComponentType() : ff.getType();
        RetrieveDescImpl rd = (RetrieveDescImpl) store.getRetrieveDesc(classType);

        SelectQueryPlan subqueryPlan = new CorrelatedExistSelectPlan(rd, store, ff, this);
        subqueryPlan.build();

        // Make the tables involved in the subquery known to the parent query.
        addQueryTables(subqueryPlan.tables);

        ConstraintSubquery subqueryConstraint = new ConstraintSubquery();
        subqueryConstraint.operation = operation;
        subqueryConstraint.plan = subqueryPlan;

        constraint.stack.add(subqueryConstraint);
    }

    /**
     * Builds the constraint plan for foreign constraints on the constraint
     * stack. This method joins the current plan with all plans
     * related by foreign constraints found in the plan hierarchy.
     *
     * @see RetrieveDescImpl
     */
    private void processForeignConstraints() {
        List currentStack = constraint.getConstraints();
        constraint.stack = new ArrayList();
        int index = 0;

        while (index < currentStack.size()) {
            ConstraintNode node = (ConstraintNode) currentStack.get(index);

            if (node instanceof ConstraintForeignFieldName) {
                processForeignFieldConstraint((ConstraintForeignFieldName) node);

            } else if (node instanceof ConstraintFieldName) {
                index = processLocalFieldConstraint((ConstraintFieldName) node, currentStack, index);

            } else if (node instanceof ConstraintFieldNameSubQuery) {
                addCorrelatedInQuery((ConstraintFieldNameSubQuery) node);

            } else {
                constraint.stack.add(node);
            }

            index++;
        }
    }

    /**
     * Joins the current plan with the constraint <code>node</code>. The constraint
     * includes the name of the parent field and the retrieve descriptor for the
     * related class. The plans will be joined with <code>OP_EQUIJOIN</code>.
     * The constraints processed here have been added by
     * {@link RetrieveDesc#addConstraint(String, RetrieveDesc)}.
     *
     * @param node Join constraint.
     */
    private void processForeignFieldConstraint(ConstraintForeignFieldName node) {
        RetrieveDescImpl rd = (RetrieveDescImpl) node.desc;

        if (rd == null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "sqlstore.constraint.noretrievedesc", // NOI18N
                    node.name, config.getPersistenceCapableClass().getName()));
        }

        SelectQueryPlan fcp = newForeignConstraintPlan(rd, node.name);

        if ((fcp.status & ST_JOINED) == 0) {
            fcp.processParentField(config, node.name);
            // Joins on constraints always join as equijoin
            processJoin(fcp, ActionDesc.OP_EQUIJOIN);
            fcp.appendAndOp = true;
        } else {
            fcp.appendAndOp = false;
        }
    }

    /**
     * Joins unrelated constraints that have been added by
     * {@link RetrieveDesc#addConstraint(String, RetrieveDesc)} where the
     * name of the foreign field is null. Other constraints have been added by
     * {@link RetrieveDesc#addConstraint(String, int, RetrieveDesc, String)}
     *
     * @param node Join constraint.
     * @param currentStack Current (old) constraint stack.
     * @param index Index in current stack.
     */
    private int processLocalFieldConstraint(ConstraintFieldName node,
                                            List currentStack,
                                            int index) {
        if (node.desc != null) {
            SelectQueryPlan fcp = ((RetrieveDescImpl) node.desc).getPlan();

            constraint.stack.add(node);

            if ((fcp.status & ST_JOINED) == 0) {
                fcp.appendAndOp = true;
                // Local fields connecting to foreign plans
                // (non relationship constraints) are not processed here
                // because we want to join on all foreign fields first.
            } else {
                // The foreign plan has already been joined. We need
                // to add another And-constraint for
                // FieldName-constraints using the same retrieve
                // descriptor.  This is only required for query filters
                // comparing local fields from different tables,
                // e.g. setFilter("empid == department.deptid") on an
                // employee query.

                // Push the remaining operand and the operator onto the stack.
                constraint.stack.add(currentStack.get(++index));
                constraint.stack.add(currentStack.get(++index));
                if (fcp.appendAndOp) {
                    constraint.addOperation(ActionDesc.OP_AND);
                    fcp.appendAndOp = false;
                }
             }
        } else {
            index = processForeignFieldNullComparision(node, currentStack, index);
        }
        return index;
    }

    /**
     * Processes a null comparision on a foreign field.
     *
     * @param node Current node..
     * @param currentStack Current (old) constraint stack.
     * @param index Index in current stack.
     */
    private int processForeignFieldNullComparision(ConstraintFieldName node,
                                                   List currentStack,
                                                   int index) {
        boolean addCurrentNode = true;
        if (node.name != null) {
            // The name entry is null for unbound constraints.
            FieldDesc f = config.getField(node.name);

            if (f instanceof ForeignFieldDesc && (index + 1 < currentStack.size())) {
                ConstraintNode nextNode = (ConstraintNode) currentStack.get(++index);
                if ((nextNode instanceof ConstraintOperation) &&
                        ((((ConstraintOperation) nextNode).operation == ActionDesc.OP_NULL) ||
                        (((ConstraintOperation) nextNode).operation == ActionDesc.OP_NOTNULL))) {

                    processNullConstraint((ForeignFieldDesc) f, nextNode);
                } else {
                    constraint.stack.add(node);
                    constraint.stack.add(nextNode);
                }
                // Current node has been processed above.
                addCurrentNode = false;
            }
        }
        if (addCurrentNode) {
            constraint.stack.add(node);
        }
        return index;
    }

    /**
     * Handles the comparison of a relationship field with (non-) null.
     * Comparisons for non-collection relationships not mapped to jointables can be
     * optimized to comparing the foreign key columns being (non-) null. All other
     * cases lead to a nested (NOT-) EXISTS query.
     *
     * @param ff Relationship field.
     * @param nextNode Constraint operation, either for null or non-null comparison.
     */
    private void processNullConstraint(ForeignFieldDesc ff, ConstraintNode nextNode) {

        if (ff.hasForeignKey()) {
            // Optimize the query to compare the foreign key fields with null.
            ArrayList localFields = ff.getLocalFields();

            for (int j = 0; j < localFields.size(); j++) {
                constraint.stack.add(new ConstraintFieldDesc((LocalFieldDesc) localFields.get(j)));
                constraint.stack.add(nextNode);
            }
        } else {
            // Otherwise, generate a nested (NOT-) EXISTS sub query.
            int subOp = ActionDesc.OP_NOTEXISTS;

            if (((ConstraintOperation) nextNode).operation == ActionDesc.OP_NOTNULL) {
                subOp = ActionDesc.OP_EXISTS;
            }

            // Add a subquery constraint for this field
            addCorrelatedExistsQuery(ff, subOp);
        }
    }

    /**
     * Creates and builds a correlated "In" subquery.
     * Merges tables from the subquery plan to the current plan and adds
     * the local fields corresponding to the subquery to the constaints.
     * The subquery is added to the constraint stack.
     *
     * @param node subquery constraint.
     */
    private void addCorrelatedInQuery(ConstraintFieldNameSubQuery node) {
        FieldDesc field = config.getField(node.fieldName);
        RetrieveDescImpl rd = (RetrieveDescImpl) node.desc;

        if (field != null && field instanceof ForeignFieldDesc) {
            ForeignFieldDesc ff = (ForeignFieldDesc) field;

            if (ff.getComponentType() != rd.getPersistenceCapableClass() ) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.constraint.unknownfield", // NOI18N
                        node.fieldName, rd.getPersistenceCapableClass().getName()));
            }

            SelectQueryPlan subqueryPlan = new CorrelatedInSelectPlan(rd, store, ff, this);
            subqueryPlan.build();

            // Make the tables involved in the subquery known to the parent query.
            addQueryTables(subqueryPlan.tables);

            // Push a new subquery constraint on the stack
            ConstraintSubquery subqueryConstraint = new ConstraintSubquery();
            subqueryConstraint.plan = subqueryPlan;
            constraint.stack.add(subqueryConstraint);

            ArrayList localFields = ff.getLocalFields();
            // Add the local fields corresponding to the subquery to the stack.
            for (int i = 0; i < localFields.size(); i++) {
                constraint.addField((LocalFieldDesc) localFields.get(i), this);
            }
        } else {
            // We didn't get a ForeignFieldDesc from config,
            // or the field is not present in the config.
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.unknownfield", // NOI18N
                    node.fieldName, rd.getPersistenceCapableClass().getName()));
        }
    }

    /**
     * Builds the constraint plan for unbound contraints between
     * different retrieve descriptors. <em>Unbound</em> constraints
     * do not navigate a relationship, i.e. there isn't a {@link
     * ConstraintForeignFieldName} connecting the two retrieve
     * descriptors. This method handles filters like <code>setFilter("empid ==
     * d.deptid")</code> on an employee query, where <code>d</code> is
     * the unbound variable.  These constraints have been added by
     * {@link RetrieveDesc#addConstraint(String, int, RetrieveDesc, String)}.
     */
    private void processUnboundConstraints() {
        List currentStack = constraint.getConstraints();
        constraint.stack = new ArrayList();

        for (int i = 0; i < currentStack.size(); i++) {
            ConstraintNode node = (ConstraintNode) currentStack.get(i);

            if (node instanceof ConstraintFieldName) {
                ConstraintFieldName fieldNode = (ConstraintFieldName) node;

                if (fieldNode.name != null) {
                    constraint.stack.add(fieldNode);
                } else if (fieldNode.desc != null) {
                    SelectQueryPlan fcp = ((RetrieveDescImpl) fieldNode.desc).getPlan();

                    // Do the join.
                    if ((fcp.status & ST_JOINED) == 0) {
                        // As this is a "real" non-relationship join,
                        // do not force the addition of an and constraint.
                        fcp.appendAndOp = false;

                        processJoin(fcp, ActionDesc.OP_NONREL_JOIN);
                    }
                }
            } else {
                constraint.stack.add(node);
            }
        }
    }

    /**
     * Builds the foreign constraint plan <code>fcp</code> without
     * adding any new fields to {@link ResultDesc} and joins
     * the plans with the join operation <code>joinOp</code>.
     *
     * @param fcp Foreign constraint plan.
     * @param joinOp Join operation.
     */
    private void processJoin(SelectQueryPlan fcp, int joinOp) {
        fcp.processConstraints();
        doJoin(fcp, joinOp);
    }

    /**
     * Sets the plan's parent field and adds the tables for the join columns
     * to the table list. The parent field is identified by <code>fieldName</code>
     * and defined in the model information of the parent class
     * <code>parentConfig</code>.
     *
     * @see ClassDesc
     */
    private void processParentField(ClassDesc parentConfig, String fieldName) {
        if (parentField == null) {
            // The plan has not been processed before
            FieldDesc f = parentConfig.getField(fieldName);

            if (f == null || !(f instanceof ForeignFieldDesc)) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.constraint.unknownfield", // NOI18N
                        fieldName, parentConfig.getPersistenceCapableClass().getName()));
            }
            parentField = (ForeignFieldDesc) f;

            // Add the join table, if neccessary.
            if (parentField.useJoinTable()) {
                // It is important to add the join table here so that this table does not
                // get lost behind the same join table in parentPlan's table list
                // See collection38
                //
                for (int i = 0; i < parentField.assocLocalColumns.size(); i++) {
                    ColumnElement col = (ColumnElement) parentField.assocLocalColumns.get(i);
                    addQueryTable(col.getDeclaringTable(), config);
                }
            }

            // Add the joined tables.
            // This is required for cases where no fields from this plan are selected
            // The side-effect for this is to create statements with no columns.
            for (int i = 0; i < parentField.foreignColumns.size(); i++) {
                ColumnElement col = (ColumnElement) parentField.foreignColumns.get(i);
                addQueryTable(col.getDeclaringTable(), config);
            }
        }
    }

    /**
     * Builds the query plan for a select type
     * {@link ActionDesc} (i.e. a {@link RetrieveDesc}).
     */
    public void build() {
        // Plan must be build only once.
        if ((status & ST_BUILT) > 0) {
            return;
        }

        processFields();

        processConstraints();

        processJoins();

        processOrderConstraints();

        status |= ST_BUILT;
   }

    /**
     * Process the fields from the retrieve descriptor's field list.
     * <em>Must be overwritten by subquery plans!</em>
     */
    protected void processFields() {
        ArrayList foreignFields = new ArrayList();
        ArrayList localFields = new ArrayList();

        LocalFieldDesc projectionField = separateFieldList(localFields, foreignFields);

        // Because of a problem with BLOB columns on SQLServer
        // fetch group fields are added to the beginning of localFields.
        processFetchGroups(localFields, foreignFields);

        // For internal queries, processForeignFields might add additional
        // fields to localFields, so we call it first.
        processForeignFields(foreignFields, localFields);
        processLocalFields(localFields, projectionField);
    }

    /**
     * Separates the retrieve descriptor's field list. Cull out the
     * foreign field constraints into <code>foreignFields</code>. Get
     * the field descriptors of local fields and put them into
     * <code>localFields</code>.
     *
     * @param localFields List of LocalFieldDesc.
     * @param foreignFields List of ConstraintFieldName.
     * @return LocalFieldDesc of the projected field.
     */
    private LocalFieldDesc separateFieldList(ArrayList localFields,
                                             ArrayList foreignFields) {

        LocalFieldDesc projectionField = null;

        while (fieldIterator.hasNext()) {
            ConstraintFieldName cfn = (ConstraintFieldName) fieldIterator.next();
            FieldDesc f = config.getField(cfn.name);

            if (f == null) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.constraint.unknownfield", // NOI18N
                        cfn.name, config.getPersistenceCapableClass().getName()));
            }

            setFieldMask(f.absoluteID);

            if (cfn.desc != null) {
                foreignFields.add(cfn);
            } else {
                localFields.add(f);
                if (cfn.isProjection()) {
                    projectionField = (LocalFieldDesc) f;
                }
            }
        }

        return projectionField;
    }

    protected void processConstraints() {
        // Constraints must be build only once.
        if ((status & ST_BUILT) > 0 || (status & ST_C_BUILT) > 0) {
            return;
        }

        processLocalConstraints();

        // Join all the statements.
        processStatements();

        processForeignConstraints();

        // Joins over unbound variables.
        processUnboundConstraints();

        status |= ST_C_BUILT;
    }

    /**
     * Joins the current plan with <code>foreignPlan</code>.
     * The join operation <code>joinOperation</code>
     * will be added to the constraint stack.
     *
     * @param foreignPlan Query plan to be joined.
     * @param joinOperation Join operation. No join constaint is
     *  added for non relationship joins.
     */
    private void doJoin(SelectQueryPlan foreignPlan, int joinOperation) {
        if ((foreignPlan.status & ST_JOINED) > 0) {
            return;
        }

        mergeConstraints(foreignPlan, joinOperation);

        mergeStatements(foreignPlan, joinOperation);

        if (foreignPlan.tables != null) {
            addQueryTables(foreignPlan.tables);
        }

        foreignPlan.status = foreignPlan.status | ST_JOINED;
    }

    /**
     * Merge the foreign statement with us.<br />
     * If there is no foreign statement,
     * this method just returns.
     *
     * @param foreignPlan Foreign plan to be joined.
     * @param joinOperation Join operator.
     */
    private void mergeStatements(SelectQueryPlan foreignPlan, int joinOperation) {
        SelectStatement fromStatement;
        SelectStatement toStatement;

        if (foreignPlan.statements.size() > 0) {
            toStatement = (SelectStatement) foreignPlan.statements.get(0);

            // Merge the foreign query with us.
            if (statements.size() > 0) {
                fromStatement = (SelectStatement) statements.get(0);

                // Copy projected columns
                fromStatement.copyColumns(toStatement);

                // For a non relationship join, we need to add all tables from the
                // foreign statement. In any other case, the tables will be added
                // when the join operation is processed by the statement.
                if (joinOperation == ActionDesc.OP_NONREL_JOIN) {
                    fromStatement.tableList.addAll(toStatement.tableList);
                }

                mergeResultDesc(foreignPlan);
                if (foreignPlan.prefetched) {
                    // If the foreign plan is marked as a prefetched plan,
                    // propagate this information to its resultDesc.
                    resultDesc.setPrefetching();
                }

                this.options |= foreignPlan.options;
            }
        }
    }

    /**
     * Merge the foreign constraints with us.<br />
     * Adds the appropriate Join-constraint to the stack and merges
     * the constraint stacks. Adds an And-constraint, if neccessary,
     * see {@link com.sun.jdo.spi.persistence.support.sqlstore.sql.constraint.Constraint#mergeConstraint}.
     *
     * @param foreignPlan Foreign plan to be joined.
     * @param joinOperation Join operator.
     */
    private void mergeConstraints(SelectQueryPlan foreignPlan, int joinOperation) {

        if (joinOperation != ActionDesc.OP_NONREL_JOIN) {
            // Add the join constraint.
            if (foreignPlan.parentField.useJoinTable()) {
                // The join table is added to the foreign plan while processing
                // parent field
                addJoinConstraint(this, foreignPlan,
                        foreignPlan.parentField.localColumns,
                        foreignPlan.parentField.assocLocalColumns, joinOperation);

                addJoinConstraint(foreignPlan, foreignPlan,
                        foreignPlan.parentField.assocForeignColumns,
                        foreignPlan.parentField.foreignColumns, joinOperation);

                // Except for oracle, the outer join condition will end up in
                // from clause. Hence, add OP_AND for Equijoin only
                if (joinOperation == ActionDesc.OP_EQUIJOIN) {
                    constraint.addOperation(ActionDesc.OP_AND);
                }
            } else {
                addJoinConstraint(this, foreignPlan,
                        foreignPlan.parentField.localColumns,
                        foreignPlan.parentField.foreignColumns, joinOperation);
            }
        }

        // Copy the constraints from the toStack.
        boolean addAnd = constraint.mergeConstraint(foreignPlan.constraint, joinOperation);

        if (addAnd || foreignPlan.appendAndOp) {
            constraint.addOperation(ActionDesc.OP_AND);
        }
    }

    /**
     * Joins <code>foreignPlan</code>'s result
     * descriptor with the current plan.
     *
     * @param foreignPlan Query plan to be joined.
     */
    private void mergeResultDesc(SelectQueryPlan foreignPlan) {
        ResultDesc foreignResult = foreignPlan.resultDesc;

        if (resultDesc != null && foreignResult != null) {
            resultDesc.doJoin(foreignResult, foreignPlan.parentField);
        } else if (resultDesc == null) {
            resultDesc = foreignResult;
        }
    }

    /**
     * Put in a join constraint to the foreign table.
     *
     * @param fromPlan The plan for fromColumns
     * @param toPlan The plan for toColumns
     * @param fromColumns List of local columns.
     * @param toColumns List of foreign columns.
     * @param joinOp Join operation. This operation is never a non relationship join.
     */
    protected void addJoinConstraint(SelectQueryPlan fromPlan,
                                     SelectQueryPlan toPlan,
                                     ArrayList fromColumns,
                                     ArrayList toColumns,
                                     int joinOp) {

        ConstraintJoin join = new ConstraintJoin();

        join.operation = joinOp;
        join.fromColumns = fromColumns;
        join.fromPlan = fromPlan;
        join.toColumns = toColumns;
        join.toPlan = toPlan;

        constraint.addJoinConstraint(join);
    }

    /**
     * Compares the statements generated for the current plan
     * against the statements generated for foreign query plans
     * and joins any statements together which it can. This method
     * joins query plans on foreign fields that have been added by
     * {@link RetrieveDesc#addResult(String,RetrieveDesc,boolean)}.
     *
     * @see RetrieveDescImpl#buildQueryPlan(SQLStoreManager, Concurrency)
     */
    private void processJoins() {

        if (foreignPlans == null) {
            return;
        }

        for (Iterator iter = foreignPlans.iterator(); iter.hasNext(); ) {
            SelectQueryPlan fp = (SelectQueryPlan) iter.next();

            if ((fp.status & ST_JOINED) == 0) {
                // Recursively join foreign plans of the foreign plan.
                fp.processJoins();

                // TODO: We only join to foreign query plans that involve one statement.
                if (statements.size() == 1 && fp.statements.size() == 1) {
                    doJoin(fp, getJoinOperator(fp));
                }
            }

            if ((fp.status & ST_JOINED) > 0) {
                // Foreign plan has been joined
                iter.remove();
            }
        }

        // Sanity check.
        if (foreignPlans != null && foreignPlans.size() > 0) {

            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "sqlstore.sql.generator.selectqueryplan.plansnotjoined")); // NOI18N
        } else {
            foreignPlans = null;
        }
    }

    /**
     * Defines the join operator based on the projection property (and
     * the navigated relationship).  Depending on the relationship
     * cardinality the plans are joined with <code>OP_EQUIJOIN</code>
     * or <code>OP_LEFTJOIN</code>. Projection queries are always
     * joined with <code>OP_LEFTJOIN</code>.)
     *
     * @param dependentPlan The dependent plan
     * @return Join Operator.
     */
    private int getJoinOperator(SelectQueryPlan dependentPlan) {
        int joinOperator;
        ForeignFieldDesc parentField = null;

        if (isProjection(this)) {
            parentField = dependentPlan.parentField;
        } else if (isProjection(dependentPlan)) {
            parentField = dependentPlan.parentField.getInverseRelationshipField();
        }

        if (parentField != null) {

            joinOperator = ActionDesc.OP_LEFTJOIN;
            // TODO: Check the parentField's cardinality?
//            if (parentField.cardinalityUPB == 1) {
//                // Join "to one" associations.
//                // There are two kinds of "to one" associations:
//                if (parentField.cardinalityLWB == 1) {
//                    // 1-1 associations: (fp.parentField.cardinalityLWB == 1)
//                    joinOperator = ActionDesc.OP_EQUIJOIN;
//                } else {
//                    // Optional associations: (fp.parentField.cardinalityLWB == 0)
//                    // The query should return an object, even if the navigated
//                    // relationship isn't set.
//                    joinOperator = ActionDesc.OP_LEFTJOIN;
//                }
//            } else { // parentField.cardinalityUPB > 1
//                // To-Many associations are always optional.
//                joinOperator = ActionDesc.OP_LEFTJOIN;
//            }
        } else {
            joinOperator = ActionDesc.OP_EQUIJOIN;
        }

        return joinOperator;
    }

    private boolean isProjection(SelectQueryPlan plan) {
        return(prefetched
                || (plan.options & RetrieveDescImpl.OPT_PROJECTION) > 0
                && (plan.options & RetrieveDescImpl.OPT_AGGREGATE) == 0);
    }

    /**
     * Converts ConstraintFieldName used in Order by constraints into
     * ConstraintFieldDesc using ConstraintFieldName#originalPlan.<br />
     *
     * <em>Currently unused functionality:</em><br />
     * Gets all the "order by" constraints from the the current stack.
     * The constraints are ordered such that any "order by" constraint
     * with position N is placed before any "order by" constraint with
     * position N+m, where m > 0.  Also any "order by" constraint with
     * no position (i.e. position < 0) are placed immediately
     * following the previous "order by" constraint with a position.
     * The order of the "order by" constraints on the constraint stack
     * is changed to effect this ordering.
     * <em>NOTE:</em> The value constraints giving the position for
     * the order by constraints is currently not generated by the
     * query compiler.
     */
    public void processOrderConstraints() {

        if ((status & ST_BUILT) > 0 || (status & ST_OC_BUILT) > 0) {
            return;
        }

        ArrayList orderByArray = new ArrayList();

        int i, pos;
        int insertAt = 0;

        // Now pull out all "order by" constraints and convert
        // ConstraintFieldName to ConstraintFieldDesc expected
        // by SelectStatement.
        if (constraint != null) {
            i = 0;
            while (i < constraint.stack.size()) {
                ConstraintNode opNode = (ConstraintNode) constraint.stack.get(i);

                if ((opNode instanceof ConstraintOperation)
                        && ((((ConstraintOperation) opNode).operation == ActionDesc.OP_ORDERBY) ||
                        (((ConstraintOperation) opNode).operation == ActionDesc.OP_ORDERBY_DESC))) {
                    pos = -1;
                    if ((i > 1) && (constraint.stack.get(i - 2) instanceof ConstraintValue)) {
                        pos = ((Integer) ((ConstraintValue) constraint.stack.get(i - 2)).getValue() ).intValue();
                        constraint.stack.remove(i - 2);
                        i = i - 1;
                    }

                    if (pos > 0) {
                        insertAt = pos;
                    }

                    for (int k = orderByArray.size(); k <= insertAt; k++) {
                        orderByArray.add(null);
                    }

                    if (orderByArray.get(insertAt) == null) {
                        orderByArray.set(insertAt, new ArrayList());
                    }

                    ConstraintNode fieldNode = (ConstraintNode) constraint.stack.get(i - 1);
                    ConstraintFieldDesc consFieldDesc = null;

                    if (fieldNode instanceof ConstraintFieldName) {
                        QueryPlan originalPlan = this;

                        if (((ConstraintField) fieldNode).originalPlan != null) {
                            originalPlan = ((ConstraintField) fieldNode).originalPlan;
                        }

                        FieldDesc fieldDesc = originalPlan.config.
                                getField(((ConstraintFieldName) fieldNode).name);

                        if (!(fieldDesc instanceof LocalFieldDesc)) {
                            throw new JDOUserException(I18NHelper.getMessage(messages,
                                    "core.generic.notinstanceof", // NOI18N
                                    fieldDesc.getClass().getName(),
                                    "LocalFieldDesc")); // NOI18N
                        }

                        consFieldDesc = new ConstraintFieldDesc((LocalFieldDesc) fieldDesc,
                                originalPlan, 1);
                    } else if (fieldNode instanceof ConstraintFieldDesc) {
                        consFieldDesc = (ConstraintFieldDesc) fieldNode;
                    } else {
                        throw new JDOUserException(I18NHelper.getMessage(messages,
                                "core.generic.notinstanceof", // NOI18N
                                fieldNode.getClass().getName(),
                                "ConstraintFieldName/ConstraintFieldDesc")); // NOI18N
                    }

                    if (((ConstraintOperation) opNode).operation == ActionDesc.OP_ORDERBY_DESC) {
                        consFieldDesc.ordering = -1;
                    }

                    // Remember constraint in orderByArray.
                    ArrayList temp = (ArrayList) (orderByArray.get(insertAt));
                    temp.add(consFieldDesc);

                    constraint.stack.remove(i);
                    constraint.stack.remove(i - 1);
                    i = i - 2 + 1;
                }
                i = i + 1;
            }
        }

        for (int j = 0, size = orderByArray.size(); j < size; j++) {
            ArrayList oa = (ArrayList) orderByArray.get(j);

            if (constraint == null) {
                constraint = new Constraint();
            }

            for (int k = 0, sizeK = oa.size(); k < sizeK; k++) {
                ConstraintFieldDesc ob = (ConstraintFieldDesc) oa.get(k);

                if (ob.ordering < 0) {
                    constraint.addField(ob);
                    constraint.addOperation(ActionDesc.OP_ORDERBY_DESC);
                } else {
                    constraint.addField(ob);
                    constraint.addOperation(ActionDesc.OP_ORDERBY);
                }
            }
        }

        status |= ST_OC_BUILT;
    }

    protected Statement newStatement() {
        return new SelectStatement(store.getVendorType(), this);
    }

    /**
     * Extract data from given <code>resultData</code>
     * @param pm The PersistenceManager.
     * @param resultData The result set from which data is to be extracted.
     * @return Result from the given <code>resultData</code>
     * @throws SQLException
     */
    public Object getResult(PersistenceManager pm, ResultSet resultData)
            throws SQLException{
        return  resultDesc.getResult(pm, resultData);
    }

}
