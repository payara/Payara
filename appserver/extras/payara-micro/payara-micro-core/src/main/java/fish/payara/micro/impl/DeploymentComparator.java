/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.micro.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Compares deployment files by their file extension.
 */
public class DeploymentComparator implements Comparator<File> {

    // All the file extensions - in the order they should be deployed.
    private final List<String> fileExtensions;

    public DeploymentComparator() {
        fileExtensions = new ArrayList<>();
        fileExtensions.add(".rar");
        fileExtensions.add(".jar");
        fileExtensions.add(".war");
    }

    /**
     * Compare two files to see which should be deployed first based on file
     * extension. Unknown extensions and null files are sorted to the back in
     * that order.
     *
     * @param f1 the first file to compare
     * @param f2 the second file to compare
     * @return a negative integer, zero or a positive integer if the first file
     * should be deployed before, at the same time as, or after the second file.
     */
    @Override
    public int compare(File f1, File f2) {
        String extension1 = null;
        String extension2 = null;

        // First check for null files, and sort them to the back
        if (f1 == null) {
            return 5;
        }
        if (f2 == null) {
            return -5;
        }

        // Now get the extensions of the files. Unknown extensions will be sorted to the back.
        for (String format : fileExtensions) {
            if (f1.getAbsolutePath().endsWith(format)) {
                extension1 = format;
            }
            if (f2.getAbsolutePath().endsWith(format)) {
                extension2 = format;
            }
        }

        // Now sort unknown extensions to the back, but in front of null files.
        if (extension1 == null) {
            return 5;
        }
        if (extension2 == null) {
            return -5;
        }

        // Get the index of the extension in the list.
        int index1 = fileExtensions.indexOf(extension1);
        int index2 = fileExtensions.indexOf(extension2);

        return index1 - index2;
    }

    /**
     * Get the possible file extensions.
     *
     * @return The possible file extensions
     */
    public List<String> getFileExtensions() {
        return fileExtensions;
    }

}
