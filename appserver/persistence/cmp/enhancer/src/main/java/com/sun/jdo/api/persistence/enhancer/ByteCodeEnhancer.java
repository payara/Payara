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

package com.sun.jdo.api.persistence.enhancer;

import java.io.InputStream;
import java.io.OutputStream;

import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


/**
 * A JDO enhancer, or byte-code enhancer, modifies the byte-codes of
 * Java class files to enable transparent loading and storing of the
 * fields of the persistent instances.
 */
public interface ByteCodeEnhancer
{
    /**
     * Enhances a given class according to the JDO meta-data. If the
     * input class has been enhanced or not - the output stream is
     * always written, either with the enhanced class or with the
     * non-enhanced class.
     *
     * @param inByteCode  The byte-code of the class to be enhanced.
     * @param outByteCode The byte-code of the enhanced class.
     *
     * @return  <code>true</code> if the class has been enhanced,
     *          <code>false</code> otherwise.
     */
   boolean enhanceClassFile(InputStream inByteCode,
                            OutputStream outByteCode)
        throws EnhancerUserException, EnhancerFatalError;


    /**
     * Enhances a given class according to the JDO meta-data. If the
     * input class has been enhanced or not - the output stream is
     * always written, either with the enhanced class or with the
     * non-enhanced class.
     * <br>
     * Furthermore the enhancer has to set the classname of
     * the enhanced class to the output stream wrapper object (it's
     * possible to get the input stream without knowing the classname).
     *
     * @param in  The byte-code of the class to be enhanced.
     * @param out The byte-code of the enhanced class.
     *
     * @return  <code>true</code> if the class has been enhanced,
     *          <code>false</code> otherwise.
     */
    boolean enhanceClassFile (InputStream         in,
                              OutputStreamWrapper out)
            throws EnhancerUserException,
                   EnhancerFatalError;

}
