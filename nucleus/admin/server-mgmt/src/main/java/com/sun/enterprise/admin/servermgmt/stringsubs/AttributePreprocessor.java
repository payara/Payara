/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.stringsubs;

/**
 * An object to pre-process the input string. This input string can either b
 */
public interface AttributePreprocessor {

    /**
     * Process the before value of the change-pair element and retrieve its value.
     * <p>
     * Note: A change-pair element is a macro definition that specifies the
     *  string to be substituted ("before") and the replacement ("after") value.
     *  <br/>
     *  E.g. &lt;change-pair id="pair1" before="@JAVA_HOME" after="$JAVA_HOME$"/&gt;
     * </p>
     * @param beforeValue The before value of change-pair.
     * @return Substituted String.
     * @see ChangePair#getBefore()
     */
    String substituteBefore(String beforeValue);

    /**
     * Process the after value of the change-pair element and retrieve its value.
     * <p>
     * Note: A change-pair element is a macro definition that specifies the
     *  string to be substituted ("before") and the replacement ("after") value.
     *  <br/>
     *  E.g. &lt;change-pair id="pair1" before="@JAVA_HOME" after="$JAVA_HOME$"/&gt;
     * </p>
     * @param afterValue The after value of change-pair.
     * @return Substituted String.
     * @see ChangePair#getAfter()
     */
    String substituteAfter(String afterValue);

    /**
     * Process the file name/member entry path. The name value of file-entry
     * can contain the substitutable variable for e.g.
     * <p>
     * &lt;file-entry name="$DOMAIN_DIRECTORY$/start.cmd"/&gt;
     * </p>
     * Path pointing to the domain directory. The value of these variable will be
     * retrieved.
     * <p>
     * Note: A file-entry defines a text file or set of files where
     * substitution has to be performed.<br/>
     * </p>
     * @param path The file path.
     * @return Substituted String.
     * @see FileEntry#getName()
     */
    String substitutePath(String path);
}
