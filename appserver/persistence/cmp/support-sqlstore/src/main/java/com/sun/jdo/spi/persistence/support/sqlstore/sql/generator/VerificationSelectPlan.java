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

import com.sun.jdo.spi.persistence.support.sqlstore.*;
import com.sun.jdo.spi.persistence.support.sqlstore.model.LocalFieldDesc;

import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * Implements Select Plan for verifying clean VC instanses.
 * @author Mitesh Meswani
 */
public class VerificationSelectPlan extends SelectQueryPlan {

    /**
     * Creates a new VerificationSelectQueryPlan.
     *
     * @param desc The Retrieve descriptor holding constraints
     * @param store Store manager executing the query.
     */
    public VerificationSelectPlan(RetrieveDesc desc, SQLStoreManager store) {
        super(desc, store, null);
    }

    /**
     * There are no real fields to be selected for verification query.
     * This method just adds the tables for the version field.
     */
    protected void processFields() {
        LocalFieldDesc[] versionFields = config.getVersionFields();
        for (int i = 0; i < versionFields.length; i++) {
            LocalFieldDesc versionField = versionFields[i];
            addTable(versionField);
        }
    }

    protected Statement newStatement() {
        return new SelectOneStatement(store.getVendorType(), this);
    }

    /**
     * Checks whether the resultset from a verification query contains atleast
     * one row.
     * @param pm This parameter is not used.
     * @param resultData The resultset containing result from the verification query
     * @return true if the resultset contains atleast one row false otherwise.
     * @throws SQLException
     */
    public Object getResult(PersistenceManager pm, ResultSet resultData)
            throws SQLException{
        boolean verificationSuccessfull = resultData.next();
        return Boolean.valueOf(verificationSuccessfull);
    }

}
