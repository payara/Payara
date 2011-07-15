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

package com.sun.enterprise.util.zip;

import java.io.File;

/** 
 * This class encapsulates the two pieces of information required to make a
 * ZipEntry -- the "real" path, and the path you want to appear in the zip file
 */
public class ZipItem 
{
	/** 
     * Construct a ZipItem
     *
	 * @param file The actual file
	 * @param name The zip entry name - i.e. the relative path in the zip file
	 * @throws ZipFileException
	 */	
	public ZipItem(File file, String name) throws ZipFileException
	{
		//if(!file.exists())
		//	throw new ZipFileException("File doesn't exist: " + file);
		if(name == null || name.length() <= 0)
			throw new ZipFileException("null or empty name for ZipItem");
		
		this.file = file;
		this.name = name;
	}

	/** 
     * Returns a String represenation of the real filename and the zip entry
     * name.
     *
	 * @return String with the path and the zip entry name
	 */	
	public String toString()
	{
		return "File: " + file.getPath() + ", name: " + name;
	}

    /**
     * Returns the zip entry name 
     * 
     * @return   the zip entry name
     */
    public String getName() 
    {
        return this.name;
    }

    /**
     * Returns the actual file
     *
     * @return  the actual file
     */
    public File getFile() 
    {
        return this.file;
    }
	
	///////////////////////////////////////////////////////////////////////////

	File	file;
	String	name;
}
