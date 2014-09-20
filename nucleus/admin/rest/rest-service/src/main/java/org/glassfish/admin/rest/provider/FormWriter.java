/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.provider;

import org.jvnet.hk2.config.Dom;

import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 *
 * @author mh124079
 * @author Ludovic Champenois ludo@dev.java.net
 */
@Produces("application/x-www-form-urlencoded")
@Provider
public class FormWriter implements MessageBodyWriter<Dom> {
    @Context
    protected javax.inject.Provider<UriInfo> uriInfo;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Dom.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(Dom data, Class<?> type, Type genericType, Annotation annotations[], MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Dom data,
            Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> headers,
            OutputStream out) throws IOException {
//        out.write(preamble.getBytes());
//        for (String name : data.keySet()) {
//            out.write("<tr><td>".getBytes());
//            out.write(name.getBytes());
//            out.write("</td><td>".getBytes());
//            out.write(data.get(name).getBytes());
//            out.write("</td></tr>".getBytes());
//        }
//        out.write(postamble.getBytes());
        out.write(constructForm(data).getBytes());
    }

    private String constructForm(Dom data) {
        StringBuilder sb= new StringBuilder();
        sb.append(
                "<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'>\n").
                append("<html><head><title>Data</title></head>\n").
                append("<body><p>Change ").append(data.toString()).append(":</p>\n" ).
                append("<form name='pair' action='").append(uriInfo.get().getAbsolutePath()).append("' method='POST'>\n").
                append("<table>\n");

        Set<String> ss = data.model.getAttributeNames();

            for (String name : ss) {
            sb.append(
                    "   <tr>\n").
                    append("       <td align='right'>").append(name).append(":</td>\n").
                    append("       <td><input type='text' name='").append(name).append("' value='").append(data.attribute(name)).append("' size='30' /></td>\n").
                    append("   </tr>\n");
        }

        sb.append(
                "   <tr><td></td><td><input type='submit' value='Set' name='submit' /></td></tr>\n" ).
                append("</table></form>\n</body></html>");

        return sb.toString();
    }

}
