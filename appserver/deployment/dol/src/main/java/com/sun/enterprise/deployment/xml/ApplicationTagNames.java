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
 * This interface holds all the XML tag names for a J2EE application
 * deployment descriptor.
 * @author Danny Coward
 */

public interface ApplicationTagNames extends TagNames {
     public static String APPLICATION = "application";
     public static String APPLICATION_NAME = "application-name";
     public static String INITIALIZE_IN_ORDER = "initialize-in-order";
     public static String MODULE = "module";
     public static String EJB = "ejb";
     public static String WEB = "web";
     public static String APPLICATION_CLIENT = "java";
     public static String CONNECTOR = "connector";
     public static String ALTERNATIVE_DD = "alt-dd";
     public static String SECUTIRY_ROLE = "security-role";
     public static String ROLE_NAME = "role-name";
     public static String CONTEXT_ROOT = "context-root";
     public static String WEB_URI = "web-uri";
     public static String LIBRARY_DIRECTORY = "library-directory";

}

