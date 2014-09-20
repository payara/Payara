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

package com.sun.jdo.api.persistence.support;

import java.util.List;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;

/**
 * SpecialDBOperation interface is defined for database specific operations.
 * @author Shing Wai Chan
 */
public interface SpecialDBOperation {

    /**
     * This method is called immediately after an instance implementing this
     * interface is created. The implementation can initialize itself using
     * supplied metaData.
     * @param metaData DatbaseMetaData of the database for which an instance
     * implementing this interface is ingratiated.
     * @param identifier Identifier of object used to obtain databaseMetaData.
     * This can be null in non managed environment.
     */
    public void initialize(DatabaseMetaData metaData,
        String identifier) throws SQLException;
    /**
     * Defines column type for result.
     * @param ps java.sql.PreparedStatement
     * @param columns List of ColumnElement corresponding to select clause
     */
    public void defineColumnTypeForResult(
        PreparedStatement ps, List columns) throws SQLException;

    /**
     * Binds specified value to parameter at specified index that is bound to
     * CHAR column.
     * @param ps java.sql.PreparedStatement
     * @param index Index of paramater marker in <code>ps</code>.
     * @param strVal value that needs to bound.
     * @param length length of the column to which strVal is bound.
     */
    public void bindFixedCharColumn(PreparedStatement ps,
         int index, String strVal, int length) throws SQLException;

}
