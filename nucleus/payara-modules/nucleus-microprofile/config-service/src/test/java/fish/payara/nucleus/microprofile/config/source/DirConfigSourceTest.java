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

import fish.payara.nucleus.microprofile.config.spi.ConfigProviderResolverImpl;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

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
    
    @Test
    public void testFindDir_AbsolutePath() throws IOException {
        // given
        MicroprofileConfigConfiguration config = mock(MicroprofileConfigConfiguration.class);
        when(configService.getMPConfig()).thenReturn(config);
        when(config.getSecretDir()).thenReturn(testDirectory.toString());
        // when
        Path sut = source.findDir();
        // then
        assertEquals(testDirectory, sut);
    }
    
    @Test
    public void testFindDir_RelativePath() throws IOException {
        // given
        System.setProperty("com.sun.aas.instanceRoot", testDirectory.toString());
        MicroprofileConfigConfiguration config = mock(MicroprofileConfigConfiguration.class);
        when(configService.getMPConfig()).thenReturn(config);
        when(config.getSecretDir()).thenReturn(".");
        
        // when
        Path sut = source.findDir();
        
        // then
        assertEquals(testDirectory, sut);
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
        examples.put(subpath( "aptdir/aptfile"), TRUE);
        examples.put(subpath( "aptdir/.unaptfile"), FALSE);
        
        assertEquals(FALSE, DirConfigSource.isAptFile(null, null));
        assertEquals(FALSE, DirConfigSource.isAptFile(subpath( "aptdir/aptnotexisting"), null));
        for (Map.Entry<Path, Boolean> ex : examples.entrySet()) {
            BasicFileAttributes atts = writeFile(ex.getKey(), "test");
            assertEquals(ex.getValue(), DirConfigSource.isAptFile(ex.getKey(), atts));
        }
        
        Path file100k = subpath("aptdir/100k-file");
        BasicFileAttributes atts100k = writeRandFile(file100k, 100*1024);
        assertEquals(TRUE, DirConfigSource.isAptFile(file100k, atts100k));
        
        Path file600k = subpath("aptdir/600k-file");
        BasicFileAttributes atts600k = writeRandFile(file600k, 600*1024);
        assertEquals(FALSE, DirConfigSource.isAptFile(file600k, atts600k));
    }
    
    @Test
    public void testParsePropertyNameFromPath() {
        // given
        Map<Path,String> examples = new HashMap<>();
        examples.put(subpath( "foo/bar/test/ex"), "foo.bar.test.ex");
        examples.put(subpath( "foo.bar.test/ex"), "foo.bar.test.ex");
        examples.put(subpath( "foo/bar.test/ex"), "foo.bar.test.ex");
        examples.put(subpath( "foo.bar/test/ex"), "foo.bar.test.ex");
        
        // we ignore the last file extension if not more than 3 chars.
        // this might lead to unexpected behaviour for a user.
        // best advice: do not use dots in filename, only in directory names.
        examples.put(subpath( "foo/bar/test/ex.txt"), "foo.bar.test.ex");
        examples.put(subpath( "foo/bar/test/ex.tar.gz"), "foo.bar.test.ex.tar");
        examples.put(subpath( "foo/bar/test/ex.helo"), "foo.bar.test.ex.helo");
        examples.put(subpath( "foo.bar/test.ex"), "foo.bar.test");
        examples.put(subpath( "foo/bar.test.ex"), "foo.bar.test");
        examples.put(subpath( "foo.bar.test.ex"), "foo.bar.test");
        examples.put(subpath( "foo/bar/test.ex"), "foo.bar.test");
        
        // when & then
        for (Map.Entry<Path, String> ex : examples.entrySet()) {
            //System.out.println(ex.getKey()+" = "+ex.getValue());
            assertEquals(ex.getValue(), DirConfigSource.parsePropertyNameFromPath(ex.getKey(), testDirectory));
        }
    }
    
    @Test
    public void testReadPropertyFromPath() throws IOException {
        // given
        Path sut = subpath("aptdir/sut-read-property");
        BasicFileAttributes attsSUT = writeFile(sut, "foobar");
        DirConfigSource.DirProperty example = new DirConfigSource.DirProperty(
          "foobar", attsSUT.lastModifiedTime(), sut, testDirectory
        );
        // when & then
        assertEquals(example, DirConfigSource.readPropertyFromPath(sut, attsSUT, testDirectory));
    }
    
    @Test
    public void testCheckLongestMatchForPath_SamePath() {
        // given
        Map<String,DirConfigSource.DirProperty> props = new HashMap<>();
        // a property with a most specific path
        String property = "foo.bar.test.ex";
        props.put(property,
            new DirConfigSource.DirProperty(
                "test", FileTime.from(Instant.now()),
                subpath( "foo/bar/test/ex"),
                testDirectory));
        source.setProperties(props);
        
        // when & then
        assertTrue(source.isLongestMatchForPath(property, subpath("foo/bar/test/ex")));
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
                      subpath( "foo/bar/test/ex"),
                      testDirectory));
        source.setProperties(props);
        
        // when & then
        assertFalse(source.isLongestMatchForPath(property, subpath( "foo/bar.test/ex")));
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
                subpath( "foo.bar/test/ex"),
                testDirectory));
        source.setProperties(props);
        
        // when & then
        assertTrue(source.isLongestMatchForPath(property, subpath( "foo/bar/test/ex")));
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
                subpath( "foo.bar/test.ex/one"),
                testDirectory));
        source.setProperties(props);
        
        // when & then
        assertTrue(source.isLongestMatchForPath(property, subpath( "foo.bar/test/ex.one.txt")));
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
                subpath( "foo.bar/test.ex/one"),
                testDirectory));
        source.setProperties(props);
        
        // when & then
        assertTrue(source.isLongestMatchForPath("foo.bar.test.ex.two", subpath( "foo.bar/test/ex.two.txt")));
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
                subpath( "foo/bar/test"),
                testDirectory));
        source.setProperties(props);
        assertEquals("test", source.getValue(property));
        
        // when
        source.removePropertyFromPath(subpath( "foo/bar/test"));
        // then
        assertTrue(source.getValue(property) == null);
        
    }
    
    @Test
    public void testUpsertPropertyFromPath_InsertCase() throws IOException {
        // given
        Path sut = subpath("aptdir/sut-upsert-property-insert");
        BasicFileAttributes attsSUT = writeFile(sut, "foobar");
        DirConfigSource.DirProperty example = new DirConfigSource.DirProperty(
            "foobar", attsSUT.lastModifiedTime(), sut, testDirectory
        );
        
        // when & then
        assertEquals(true, source.upsertPropertyFromPath(sut, attsSUT));
        assertEquals(example.propertyValue, source.getValue("aptdir.sut-upsert-property-insert"));
    }
    
    @Test
    public void testUpsertPropertyFromPath_UpdateCase() throws IOException {
        // given
        Path sut = subpath("aptdir/sut-upsert-property-update");
        BasicFileAttributes attsSUT = writeFile(sut, "foobar");
        DirConfigSource.DirProperty example = new DirConfigSource.DirProperty(
            "foobar", attsSUT.lastModifiedTime(), sut, testDirectory
        );
        
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
        examples.put(subpath( "init-watcher/foo/bar/test/ex"), "init-watcher.foo.bar.test.ex");
        examples.put(subpath( "init-watcher/foo.bar.test/hello"), "init-watcher.foo.bar.test.hello");
        examples.put(subpath( "init-watcher/.foo/ex"), "init-watcher.foo.ex");
        for (Map.Entry<Path, String> ex : examples.entrySet()) {
            writeFile(ex.getKey(), "foobar");
        }
        Files.createSymbolicLink(subpath( "init-watcher/foo.hello"), subpath("init-watcher/.foo/ex"));
        
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
        writeFile(subpath("watcher-files/foobar"), "test");
        writeFile(subpath("watcher-files/.hidden/foobar"), "hidden");
        Files.createSymbolicLink(subpath("watcher-files/revealed"), subpath("watcher-files/.hidden/foobar"));
        
        DirConfigSource.DirPropertyWatcher watcher = source.createWatcher(subpath("watcher-files"));
        exec.scheduleWithFixedDelay(watcher, 0, 10, TimeUnit.MILLISECONDS);
        
        assertEquals("test", source.getValue("watcher-files.foobar"));
        assertEquals("hidden", source.getValue("watcher-files.revealed"));
        
        // when
        writeFile(subpath("watcher-files/foobar"), "test2");
        writeFile(subpath("watcher-files/example"), "test2");
        writeFile(subpath("watcher-files/reveal/.hidden"), "test2");
        writeFile(subpath("watcher-files/.hidden/foobar"), "showme");
        Files.delete(subpath("watcher-files/revealed"));
        Files.createSymbolicLink(subpath("watcher-files/revealed"), subpath("watcher-files/.hidden/foobar"));
        Thread.sleep(100);
        
        // then
        assertEquals("test2", source.getValue("watcher-files.foobar"));
        assertEquals("test2", source.getValue("watcher-files.example"));
        assertEquals("showme", source.getValue("watcher-files.revealed"));
        assertEquals(null, source.getValue("watcher-files.reveal.hidden"));
    }
    
    @Test
    public void testPropertyWatcher_RunNewDir() throws Exception {
        // given
        writeFile(subpath("watcher-newdir/test"), "test");
        
        DirConfigSource.DirPropertyWatcher watcher = source.createWatcher(subpath("watcher-newdir"));
        exec.scheduleWithFixedDelay(watcher, 0, 10, TimeUnit.MILLISECONDS);
        
        assertEquals("test", source.getValue("watcher-newdir.test"));
        
        // when
        writeFile(subpath("watcher-newdir/foobar/test"), "test");
        writeFile(subpath("watcher-newdir/.hidden/foobar"), "test");
        Thread.sleep(100);
        
        // then
        assertEquals("test", source.getValue("watcher-newdir.foobar.test"));
        assertEquals(null, source.getValue("watcher-newdir.hidden.foobar"));
    }
    
    @Test
    public void testPropertyWatcher_RunRemove() throws Exception {
        // given
        writeFile(subpath("watcher-remove/test"), "test");
        
        DirConfigSource.DirPropertyWatcher watcher = source.createWatcher(subpath("watcher-remove"));
        exec.scheduleWithFixedDelay(watcher, 0, 10, TimeUnit.MILLISECONDS);
        
        assertEquals("test", source.getValue("watcher-remove.test"));
        
        // when
        Files.delete(subpath("watcher-remove/test"));
        Thread.sleep(100);
        
        // then
        assertEquals(null, source.getValue("watcher-remove.test"));
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
    
    private static Path subpath(String subpath) {
        return Paths.get(testDirectory.toString(), subpath);
    }

}
