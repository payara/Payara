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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DirConfigSourceTest {

    private static Path testDirectory;
    private static DirConfigSource source;
    
    @BeforeClass
    public static void setUp() throws IOException {
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
        examples.put(Paths.get(testDirectory.toString(), "/foo/bar/test/ex"), "foo.bar.test.ex");
        examples.put(Paths.get(testDirectory.toString(), "/foo.bar.test/ex"), "foo.bar.test.ex");
        examples.put(Paths.get(testDirectory.toString(), "/foo/bar.test/ex"), "foo.bar.test.ex");
        examples.put(Paths.get(testDirectory.toString(), "/foo.bar/test/ex"), "foo.bar.test.ex");
        
        // we ignore the last file extension. always. this might lead to unexpected behaviour for a user.
        // best advice: do not use dots in filename, only in directory names.
        examples.put(Paths.get(testDirectory.toString(), "/foo/bar/test/ex.txt"), "foo.bar.test.ex");
        examples.put(Paths.get(testDirectory.toString(), "/foo/bar/test/ex.tar.gz"), "foo.bar.test.ex.tar");
        examples.put(Paths.get(testDirectory.toString(), "/foo.bar/test.ex"), "foo.bar.test");
        examples.put(Paths.get(testDirectory.toString(), "/foo/bar.test.ex"), "foo.bar.test");
        examples.put(Paths.get(testDirectory.toString(), "/foo.bar.test.ex"), "foo.bar.test");
        examples.put(Paths.get(testDirectory.toString(), "/foo/bar/test.ex"), "foo.bar.test");
        
        // when & then
        for (Map.Entry<Path, String> ex : examples.entrySet()) {
            System.out.println(ex.getKey()+" = "+ex.getValue());
            assertEquals(ex.getValue(), source.parsePropertyNameFromPath(ex.getKey()));
        }
    }
}
