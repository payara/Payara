/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.apache.catalina.connector;

import org.apache.catalina.LogFacade;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Coyote implementation of the servlet output stream.
 * 
 * @author Costin Manolache
 * @author Remy Maucherat
 */
public class CoyoteOutputStream 
    extends ServletOutputStream {

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();


    // ----------------------------------------------------- Instance Variables


    protected OutputBuffer ob;


    // ----------------------------------------------------------- Constructors


    // BEGIN S1AS 6175642
    /*
    protected CoyoteOutputStream(OutputBuffer ob) {
    */
    public CoyoteOutputStream(OutputBuffer ob) {
    // END S1AS 6175642
        this.ob = ob;
    }
    
    
    // --------------------------------------------------------- Public Methods


    /**
    * Prevent cloning the facade.
     * @return throws {@link CloneNotSupportedException}
     * @throws CloneNotSupportedException always
    */
    @Override
    protected Object clone()
        throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

  
    // -------------------------------------------------------- Package Methods


    /**
     * Clear facade.
     */
    void clear() {
        ob = null;
    }


    // --------------------------------------------------- OutputStream Methods

    @Override
    public void write(int i)
        throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        ob.writeByte(i);
    }

    @Override
    public void write(byte[] b)
        throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len)
        throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        ob.write(b, off, len);
    }


    /**
     * Will send the buffer to the client.
     */
    @Override
    public void flush()
        throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        ob.flush();
    }


    @Override
    public void close() throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        ob.close();
    }


    // -------------------------------------------- ServletOutputStream Methods


    @Override
    public void print(String s)
        throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        ob.write(s);
    }


    @Override
    public boolean isReady() {
        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }
        return ob.isReady();
    }


    @Override
    public void setWriteListener(WriteListener writeListener) {
        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        if (writeListener == null) {
            throw new NullPointerException(rb.getString(LogFacade.NULL_WRITE_LISTENER_EXCEPTION));
        }

        ob.setWriteListener(writeListener);
    }
}
