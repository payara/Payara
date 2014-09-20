/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.pe;

import com.sun.enterprise.util.i18n.StringManager;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;

final class ProfileTransformer {
    
    private final File baseXml;
    private final List<File> styleSheets;
    private final File destDir;
    private final EntityResolver er;
    private final Properties op;
    private static final StringManager sm = StringManager.getManager(ProfileTransformer.class);
    
    ProfileTransformer(final File baseXml, final List<File> styleSheets, final File destDir,
        final EntityResolver er, final Properties op) {
        if (baseXml == null || styleSheets == null || destDir == null) {
            throw new IllegalArgumentException("null arguments");
        }
        this.baseXml     = baseXml;
        this.styleSheets = Collections.unmodifiableList(styleSheets);
        this.destDir     = destDir;
        this.er          = er;
        this.op          = new Properties(op);
    }
    
    File transform() throws ProfileTransformationException {
        if (styleSheets.isEmpty()) {
            return ( baseXml );
        }
        BufferedOutputStream bos = null;
        try {
            final String fn = baseXml.getName();
            final DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            // it is OK to keep this as a non-validating parser.
            final DocumentBuilder builder   = bf.newDocumentBuilder();
            builder.setEntityResolver(er);
            final TransformerFactory tf     = TransformerFactory.newInstance();
            Document doc  = null;
            DOMSource src;
            int cnt       = 0;
            File   rfn    = null;
            for (final File ss : styleSheets) {
                //System.out.println("ss = " + ss.getAbsolutePath());
                if (cnt == 0)
                    doc = builder.parse(baseXml);
                src             = new DOMSource(doc);
                rfn             = new File(destDir, fn + "transformed" + cnt);
                bos             = new BufferedOutputStream(new FileOutputStream(rfn));
                final StreamResult result = new StreamResult(bos);
                final StreamSource sss    = new StreamSource(ss);
                final Transformer xf      = tf.newTransformer(sss);
                xf.setURIResolver(new TemplateUriResolver());
                xf.setOutputProperties(op);
                xf.transform(src, result);
                doc = builder.parse(rfn);
                cnt++;
                final String msg = sm.getString("xformPhaseComplete", ss.getAbsolutePath(), rfn.getAbsolutePath());
                System.out.println(msg);
            }
            return ( rfn );
        } catch (final Exception e) {
            throw new ProfileTransformationException(e);
        } finally {
            try {
                if (bos != null) bos.close();
            } catch (IOException eee) {
                //Have to squelch
            }
        }
    }
    
    private static class TemplateUriResolver implements URIResolver {
        
        @Override
        public Source resolve (final String href, final String base) throws TransformerException {
            try {
                StreamSource source     = null;
                final URI baseUri       = new URI(base);
                final URI tbResolved    = baseUri.resolve(href);
                final boolean isFileUri = tbResolved.toString().toLowerCase(Locale.ENGLISH).startsWith("file:");
                if (isFileUri) {
                    final File f = new File(tbResolved);
                    if (f.exists()) {
                        //System.out.println("File Exists: " + f.toURI());
                        source = new StreamSource (f);
                    }
                } // in all other cases, let the processor take care of it
                return ( source ) ;
            } catch(final Exception e) {
                throw new TransformerException(e);
            }
        }
    }
}
