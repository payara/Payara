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

import java.io.Writer;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

/**
 * {@link Writer} that spools the output and writes to another {@link Writer} later.
 *
 * @author Kohsuke Kawaguchi
 */
public /*for now, until Hudson migration completes*/ final class CharSpool extends Writer {
    private List<char[]> buf;

    private char[] last = new char[1024];
    private int pos;

    @Override
    public void write(char cbuf[], int off, int len) {
        while(len>0) {
            int sz = Math.min(last.length-pos,len);
            System.arraycopy(cbuf,off,last,pos,sz);
            len -= sz;
            off += sz;
            pos += sz;
            renew();
        }
    }

    private void renew() {
        if(pos<last.length)
            return;

        if(buf==null)
            buf = new LinkedList<char[]>();
        buf.add(last);
        last = new char[1024];
        pos = 0;
    }

    @Override
    public void write(int c) {
        renew();
        last[pos++] = (char)c;
    }

    @Override
    public void flush() {
        // noop
    }

    @Override
    public void close() {
        // noop
    }

    public void writeTo(Writer w) throws IOException {
        if(buf!=null) {
            for (char[] cb : buf) {
                w.write(cb);
            }
        }
        w.write(last,0,pos);
    }
}
