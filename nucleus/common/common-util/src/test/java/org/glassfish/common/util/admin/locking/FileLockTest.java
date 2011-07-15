/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.common.util.admin.locking;

import junit.framework.Assert;
import org.glassfish.common.util.admin.ManagedFile;
import org.junit.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

/**
 * Tests for ManagedFile.writeLock and ManagedFile.readLock.
 * 
 */
public class FileLockTest {

    enum States {LOCKED, RELEASED}
    volatile States mainWriteState;
    volatile States[] writeStates = new States[5];
    volatile States[] readStates = new States[5];

    @Test
    public void writeLock() throws IOException {

        final Random random = new Random();
        File f = getFile();
        try {
            final ManagedFile managed = new ManagedFile(f, 1000, 1000);
            Lock fl = managed.accessWrite();
            mainWriteState=States.LOCKED;
            List<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
            final ExecutorService executor = Executors.newFixedThreadPool(10);
            for (int i=0;i<3;i++) {
                final int number = i;
                results.add(executor.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        try {
                            final Lock second = managed.accessWrite();
                            writeStates[number] = States.LOCKED;
                            assertWriteStates();
                            writeStates[number]= States.RELEASED;
                            second.unlock();
                            assertWriteStates();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return Boolean.TRUE;
                    }
                }));
                results.add(executor.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        try {
                            final Lock second = managed.accessRead();
                            readStates[number] = States.LOCKED;
                            assertWriteStates();
                            Thread.sleep(random.nextInt(300));
                            readStates[number]= States.RELEASED;
                            second.unlock();
                            assertWriteStates();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return Boolean.TRUE;
                    }
                }));
                
            }
            Thread.sleep(100);
            mainWriteState = States.RELEASED;
            fl.unlock();
            for (Future<Boolean> result : results) {
                Boolean exitCode = result.get();
                Assert.assertEquals(exitCode.booleanValue(), true);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void mixedLock() throws IOException {

        final Random random = new Random();
        File f = getFile();
        try {
            final ManagedFile managed = new ManagedFile(f, 1000, 1000);
            Lock fl = managed.accessWrite();
            mainWriteState=States.LOCKED;
            List<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
            final ExecutorService executor = Executors.newFixedThreadPool(10);
            for (int i=0;i<3;i++) {
                final int number = i;
                results.add(executor.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        try {
                            final Lock second = managed.accessRead();
                            readStates[number] = States.LOCKED;
                            assertWriteStates();
                            Thread.sleep(random.nextInt(300));
                            readStates[number]= States.RELEASED;
                            second.unlock();
                            assertWriteStates();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return Boolean.TRUE;
                    }
                }));

            }
            Thread.sleep(300);
            mainWriteState = States.RELEASED;
            fl.unlock();
            for (Future<Boolean> result : results) {
                Boolean exitCode = result.get();
                Assert.assertEquals(exitCode.booleanValue(), true);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    public void assertWriteStates() {
        int writeLocked = 0;
        int readLocked = 0;
        if (mainWriteState==States.LOCKED) writeLocked++;
        for (int i=0;i<5;i++) {
            if (writeStates[i]==States.LOCKED) writeLocked++;
            if (readStates[i]==States.LOCKED) readLocked++;
        }
        System.out.println("Status M : " + mainWriteState + " W " + writeLocked + " R " + readLocked);

        // never more than 1 locked writer
        if (writeLocked>1) {
            throw new AssertionError("More than 1 thread in write state");
        }
    }

    @Test
    public void readLock() throws IOException {

        File f = getFile();
        try {
            final ManagedFile managed = new ManagedFile(f, 1000, 1000);
            Lock fl = managed.accessRead();

            List<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
            for (int i=0;i<5;i++) {
                final int number = i;
                results.add(Executors.newFixedThreadPool(2).submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        try {
                            Lock second = managed.accessRead();
                            second.unlock();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return Boolean.TRUE;
                    }
                }));
            }
            Thread.sleep(100);
            fl.unlock();
            for (Future<Boolean> result : results) {
                Boolean exitCode = result.get();
                Assert.assertEquals(exitCode.booleanValue(), true);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void timeOutTest() throws Exception {
        final File f = getFile();
        final ManagedFile managed = new ManagedFile(f, 1000, 10000);
        Lock fl;
        try {
            fl = managed.accessWrite();
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Obtained first lock, waiting about 500 for secondary lock to timeout...");
        try {
        Executors.newFixedThreadPool(2).submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                long now = System.currentTimeMillis();
                ManagedFile m = new ManagedFile(f, 500, 1000);
                try {
                    Lock lock = m.accessRead();
                    lock.unlock();
                    throw new RuntimeException("Test failed, got the lock that should have timed out");
                } catch(TimeoutException e) {
                    System.out.println("Great, got timed out after " + (System.currentTimeMillis() - now));
                }
                return null;
            }
        }).get();
        // let's check we also cannot get the write lock...
        ManagedFile m = new ManagedFile(f, 100, 100);
        try {
            Lock lock = m.accessWrite();
            lock.unlock();
            throw new RuntimeException("Test failed, got the write lock that should have timed out");
        } catch(TimeoutException e) {
            System.out.println("Even better, got timed out trying to get another write lock");
        }
        fl.unlock();
        } catch(Exception e) {
            e.printStackTrace();
            fl.unlock();
        }
    }

    @Test
     public void lockAndReadTest() throws IOException {
         File f = File.createTempFile("common-util-FileLockTest", "tmp");
         try {

             // Now let's try to write the file.
             FileWriter fw = new FileWriter(f);
             fw.append("FileLockTest reading passed !");
             fw.close();
             
             final ManagedFile managed = new ManagedFile(f, 1000, 1000);
             Lock fl = managed.accessRead();
             FileReader fr = new FileReader(f);
             char[] chars = new char[1024];
             int length = fr.read(chars);
             fr.close();

             fl.unlock();

             // Let's read it back
             System.out.println(new String(chars,0, length));

         } catch(Exception e) {
             e.printStackTrace();
         } finally {
             f.delete();
         }
     }

     @Test
     public void lockForReadAndWriteTest() throws IOException {
         // on unixes, there is no point on testing locking access through
         // normal java.io APIs since several outputstream can be created and
         // the last one to close always win.
         if (!System.getProperty("os.name").toUpperCase().contains("WINDOWS")) {
             return;
         }
         File f = File.createTempFile("common-util-FileLockTest", "tmp");
         try {

             // Now let's try to write the file.
             FileWriter fw = new FileWriter(f);
             fw.append("lockForReadAndWriteTest passed !");
             fw.flush();
             fw.close();

             try {
                 System.out.println("file length " + f.length());
                 FileReader fr = new FileReader(f);
                 char[] chars = new char[1024];
                 int length=fr.read(chars);
                 fr.close();
                 System.out.println(new String(chars, 0, length));
                 
             } catch(IOException unexpected) {
                 System.out.println("Failed, got an exception reading : " + unexpected.getMessage());
                 throw unexpected;
             }

             final ManagedFile managed = new ManagedFile(f, 1000, 1000);
             Lock fl = managed.accessRead();

             FileWriter fwr=null;
             try {
                fwr = new FileWriter(f);
                fwr.append("lockForReadAndWriteTest failed !");
                fwr.close();
             } catch(IOException expected) {
                 System.out.println("Got an expected exception : " + expected.getMessage());
             }

             fl.unlock();

             
             if (f.length()==0) {
                 System.out.println("The write lock was an advisory lock, file content was deleted !");
                 return;
             }
             FileReader fr = new FileReader(f);
             char[] chars = new char[1024];
             int length=fr.read(chars);
             fr.close();


             // Let's read it back
             if (length>0) {
                System.out.println(new String(chars,0,length));
             } else {
                 System.out.println("lockForReadAndWriteTest failed, file content is empty");
             }
             //Assert.assertTrue(new String(chars).contains("passed"));

         } catch(Exception e) {
             e.printStackTrace();
         } finally {
             f.delete();
         }
     }

    @Test
    public void lockAndWriteTest() throws IOException {
        File f = File.createTempFile("common-util-FileLockTest", "tmp");
        try {
            final ManagedFile managed = new ManagedFile(f, 1000, 1000);
            ManagedFile.ManagedLock fl = managed.accessWrite();

            // Now let's try to write the file.
            RandomAccessFile raf = fl.getLockedFile();
            raf.writeUTF("lockAndWriteTest Passed !");

            fl.unlock();

            // Let's read it back
            FileReader fr = new FileReader(f);
            char[] chars = new char[1024];
            int length = fr.read(chars);
            fr.close();
            f.delete();
            System.out.println(new String(chars,0,length));
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    //@Test
    public void lockAndRenameTest() throws IOException {
        File f = File.createTempFile("common-util-FileLockTest", "tmp");
        try {
            final ManagedFile managed = new ManagedFile(f, 1000, 1000);
            Lock fl = managed.accessWrite();

            File dest = new File("filelock");

            if (f.renameTo(new File("filelock"))) {
                System.out.println("File renaming successful");
            } else {
                System.out.println("File renaming failed");
            }


            if (dest.exists()) {
                System.out.println("File is there...");    
            }
            dest.delete();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public File getFile() throws IOException {
        Enumeration<URL> urls = getClass().getClassLoader().getResources("adminport.xml");
        if (urls.hasMoreElements()) {
            try {
                File f = new File(urls.nextElement().toURI());
                if (f.exists()) {
                    return f;
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("No DomainTest.xml found !");
        }
        return null;
    }
}
