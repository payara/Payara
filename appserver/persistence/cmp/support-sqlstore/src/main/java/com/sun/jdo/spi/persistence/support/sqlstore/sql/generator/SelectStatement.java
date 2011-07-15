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
 * SelectStatement.java
 *
 * Created on October 3, 2001
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.sql.generator;

import org.netbeans.modules.dbschema.ColumnElement;

import com.sun.jdo.api.persistence.support.JDOFatalDataStoreException;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.spi.persistence.support.sqlstore.ActionDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.ValueFetcher;
import com.sun.jdo.spi.persistence.support.sqlstore.database.DBVendorType;
import com.sun.jdo.spi.persistence.support.sqlstore.model.TableDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.RetrieveDescImpl;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.constraint.ConstraintField;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.constraint.ConstraintFieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.constraint.ConstraintNode;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.constraint.ConstraintOperation;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.constraint.ConstraintJoin;
import org.glassfish.persistence.common.I18NHelper;

import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;


/**
 * This class generates select statements.
 */
public class SelectStatement extends Statement {

    /** Flag indicating if this statement has been joined. */
    private boolean isJoined;

    private StringBuffer orderClause = new StringBuffer();

    /** SelectQueryplan */
    SelectQueryPlan plan;

    public SelectStatement(DBVendorType vendorType, SelectQueryPlan plan) {
        super(vendorType);
        this.plan = plan;
        constraint = plan.getConstraint();
    }

    public boolean isJoined() {
        return isJoined;
    }

    public void markJoined() {
        isJoined = true;
    }

    public ColumnRef addColumn(ColumnElement columnElement,
                          QueryTable queryTable) {

        ColumnRef columnRef = null;

        if ((columnRef = getColumnRef(columnElement)) == null) {
            columnRef = new ColumnRef(columnElement, queryTable);
            addColumnRef(columnRef);
        }

        return columnRef;
    }

    public void copyColumns(SelectStatement sourceStatement) {
        ArrayList columnRefs = sourceStatement.getColumnRefs();

        int index = columns.size() + 1;

        for (int i = 0; i < columnRefs.size(); i++) {
            //addColumnRef((ColumnRef) columnRefs.get(i));
            ColumnRef cref = (ColumnRef) columnRefs.get(i);
            cref.setIndex(index + i);
            columns.add(cref);
        }
    }

    protected boolean isUpdateLockRequired(QueryTable table) {
        return (plan.options & RetrieveDescImpl.OPT_AGGREGATE) == 0
                && table.getTableDesc().isUpdateLockRequired();
    }

    public void appendTableText(StringBuffer text, QueryTable table) {
        super.appendTableText(text, table);

        if (isUpdateLockRequired(table)) {
            //Append eqivalent of "with (updlock)" to the table text
            text.append(vendorType.getHoldlock() );

            //For efficiency, the test whether a database is capable of supporting
            //holdlock would be made in method getText() where we generate
            //text corresponding to vendorType.getForUpdate()
        }
    }

    /**
      * Determines if Column Type definition is needed for this statement.
      * Column Type definition is a performance optimization that allows defining
      * Column Type for the resultset.
      * If the query to be executed is counting pc instances, the column used
      * inside COUNT() is one of the pk columns. If the pk column happens to be
      * not convertable to an int (for example timestamp), database will throw an
      * exception. To prevent this situation, column type definition should not
      * be performed on such queries.
      *
      * @return true if column type definition is needed, false otherwise.
      */
     public boolean isColumnTypeDefinitionNeeded() {
         return (plan.options & RetrieveDescImpl.OPT_COUNT_PC ) == 0;
     }

    /** @inheritDoc */
    public QueryPlan getQueryPlan() {
        return plan;
    }

    /**
     * @inheritDoc
     */
    protected void generateStatementText() {
        // Because join conditions for ANSI outer joins end up in the
        // from clause, the constraint stack has to be processed before we
        // generate the from clause.
        StringBuffer constraints = processConstraints();
        StringBuffer outerJoinText = processOuterJoinConstraints();

        if (outerJoinText != null && outerJoinText.length() > 0) {
            if (constraints.length() > 0 ) {
                constraints.append(" and ");
            }
            constraints.append(outerJoinText);
        }

        StringBuffer whereClause = new StringBuffer();

        if (constraints.length() > 0 ) {
            whereClause.append(" where ").append(constraints);
        }

        if ((plan.options & RetrieveDescImpl.OPT_COUNT_PC) == 0) {
            generateRegularStatementText(whereClause);
        } else {
            generateCountStatementText(whereClause);
        }
    }

    /**
     * Generates the statement text for a "regular" select query.
     * Count queries have to get special attention in case of objects
     * with composite primary key.
     *
     * @param whereClause Query's where clause.
     * @see #generateCountStatementText
     */
    private void generateRegularStatementText(StringBuffer whereClause) {

        statementText = new StringBuffer();

        StringBuffer columnText = generateColumnText();
        String tableListText = generateTableListText();
        String aggregateText = getAggregateText();
        String aggregateEnd = (aggregateText.length() > 0) ? ")" : ""; // NOI18N

        if (orderClause.length() > 0) {
            orderClause.insert(0, " order by ");
        }

        final boolean updateLockRequired = isUpdateLockRequired();
        StringBuffer forUpdateClause = generateForUpdateClause(updateLockRequired);
        String distinctText = getDistinctText(updateLockRequired);

        // Create the query filling in the column list, table name, etc.
        statementText.append("select "). // NOI18N
                append(aggregateText).append(distinctText).append(columnText).append(aggregateEnd).
                append(" from ").append(tableListText). // NOI18N
                append(whereClause).append(orderClause).append(forUpdateClause);
    }

    /**
     * Generates the statement text for a count query. Count queries
     * on persistence capable objects have been mapped to selecting the
     * primary key columns in
     * {@link SelectQueryPlan#addFetchGroups(int, ArrayList, ArrayList)}.
     * Queries w/o a distinct restriction can be relaxed to select only
     * one of the primary key columns. Distinct queries on objects with
     * composite primary key have to be treated special to avoid duplicates.
     *
     * @param whereClause Query's where clause.
     * @see #generateCorrelatedExistsText
     */
    private void generateCountStatementText(StringBuffer whereClause) {

        final int selectedColumns = columns.size();

        if (selectedColumns == 1) {
            // Single PK. Call regular statement generation.
            generateRegularStatementText(whereClause);
        } else {
            boolean oneTable = tableList.size() == 1;

            if ((plan.options & RetrieveDescImpl.OPT_DISTINCT) == 0
                    || oneTable) {
                // Without DISTINCT or when querying just one table, we can
                // select only one of the pk columns and get the correct result.
                // Remove the rest.
                for (int i = selectedColumns; i > 1; ) { columns.remove(--i); }
                if (oneTable) {
                    // When selecting only one table, remove the DISTINCT
                    // contraint from the query options to get all rows.
                    plan.options &= ~RetrieveDescImpl.OPT_DISTINCT;
                }
                // Now call regular statement generation.
               generateRegularStatementText(whereClause);
            } else {
                // This is a distinct count on objects having a composite pk and the query
                // includes join constraints. We map this to a correlated exists query.
                // Note: columns can only be cleared after this call.
                generateCorrelatedExistsText(whereClause);

                // Since we're not selecting any columns in this query, remove all column
                // information. Oracle's special DB operation code gets confused otherwise.
                // See OracleSpecialDBOperation#defineColumnTypeForResult(Statement, List)
                columns.clear();
            }
        }
    }

    /**
     * Generates the text for a correlated exists query. Count distinct queries on
     * objects with composite primary key are mapped to a correlated exists query.
     * The "outer" select on the primary table to is correlated
     * to the "inner" select by the already generated where clause.
     *
     * @param whereClause Query's where clause.
     */
    private void generateCorrelatedExistsText(StringBuffer whereClause) {

        statementText = new StringBuffer();

        // TODO: Use correlated exists subquery in SelectQueryPlan?
        // - Do we handle secondary tables correctly?
        // - Do we need order by and for update clauses?

        // Generate for update clause while we still have all tables in tableList
        boolean updateLockRequired = isUpdateLockRequired();
        StringBuffer forUpdateClause = generateForUpdateClause(updateLockRequired);

        StringBuffer primaryTableText = new StringBuffer();
        QueryTable primaryTable = generatePrimaryTableText(primaryTableText);

        // Prepare the generation of the correlated "inner" select clause by
        // removing the primary table from the table list.
        // As count queries are never generated internally, we're preparing
        // a user query here. User queries never have outer joins. It's safe
        // remove the table from tableList.
        tableList.remove(primaryTable);
        String tableListText = generateTableListText();

        // Create the query with the previous generated parts.
        statementText.append("select count(*) from "). // NOI18N
                append(primaryTableText).
                append(" where exists (select * from "). // NOI18N
                append(tableListText).append(whereClause).append(")"). //NOI18N
                append(forUpdateClause);
    }

    /**
     * Generates the table text for the first column of the column list.
     *
     * @param primaryTableText Takes the resulting statement text.
     * @return The table from the first selected column.
     */
    private QueryTable generatePrimaryTableText(StringBuffer primaryTableText) {
        // Get the primary table from the first selected column.
        // TODO: Is the first column always mapped to the primary table?
        QueryTable primaryTable = ((ColumnRef)columns.get(0)).getQueryTable();

        // Generate the table text.
        appendTableText(primaryTableText, primaryTable);

        return primaryTable;
    }

    protected StringBuffer generateColumnText() {
        StringBuffer columnText = new StringBuffer();

        for (int i = 0; i < columns.size(); i++) {
            ColumnRef cr = (ColumnRef) columns.get(i);

            columnText.append("t").append(cr.getQueryTable().getTableIndex()).append("."); // NOI18N
            appendQuotedText(columnText, cr.getName());
            columnText.append(", "); // NOI18N
        }
        columnText.delete(columnText.length() - 2, columnText.length());
        return columnText;
    }

    private String getAggregateText() {
        int aggregateOption = plan.options & RetrieveDescImpl.OPT_AGGREGATE;

        switch (aggregateOption) {
            case RetrieveDescImpl.OPT_AVG:
                return "AVG( "; // NOI18N
            case RetrieveDescImpl.OPT_MIN:
                return "MIN("; // NOI18N
            case RetrieveDescImpl.OPT_MAX:
                return "MAX("; // NOI18N
            case RetrieveDescImpl.OPT_SUM:
                return "SUM("; // NOI18N
            case RetrieveDescImpl.OPT_COUNT:
            case RetrieveDescImpl.OPT_COUNT_PC:
                return "COUNT("; // NOI18N
            default:
                return ""; // NOI18N
        }
    }

    private StringBuffer generateForUpdateClause(boolean updateLockRequired) {
        StringBuffer forUpdateClause = new StringBuffer();

        if (updateLockRequired) {
            // Check if vendor actually supports updatelock
            if (!vendorType.isUpdateLockSupported() ) {
                // Throw an exception user wanted to have update lock
                // But vendor is not supporting it. Do not allow user to proceed
                throw new JDOFatalDataStoreException(I18NHelper.getMessage(messages,
                        "sqlstore.selectstatement.noupdatelocksupport"));// NOI18N
            }

            // generating the ForUpdate Clause
            String vendorForUpdate = vendorType.getForUpdate().trim();
            boolean vendorHasForUpdateClause = (vendorForUpdate.length() != 0);

            if (vendorHasForUpdateClause) {
                forUpdateClause.append(" ").append(vendorForUpdate).append(" ");

                if (vendorType.isLockColumnListSupported()) {
                    for (int i = 0; i < tableList.size(); i++) {
                        QueryTable queryTable = (QueryTable) tableList.get(i);
                        if (isUpdateLockRequired(queryTable)) {
                            TableDesc tableDesc = queryTable.getTableDesc();
                            //Get the first column of primary key
                            ColumnElement ce = (ColumnElement) tableDesc.getKey().getColumns().get(0);
                            forUpdateClause.append("t").append(i).append("."); // NOI18N
                            appendQuotedText(forUpdateClause, ce.getName().getName());
                            forUpdateClause.append(", "); // NOI18N
                        }
                    }
                    // Remove trailing ", "
                    forUpdateClause.delete(forUpdateClause.length() - 2, forUpdateClause.length());
                }
            }
        }

        return forUpdateClause;
    }

    private String getDistinctText(boolean updateLockRequired) {
        String distinctText = ""; // NOI18N

        if ((plan.options & RetrieveDescImpl.OPT_DISTINCT) > 0) {
            if( !updateLockRequired || vendorType.isDistinctSupportedWithUpdateLock()) {
                //Include DISTINCT only if update lock is not required
                //If update lock is required, include DISTINCT only if vendor supports update lock with DISTINCT
                //(e.g. ORACLE throws ORA-01786 if DISTINCT and FOR UPDATE are used in same query

                //For the case where update lock is required and vendor does not support DISTINCT/
                //with update lock, we would do DISTINCT in our code after retrieving the data
                //see SQLStoreManger::retrieve()
                distinctText = "distinct "; // NOI18N
            }
        }
        return distinctText;
    }

    /**
     * Determines if an update lock is required while executing this query.
     *
     * @return True if any of the tables invloved in this query requires
     * update lock
     */
    private boolean isUpdateLockRequired() {
        boolean updateLockRequired = false;

        // TODO: We can optimize this by storing the value in a member variable
        for (int i = 0; i < tableList.size() && !updateLockRequired; i++) {
            QueryTable queryTable = (QueryTable) tableList.get(i);
            updateLockRequired = isUpdateLockRequired(queryTable);
        }

        return updateLockRequired;
    }

    /**
     * Processes Order By constraints and calls the super class
     * method for all other constrains.
     */
    protected void processRootConstraint(ConstraintOperation opNode,
                                         List stack,
                                         StringBuffer whereText) {
        int op = opNode.operation;
        int opInfo = operationFormat(op);

        if ((opInfo & OP_ORDERBY_MASK) > 0) {
            stack.remove(stack.size() - 1);
            ConstraintNode node = (ConstraintNode) stack.get(stack.size() - 1);

            if (!(node instanceof ConstraintField)) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.constraint.needfieldnode")); // NOI18N
            } else {
                processOrderByField((ConstraintFieldDesc) node, op);
                stack.remove(stack.size() - 1);
            }
        } else {
            super.processRootConstraint(opNode, stack, whereText);
        }
    }

    protected void processIrregularOperation(ConstraintOperation opNode,
                                             int opCode,
                                             List stack,
                                             StringBuffer result) {
        switch (opCode) {
            case ActionDesc.OP_EQUIJOIN:
                processJoinOperation((ConstraintJoin)opNode, result);
                break;
            default:
                super.processIrregularOperation(opNode, opCode, stack, result);
        }
    }

    /**
     * Process outer join constraints for this statement.
     *
     * @return A string buffer representing outer join conditions for this
     * statement. Please note that the returned string buffer will have text
     * only for Oracle
     */
    private StringBuffer processOuterJoinConstraints() {
        StringBuffer joinCondition = null;
        final List joinStack = constraint.getOuterJoinConstraints();
        final int joinStackSize = joinStack.size();

        if (joinStackSize > 0) {
            joinCondition = new StringBuffer();
            for (int i = 0; i < joinStackSize; i++) {
                ConstraintJoin joinNode = (ConstraintJoin) joinStack.get(i);
                processJoinOperation(joinNode, joinCondition);
            }
        }
        return joinCondition;
    }

    /**
     * Generates a join condition for specified jnode. Equi joins and native
     * outer joins end  up in the where clause. Ansi compliant outer join
     * conditions end up in the from clause.
     *
     * @param jnode Join constraint.
     * @param whereText String buffer taking the join condition. Generated
     * join condition is appended to this string buffer.
     */
    private void processJoinOperation(ConstraintJoin jnode,
                                      StringBuffer whereText) {
        int opCode = jnode.operation;
        // Generate ANSI outer joins if doAnsiJoin == true,
        // i.e. the vendor has no "native" outer join semantics.
        boolean doAnsiJoin = opCode != ActionDesc.OP_EQUIJOIN
                && !vendorType.isNativeOuterJoin();

        if (doAnsiJoin) {
            generateAnsiJoin(jnode, opCode);
        } else {
            generateJoin(jnode, whereText, opCode);
        }
    }

    /**
     * Generate a "regular" join. The columns for the join condition
     * end up in the where clause. The corresponding tables are added
     * directly to the member variable tableList.
     * Note that this method is normally called to process EQUI- joins
     * only. For databases that are marked for native join sematics,
     * this method will be called to process both EQUI- and OUTER- joins.
     *
     * @param jnode Join constraint.
     * @param whereText String buffer taking the join condition. Generated
     * join condition is appended to this string buffer.
     * @param opCode Join operation.
     */
    private void generateJoin(ConstraintJoin jnode,
                              StringBuffer whereText,
                              int opCode) {

        for (int i = 0; i < jnode.fromColumns.size(); i++) {
            ColumnElement fromColumn = (ColumnElement)jnode.fromColumns.get(i);
            ColumnElement toColumn = (ColumnElement)jnode.toColumns.get(i);
            QueryTable fromTable = findQueryTable(jnode.fromPlan, fromColumn);
            QueryTable toTable = findQueryTable(jnode.toPlan, toColumn);

            addQueryTable(fromTable);
            addQueryTable(toTable);
            toTable.prevTable = null;

            appendJoinCondition(whereText,
                    fromTable, toTable, fromColumn, toColumn,
                    getJoinOperator(opCode));

            if (opCode == ActionDesc.OP_LEFTJOIN ) {
                // Append oracle style (+) or similar.
                whereText.append(vendorType.getLeftJoinPost());
            }
        }
    }

    /**
     * Generates an ANSI compliant join. The columns for the join
     * condition end up in the from clause. The joined tables
     * are added indirectly to the member variable tableList by
     * being added to {@link QueryTable#nextTable} associated with
     * the "from-" table.
     *
     * @param jnode Join constraint.
     * @param opCode Join operation.
     * @see #processFromClause
     */
    private void generateAnsiJoin(ConstraintJoin jnode, int opCode) {

        for (int i = 0; i < jnode.fromColumns.size(); i++) {
            ColumnElement fromColumn = (ColumnElement)jnode.fromColumns.get(i);
            ColumnElement toColumn = (ColumnElement)jnode.toColumns.get(i);
            QueryTable fromTable = findQueryTable(jnode.fromPlan, fromColumn);
            QueryTable toTable = findQueryTable(jnode.toPlan, toColumn);

            // Process the from clause
            processFromClause(fromTable, toTable);

            // Process the on clause.
            if (toTable.onClause == null) {
                toTable.onClause = new StringBuffer();
            }

            appendJoinCondition(toTable.onClause, fromTable, toTable, fromColumn, toColumn, "="); //NOI18N

            fromTable.joinOp = opCode;
        }
    }

    /**
     * Processes specified fromTable and toTable to generate appropriate from
     * clause when table text is generated.
     * toTable is added to fromTable.nextTable if not already present. See
     * {@link #appendAnsiJoinTableText(StringBuffer,QueryTable)} for
     * details on how this is used to generate table text.
     *
     * @param fromTable The from table
     * @param toTable The to table.
     */
    private static void processFromClause(QueryTable fromTable, QueryTable toTable) {

        if (toTable.prevTable != null && toTable.prevTable != fromTable) {
            // TODO
        }

        // TODO:
        // Check that these tables aren't already participating in
        // a join (except to each other).  If either of them are
        // then we will have to make a new table alias for that
        // table and put in an addtional equijoin between the old
        // alias and the new based, of course, on the key columns.

        if (fromTable.nextTable == null) {
            fromTable.nextTable = new ArrayList();
            fromTable.nextTable.add(toTable);
            toTable.prevTable = fromTable;
        } else {
            // Make sure we don't add the same table twice.
            if (!fromTable.nextTable.contains(toTable)) {
                fromTable.nextTable.add(toTable);
                toTable.prevTable = fromTable;
            }
        }
    }

    /**
     * Appends join condition corresponding to specified fromTable, toTable,
     * fromColumn, toColumn, joinOp to result. If result is not empty " and "
     * will be appended to it before appending the join condition.
     *
     * @param result The string buffer to which the condition will be appended.
     * @param fromTable The from table.
     * @param toTable The to table.
     * @param fromColumn The from column.
     * @param toColumn The to column.
     * @param joinOp Join operation.
     */
    private void appendJoinCondition(StringBuffer result,
                                     QueryTable fromTable, QueryTable toTable,
                                     ColumnElement fromColumn, ColumnElement toColumn,
                                     String joinOp) {
        if (result.length() > 0) {
            // Composite fk.
            result.append(" and ");
        }

        result.append("t").append(fromTable.getTableIndex()).append("."); // NOI18N
        appendQuotedText(result, fromColumn.getName().getName());
        result.append(" ").append(joinOp). // NOI18N
           append(" t").append(toTable.getTableIndex()).append("."); // NOI18N
        appendQuotedText(result, toColumn.getName().getName());
    }

    /**
     * Returns join operator for specified operation.
     *
     * @param operation The join operation. Should be one of
     * ActionDesc.OP_EQUIJOIN, ActionDesc.OP_LEFTJOIN or ActionDesc.OP_RIGHTJOIN.
     * @return join operator for specified operation.
     */
    protected String getJoinOperator(int operation) {
        String result = null;

        switch (operation) {
            case ActionDesc.OP_EQUIJOIN:
                result = " = "; // NOI18N
                break;
            case ActionDesc.OP_LEFTJOIN:
                result = vendorType.getLeftJoin();
                break;
            case ActionDesc.OP_RIGHTJOIN:
                result = vendorType.getRightJoin();
                break;
            default:
                throw new JDOFatalInternalException(
                        I18NHelper.getMessage(messages,
                        "core.constraint.illegalop", operation)); // NOI18N
        }
        return result;
    }

    private static QueryTable findQueryTable(QueryPlan plan, ColumnElement ce) {
        QueryTable table = plan.findQueryTable(ce.getDeclaringTable());

        if (table == null) {
            // TODO: throw exception
        }

        return table;
    }

    private String generateTableListText() {
        StringBuffer str = new StringBuffer();

        for (int i = 0; i < tableList.size(); i++) {
            QueryTable t = (QueryTable) tableList.get(i);

            if (t.prevTable == null && t.nextTable == null) {
                appendTableText(str, t);
                str.append(", "); // NOI18N
            } else {
                // Table is part of an outer join list.

                if (t.prevTable == null) {
                    // Beginning of the list.

                    appendAnsiJoinText(str, t);
                } else {
                    // The table is in the "middle" of the list.

                    while (t.prevTable != null) {
                        t = t.prevTable;
                    }

                    if (!tableList.contains(t)) {
                        // Outer join list starts with a join table.
                        // Because join tables aren't in the table list,
                        // they wouldn't be included in the table text.

                        appendAnsiJoinText(str, t);
                    }
                }
            }
        }

        str.delete(str.length() - 2, str.length());

        return str.toString();
    }

    private void appendAnsiJoinText(StringBuffer str, QueryTable t) {
        // TODO: getTableListStart() and getTableListEnd() returns ""
        // for all the databases. Do we need it ?
        str.append(vendorType.getTableListStart());
        appendAnsiJoinTableText(str, t);
        str.append(vendorType.getTableListEnd());
        str.append(", "); // NOI18N
    }

    /**
     * Appends sql text corresponding to specified <code>table</code> to specified text.
     * The linked list starting with table.nextTable is walked recursively
     * to generate join text.
     *
     * @param text The string buffer receiving sql text.
     * @param table Table to be joined.
     */
    private void appendAnsiJoinTableText(StringBuffer text, QueryTable table) {

        if (table.joinOp == ActionDesc.OP_RIGHTJOIN) {
            text.append(vendorType.getRightJoinPre());
        }

        if (table.prevTable == null) {
            appendTableText(text, table);
        }

        for (int i = 0; i < table.nextTable.size(); i++) {
            QueryTable toTable = (QueryTable) table.nextTable.get(i);
            text.append(getJoinOperator(table.joinOp)).append(" "); // NOI18N

            appendTableText(text, toTable);

            if (toTable.onClause != null) {
                text.append(" on "); // NOI18N
                text.append(toTable.onClause);
            }

            if (toTable.nextTable != null) {
                appendAnsiJoinTableText(text, toTable);
            }

            // Note: Since this method is called only for ANSI joins,
            // and only oracle has getLeftJoinPost() defined, we will never
            // append any text through following code.
            if (table.joinOp == ActionDesc.OP_LEFTJOIN) {
                text.append(vendorType.getLeftJoinPost());
            }
        }
    }

    /**
     * Adds a column corresponding to field <code>fieldNode</code> and
     * order operation <code>op</code> to the order constraint.
     *
     * @param fieldNode Constraint on a field name of a persistence capable class.
     * @param op Order operation.
     */
    private void processOrderByField(ConstraintFieldDesc fieldNode, int op) {
        QueryPlan thePlan = getOriginalPlan(fieldNode);
        StringBuffer orderText = new StringBuffer();

        generateColumnText(fieldNode.desc, thePlan, orderText);

        if (op == ActionDesc.OP_ORDERBY_DESC) {
            orderText.append(" desc"); // NOI18N
        }

        if (orderClause.length() > 0) {
            orderText.append(", "); // NOI18N
            orderText.append(orderClause);
        }
        orderClause = orderText;
    }

    /**
     * Binds input valus corrsponding to this <code>Statement</code> object to
     * database statement s.
     * @param s The database statement.
     * @param parameters If an InputValue to be bound is a parameter, the actual
     * value is contained in this.
     * @throws SQLException
     */
    public void bindInputValues(DBStatement s, ValueFetcher parameters)
            throws SQLException {
        for (int i = 0, size = inputDesc.values.size(); i < size; i++) {
            InputValue inputVal = (InputValue) inputDesc.values.get(i);
            s.bindInputColumn(i + 1, getInputValue(inputVal, parameters),
                    inputVal.getColumnElement(), vendorType);
        }
    }

    /**
     * Get Input values to be bound to this statement.
     * @param parameters If an InputValue to be bound is a parameter, the actual
     * value is contained in this.
     * @return An Object array containing input values to be bound to this statement.
     */
    private Object[] getInputValues(ValueFetcher parameters) {
        final int size = inputDesc.values.size();
        Object[] inputValues = new Object[size];
        for (int i = 0; i < size; i++) {
            InputValue inputValue = (InputValue) inputDesc.values.get(i);
            inputValues[i] = getInputValue(inputValue, parameters);
        }
        return inputValues;
    }

    /**
     * Gets formatted sql text corrsponding to this statement object. The text
     * also contains values for input to the statement.
     * @param parameters The input paramters to this statement.
     * @return formatted sql text corrsponding to this statement object.
     */
    public String getFormattedSQLText(ValueFetcher parameters) {
        return formatSqlText(getText(), getInputValues(parameters)) ;
    }

    /**
     * Gets actual value corresponding to <code>inputVal</code>. If
     * <code>inputVal</code> is an instanceof InputValue then value contained
     * in inputVal is returned. If <code>inputVal</code> is an instanceof
     * InputParamValue then value returned is obtained from <code>parameters
     * </code>.
     * @param inputVal The input value.
     * @param parameters The parameters.
     * @return Appropriate value as described above.
     */
    private static Object getInputValue(InputValue inputVal,
            ValueFetcher parameters) {
        Object val;
        if (inputVal instanceof InputParamValue) {
            int paramIndex = ((InputParamValue) inputVal).getParamIndex().intValue();
            val = parameters.getValue(paramIndex);
        }
        else {
            val = inputVal.getValue();
        }
        return val;
    }

}
