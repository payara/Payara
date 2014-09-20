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

package org.glassfish.persistence.common;

/** 
 * @author Marina Vatkina
 * This interface defines string constants used by the database generation.
 */
public interface DatabaseConstants {

    // Substrings used to construct file names.
    char NAME_SEPARATOR = '_'; // NOI18N

    String JDBC_FILE_EXTENSION = ".jdbc"; // NOI18N
    String SQL_FILE_EXTENSION = ".sql"; // NOI18N
    String CREATE             = "create"; // NOI18N
    String DROP               = "drop"; // NOI18N
    String DDL                = "DDL"; // NOI18N

    // Known file name suffixes.
    String CREATE_DDL_JDBC_FILE_SUFFIX = NAME_SEPARATOR + CREATE + DDL + JDBC_FILE_EXTENSION;
    String DROP_DDL_JDBC_FILE_SUFFIX   = NAME_SEPARATOR + DROP + DDL + JDBC_FILE_EXTENSION;
    String CREATE_SQL_FILE_SUFFIX     = NAME_SEPARATOR + CREATE + SQL_FILE_EXTENSION;
    String DROP_SQL_FILE_SUFFIX       = NAME_SEPARATOR + DROP + SQL_FILE_EXTENSION;

    /**
     * Used as key to set propety in DeploymentContext indicating to override JTA dataource
     */
    String JTA_DATASOURCE_JNDI_NAME_OVERRIDE = "org.glassfish.jta.datasource.jndi.name";

    // Flag used to indicate a database generation mode.
    public static final String JAVA_TO_DB_FLAG = "java-to-database"; // NOI18N
}
