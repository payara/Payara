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

package com.sun.jdo.spi.persistence.support.sqlstore.sql.generator;

import com.sun.jdo.spi.persistence.support.sqlstore.RetrieveDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.SQLStoreManager;
import com.sun.jdo.spi.persistence.support.sqlstore.ActionDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.ForeignFieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.ClassDesc;
import org.netbeans.modules.dbschema.ColumnElement;

import java.util.ArrayList;

/**
 * Implements the select plan for correlated subqueries.
 *
 * @author Mitesh Meswani
 * @author Markus Fuchs
 */
public abstract class CorrelatedSelectPlan extends SelectQueryPlan {
    /** The parent plan for this subquery */
    protected SelectQueryPlan parentPlan;

    public CorrelatedSelectPlan(RetrieveDesc desc,
                                SQLStoreManager store,
                                ForeignFieldDesc parentField,
                                SelectQueryPlan parentPlan) {

        super(desc, store, null);
        this.parentField = parentField;
        this.parentPlan = parentPlan;
    }

    /**
     * The constraints for correlated subqueries are added here.
     * The constraints are:
     * <ul>
     * <li>The correlated constraint joining this subquery with the parent plan</li>
     * <li>A join constraint if the parent field uses join table</li>
     * </ul>
     */
    protected void processConstraints() {

        // Process the constraint on the stack.`
        super.processConstraints();

        doCorrelatedJoin();

        processJoinTable();

        // Process any extra statement added as the result of #addTable.
        processStatements();
    }

    /**
     * Must be implemented by the sub classes.
     */
    protected abstract void doCorrelatedJoin();

    /**
     * Enhance the select statement to include the join table if the
     * relationship is mapped via a join table.
     */
    private void processJoinTable() {

        if (parentField.useJoinTable()) {
            addQueryTables(parentField.assocForeignColumns, config);

            // Put in a join for the association table.
            // Subqueries always join via equijoin.
            addJoinConstraint(this, this,
                    parentField.assocForeignColumns,
                    parentField.foreignColumns, ActionDesc.OP_EQUIJOIN);
        }
    }

    /**
     * Adds the query tables corresponding to the columns in <code>columnList</code>.
     *
     * @param columnList List of columns.
     * @param config Class configuration corresponding to columns.
     */
    protected void addQueryTables(ArrayList columnList, ClassDesc config) {
        for (int i = 0; i < columnList.size(); i++) {
            ColumnElement col = (ColumnElement) columnList.get(i);
            addQueryTable(col.getDeclaringTable(), config);
        }
    }

}
