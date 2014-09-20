/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * foo.java
 *
 * Created on November 11, 2001, 12:09 AM
 */

package com.sun.enterprise.util.io;
import java.io.*;
import java.util.*;

/**
 *
 * @author  bnevins
 * @version 
 */
public abstract class FileLister
{
	FileLister(File root)
	{
		mainRoot = root;
		fileList = new ArrayList<File>();
	}
    public void keepEmptyDirectories()
    {
        keepEmpty = true;
    }

	abstract protected boolean relativePath();
	
	public String[] getFiles()
	{
		getFilesInternal(mainRoot);
		String[] files = new String[fileList.size()];
		
		if(files.length <= 0)
			return files;

		int len = 0;
		
		if(relativePath())
			len = mainRoot.getPath().length() + 1;
		
		for(int i = 0; i < files.length; i++)
		{
			files[i] = (fileList.get(i)).getPath().substring(len).replace('\\', '/');
		}
		
		Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);
		return files;
	}
	
	
	public void getFilesInternal(File root)
	{
		File[] files = root.listFiles();
		
		for(int i = 0; i < files.length; i++)
		{
			if(files[i].isDirectory())
			{
				getFilesInternal(files[i]);
			}
			else
				fileList.add(files[i]);	// actual file
		}
        // add empty directory, if the option is turned on
        if(files.length <= 0 && keepEmpty)
            fileList.add(root);

	}
		
		
    private	ArrayList<File>	fileList = null;
    private File		mainRoot	 = null;
    private boolean		keepEmpty	 = false;
}



