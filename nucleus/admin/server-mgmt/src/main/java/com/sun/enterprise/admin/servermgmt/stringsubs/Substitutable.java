/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.stringsubs;

import java.io.Reader;
import java.io.Writer;

/**
 * Defines the creation of {@link Reader} & {@link Writer} for a file
 * on which string substitution has to be performed.
 */
public interface Substitutable {
    /**
     * Gets the processing entity name on which string substitution
     * operation is carrying on.
     *
     * @return Name of the entity. 
     */
    String getName();

    /**
     * Gets the character stream from the input.
     * <p> Implementation note: It is a good idea for the input
     * stream to be buffered.</p>
     *
     * @return  A Reader.
     */
    Reader getReader();

    /**
     * Gets the {@link Writer} object to write the character stream in to 
     * the output.
     * <p> Implementation note: It is a good idea for the output
     * stream to be buffered.</p>
     *
     * @return  A Writer.
     */
    Writer getWriter();

    /**
     * Called at the completion of the substitution process to perform
     * post operation. For e.g closing of reader/writer, cleaning of the
     * temporary data... etc.
     */
    void finish();
}
