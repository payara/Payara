/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package com.sun.enterprise.loader;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Determine if classpath item exists in given root folder.
 * If directory is empty, or not exist at time of registration, it will be registered to watch for changes.
 */
class ItemChecker extends WatchProcessor {

    private Set<String> prefixes = new HashSet<>();
    /**
     * How many directories below root we'll scan for prefixes
     */
    private static final int WALK_DEPTH = 3;
    private boolean lateRegistration;

    protected ItemChecker(Path root, WatchService watchService) {
        super(root, watchService);
    }

    @Override
    protected void register() {
        if (registered) {
            return;
        }
        try {
            this.prefixes = AccessController.doPrivileged((PrivilegedExceptionAction<Set<String>>)this::readPrefixes);
            if (prefixes.isEmpty()) {
                LOGGER.fine(() -> "Registering watch for " + root);
            } else {
                LOGGER.fine(() -> "Will lookup classes in " + root + " for "+prefixes);
            }
            registered = true;
            if (prefixes.isEmpty() || lateRegistration) {
                registerFilesystemWatch();
            }
        } catch (PrivilegedActionException e) {
            LOGGER.log(Level.WARNING, e, () -> "Failed to register watcher for " + root);
        }
    }

    private void registerLater() {
        this.lateRegistration = true;
    }

    /**
     * Find valid classpath prefixes in root directory.
     * We go three levels deep and collect any directories with files or the directories at deepest level
     * @return
     * @throws IOException
     */
    Set<String> readPrefixes() throws IOException {
        Set<Path> prefixPaths = new HashSet<>();
        int level0 = root.getNameCount();
        Files.walkFileTree(root, Collections.emptySet(), WALK_DEPTH+1, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (attrs.isRegularFile()) {
                    // root resources stand for themselves, others contribute to their package
                    Path target = file.getParent().equals(root) ? file : file.getParent();
                    prefixPaths.add(target);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                // the directories at deepest level are also prefixes
                if (dir.getNameCount() - level0 == WALK_DEPTH) {
                    prefixPaths.add(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return prefixPaths.stream()
                .map(path -> root.relativize(path))
                .map(path -> path.toString().replace('\\','/'))
                .collect(Collectors.toSet());
    }

    @Override
    protected boolean created(Path filename) {
        LOGGER.fine(() -> root.toAbsolutePath() + ": "+filename+" created");
        // file watcher is not recursive, and therefore we're only notified about top level directories.
        // this is still good approximation
        prefixes.add(filename.toString());
        return true;
    }

    /**
     * Check if item might be found in root directory.
     * Answer of {@code false} will prevent further lookup of the item. Therefore implementation might answer with false
     * positives, but may not return false negatives.
     * @param item item to look for
     * @return false if the item is not present in directory
     */
    public boolean hasItem(String item) {
        // if we're not registered, the directory is surely empty -> false
        // if we overflowed, we cannot be sure -> true is safe answer
        // otherwise find if item is within known prefixes
        return registered && (overflowed || prefixes.stream().anyMatch(prefix -> item.startsWith(prefix)));
    }

    static ItemChecker registerExisting(Path root, WatchService watchService) {
        ItemChecker resolver = new ItemChecker(root, watchService);
        resolver.register();
        return resolver;
    }

    static ItemChecker registerNonExisting(Path root, WatchService watchService) {
        ItemChecker resolver = new ItemChecker(root, watchService);
        resolver.registerLater();
        return resolver;
    }
}
