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
 * DDLTemplateFormatter.java
 *
 * Created on Jan 14, 2003
 */

package com.sun.jdo.spi.persistence.generator.database;

import java.text.MessageFormat;

/*
 * This class provides methods that format strings containing DDL.  The
 * resulting strings are dependent on a particular MappingPolicy.
 *
 * @author Jie Leng, Dave Bristor
 */
// XXX FIXME This will not work in the unlikely event that 2 apps are being
// deployed at once.  It has reset invoked in DDLGenerator.generateDDL, but
// if another generateDDL can happen simultaneously, we're in trouble.
class DDLTemplateFormatter {
    /** Formatter for the start of "create table" DDL. */
    private static MessageFormat createTableStart = null; 

    /** Formatter for the start of "create index" DDL. */
    private static MessageFormat createIndex = null;

    /** Formatter for "add constraint" DDL. */
    private static MessageFormat alterTableAddConstraintStart = null; 

    /** Formatter for "drop constraing" DDL. */
    private static MessageFormat alterTableDropConstraint = null;

    /** Formatter for DDL for adding a PK constraint. */
    private static MessageFormat primaryKeyConstraint = null; 

    /** Formatter for DDL for adding an FK constraint. */
    private static MessageFormat foreignKeyConstraint = null; 

    /** Formatter for "drop table" DDL. */
    private static MessageFormat dropTable = null;

    
    /**
     * Prevent instantiation.
     */
    private DDLTemplateFormatter() {
    }

    /**
     * Resets MessageFormats for code generation as per policy.
     * @param mappingPolicy Policy that determines formatters provided by
     * this class.
     */
    static void reset(MappingPolicy mappingPolicy) {
        createTableStart = new MessageFormat(
                mappingPolicy.getCreateTableStart());
	// Added for Symfoware support as indexes on primary keys are mandatory
        createIndex = new MessageFormat(
                mappingPolicy.getCreateIndex());

        alterTableAddConstraintStart = new MessageFormat(
                mappingPolicy.getAlterTableAddConstraintStart());

        alterTableDropConstraint = new MessageFormat(
                mappingPolicy.getAlterTableDropConstraint());

        primaryKeyConstraint = new MessageFormat(
                mappingPolicy.getPrimaryKeyConstraint());

        foreignKeyConstraint = new MessageFormat(
                mappingPolicy.getForeignKeyConstraint());

        dropTable = new MessageFormat(
                mappingPolicy.getDropTable());
    }

    
    /**
     * @returns A String formatted for the start of "create table" DDL.
     */
    static String formatCreateTable(Object o) {
        return createTableStart.format(o);
    }

    /**
     * @returns A String formatted for the start of "create index" DDL.
     */
    static String formatCreateIndex(Object o) {
        return createIndex.format(o);
    }

    /**
     * @returns A String formatted for "add constraint" DDL.
     */
    static String formatAlterTableAddConstraint(Object o) {
        return alterTableAddConstraintStart.format(o);
    }

    /**
     * @returns A String formatted for "drop constraint" DDL.
     */
    static String formatAlterTableDropConstraint(Object o) {
        return alterTableDropConstraint.format(o);
    }

    /**
     * @returns A String formatted for DDL for adding a PK constraint.
     */
    static String formatPKConstraint(Object o) {
        return primaryKeyConstraint.format(o);
    }

    /**
     * @returns A String formatted for DDL for adding an FK constraint.
     */
    static String formatFKConstraint(Object o) {
        return foreignKeyConstraint.format(o);
    }

    /**
     * @returns A String formatted for "drop table" DDL.
     */
    static String formatDropTable(Object o) {
        return dropTable.format(o);
    }
}
