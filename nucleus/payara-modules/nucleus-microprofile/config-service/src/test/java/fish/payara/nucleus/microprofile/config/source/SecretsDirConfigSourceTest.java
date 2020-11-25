/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.nucleus.microprofile.config.source;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 *
 * @author steve
 */
public class SecretsDirConfigSourceTest {

    private Path testDirectory;
    private SecretsDirConfigSource source;

    @Before
    public void setUp() throws IOException {
        testDirectory = Files.createTempDirectory("microprofile-config-test");
        
        // create a couple of test simple files
        Path file1 = Paths.get(testDirectory.toString(), "property1");
        Path file2 = Paths.get(testDirectory.toString(), "property2");
        Path fileHidden = Paths.get(testDirectory.toString(), ".hidden-property");
        file1 = Files.createFile(file1);
        Files.write(file1, "value1".getBytes());
        file2 = Files.createFile(file2);
        Files.write(file2, "value2".getBytes());
        fileHidden = Files.createFile(fileHidden);
        
        // create a subdirectory structure with test files
        Path mounted = Paths.get(testDirectory.toString(), "foo", "bar");
        Files.createDirectories(mounted);
        
        Path fileMounted = Paths.get(mounted.toString(), "property3");
        fileMounted = Files.createFile(fileMounted);
        Files.write(fileMounted, "value3".getBytes());
        
        // create "foo/bar/..data/property4" and symlink from "foo/bar/property4" as done on K8s
        Path mountedK8s = Paths.get(mounted.toString(), "..data");
        Files.createDirectories(mountedK8s);
        Path fileK8sMounted = Paths.get(mountedK8s.toString(), "property4");
        fileK8sMounted = Files.createFile(fileK8sMounted);
        Files.write(fileK8sMounted, "value4".getBytes());
        Path fileK8sSymlink = Paths.get(mounted.toString(), "property4");
        fileK8sSymlink = Files.createSymbolicLink(fileK8sSymlink, fileK8sMounted);
    
        // create & load
        source = new SecretsDirConfigSource(testDirectory);
    }

    @After
    public void tearDown() throws IOException {
        Files.walk(testDirectory)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    /**
     * Test of getProperties method, of class SecretsDirConfigSource.
     */
    @Test
    public void testGetProperties() {
        Map<String, String> expected = new HashMap<>();
        expected.put("property1", "value1");
        expected.put("property2", "value2");
        expected.put("foo.bar.property3", "value3");
        expected.put("foo.bar.property4", "value4");
        assertEquals(expected, source.getProperties());
    }

    /**
     * Test of getPropertyNames method, of class SecretsDirConfigSource.
     */
    @Test
    public void testGetPropertyNames() {
        assertEquals(new HashSet<>(asList("property1", "property2", "foo.bar.property3", "foo.bar.property4")), source.getPropertyNames());
    }

    /**
     * Test of getValue method, of class SecretsDirConfigSource.
     */
    @Test
    public void testGetValue() {
        assertEquals("value1", source.getValue("property1"));
        assertEquals("value2", source.getValue("property2"));
        assertEquals("value3", source.getValue("foo.bar.property3"));
        assertEquals("value4", source.getValue("foo.bar.property4"));
    }

    /**
     * Test of getName method, of class SecretsDirConfigSource.
     */
    @Test
    public void testGetName() {
        assertEquals("Secrets Directory", source.getName());
    }

    /**
     * Test the changed Property
     * @throws java.io.IOException
     */
    @Test
    public void testChangeProperty() throws IOException {
        assertEquals("value1", source.getValue("property1"));
        // change the file
        Path file1 = Paths.get(testDirectory.toString(), "property1");
        Files.write(file1, "value-changed".getBytes());
        try {
            FileTime nowplus1sec = FileTime.fromMillis(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1));
            Files.setLastModifiedTime(file1, nowplus1sec);
            assertEquals("value-changed", source.getValue("property1"));
        } finally {
            // clean up
            Files.write(file1, "value1".getBytes());
        }
    }
    
    /**
     * Test the changed Property in subdirectories
     * @throws java.io.IOException
     */
    @Test
    public void testChangePropertyInSubdir() throws IOException {
        assertEquals("value4", source.getValue("foo.bar.property4"));
        // change the file
        Path file = Paths.get(testDirectory.toString(), "foo", "bar", "..data", "property4");
        Files.write(file, "value-changed".getBytes());
        try {
            FileTime nowplus1sec = FileTime.fromMillis(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1));
            Files.setLastModifiedTime(file, nowplus1sec);
            assertEquals("value-changed", source.getValue("foo.bar.property4"));
        } finally {
            // clean up
            Files.write(file, "value4".getBytes());
        }
    }
    
    /**
     * Tests getting a new property as the file has now appeared
     */
    @Test
    public void testNewFile() throws IOException {
        assertNull(source.getValue("property-new"));
        // change the file
        Path file1 = Paths.get(testDirectory.toString(), "property-new");
        Files.write(file1, "newValue".getBytes());
        try {
            assertEquals("newValue", source.getValue("property-new"));
        } finally {
            // clean up
            Files.delete(file1);
        }
    }
    
    /**
     * Tests getting a new property as the file has now appeared in a subdirectory
     */
    @Test
    public void testNewFileInSubdir() throws IOException {
        assertNull(source.getValue("foo.bar.property-new"));
        // change the file
        Path file = Paths.get(testDirectory.toString(), "foo", "bar", "..data", "property-new");
        Files.write(file, "newValue".getBytes());
        Path fileSymlink = Paths.get(testDirectory.toString(), "foo", "bar", "property-new");
        Files.createSymbolicLink(fileSymlink, file);
        try {
            assertEquals("newValue", source.getValue("foo.bar.property-new"));
        } finally {
            // clean up
            Files.delete(file);
            Files.delete(fileSymlink);
        }
    }
    
    @Test
    public void testBadDirectoryNoBlowUp() {
        assertNull(new SecretsDirConfigSource(Paths.get(testDirectory.toString(), "FOOBLE")).getValue("BILLY"));
    }
}
