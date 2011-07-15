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
 * UpdateQueryPlan.java
 *
 * Created on October 3, 2001
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.sql.generator;

import org.netbeans.modules.dbschema.ColumnElement;
import org.netbeans.modules.dbschema.TableElement;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.spi.persistence.support.sqlstore.ActionDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.SQLStoreManager;
import com.sun.jdo.spi.persistence.support.sqlstore.Transaction;
import com.sun.jdo.spi.persistence.support.sqlstore.model.*;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.UpdateJoinTableDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.UpdateObjectDescImpl;
import org.glassfish.persistence.common.I18NHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * This class is used to generated update/insert/delete statements.
 */
public class UpdateQueryPlan extends QueryPlan {

    /** Flag indicating whether we use batch update. */
    private boolean batch = false;

    private UpdateObjectDescImpl updateDesc;

    public UpdateQueryPlan(ActionDesc desc, SQLStoreManager store) {
        super(desc, store);
        this.updateDesc = (UpdateObjectDescImpl) desc;
        this.action = getAction(updateDesc.getUpdateAction());
    }

    private static int getAction(int updateAction) {
        if (updateAction == ActionDesc.LOG_CREATE) {
            return ACT_INSERT;
        } else if (updateAction == ActionDesc.LOG_DESTROY) {
            return ACT_DELETE;
        } else if (updateAction == ActionDesc.LOG_UPDATE) {
            return ACT_UPDATE;
        } else {
            return ACT_NOOP;
        }
    }

    /**
     * Specifies an field the data for which needs to be updated,
     * and the mapped columns for which therefor need to be updated.
     * For update queries the column will be put in the set lists,
     * and for insert queries the column will be put into the
     * insert values lists.
     *
     * @param fieldDesc Updated field corresponding to a column in the database.
     * @param value New value.
     */
    private void addColumn(LocalFieldDesc fieldDesc, Object value) {

        // Ignore secondary tracked fields.
        if ((fieldDesc.sqlProperties & FieldDesc.PROP_SECONDARY_TRACKED_FIELD) > 0) {
            return;
        }

        for (Iterator iter = fieldDesc.getColumnElements(); iter.hasNext(); ) {
            ColumnElement columnElement = (ColumnElement) iter.next();
            TableElement tableElement = columnElement.getDeclaringTable();

            if (tableElement == null) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.configuration.fieldnotable", // NOI18N
                        fieldDesc.getName()));
            }

            QueryTable t = findQueryTable(tableElement);
            UpdateStatement s = null;

            if (t == null) {
                t = addQueryTable(tableElement, null);
                s = (UpdateStatement) addStatement(t);
            } else {
                s = (UpdateStatement) getStatement(t);
            }

            if (fieldDesc.isVersion() && action == ACT_UPDATE) {
                // For update, version columns will be flagged specially.
                s.addVersionColumn(columnElement);
            } else {
                s.addColumn(columnElement, value);
            }

        }
    }

    public void build() {
        build(false);
    }

    /**
     * Builds a UpdateQueryPlan for an object based update
     * type (i.e. insert, update, delete) ActionDesc.
     *
     * @param batch Flag indicating whether we use batch update.
     */
    public void build(boolean batch) {

        if ((status & ST_BUILT) > 0) return;

        this.batch = batch;

        generateStatements();

        addColumns();

        if (statements.size() > 0) {
            processStatements();
        }

        processJoinTables();

        status = status | ST_BUILT;
    }

    private void generateStatements() {
        // For insert and delete we build a statement for each table
        if ((action == ACT_DELETE) || (action == ACT_INSERT)) {
            Iterator iter = config.getTables();
            while (iter.hasNext()) {
                TableDesc t = (TableDesc) iter.next();

                // Skip join tables
                if (!t.isJoinTable()) {
                    if (findQueryTable(t.getTableElement()) == null)
                        addStatement(addQueryTable(t));
                }
            }

            // For insert statements, we need to reverse the order.
            if (action == ACT_INSERT) {
                int i = 0;
                int j = statements.size() - 1;

                while (i < j) {
                    Statement s = (Statement) statements.get(i);
                    statements.set(i, statements.get(j));
                    statements.set(j, s);
                    i++;
                    j--;
                }
            }
        }
    }

    /**
     * For inserts and updates we figure out values (they might be hidden)
     * and add the columns necessary. <code>addColumn</code> will figure out
     * which tables need to be included.
     */
    private void addColumns() {
        if ((action == ACT_UPDATE) || (action == ACT_INSERT)) {
            List updatedFields = updateDesc.getUpdatedFields();
            int size = (updatedFields != null) ? updatedFields.size() : 0;
            for (int i = 0; i < size; i++) {
                LocalFieldDesc f = (LocalFieldDesc) updatedFields.get(i);
                Object value = batch ? f : updateDesc.getAfterValue(f);
                addColumn(f, value);
            }
        }
    }

    private void addConstraints(UpdateStatement statement,
                                ArrayList localFields,
                                ArrayList foreignFields,
                                ArrayList columns) {

        boolean isBeforeImageRequired = updateDesc.isBeforeImageRequired();
        for (int i = 0; i < localFields.size(); i++) {
            LocalFieldDesc lf = (LocalFieldDesc) localFields.get(i);
            LocalFieldDesc ff = (LocalFieldDesc) foreignFields.get(i);
            ColumnElement ce = (ColumnElement) columns.get(i);

            addConstraint(statement, lf, ff, ce, isBeforeImageRequired);
        }

        // Add the constraint on the version field if needed.
        if (getConfig().hasVersionConsistency() && action != ACT_INSERT) {
            QueryTable table = (QueryTable) statement.getQueryTables().get(0);
            LocalFieldDesc versionField = table.getTableDesc().getVersionField();
            ColumnElement ce = (ColumnElement) versionField.getColumnElements().next();

            addConstraint(statement, versionField, versionField, ce, false);
        }

        statement.markConstraintAdded();
    }

    private void addConstraint(UpdateStatement statement,
                               LocalFieldDesc lf,
                               LocalFieldDesc ff,
                               ColumnElement ce,
                               boolean isBeforeImageRequired) {

        if (action != ACT_INSERT) {
            if (batch) {
                statement.addConstraint(ce, lf, ff);
            }
            else {
                Object value = isBeforeImageRequired ?
                    updateDesc.getBeforeValue(ff) : updateDesc.getAfterValue(ff);
                statement.addConstraint(lf, value);
            }
        } else {
            Object value = batch ? ff : updateDesc.getAfterValue(ff);
            statement.addColumn(ce, value);
        }
    }

    private void addSecondaryTableConstraint(UpdateStatement statement) {
        QueryTable table = (QueryTable) statement.getQueryTables().get(0);
        ReferenceKeyDesc key = table.getTableDesc().getPrimaryTableKey();

        ArrayList localFields = key.getReferencingKey().getFields();
        ArrayList foreignFields = key.getReferencedKey().getFields();
        ArrayList columns = key.getReferencingKey().getColumns();

        addConstraints(statement, localFields, foreignFields, columns);
    }

    private void addBasetableConstraint(UpdateStatement statement) {
        QueryTable table = (QueryTable) statement.getQueryTables().get(0);
        KeyDesc key = table.getTableDesc().getKey();

        ArrayList localFields = key.getFields();
        ArrayList columns = key.getColumns();

        addConstraints(statement, localFields, localFields, columns);
    }

    private void processRelatedStatements(UpdateStatement statement) {
        ArrayList secondaryTableStatements = statement.getSecondaryTableStatements();

        if (secondaryTableStatements != null) {
            for (int i = 0; i < secondaryTableStatements.size(); i++) {
                UpdateStatement secondaryTableStatement = (UpdateStatement) secondaryTableStatements.get(i);

                if (!secondaryTableStatement.isConstraintAdded()) {
                    processRelatedStatements(secondaryTableStatement);

                    addSecondaryTableConstraint(secondaryTableStatement);
                }
            }
        }
    }

    protected void processStatements() {
        int size = statements.size();

        if (size > 1) {
            super.processStatements();

            for (int i = 0; i < size; i++) {
                UpdateStatement statement = (UpdateStatement) statements.get(i);

                if (!statement.isConstraintAdded())
                    processRelatedStatements(statement);
            }
        }

        UpdateStatement masterStatement = null;

        if (size == 1)
            masterStatement = (UpdateStatement) statements.get(0);
        else {
            // Look for the master statement. It should be the one
            // with no constraints added.
            for (int i = 0; i < size; i++) {
                masterStatement = (UpdateStatement) statements.get(i);

                if (!masterStatement.isConstraintAdded()) break;
            }
        }

        if (action != ACT_INSERT)
            addBasetableConstraint(masterStatement);

        if ((action != ACT_INSERT) && (updateDesc.getConcurrency() != null))
            updateDesc.getConcurrency().update(this);
    }

    private void processJoinTables() {
        Collection fields = updateDesc.getUpdatedJoinTableFields();

        if (fields == null) return;

        Iterator fieldIter = fields.iterator();

        ArrayList deleteStatements = new ArrayList();
        ArrayList insertStatements = new ArrayList();

        while (fieldIter.hasNext()) {
            ForeignFieldDesc f = (ForeignFieldDesc) fieldIter.next();
            Collection descs = updateDesc.getUpdateJoinTableDescs(f);
            Iterator descIter = descs.iterator();

            ColumnElement c = (ColumnElement) f.assocLocalColumns.get(0);
            QueryTable t = addQueryTable(config.findTableDesc(c.getDeclaringTable()));

            while (descIter.hasNext()) {
                UpdateJoinTableDesc desc = (UpdateJoinTableDesc) descIter.next();
                int action = getAction(desc.getAction());

                UpdateStatement s = (UpdateStatement) createStatement(t);
                s.setAction(action);

                if (action == ACT_INSERT) {
                    insertStatements.add(s);
                } else if (action == ACT_DELETE) {

                    // RESOLVE: There are redundant deletes from join tables that causes
                    // update to fail with no rows affected. To work around this problem
                    // for now, we set the minAffectedRows to 0.
                    // We need to figure out why there are redundant deletes.

                    s.minAffectedRows = 0;
                    deleteStatements.add(s);
                }

                s.addLocalConstraints(action, f, desc.getParentStateManager());
                s.addForeignConstraints(action, f, desc.getForeignStateManager());
            }
        }

        // All join table delete statements have to go first and all
        // join table insert statements have to go last.

        ArrayList oldStatements = statements;
        statements = deleteStatements;
        statements.addAll(oldStatements);
        statements.addAll(insertStatements);
    }

    protected Statement newStatement() {
        return new UpdateStatement(store.getVendorType(), this, batch);
    }

    /**
     * Determines if the amount of batched operations exceeded a threshold.
     * @param tran the transaction
     * @return true if the amount of batched operations exceeded a threshold
     */
    public boolean checkBatchThreshold(Transaction tran)
    {
        for (int i = 0, size = statements.size(); i < size; i++) {
            UpdateStatement updateStatement = (UpdateStatement) statements.get(i);
            if (updateStatement.exceedsBatchThreshold(tran))
                return true;
        }
        return false;
    }

}
