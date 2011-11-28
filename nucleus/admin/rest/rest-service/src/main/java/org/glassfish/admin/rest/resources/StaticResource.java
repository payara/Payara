/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.resources;

import java.io.InputStream;
import javax.ws.rs.GET;
import javax.ws.rs.Path;


import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * @author Ludovic Champenois ludo@dev.java.net
 */
@Path("/static/")
public class StaticResource {

    private final String PATH_INSIDE_JAR = "org/glassfish/admin/rest/static/";
    private final String mimes[] = {
        ".bmp", "image/bmp",
        ".bz", "application/x-bzip",
        ".bz2", "application/x-bzip2",
        ".css", "text/css",
        ".gz", "application/x-gzip",
        ".gzip", "application/x-gzip",
        ".htm", "text/html",
        ".html", "text/html",
        ".htmls", "text/html",
        ".htx", "text/html",
        ".ico", "image/x-icon",
        ".jpe", "image/jpeg",
        ".jpe", "image/pjpeg",
        ".jpeg", "image/jpeg",
        ".jpg", "image/jpeg",
        ".js", "application/x-javascript",
        ".javascript", "application/x-javascript",
        ".json", "application/json",
        ".png", "image/png",
        ".text", "text/plain",
        ".tif", "image/tiff",
        ".tiff", "image/tiff",
        ".xml", "text/xml",
        ".zip", "application/zip"
    };

    @GET
    @Path("{resource: .+}")
    public Response getPath(@PathParam("resource") String resource) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(PATH_INSIDE_JAR + resource);
        Response r = null;
        String m = getMime(resource);
        ResponseBuilder rp = Response.ok(is, m);
        rp.header("resource3-header", m);
        r = rp.build();
        return r;

    }

    private String getMime(String extension) {
        for (int i = 0; i < mimes.length; i = i + 2) {
            if (extension.endsWith(mimes[i])) {
                return mimes[i + 1];
            }
        }
        return "text/plain";
    }
}
