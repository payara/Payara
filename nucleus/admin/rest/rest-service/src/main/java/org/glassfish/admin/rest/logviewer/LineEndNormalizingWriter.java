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

package org.glassfish.admin.rest.logviewer;

import java.io.FilterWriter;
import java.io.Writer;
import java.io.IOException;

/**
 * Finds the lone LF and converts that to CR+LF.
 *
 * <p>
 * Internet Explorer's <tt>XmlHttpRequest.responseText</tt> seems to
 * normalize the line end, and if we only send LF without CR, it will
 * not recognize that as a new line. To work around this problem,
 * we use this filter to always convert LF to CR+LF.
 *
 * @author Kohsuke Kawaguchi
 */
public /*for now, until Hudson migration completes*/ class LineEndNormalizingWriter extends FilterWriter {

    private boolean seenCR;

    public LineEndNormalizingWriter(Writer out) {
        super(out);
    }

    @Override
    public void write(char cbuf[]) throws IOException {
        write(cbuf, 0, cbuf.length);
    }

    @Override
    public void write(String str) throws IOException {
        write(str,0,str.length());
    }

    @Override
    public void write(int c) throws IOException {
        if(!seenCR && c==LF)
            super.write("\r\n");
        else
            super.write(c);
        seenCR = (c==CR);
    }

    @Override
    public void write(char cbuf[], int off, int len) throws IOException {
        int end = off+len;
        int writeBegin = off;

        for( int i=off; i<end; i++ ) {
            char ch = cbuf[i];
            if(!seenCR && ch==LF) {
                // write up to the char before LF
                super.write(cbuf,writeBegin,i-writeBegin);
                super.write("\r\n");
                writeBegin=i+1;
            }
            seenCR = (ch==CR);
        }

        super.write(cbuf,writeBegin,end-writeBegin);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        int end = off+len;
        int writeBegin = off;

        for( int i=off; i<end; i++ ) {
            char ch = str.charAt(i);
            if(!seenCR && ch==LF) {
                // write up to the char before LF
                super.write(str,writeBegin,i-writeBegin);
                super.write("\r\n");
                writeBegin=i+1;
            }
            seenCR = (ch==CR);
        }

        super.write(str,writeBegin,end-writeBegin);
    }

    private static final int CR = 0x0D;
    private static final int LF = 0x0A;
}
