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
 * ConcurrencyOptVerify.java
 *
 * Created on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.sql.concurrency;

import com.sun.jdo.spi.persistence.support.sqlstore.model.FieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.LocalFieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.generator.QueryPlan;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.generator.UpdateQueryPlan;

import java.util.ArrayList;
import java.util.BitSet;

/**
 */
public class ConcurrencyOptVerify extends ConcurrencyCheckDirty {

    /**
     * Find all the local fields that have been updated
     * and use their concurrencyGroup to set the verifyGroupMask.
     */
    protected BitSet prepareVerifyGroupMask(UpdateQueryPlan plan) {
        ArrayList fields;
        BitSet verifyGroupMask = new BitSet();
        int action = plan.getAction();

        for (int i = 0; i <= 1; i++) {
            if (i == 0) {
                fields = plan.getConfig().fields;
            } else {
                fields = plan.getConfig().hiddenFields;
            }

            if (fields == null) {
                continue;
            }

            for (int j = 0; j < fields.size(); j++) {
                FieldDesc f = (FieldDesc) fields.get(j);

                if ((f instanceof LocalFieldDesc) &&
                        (f.sqlProperties & FieldDesc.PROP_IN_CONCURRENCY_CHECK) > 0) {
 
                    // In the case of a deleted instance with no modified fields
                    // we use the present fields in the before image to perform
                    // the concurrency check.
                    if (afterImage.getSetMaskBit(f.absoluteID) ||
                            ((action == QueryPlan.ACT_DELETE) &&
                            beforeImage.getPresenceMaskBit(f.absoluteID))) {
                        if (f.concurrencyGroup != -1) {
                            verifyGroupMask.set(f.concurrencyGroup);
                        }
                    }
                }
            }
        }

        return verifyGroupMask;
    }

    protected boolean isFieldVerificationRequired(LocalFieldDesc lf,
                                                  BitSet verifyGroupMask) {
        boolean fieldVerificationRequired = true;

        if (lf.concurrencyGroup == -1) {
            if (!afterImage.getSetMaskBit(lf.absoluteID)) {
                fieldVerificationRequired = false;
            }
        } else {
            if (!verifyGroupMask.get(lf.concurrencyGroup)) {
                fieldVerificationRequired = false;
            }
        }

        return fieldVerificationRequired;
    }

    public Object clone() {
        return new ConcurrencyOptVerify();
    }
}






