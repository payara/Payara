/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/base/UploadDownloadMgrTest.java,v 1.5 2007/05/05 05:23:53 tcfujii Exp $
* $Revision: 1.5 $
* $Date: 2007/05/05 05:23:53 $
*/
package org.glassfish.admin.amxtest.base;

import com.sun.appserv.management.base.UploadDownloadMgr;
import com.sun.appserv.management.util.misc.ExceptionUtil;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;
import org.glassfish.admin.amxtest.PropertyKeys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 Tests {@link UploadDownloadMgr}.
 <p/>
 NOTE: multiple uploads and downloads are designed to test the thread-safeness
 of the MBean.
 */
public final class UploadDownloadMgrTest
        extends AMXTestBase {
    public UploadDownloadMgrTest()
            throws IOException {
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(true);
    }

    public UploadDownloadMgr
    getUploadDownloadMgr() {
        return (getDomainRoot().getUploadDownloadMgr());
    }


    public Object
    upload(
            final String name,
            final int totalSize)
            throws IOException {
        final UploadDownloadMgr mgr = getUploadDownloadMgr();
        //mgr.setTrace( true );

        final int chunkSize = 32 * 1024;

        final long start = now();

        final Object uploadID = mgr.initiateUpload(name, totalSize);
        int remaining = totalSize;
        boolean done = false;
        while (remaining != 0) {
            final int actual = remaining < chunkSize ? remaining : chunkSize;

            final byte[] bytes = new byte[actual];
            done = mgr.uploadBytes(uploadID, bytes);
            remaining -= actual;
            //trace( "uploaded: " + (totalSize - remaining) );
        }
        assert (done);

        printElapsed("UploadDownloadMgr.upload: " + totalSize + " bytes", start);
        return (uploadID);
    }


    private File
    createTempFile(final long totalSize)
            throws IOException {
        final long start = now();

        final File temp = File.createTempFile("UploadDownloadMgrTest", "junk");

        temp.deleteOnExit();

        final FileOutputStream os = new FileOutputStream(temp);

        try {
            long remaining = totalSize;

            final byte[] junk = new byte[1024 * 1024];

            while (remaining != 0) {
                final long actual = remaining < junk.length ? remaining : junk.length;

                os.write(junk, 0, (int) actual);
                remaining -= actual;
            }
            os.close();
        }
        catch (IOException e) {
            os.close();
            temp.delete();
            throw e;
        }

        assert (temp.length() == totalSize);

        printElapsed("UploadDownloadMgr.createTempFile: " +
                totalSize + " bytes", start);
        return (temp);
    }

    public File
    testDownloadFile(
            final int testSize,
            final int chunkSize)
            throws IOException {
        final UploadDownloadMgr mgr =
                getDomainRoot().getUploadDownloadMgr();

        final File testFile = createTempFile(testSize);

        final long start = now();
        final Object id = mgr.initiateDownload(testFile, true);

        //trace( "initated download for: " + id + " file = " + testFile.toString() );
        final int maxChunkSize = mgr.getMaxDownloadChunkSize();
        final int actualChunkSize = chunkSize < maxChunkSize ?
                chunkSize : maxChunkSize;

        final long length = mgr.getDownloadLength(id);
        long doneSoFar = 0;
        while (doneSoFar < length) {
            final byte[] bytes = mgr.downloadBytes(id, actualChunkSize);
            doneSoFar += bytes.length;
        }

        printElapsed("UploadDownloadMgr.testDownloadFile: " +
                testSize + " bytes" + " chunksize = " + actualChunkSize, start);
        return (testFile);
    }

    private final int K = 1024;
    private final int MEGABYTE = K * K;

    public void
    testDownloadFileBufferSameSizeSmallerThanDownload()
            throws IOException {
        final int size = 256 * K;

        testDownloadFile(size, size - 1);
    }

    public void
    testDownloadFileBufferSameSizeAsDownload()
            throws IOException {
        final int size = 256 * K;
        testDownloadFile(size, size);
    }

    public void
    testDownloadFileBufferLargerThanDownload()
            throws IOException {
        final int size = 256 * K;
        testDownloadFile(size, size + 1);
    }

    public void
    testDownloadSmallFile()
            throws IOException {
        final int size = 50 * K;
        testDownloadFile(size, size + 1);
        testDownloadFile(size, size);
    }


    public void
    testDownloadTinyFile()
            throws IOException {
        final int size = 1;
        testDownloadFile(size, size + 1);
        testDownloadFile(size, size);
    }

    public void
    testDownloadBigFile()
            throws IOException {
        final long start = now();
        Integer def = new Integer(PropertyKeys.DEFAULT_UPLOAD_DOWNLOAD_MGR_TEST_BIG_FILE_KB);
        final int kb =
                getEnvInteger(PropertyKeys.UPLOAD_DOWNLOAD_MGR_TEST_BIG_FILE_KB, def).intValue();
        assert (kb >= 1) :
                "Test size must be positive, value for " +
                        PropertyKeys.UPLOAD_DOWNLOAD_MGR_TEST_BIG_FILE_KB +
                        ": " + kb;

        testDownloadFile(kb * K, MEGABYTE);

        printElapsed("UploadDownloadMgrTest.testDownloadBigFile: " + kb + "kb", start);
    }


    private final class UploadDownloadTestThread
            extends Thread {
        Throwable mThrowable;
        boolean mDone;
        final int mLength;
        long mElapsed;

        public UploadDownloadTestThread(final int length) {
            mThrowable = null;
            mDone = false;
            mLength = length;
            mElapsed = 0;
        }

        public void
        run() {
            mDone = false;
            try {
                final long start = System.currentTimeMillis();

                final File f = testDownloadFile(mLength, 1 * K);
                upload(f.toString(), mLength);

                mElapsed = System.currentTimeMillis() - start;
            }
            catch (Throwable t) {
                mThrowable = t;
            }
            mDone = true;
        }

        long
        getLength() {
            return mLength;
        }

        long
        getElapsed() {
            return mElapsed;
        }

        Throwable
        getThrowable() {
            return (mThrowable);
        }

        public boolean
        done() {
            return (mDone);
        }
    }

    /**
     This test is an attempt to find any synchronization bugs.
     */
    public void
    testHeavilyThreaded()
            throws IOException {
        Integer def = new Integer(PropertyKeys.DEFAULT_UPLOAD_DOWNLOAD_MGR_TEST_THREADS);

        int numThreads = getEnvInteger(PropertyKeys.UPLOAD_DOWNLOAD_MGR_TEST_THREADS, def).intValue();
        if (numThreads <= 0) {
            numThreads = 1;
        }

        printVerbose("UploadDownloadMgrTest.testHeavilyThreaded: using " + numThreads + " threads.");

        final UploadDownloadTestThread[] threads =
                new UploadDownloadTestThread[numThreads];

        // create and start all the threads
        for (int i = 0; i < numThreads; ++i) {
            threads[i] = new UploadDownloadTestThread(i * K + 1);
            threads[i].start();
        }

        // wait till done
        boolean done = false;
        while (true) {
            int numDone = 0;
            for (int i = 0; i < numThreads; ++i) {
                if (threads[i].done()) {
                    ++numDone;
                }
            }

            if (numDone == numThreads) {
                break;
            }

            printVerbose("UploadDownloadMgrTest.testHeavilyThreaded: waiting for " +
                    (numThreads - numDone) + " of " + numThreads + " threads ");
            mySleep(1000);
        }

        // verify success
        for (int i = 0; i < numThreads; ++i) {
            assert (threads[i].done());
            assert (threads[i].getThrowable() == null) :
                    ExceptionUtil.getStackTrace(threads[i].getThrowable());
        }

    }

    public void
    testUploadFile1()
            throws IOException {
        final Object id = upload("./deploy.temp1." + now(), 1024 * K);
    }

    public void
    testUploadFile2()
            throws IOException {
        final Object id = upload("./deploy.temp2." + now(), 1 + 100 * K);
    }

    public void
    testUploadFile3()
            throws IOException {
        final Object id = upload("./deploy.temp3." + now(), 1);
    }

    public void
    testUploadFile4()
            throws IOException {
        final Object id = upload("./deploy.temp4." + now(), K + 1);
    }

    public void
    testUploadFile5()
            throws IOException {
        final Object id = upload(null, 1 + 2048 * K);
	}
}


