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

import org.glassfish.common.util.admin.ManagedFile;
import org.junit.Ignore;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

/**
 * Standalone test to hold a read or write lock.
 *
 * usage java cp ... TestClient [ read | write ]
 *
 * By default a read lock is obtained.
 *
 */
@Ignore
public class TestClient {

    public static void main(String[] args) {
        FileLockTest test = new FileLockTest();
        byte bytes[] = new byte[100];
        String mode = "read";
        if (args.length>0) {
            mode = args[0];
        }
        try {
            File f = test.getFile();
            ManagedFile managed = new ManagedFile(f, -1, -1);
            Lock lock = null;
            try {
                if (mode.equals("read")) {
                    lock = managed.accessRead();
                } else
                if (mode.equals("write")) {
                    lock = managed.accessWrite();
                } else {
                    //System.out.println("usage : TestClient [ read | write ]. Invalid option : " + mode);
                    return;
                }

            } catch (TimeoutException e) {
                e.printStackTrace();
                return;
            }
            //System.out.println("I have the lock in "+ mode +" mode, press enter to release ");
            System.in.read(bytes);
            lock.unlock();
            //System.out.println("released");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return;
        }

    }
}
