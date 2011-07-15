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

/*
 * AppServObj.java
 *
 * Information common to both source and target domains
 * and references throughout the tool.
 *
 * Created on November 29, 2007, 4:04 PM
 *
 */

package com.sun.enterprise.tools.upgrade.common;

/**
 *
 * @author rebeccas
 */
public interface DomainInfoObj {
	/**
	 * The directory structure of appserver versions
	 * can change between products.  Each appserver
	 * domain must be able to identify if it is
	 * a valid directory reference.
	 *  
	 * @param	s path name of the domain.
	 * @return	true path of a valid domain 
	 *			false invalid domain.
	 */
	public boolean isValidPath(String s);
	
	/**
	 * The directory path provided by the user.
	 * For the source domain usually the complete
	 * path including the domain name.  For the
	 * target the path to the directory to contain
	 * the upgraded source domain.
	 *	@param	s path name of the domain.
	 */
	public void setInstallDir(String s);
	public String getInstallDir();
	
	/*
	 * The full path to the source or target 
	 * domain directory.
	 */
	public String getDomainDir();
	
	/*
	 * The "basename" of the domain directory
	 * (e.g. the File.getName() value of a pathname).
	 *
	 * @param	d basename of the domain.
	 */
	public void setDomainName(String d);
	
	/**
	 * The parent pathname of the domain path.
	 */
	public String getDomainRoot();
	
	/**
	 * The the fullpath to the appserver's 
	 * configuation file (e.g. domain.xml).
	 */
	public String getConfigXMLFile();
	
	/*
	 *  The appserver's version. 
	 */
	public String getVersion();
	
	/**
	 * The appserver's profile name.
	 */
	public String getEdition();
	
	/**
	 * An internally used product version, edition
	 * string.
	 */
	public String getVersionEdition();
	
}
