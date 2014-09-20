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
 * TestInformation.java
 *
 * Created on December 4, 2000, 4:29 PM
 */

package com.sun.enterprise.tools.verifier;

/**
 * Holds information about a particular test like the name of the class
 * implementing the test or the version of the spec it applies to
 *
 * @author Jerom Dochez
 */
public class TestInformation extends Object {

    private String className = null;
    private String minimumVersion = null;
    private String maximumVersion = null;

    /**
     * @return the className implementing the test
     */
    public String getClassName() {
        return className;
    }

    /*
     * set the class name implementing the test
     * 
     * @param className is the class name
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @return the minimum version of the spec this test applies to
     */
    public String getMinimumVersion() {
        return minimumVersion;
    }

    /**
     * set the minimum version of the spec this test applies to
     *
     * @param minimumVersion is the version
     */
    public void setMinimumVersion(String minimumVersion) {
        this.minimumVersion = minimumVersion;
    }

    /**
     * @return maximumVersion the maximum version of the spec this test applies to
     */
    public String getMaximumVersion() {
        return maximumVersion;
    }

    /**
     * set the minimum version of the spec this test applies to
     *
     * @param maximumVersion is the version
     */
    public void setMaximumVersion(String maximumVersion) {
        this.maximumVersion = maximumVersion;
    }
}
