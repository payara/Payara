/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2025 Payara Foundation and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

/**
 * Thread implementation to delete files on JVM shutdown. This Thread is registered
 * as shutdown hook with initialisation of the {@link FileUtils} class.
 *
 * @author Sven Diedrichsen
 */
public final class FileDeletionThread extends Thread {

    private static final Logger LOG = Logger.getLogger(FileDeletionThread.class.getName());
    /**
     * Maximum number of file deletion attempts.
     */
    private static final int MAX_DELETION_ATTEMPTS = 3;
    /**
     * Files to delete on JVM shutdown.
     */
    private final Set<File> filesToDelete = new ConcurrentSkipListSet<>();

    /**
     * Mark file for deletion on JVM shutdown.
     * @param file file to delete on shutdown.
     */
    public void add(File file) {
        if (file != null) {
            filesToDelete.add(file);
        }
    }

    @Override
    public void run() {
        // map to keep track of failed deletion attempts
        Map<File, Integer> deletionAttempts = new HashMap<>();
        // to make sure files added during iteration are deleted in successive runs
        while (!filesToDelete.isEmpty()) {
            Iterator<File> fileIterator = filesToDelete.iterator();
            while (fileIterator.hasNext()) {
                final File fileToDelete = fileIterator.next();
                final boolean deletionSuccessful = delete(fileToDelete);
                if (deletionSuccessful ||
                        deletionAttempts.compute(
                            fileToDelete,
                            (file, attempts) -> attempts == null ? 1 : attempts + 1
                        ) > MAX_DELETION_ATTEMPTS) {
                    // file deleted or attempted deletion exceeded max attempts
                    fileIterator.remove();
                    deletionAttempts.remove(fileToDelete);
                }
            }
        }
        deletionAttempts.clear();
    }

    private boolean delete(File file) {
        if (file.exists()) {
            try {
                return Files.walk(file.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .filter(f -> !f.delete())
                    .count() == 0;
            } catch (IOException e) {
                LOG.info("Cannot delete file "+file);
            }
        }
        return true;
    }

}
