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

package com.sun.jdo.spi.persistence.generator.database;

/** 
 * This interface defines string constants used by the database generation.
 *
 * @author Jie Leng, Dave Bristor
 */
// XXX Rename this class to "Constants"
interface DatabaseGenerationConstants {

    /** Separator between property name bases and indicators. */
    static final char DOT = '.';

    /** Indicator that property is for a maximum length. */
    static final String INDICATOR_MAXIMUM_LENGTH = "maximum-length"; //NOI18N

    /**
     * (Partial) indicator that property is for attributes of SQL.  The prefix
     * is recognized by alone by itself in MappingFile, and is used by other
     * DatabaseGenerationConstants.
     */
    static final String INDICATOR_JDBC_PREFIX = "jdbc"; //NOI18N

    /** Indicator that property designates the length of mapped SQL type. */
    static final String INDICATOR_JDBC_LENGTH =
        INDICATOR_JDBC_PREFIX + "-" + INDICATOR_MAXIMUM_LENGTH; //NOI18N

    /** Indicator that property designates the nullability of mapped SQL type. */
    static final String INDICATOR_JDBC_NULLABLE =
        INDICATOR_JDBC_PREFIX + "-nullable"; //NOI18N

    /** Indicator that property designates the precision of mapped SQL type. */
    static final String INDICATOR_JDBC_PRECISION =
        INDICATOR_JDBC_PREFIX + "-precision"; //NOI18N

    /** Indicator that property designates the scale of mapped SQL type. */
    static final String INDICATOR_JDBC_SCALE =
        INDICATOR_JDBC_PREFIX + "-scale"; //NOI18N

    /** Indicator that property designates the type of a mapped SQL type. */
    static final String INDICATOR_JDBC_TYPE =
        INDICATOR_JDBC_PREFIX + "-type"; //NOI18N
}
