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

package com.sun.enterprise.deployment.xml;

/**
 * These names match names defined in persistence.xsd file
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public interface PersistenceTagNames extends TagNames {

    public static String PERSISTENCE = "persistence";
    public static String PERSISTENCE_UNIT = "persistence-unit";
    public static String DESCRIPTION = "description";
    public static String PROVIDER = "provider";
    public static String JTA_DATA_SOURCE = "jta-data-source";
    public static String NON_JTA_DATA_SOURCE = "non-jta-data-source";
    public static String JAR_FILE = "jar-file";
    public static String MAPPING_FILE = "mapping-file";
    public static String CLASS = "class";
    public static String EXCLUDE_UNLISTED_CLASSES = "exclude-unlisted-classes";
    public static String PROPERTIES = "properties";
    public static String PROPERTY = "property";
    public static String PROPERTY_NAME = "name";
    public static String PROPERTY_VALUE = "value";
    public static String NAME = "name";
    public static String TRANSACTION_TYPE = "transaction-type";
    public static String SHARED_CACHE_MODE = "shared-cache-mode";
    public static String VALIDATION_MODE = "validation-mode";

}
