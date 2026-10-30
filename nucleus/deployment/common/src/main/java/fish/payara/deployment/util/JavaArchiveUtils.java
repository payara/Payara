/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.deployment.util;

import java.io.File;

/**
 * General Java Archive manipulation utilities.
 */
public final class JavaArchiveUtils {
    //Suppress default constructor
    private JavaArchiveUtils() {
        throw new AssertionError();
    }

    /**
     * Tests whether a file name has the Java archive extension.
     *
     * @param fileName the file name to test.
     * @param processEar if {@code true}, tests the EAR extension.
     * @return {@code true} if the file name is not {@code null} and has Java archive extension; {@code false} otherwise.
     */
    public static boolean hasJavaArchiveExtension(String fileName, boolean processEar) {
        if (fileName == null) {
            return false;
        }
        return fileName.endsWith(".war") || fileName.endsWith(".rar") || fileName.endsWith(".jar") || (processEar && fileName.endsWith(".ear"));
    }

    /**
     * Removes the Java archive extension from the file name.
     *
     * @param fileName the file name.
     * @param processEar if {@code true}, removes the EAR extension.
     * @return The file name without extension.
     */
    public static String removeJavaArchiveExtension(String fileName, boolean processEar) {
        if (hasJavaArchiveExtension(fileName, processEar)) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }

    /**
     * Tests whether a file name has the Web archive extension.
     *
     * @param fileName the file name to test.
     * @return {@code true} if the file name has Web archive extension; {@code false} otherwise.
     */
    public static boolean hasWebArchiveExtension(String fileName) {
        if (fileName == null) {
            return false;
        }
        return fileName.endsWith(".war");
    }

    /**
     * Tests whether {@code archive} contains {@code WEB-INF} subdirectory.
     *
     * @param archive the archive to test.
     * @return {@code true} if the archive contains {@code WEB-INF} subdirectory; {@code false} otherwise.
     */
    public static boolean hasWebInf(File archive) {
        if (archive == null) {
            return false;
        }
        return new File(archive, "WEB-INF").exists();
    }

    /**
     * Tests whether {@code archive} has context root.
     * <p/>
     * Note: now tests only Web archives.
     *
     * @param archive the archive to test.
     * @return {@code true} if the archive has context root; {@code false} otherwise.
     */
    public static boolean hasContextRoot(File archive) {
        if (archive == null) {
            return false;
        }
        return archive.isFile() ? hasWebArchiveExtension(archive.getName()) : archive.isDirectory() && hasWebInf(archive);
    }
}
