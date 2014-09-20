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
 * ConcurrencyCheckDirty.java
 *
 * Created on March 19, 2002
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.sql.concurrency;

import org.netbeans.modules.dbschema.ColumnElement;
import com.sun.jdo.spi.persistence.support.sqlstore.SQLStateManager;
import com.sun.jdo.spi.persistence.support.sqlstore.UpdateObjectDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.FieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.LocalFieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.generator.QueryTable;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.generator.Statement;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.generator.UpdateQueryPlan;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;

/**
 */
public class ConcurrencyCheckDirty extends ConcurrencyDBNative {

    public void commit(UpdateObjectDesc updateDesc,
                       SQLStateManager beforeImage,
                       SQLStateManager afterImage,
                       int logReason) {
        this.beforeImage = beforeImage;
        this.afterImage = afterImage;
    }

    public void update(UpdateQueryPlan plan) {
        boolean debug = logger.isLoggable();

        if (debug) {
            logger.fine("sqlstore.sql.concurrency.concurrencychkdirty", beforeImage); // NOI18N
        }

        if (beforeImage != null) {
            ArrayList fields = plan.getConfig().fields;
            BitSet verifyGroupMask = prepareVerifyGroupMask(plan);

            for (int i = 0; i < 2; i++) {

                if (i == 0) {
                    fields = plan.getConfig().fields;
                } else if (i == 1) {
                    fields = plan.getConfig().hiddenFields;
                }

                if (fields == null) {
                    continue;
                }

                for (int j = 0; j < fields.size(); j++) {
                    FieldDesc f = (FieldDesc) fields.get(j);

                    if (f instanceof LocalFieldDesc) {
                        LocalFieldDesc lf = (LocalFieldDesc) f;

                        // Make sure the field is marked for concurrency check and is present
                        // Also, skip all fields that are marked as secondary tracked fields.
                        //
                        // RESOLVE: we need to fetch the fields that are not present.
                        if (((lf.sqlProperties & FieldDesc.PROP_IN_CONCURRENCY_CHECK) > 0) &&
                                ((lf.sqlProperties & FieldDesc.PROP_SECONDARY_TRACKED_FIELD) == 0) &&
                                beforeImage.getPresenceMaskBit(lf.absoluteID)) {

                            if (isFieldVerificationRequired(lf, verifyGroupMask)) {
                                Object val = null;
                                val = lf.getValue(this.beforeImage);
                                addConstraint(plan, lf, val);
                            }
                        }
                    }
                }
            }
        }

        if (debug) {
            logger.fine("sqlstore.sql.concurrency.concurrencychkdirty.exit"); // NOI18N
        }
    }

    protected BitSet prepareVerifyGroupMask(UpdateQueryPlan plan) {
        return null;
    }

    protected boolean isFieldVerificationRequired(LocalFieldDesc lf,
                                                  BitSet verifyGroupMask) {
         return true;
    }

    /**
     * Adds a comparison for local field <CODE>lf</CODE> and value <CODE>val</CODE>
     * to the corresponding statements in UpdateQueryPlan <CODE>plan</CODE>.
     */
    private static void addConstraint(UpdateQueryPlan plan, LocalFieldDesc lf, Object val) {
        for (Iterator iter = lf.getColumnElements(); iter.hasNext(); ) {
            ColumnElement c = (ColumnElement) iter.next();

            for (int i = 0; i < plan.statements.size(); i++) {
                Statement s = (Statement) plan.statements.get(i);

                for (int j = 0; j < s.tableList.size(); j++) {
                    QueryTable t = (QueryTable) s.tableList.get(j);

                    if (t.getTableDesc().getTableElement() == c.getDeclaringTable()) {
                        s.addConstraint(lf, val);
                    }
                }
            }
        }
    }

    public Object clone() {
        return new ConcurrencyCheckDirty();
    }
}






