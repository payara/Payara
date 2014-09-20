/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.v3.services.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.http.server.util.AlternateDocBase;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;

import static org.glassfish.grizzly.http.server.StaticHttpHandlerBase.sendFile;

/**
 * {@link StaticHttpHandler}, which additionally can check registered
 * {@link AlternateDocBase}s to serve requested resources.
 * 
 * @author Alexey Stashok
 */
public class ADBAwareHttpHandler extends StaticHttpHandler {
    private static final Logger LOGGER = Grizzly.logger(ADBAwareHttpHandler.class);
    
    private final List<AlternateDocBase> alternateDocBases =
            new ArrayList<AlternateDocBase>();

    public ADBAwareHttpHandler() {
        // make sure the default "." docRoot won't be added
        super((Set<String>) null);
    }
    
    /**
     * Add {@link AlternateDocBase} to be checked for requested resources.
     * 
     * @param urlPattern
     * @param docBase absolute path
     */
    public void addAlternateDocBase(final String urlPattern,
            final String docBase) {
        
        if (urlPattern == null) {
            throw new IllegalArgumentException("The urlPattern argument can't be null");
        } else if (docBase == null) {
            throw new IllegalArgumentException("The docBase argument can't be null");
        }

        AlternateDocBase alternateDocBase = new AlternateDocBase();
        alternateDocBase.setUrlPattern(urlPattern);
        alternateDocBase.setDocBase(docBase);
        alternateDocBase.setBasePath(getBasePath(docBase));
        
        alternateDocBases.add(alternateDocBase);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean handle(final String uri,
            final Request request, final Response response) throws Exception {
        final File file = lookupInADB(uri);
        if (file != null) {
            serveFile(file, request, response);
            return true;
        }
        
        return super.handle(uri, request, response);
    }

    /**
     * Get base path.
     */
    private String getBasePath(final String docBase) {
        return new File(docBase).getAbsolutePath();
    }

    private void serveFile(final File file, final Request request,
            final Response response) throws IOException {
        // If it's not HTTP GET - return method is not supported status
        if (!Method.GET.equals(request.getMethod())) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "File found {0}, but HTTP method {1} is not allowed",
                        new Object[] {file, request.getMethod()});
            }
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
            response.setHeader(Header.Allow, "GET");
            return;
        }

        pickupContentType(response, file.getPath());
        
        addToFileCache(request, response, file);
        sendFile(response, file);
    }
    
    private File lookupInADB(final String uri) {
        final AlternateDocBase adb = AlternateDocBase.findMatch(
                uri, alternateDocBases);
        if (adb != null) {
            File file = new File(adb.getBasePath(), uri);
            boolean exists = file.exists();
            boolean isDirectory = file.isDirectory();

            if (exists && isDirectory) {
                file = new File(file, "/index.html");
                exists = file.exists();
                isDirectory = file.isDirectory();
            }

            if (exists && !isDirectory) {
                return file;
            }
        }
        
        return null;
    }
}