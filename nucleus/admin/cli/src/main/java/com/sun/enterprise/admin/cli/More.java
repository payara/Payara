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

package com.sun.enterprise.admin.cli;

import java.io.Writer;
import java.io.Reader;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;
import java.io.BufferedReader;


/**
 * More.java
 *
 * @author <a href="mailto:toby.h.ferguson@sun.com">Toby H Ferguson</a>
 *
 * This class provides a primitive more(1) functionality, paging
 * through the given file, with an optional pagelength.
 */

public class More {
    private BufferedReader in;
    private BufferedWriter out;
    private String quit;
    private String prompt;

    public More(int linesPerPage, Reader src, Writer dest,
            Reader fromUser, Writer toUser, String quitPrefix, String prompt)
            throws IOException {
        in = new BufferedReader(fromUser);
        out = new BufferedWriter(toUser);
        quit = quitPrefix;
        this.prompt = prompt;

        Pager pager = new Pager(linesPerPage, src, dest);
	do {
            pager.nextPage();
	} while (pager.hasNext() && wantsToContinue());
    }

    /**
     * Return false iff the line read from the user starts with the
     * quit character. This is a blocking call, waiting on input
     * from the user
     */
    boolean wantsToContinue() throws IOException {
        out.write(prompt);
        out.newLine();
        out.flush();
        String line = in.readLine();
        return line != null && !line.startsWith(quit);
    }
}
