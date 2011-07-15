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

package org.glassfish.obrbuilder.xmlentities;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.URI;

/**
 * Utility class to marshall and unmarshall to and from an obr.xml.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class ObrXmlReaderWriter {
    /*
     * It uses JAXB to do the necessary XML marshalling/unmasrshalling
     */

    /**
     * Reads the XML document from the input stream and maps it to a Java object graph.
     * Closing the stream is caller's responsibility.
     *
     * @param is InputStream to read the XML content from
     * @return an inmemory representation of the XML content
     * @throws IOException
     */
    public Repository read(InputStream is) throws IOException {
        try {
            Unmarshaller unmarshaller = getUnmarshaller();
            return Repository.class.cast(unmarshaller.unmarshal(is));
        } catch (JAXBException je) {
            IOException ioe = new IOException();
            ioe.initCause(je);
            throw ioe;
        }

    }

    /**
     * @see #read(java.io.InputStream)
     */
    public Repository read(URI input) throws IOException {
        InputStream is = new BufferedInputStream(input.toURL().openStream());
        try {
            return read(is);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * @see #read(java.io.InputStream) 
     */
    public Repository read(File input) throws IOException {
        return read(input.toURI());
    }

    /**
     * Writes  a Java object graph in XML format.
     * Closing the stream is caller's responsibility.
     *
     * @param repository Repository object to be written out
     * @param os         target stream to write to
     * @throws IOException
     */
    public void write(Repository repository, OutputStream os) throws IOException {
        try {
            getMarshaller(repository.getClass()).marshal(repository, os);
        } catch (JAXBException je) {
            IOException ioe = new IOException();
            ioe.initCause(je);
            throw ioe;
        }

    }

    /**
     * @see #write(Repository, java.io.OutputStream)
     */
    public void write(Repository repository, File out) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(out));
        try {
            write(repository, os);
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private Marshaller getMarshaller(Class<?> clazz) throws JAXBException {
        JAXBContext jc = getJAXBContext();
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                Boolean.TRUE);
        return marshaller;
    }

    private Unmarshaller getUnmarshaller() throws JAXBException {
        JAXBContext jc = getJAXBContext();
        return jc.createUnmarshaller();
    }

    private JAXBContext getJAXBContext() throws JAXBException {
        return JAXBContext.newInstance(ObjectFactory.class);
    }


}
