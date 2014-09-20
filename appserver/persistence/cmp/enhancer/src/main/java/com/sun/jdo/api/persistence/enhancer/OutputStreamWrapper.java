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

//OutputStreamWrapper - Java Source


//***************** package ***********************************************

package com.sun.jdo.api.persistence.enhancer;


//***************** import ************************************************

import java.io.OutputStream;


//#########################################################################
/**
 *  This class serves as a wrapper for an output stream of a class file. The
 *  stream is passed as a parameter to the byte code enhancer, that can
 *  sets the classname of the written Java class to the wrapper.
 *  <br>
 *  This wrapper is necessary to determine the classname outside the enhancer,
 *  after the class has been enhanced, since do do not always know the
 *  classname of an opened input stream.
 *  </p>
 */
//#########################################################################

public class OutputStreamWrapper
{


    /**
     *  The wrapped output stream.
     */
    private OutputStream out;


    /**
     *  The classname of the written Java class. This parameter
     *  is set by the enhancer.
     */
    private String className = null;


    /**********************************************************************
     *  Constructs a new object.
     *
     *  @param  out  The output stream to wrap.
     *********************************************************************/

    public OutputStreamWrapper (OutputStream out)
    {

        this.out = out;

    }  //OutputStreamWrapper.<init>


    /**********************************************************************
     *  Gets the wrapped output stream.
     *
     *  @return  The wrapped output stream.
     *
     *  @see #out
     *********************************************************************/

    public final OutputStream getStream ()
    {

        return this.out;

    }  //NamedOuptutStream.getStream()


    /**********************************************************************
     *  Gets the classname of the written Java class. This method should be
     *  called after the class has been enhanced.
     *
     *  @return  The name of the written Java class.
     *
     *  @see  #className
     *********************************************************************/

    public final String getClassName ()
    {

        return this.className;

    }  //OutputStreamWrapper.getClassName()


    /**********************************************************************
     *  Sets the name of the written Java class. This method should be called
     *  by the enhancer.
     *
     *  @param  classname  The name of the Java class.
     *
     *  @see  #className
     *********************************************************************/

    public final void setClassName (String classname)
    {

        this.className = classname;

    }  //OutputStreamWrapper.setClassName()


}  //OutputStreamWrapper


//OutputStreamWrapper - Java Source End
