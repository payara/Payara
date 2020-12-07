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

import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class DirConfigSourceTest {

    private static Path testDirectory;
    private static DirConfigSource source;
    
    @BeforeClass
    public static void setUp() throws IOException {
        testDirectory = Files.createTempDirectory("microprofile-config-test-");
        // create & load
        source = new DirConfigSource(testDirectory);
    }

    @AfterClass
    public static void tearDown() throws IOException {
        Files.walk(testDirectory)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }
    
    @Test
    public void testParsePropertyNameFromPath() {
        // given
        Map<Path,String> examples = new HashMap<>();
        examples.put(Paths.get(testDirectory.toString(), "foo/bar/test/ex"), "foo.bar.test.ex");
        examples.put(Paths.get(testDirectory.toString(), "foo.bar.test/ex"), "foo.bar.test.ex");
        examples.put(Paths.get(testDirectory.toString(), "foo/bar.test/ex"), "foo.bar.test.ex");
        examples.put(Paths.get(testDirectory.toString(), "foo.bar/test/ex"), "foo.bar.test.ex");
        
        // we ignore the last file extension. always. this might lead to unexpected behaviour for a user.
        // best advice: do not use dots in filename, only in directory names.
        examples.put(Paths.get(testDirectory.toString(), "foo/bar/test/ex.txt"), "foo.bar.test.ex");
        examples.put(Paths.get(testDirectory.toString(), "foo/bar/test/ex.tar.gz"), "foo.bar.test.ex.tar");
        examples.put(Paths.get(testDirectory.toString(), "foo.bar/test.ex"), "foo.bar.test");
        examples.put(Paths.get(testDirectory.toString(), "foo/bar.test.ex"), "foo.bar.test");
        examples.put(Paths.get(testDirectory.toString(), "foo.bar.test.ex"), "foo.bar.test");
        examples.put(Paths.get(testDirectory.toString(), "foo/bar/test.ex"), "foo.bar.test");
        
        // when & then
        for (Map.Entry<Path, String> ex : examples.entrySet()) {
            System.out.println(ex.getKey()+" = "+ex.getValue());
            assertEquals(ex.getValue(), source.parsePropertyNameFromPath(ex.getKey()));
        }
    }
    
    @Test
    public void testCheckLongestMatchForPath_PathDepthLessSpecific() {
        // given
        Map<String,DirConfigSource.DirProperty> props = new HashMap<>();
        // a property with a most specific path
        String property = "foo.bar.test.ex";
        props.put(property,
                  new DirConfigSource.DirProperty(
                      "test", FileTime.from(Instant.now()),
                      Paths.get(testDirectory.toString(), "foo/bar/test/ex"),
                      testDirectory));
        source.setProperties(props);
        
        // when & then
        assertFalse(source.isLongerMatchForPath(property, Paths.get(testDirectory.toString(), "foo/bar.test/ex")));
    }
    
    @Test
    public void testCheckLongestMatchForPath_PathDepthMoreSpecific() {
        // given
        Map<String,DirConfigSource.DirProperty> props = new HashMap<>();
        // a property with a most specific path
        String property = "foo.bar.test.ex";
        props.put(property,
            new DirConfigSource.DirProperty(
                "test", FileTime.from(Instant.now()),
                Paths.get(testDirectory.toString(), "foo.bar/test/ex"),
                testDirectory));
        source.setProperties(props);
        
        // when & then
        assertTrue(source.isLongerMatchForPath(property, Paths.get(testDirectory.toString(), "foo/bar/test/ex")));
    }
    
    @Test
    public void testCheckLongestMatchForPath_PathDepthEqualMoreSpecific() {
        // given
        Map<String,DirConfigSource.DirProperty> props = new HashMap<>();
        // a property with a most specific path
        String property = "foo.bar.test.ex.one";
        props.put(property,
            new DirConfigSource.DirProperty(
                "test", FileTime.from(Instant.now()),
                Paths.get(testDirectory.toString(), "foo.bar/test.ex/one"),
                testDirectory));
        source.setProperties(props);
        
        // when & then
        assertTrue(source.isLongerMatchForPath(property, Paths.get(testDirectory.toString(), "foo.bar/test/ex.one.txt")));
    }
    
    @Test
    public void testCheckLongestMatchForPath_PropNotPresent() {
        // given
        Map<String,DirConfigSource.DirProperty> props = new HashMap<>();
        // a property with a most specific path
        String property = "foo.bar.test.ex.one";
        props.put(property,
            new DirConfigSource.DirProperty(
                "test", FileTime.from(Instant.now()),
                Paths.get(testDirectory.toString(), "foo.bar/test.ex/one"),
                testDirectory));
        source.setProperties(props);
        
        // when & then
        assertTrue(source.isLongerMatchForPath("foo.bar.test.ex.two", Paths.get(testDirectory.toString(), "foo.bar/test/ex.two.txt")));
    }
    
    @Test
    public void testRemovePropertyFromPath() {
        // given
        Map<String,DirConfigSource.DirProperty> props = new HashMap<>();
        // a property with a most specific path
        String property = "foo.bar.test";
        props.put(property,
            new DirConfigSource.DirProperty(
                "test", FileTime.from(Instant.now()),
                Paths.get(testDirectory.toString(), "foo/bar/test"),
                testDirectory));
        source.setProperties(props);
        assertEquals("test", source.getValue(property));
        
        // when
        source.removePropertyFromPath(Paths.get(testDirectory.toString(), "foo/bar/test"));
        // then
        assertTrue(source.getValue(property) == null);
        
    }

    @Test
    public void testInitializeProperties_SimpleFiles() throws IOException {
        // given
        // only the most specific should be picked up (=test3)
        writeFile(testDirectory, "foo.bar.test", "test");
        writeFile(testDirectory, "foo.bar/test", "test2");
        writeFile(testDirectory, "foo/bar/test", "test3");
        
        //when
        source.initializePropertiesFromPath(testDirectory);
        
        //then
        assertEquals("test3", source.getValue("foo.bar.test"));
    }
    
    @Test
    public void testInitializeProperties_IgnoreHidden() throws IOException {
        // given
        // none of these should be picked up (hidden file or dir)
        writeFile(testDirectory, ".hidden.bar.test", "test");
        writeFile(testDirectory, ".hidden/bar.test", "test");
        //when
        source.initializePropertiesFromPath(testDirectory);
        //then
        assertEquals(null, source.getValue("hidden.bar.test"));
    }
    
    @Test(expected = IOException.class)
    public void testInitializeProperties_FailDirectory() throws IOException {
        // given
        Path failDir = Paths.get("/tmp/fail-112312");
        //when & then
        source.initializePropertiesFromPath(failDir);
    }
    
    public static Path writeFile(Path parentDir, String filename, String content) throws IOException {
        Path file = Paths.get(parentDir.toString(), filename);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

}
