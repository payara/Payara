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

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import static com.sun.enterprise.loader.DirWatcher.hasItem;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.createTempDirectory;

public class DirWatcherTest {


    @Test
    public void existingItemsFound() throws IOException {
        Path root = createTempDirectory(Paths.get("target") , "watch");
        createFile(root.resolve("file1"));
        createFile(root.resolve("file2"));
        createDirectories(root.resolve("com/empty/"));
        createDirectories(root.resolve("com/actually/used"));
        createFile(root.resolve("com/actually/file3"));
        createFile(root.resolve("com/actually/used/file4"));
        createDirectories(root.resolve("META-INF/services"));
        createFile(root.resolve("META-INF/beans.xml"));
        createFile(root.resolve("META-INF/services/javax.service.type"));

        DirWatcher.register(root);

        assertTrue("root item should be found", () -> hasItem(root, "file1"));
        assertTrue("root item should be found", () -> hasItem(root, "file2"));
        assertFalse("non existing method should not be found", () -> hasItem(root, "file3"));
        assertFalse("empty directory should be ignored", () -> hasItem(root, "com/empty/file3"));
        assertTrue("existing shallow item should be found", () -> hasItem(root, "com/actually/file3"));
        assertTrue("existing deep item should be found", () -> hasItem(root, "com/actually/used/file4"));
        assertTrue("deep item should be guessed to exist if other items exist", () -> hasItem(root, "com/actually/used/file5"));

        assertTrue("META-INF entry should be found", () -> hasItem(root, "META-INF/services/javax.service.type"));
        assertTrue("existing shallow item should be found", () -> hasItem(root, "META-INF/beans.xml"));
    }

    @Test
    public void itemsFoundAsTheyAreAdded() throws IOException {
        Path root = createTempDirectory(Paths.get("target") , "watch");
        DirWatcher.register(root);
        assertFalse("non-existing root item should not be found", () -> hasItem(root, "file1"));
        createFile(root.resolve("file1"));
        createFile(root.resolve("file2"));
        assertTrue("root item should be found", () -> hasItem(root, "file1"));
        assertTrue("root item should be found", () -> hasItem(root, "file2"));

        createDirectories(root.resolve("com/empty/"));

        assertTrue("only first level is considered for items added after registration", () -> hasItem(root, "com/"));
        assertTrue("only first level is considered for items added after registration", () -> hasItem(root, "com/empty"));
        assertTrue("only first level is considered for items added after registration", () -> hasItem(root, "com/empty/reallyEmpty"));

        createDirectories(root.resolve("com/actually/"));
        createFile(root.resolve("com/actually/file3"));
        assertTrue("only first level is considered for items added after registration", () -> hasItem(root, "com/actually/file3"));

        createDirectories(root.resolve("META-INF/services"));
        createFile(root.resolve("META-INF/beans.xml"));
        createFile(root.resolve("META-INF/services/javax.service.type"));
        assertTrue("only first level is considered for items added after registration", () -> hasItem(root, "META-INF/services/javax.service.type"));
        assertTrue("only first level is considered for items added after registration", () -> hasItem(root, "META-INF/beans.xml"));
    }

    @Test
    public void itemsFoundIfRootDoesNotExist() throws IOException {
        Path root = createTempDirectory(Paths.get("target") , "watch").resolve("later");
        DirWatcher.register(root);
        assertFalse("non-existing root item should not be found", () -> hasItem(root, "file1"));

        createDirectory(root);
        createFile(root.resolve("file1"));
        createFile(root.resolve("file2"));
        assertTrue("root item should be found", () -> hasItem(root, "file1"));
        assertTrue("root item should be found", () -> hasItem(root, "file2"));

        createDirectories(root.resolve("com/empty/"));

        assertTrue("only first level is considered for items added after registration", () -> hasItem(root, "com/"));
        assertTrue("only first level is considered for items added after registration", () -> hasItem(root, "com/empty"));
        assertTrue("only first level is considered for items added after registration", () -> hasItem(root, "com/empty/reallyEmpty"));
    }

    @Test
    public void itemsFoundIfTwoLevelsDoNotExist() throws IOException {
        Path root = createTempDirectory(Paths.get("target") , "watch").resolve("later/and/more");
        DirWatcher.register(root);
        assertFalse("non-existing root item should not be found", () -> hasItem(root, "file1"));

        createDirectories(root);
        createFile(root.resolve("file1"));
        createFile(root.resolve("file2"));
        assertTrue("root item should be found", () -> hasItem(root, "file1"));
        assertTrue("root item should be found", () -> hasItem(root, "file2"));

        createDirectories(root.resolve("com/empty/"));

        assertTrue("only first level is considered for items added after registration", () -> hasItem(root, "com/"));
        assertTrue("only first level is considered for items added after registration", () -> hasItem(root, "com/empty"));
        assertTrue("only first level is considered for items added after registration", () -> hasItem(root, "com/empty/reallyEmpty"));
    }

    static void assertTrue(String message, Supplier<Boolean> test) {
        for(int i=0; i<1000; i++) { // watch service takes some time to react, so let's make few attempts before giving up
            Boolean result = test.get();
            if (result) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // let's ignore it for the moment
            }
        }
        throw new AssertionError(message);
    }

    static void assertFalse(String message, Supplier<Boolean> test) {
        assertTrue(message, () -> !test.get());
    }
}
