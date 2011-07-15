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

import com.sun.jdo.spi.persistence.support.sqlstore.ActionDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.RetrieveDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.SQLStoreManager;
import com.sun.jdo.spi.persistence.support.sqlstore.model.ForeignFieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.LocalFieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.constraint.ConstraintFieldDesc;

import java.util.ArrayList;

/**
 * Implements the select plan for Exist-Subqueries.
 *
 * @author Mitesh Meswani
 * @author Markus Fuchs
 */
public class CorrelatedExistSelectPlan extends CorrelatedSelectPlan {

    public CorrelatedExistSelectPlan(RetrieveDesc desc,
                                     SQLStoreManager store,
                                     ForeignFieldDesc parentField,
                                     SelectQueryPlan parentPlan) {

        super(desc, store, parentField, parentPlan);
    }

    /**
     * There are no real fields to be selected for an (NOT)EXIST query.
     * This method just adds the table for the nested select.
     * The statement for nested select is created as a side effect.
     */
    protected void processFields() {
        for (int i = 0; i < parentField.foreignFields.size(); i++) {
            LocalFieldDesc field = (LocalFieldDesc) parentField.foreignFields.get(i);
            addTable(field);
        }
    }

    /**
     * The correlated constraint joining this subquery with the parent field.
     * The joined table is added as a side-effect.
     */
    protected void doCorrelatedJoin() {
        ArrayList foreignFields = null;

        if (parentField.useJoinTable()) {
            foreignFields = parentField.assocLocalFields;
            // The join table is included in #processJoinTable
        } else {
            foreignFields = parentField.foreignFields;
        }

        ArrayList localFields = parentField.localFields;
        // Add the constraint linking the parent query with the subquery.
        for (int i = 0; i < localFields.size(); i++) {
            LocalFieldDesc la = (LocalFieldDesc) localFields.get(i);
            LocalFieldDesc fa = (LocalFieldDesc) foreignFields.get(i);

            ConstraintFieldDesc lcfd = new ConstraintFieldDesc(la, parentPlan);
            ConstraintFieldDesc fcfd = new ConstraintFieldDesc(fa, this);

            constraint.addField(lcfd);
            constraint.addField(fcfd);
            // Subqueries always join via equijoin.
            constraint.addOperation(ActionDesc.OP_EQ);
        }
    }

    protected Statement newStatement() {
        return new SelectOneStatement(store.getVendorType(), this);
    }

}
