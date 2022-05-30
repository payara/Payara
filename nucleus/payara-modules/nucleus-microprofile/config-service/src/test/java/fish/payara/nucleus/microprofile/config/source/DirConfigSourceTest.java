/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2021 Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.OS;
import fish.payara.nucleus.microprofile.config.spi.ConfigProviderResolverImpl;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.AssumptionViolatedException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DirConfigSourceTest {

    private static Path testDirectory;
    private static DirConfigSource source;
    private static ScheduledExecutorService exec = Executors.newScheduledThreadPool(3);
    private static ConfigProviderResolverImpl configService;
    
    @BeforeClass
    public static void setUp() throws IOException {
        testDirectory = Files.createTempDirectory("microprofile-config-test-");
        configService = mock(ConfigProviderResolverImpl.class);
        // create & load
        source = new DirConfigSource(testDirectory, configService);
    }

    @AfterClass
    public static void tearDown() throws IOException, InterruptedException {
        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.SECONDS);
        Files.walk(testDirectory)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }
    
    @After
    public void cleanProps() {
        // reset properties map after every test to avoid side effects
        source.setProperties(Collections.emptyMap());
    }
    
    @Test
    public void testFindDir_NullPath() throws IOException {
        // given
        MicroprofileConfigConfiguration config = mock(MicroprofileConfigConfiguration.class);
        when(configService.getMPConfig()).thenReturn(config);
        when(config.getSecretDir()).thenReturn(null);
        // when
        Optional<Path> sut = source.findDir();
        // then
        assertFalse(sut.isPresent());
    }
    
    @Test
    public void testFindDir_NotExistingPath() throws IOException {
        // given
        MicroprofileConfigConfiguration config = mock(MicroprofileConfigConfiguration.class);
        when(configService.getMPConfig()).thenReturn(config);
        when(config.getSecretDir()).thenReturn(DirConfigSource.DEFAULT_DIR);
        // when
        Optional<Path> sut = source.findDir();
        // then
        assertFalse(sut.isPresent());
    }
    
    @Test
    public void testFindDir_AbsolutePath() throws IOException {
        // given
        MicroprofileConfigConfiguration config = mock(MicroprofileConfigConfiguration.class);
        when(configService.getMPConfig()).thenReturn(config);
        when(config.getSecretDir()).thenReturn(testDirectory.toString());
        // when
        Optional<Path> sut = source.findDir();
        // then
        assertEquals(testDirectory, sut.get());
    }
    
    @Test
    public void testFindDir_RelativePath() throws IOException {
        // given
        System.setProperty("com.sun.aas.instanceRoot", testDirectory.toString());
        MicroprofileConfigConfiguration config = mock(MicroprofileConfigConfiguration.class);
        when(configService.getMPConfig()).thenReturn(config);
        when(config.getSecretDir()).thenReturn(".");
        
        // when
        Optional<Path> sut = source.findDir();
        
        // then
        assertEquals(testDirectory, sut.get());
    }
    
    @Test
    public void testIsAptDir() throws IOException {
        // given
        Map<Path, Boolean> examples = new HashMap<>();
        examples.put(subpath( "aptdir"), TRUE);
        examples.put(subpath( ".unaptdir"), FALSE);
    
        // when & then
        assertEquals(FALSE, DirConfigSource.isAptDir(null));
        assertEquals(FALSE, DirConfigSource.isAptDir(subpath( "aptnotexisting")));
        for (Map.Entry<Path, Boolean> ex : examples.entrySet()) {
            Files.createDirectories(ex.getKey());
            assertEquals(ex.getValue(), DirConfigSource.isAptDir(ex.getKey()));
        }
    }
    
    @Test
    public void testIsAptFile() throws IOException {
        
        Map<Path, Boolean> examples = new HashMap<>();
        examples.put(subpath( "aptdir", "aptfile"), TRUE);
        examples.put(subpath( "aptdir", ".unaptfile"), FALSE);
        
        assertEquals(FALSE, DirConfigSource.isAptFile(null, null));
        assertEquals(FALSE, DirConfigSource.isAptFile(subpath( "aptdir", "aptnotexisting"), null));
        for (Map.Entry<Path, Boolean> ex : examples.entrySet()) {
            BasicFileAttributes atts = writeFile(ex.getKey(), "test");
            assertEquals(ex.getValue(), DirConfigSource.isAptFile(ex.getKey(), atts));
        }
        
        Path file100k = subpath("aptdir", "100k-file");
        BasicFileAttributes atts100k = writeRandFile(file100k, 100*1024);
        assertEquals(TRUE, DirConfigSource.isAptFile(file100k, atts100k));
        
        Path file600k = subpath("aptdir", "600k-file");
        BasicFileAttributes atts600k = writeRandFile(file600k, 600*1024);
        assertEquals(FALSE, DirConfigSource.isAptFile(file600k, atts600k));
    }
    
    @Test
    public void testParsePropertyNameFromPath() {
        // given
        Map<Path,String> examples = new HashMap<>();
        examples.put(subpath( "foo", "bar", "test", "ex"), "foo.bar.test.ex");
        examples.put(subpath( "foo.bar.test", "ex"), "foo.bar.test.ex");
        examples.put(subpath( "foo", "bar.test", "ex"), "foo.bar.test.ex");
        examples.put(subpath( "foo.bar", "test", "ex"), "foo.bar.test.ex");
        
        // when & then
        for (Map.Entry<Path, String> ex : examples.entrySet()) {
            //System.out.println(ex.getKey()+" = "+ex.getValue());
            assertEquals(ex.getValue(), DirConfigSource.parsePropertyNameFromPath(ex.getKey(), testDirectory));
        }
    }
    
    @Test
    public void testReadPropertyFromPath() throws IOException {
        // given
        Path sut = subpath("aptdir", "sut-read-property");
        BasicFileAttributes attsSUT = writeFile(sut, "foobar");
        DirConfigSource.DirProperty example = new DirConfigSource.DirProperty(
          "foobar", attsSUT.lastModifiedTime(), sut
        );
        // when & then
        assertEquals(example, DirConfigSource.readPropertyFromPath(sut, attsSUT, testDirectory));
    }
    
    @Test
    public void testCheckLongestMatchForPath_SamePath() {
        // given
        List<Path> paths = Arrays.asList(
            subpath("foo.bar.ex.test.hello"),
            subpath("foo.bar.ex.test", "hello.txt"),
            subpath("foo.bar.ex", "test", "hello.txt"),
            subpath("foo.bar", "ex", "test", "hello.txt")
        );
        
        // when & then
        for (Path p : paths)
            assertTrue(DirConfigSource.isLongestMatchForPath(testDirectory, p, p));
    }
    
    @Test
    public void testCheckLongestMatchForPath_PathDepthGrowing() {
        // given
        String propFile = "foo.bar.ex.test.hello";
        
        for (int i = 0; i < propFile.chars().filter(ch -> ch == '.').count(); i++) {
            // when
            String newPath = "";
            if (OS.isWindows()) {
                newPath = propFile.replaceFirst("\\.", "\\\\");
            } else {
                newPath = propFile.replaceFirst("\\.", File.separator);
            }
            
            // then
            assertTrue(DirConfigSource.isLongestMatchForPath(testDirectory, subpath(propFile), subpath(newPath)));
            assertFalse(DirConfigSource.isLongestMatchForPath(testDirectory, subpath(newPath), subpath(propFile)));
            
            propFile = newPath;
        }
    }
    
    @Test
    public void testCheckLongestMatchForPath_PathDepthShrinking() {
        // given
        String propFile = Paths.get("foo", "bar", "ex", "test", "hello").toString();
        
        for (int i = 0; i < propFile.chars().filter(ch -> ch == '.').count(); i++) {
            // when
            String newPath = propFile.replaceFirst(File.separator, ".");
            
            // then
            assertFalse(DirConfigSource.isLongestMatchForPath(testDirectory, subpath(propFile), subpath(newPath)));
            assertTrue(DirConfigSource.isLongestMatchForPath(testDirectory, subpath(newPath), subpath(propFile)));
            
            propFile = newPath;
        }
    }
    
    @Test
    public void testCheckLongestMatchForPath_PathDepthEqualMoreSpecificDotsLeftGrowing() {
        // given
        String prop = "foo.bar.example.test.hello.world.stranger.ohmy.how.long.is.this";
        
        // get all dot positions
        int dotcount = (int)prop.chars().filter(ch -> ch == '.').count();
        int[] pos = new int[dotcount];
        int offset = 0;
        for (int i = 0; i < pos.length; i++) {
            pos[i] = prop.indexOf(".", offset);
            offset = pos[i]+1;
        }
    
        // move the slashes on the string and check for being more specific
        for (int i = 0; i < dotcount-6; i++) {
            // when
            StringBuffer oldPath = new StringBuffer(prop)
                .replace(pos[i], pos[i]+1, File.separator)
                .replace(pos[i+2], pos[i+2]+1, File.separator)
                .replace(pos[i+5], pos[i+5]+1, File.separator);
            //System.out.println(oldPath.toString());
            StringBuffer newPath = new StringBuffer(prop)
                .replace(pos[i+1], pos[i+1]+1, File.separator)
                .replace(pos[i+3], pos[i+3]+1, File.separator)
                .replace(pos[i+6], pos[i+6]+1, File.separator);
            //System.out.println(newPath.toString());
            
            // then
            // --> we assert that independent on the number of dots in dir names (the total count of dots present
            //     does not change above), the path with a dot more counting from the left is always more specific.
            //     keep in mind that the number of subdirectory levels is not changing!
            assertTrue(DirConfigSource.isLongestMatchForPath(testDirectory, subpath(oldPath.toString()), subpath(newPath.toString())));
        }
    }
    
    @Test
    public void testCheckLongestMatchForPath_PathDepthEqualMoreSpecificDotMoving() {
        // given
        List<String[]> propPaths = Arrays.asList(
            new String[]{"foo.bar", "example", "test", "hello"},
            new String[]{"foo", "bar.example", "test", "hello"},
            new String[]{"foo", "bar", "example.test", "hello"},
            new String[]{"foo", "bar", "example", "test.hello"}
        );
        
        // move the slashes on the string and check for being more specific
        for (int i = 1; i < propPaths.size()-1; i++) {
            // when & then
            // --> we assert that a larger number of subdirectories before a dot is always a more specific path
            assertTrue(DirConfigSource.isLongestMatchForPath(testDirectory, subpath(propPaths.get(i-1)), subpath(propPaths.get(i))));;
        }
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
                // different from call below, as we want to test a missing property!
                subpath("foo.bar", "test.ex", "one")));
        source.setProperties(props);
        
        // when & then
        assertTrue(source.isLongestMatchForPath("foo.bar.test.ex.two", subpath( "foo.bar", "test", "ex.two.txt")));
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
                subpath( "foo", "bar", "test")));
        source.setProperties(props);
        assertEquals("test", source.getValue(property));
        
        // when
        source.removePropertyFromPath(subpath( "foo", "bar", "test"));
        // then
        assertTrue(source.getValue(property) == null);
        
    }
    
    @Test
    public void testUpsertPropertyFromPath_InsertCase() throws IOException {
        // given
        Path sut = subpath("aptdir", "sut-upsert-property-insert");
        BasicFileAttributes attsSUT = writeFile(sut, "foobar");
        DirConfigSource.DirProperty example = new DirConfigSource.DirProperty(
            "foobar", attsSUT.lastModifiedTime(), sut);
        
        // when & then
        assertEquals(true, source.upsertPropertyFromPath(sut, attsSUT));
        assertEquals(example.propertyValue, source.getValue("aptdir.sut-upsert-property-insert"));
    }
    
    @Test
    public void testUpsertPropertyFromPath_UpdateCase() throws IOException {
        // given
        Path sut = subpath("aptdir", "sut-upsert-property-update");
        BasicFileAttributes attsSUT = writeFile(sut, "foobar");
        DirConfigSource.DirProperty example = new DirConfigSource.DirProperty(
            "foobar", attsSUT.lastModifiedTime(), sut);
        
        assertEquals(true, source.upsertPropertyFromPath(sut, attsSUT));
        assertEquals(example.propertyValue, source.getValue("aptdir.sut-upsert-property-update"));
        
        // when & then
        BasicFileAttributes attsUpdate = writeFile(sut, "foobar2");
        assertEquals(true, source.upsertPropertyFromPath(sut, attsUpdate));
        assertEquals("foobar2", source.getValue("aptdir.sut-upsert-property-update"));
    }
    
    @Test
    public void testPropertyWatcher_RegisterAndInit() throws Exception {
        // given
        Map<Path,String> examples = new HashMap<>();
        examples.put(subpath( "init-watcher", "foo", "bar", "test", "ex"), "init-watcher.foo.bar.test.ex");
        examples.put(subpath( "init-watcher", "foo.bar.test", "hello"), "init-watcher.foo.bar.test.hello");
        examples.put(subpath( "init-watcher", ".foo", "ex"), "init-watcher.foo.ex");
        for (Map.Entry<Path, String> ex : examples.entrySet()) {
            writeFile(ex.getKey(), "foobar");
        }

        try {
            Files.createSymbolicLink(subpath( "init-watcher", "foo.hello"), subpath("init-watcher", ".foo", "ex"));
        } catch (FileSystemException fileSystemException) {
            if (OS.isWindows() && fileSystemException.getReason().contains("A required privilege is not held by the client")) {
                throw new AssumptionViolatedException("Permissions to create Symbolic Links not granted", fileSystemException);
            }
        }
        
        // when
        DirConfigSource.DirPropertyWatcher watcher = source.createWatcher(subpath("init-watcher"));
        
        // then
        assertEquals("foobar", source.getValue("init-watcher.foo.bar.test.ex"));
        assertEquals("foobar", source.getValue("init-watcher.foo.bar.test.hello"));
        assertEquals(null, source.getValue("init-watcher.foo.ex"));
        assertEquals("foobar", source.getValue("init-watcher.foo.hello"));
    }
    
    @Test
    public void testPropertyWatcher_RunFilesNewUpdate() throws Exception {
        // given
        writeFile(subpath("watcher-files", "foobar"), "test");
        writeFile(subpath("watcher-files", ".hidden", "foobar"), "hidden");

        try {
            Files.createSymbolicLink(subpath("watcher-files", "revealed"), subpath("watcher-files", ".hidden", "foobar"));
        } catch (FileSystemException fileSystemException) {
            if (OS.isWindows() && fileSystemException.getReason().contains("A required privilege is not held by the client")) {
                throw new AssumptionViolatedException("Permissions to create Symbolic Links not granted", fileSystemException);
            }
        }

        
        DirConfigSource.DirPropertyWatcher watcher = source.createWatcher(subpath("watcher-files"));
        exec.scheduleWithFixedDelay(watcher, 0, 1, TimeUnit.NANOSECONDS);
        
        assertEquals("test", source.getValue("watcher-files.foobar"));
        assertEquals("hidden", source.getValue("watcher-files.revealed"));
        
        // when
        writeFile(subpath("watcher-files", "foobar"), "test2");
        writeFile(subpath("watcher-files", "example"), "test2");
        writeFile(subpath("watcher-files", "reveal", ".hidden"), "test2");
        writeFile(subpath("watcher-files", ".hidden", "foobar"), "showme");
        Files.delete(subpath("watcher-files", "revealed"));
        Files.createSymbolicLink(subpath("watcher-files", "revealed"), subpath("watcher-files", ".hidden", "foobar"));
        Thread.sleep(100);

        // then
        await(()-> {
            assertEquals("test2", source.getValue("watcher-files.foobar"));
            assertEquals("test2", source.getValue("watcher-files.example"));
            assertEquals("showme", source.getValue("watcher-files.revealed"));
            assertEquals(null, source.getValue("watcher-files.reveal.hidden"));
        });
    }

    static void await(Runnable test) {
        await(2000, 50, test);
    }

    static void await(long timeout, int delay, Runnable test) {
        long expireAt = System.currentTimeMillis() + timeout;
        while(true) {
            try {
                Thread.sleep(delay);
                test.run();
                break;
            } catch (AssertionError assertionError) {
                if (System.currentTimeMillis() > expireAt) {
                    throw assertionError;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }
    
    @Test
    public void testPropertyWatcher_RunNewDir() throws Exception {
        // given
        writeFile(subpath("watcher-newdir", "test"), "test");
        
        DirConfigSource.DirPropertyWatcher watcher = source.createWatcher(subpath("watcher-newdir"));
        exec.scheduleWithFixedDelay(watcher, 0, 1, TimeUnit.NANOSECONDS);
        
        assertEquals("test", source.getValue("watcher-newdir.test"));
        
        // when
        writeFile(subpath("watcher-newdir", "foobar/test"), "test");
        writeFile(subpath("watcher-newdir", ".hidden/foobar"), "test");
        Thread.sleep(100);
        
        // then
        await( () -> {
            assertEquals("test", source.getValue("watcher-newdir.foobar.test"));
            assertEquals(null, source.getValue("watcher-newdir.hidden.foobar"));
        });
    }
    
    @Test
    public void testPropertyWatcher_RunRemove() throws Exception {
        // given
        writeFile(subpath("watcher-remove", "test"), "test");
        
        DirConfigSource.DirPropertyWatcher watcher = source.createWatcher(subpath("watcher-remove"));
        exec.scheduleWithFixedDelay(watcher, 0, 1, TimeUnit.NANOSECONDS);
        
        assertEquals("test", source.getValue("watcher-remove.test"));
        
        // when
        Files.delete(subpath("watcher-remove", "test"));
        Thread.sleep(100);
        
        // then
        await(() -> {
            assertEquals(null, source.getValue("watcher-remove.test"));
        });
    }
    
    public static BasicFileAttributes writeFile(Path filepath, String content) throws IOException {
        Files.createDirectories(filepath.getParent());
        Files.write(filepath, content.getBytes(StandardCharsets.UTF_8));
        return Files.readAttributes(filepath, BasicFileAttributes.class);
    }
    
    public static BasicFileAttributes writeRandFile(Path filepath, int bytes) throws IOException {
        Files.createDirectories(filepath.getParent());
        
        Random rnd = new Random();
        byte[] content = new byte[bytes];
        rnd.nextBytes(content);
        
        Files.write(filepath, content);
        return Files.readAttributes(filepath, BasicFileAttributes.class);
    }
    
    private static Path subpath(String... subpath) {
        return Paths.get(testDirectory.toString(), subpath);
    }

}
