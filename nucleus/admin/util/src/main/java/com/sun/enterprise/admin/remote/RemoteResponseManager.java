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

package com.sun.enterprise.admin.remote;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.logging.Logger;

import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

import static com.sun.enterprise.util.StringUtils.ok;

/**
 * This class is responsible for handling the Remote Server response.
 * Note that an unusul paradigm is used here.  Success is signaled by throwing
 * a "success" exception.  This breaks the overarching rule about Exceptions but
 * is very useful in CLI.  CLI has the pattern of:
 * Error:  Throw an Exception
 * Success: Don't throw an Exception
 * The logic becomes difficult.  The command itself has to know how to print a 
 * success message properly instead of just putting such a message inside an Exception
 * object and throwing it.  In such a system it is cleaner to do this:
 * Error: throw failure exception
 * Success: throw success exception
 * @author bnevins
 */
public class RemoteResponseManager implements ResponseManager {
    private static final LocalStringsImpl strings =
            new LocalStringsImpl(RemoteResponseManager.class);

    public RemoteResponseManager(InputStream in, int code, Logger logger)
                                throws RemoteException, IOException {
        this.code = code;
        this.logger = logger;

        // make a copy of the stream.  O/w if Manifest.read() blows up -- the
        // data would be gone!
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtils.copy(in, baos, 0);
        
        responseStream = new ByteArrayInputStream(baos.toByteArray());
        response = baos.toString();
        
        if(!ok(response))
            throw new RemoteFailureException(strings.get("emptyResponse"));
        
        logger.finer("------- RAW RESPONSE  ---------");
        logger.finer(response);
        logger.finer("------- RAW RESPONSE  ---------");
    }

    public void process() throws RemoteException {
        checkCode();  // Exception == Goodbye!
        try { 
            handleManifest();
        } 
        catch(RemoteFailureException e) {
            // Manifest obj was ok -- remote failure
            throw e;
        }
        catch(IOException e) {
            // ignore -- move on to Plain Text...
        }
        // put a try around this if another type of response is added...
        handlePlainText();
        throw new RemoteFailureException(strings.get("internal", response));
    }

    public Map<String,String> getMainAtts() {
        return mainAtts;
    }
    private void checkCode() throws RemoteFailureException {
        if(code != HTTP_SUCCESS_CODE) {
            throw new RemoteFailureException(strings.get("badHttpCode", code));
        }
    }
    
    private void handleManifest() throws RemoteException, IOException{
        ManifestManager mgr = new ManifestManager(responseStream, logger);
        mainAtts = mgr.getMainAtts();
        mgr.process();
    }

    private void handlePlainText() throws RemoteException{
        PlainTextManager mgr = new PlainTextManager(response);
        mgr.process();
    }

    private int                     code;
    private Logger                  logger;
    final InputStream               responseStream;
    final String                    response;
    private static final int        HTTP_SUCCESS_CODE = 200;
    private Manifest                m;
    private Map<String, String>     mainAtts = Collections.emptyMap();
}
