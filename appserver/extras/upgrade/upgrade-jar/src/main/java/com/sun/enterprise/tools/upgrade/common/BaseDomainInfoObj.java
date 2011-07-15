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
 * BaseAppSrcObj.java
 *
 * Created on November 29, 2007, 4:14 PM
 */

package com.sun.enterprise.tools.upgrade.common;

import java.io.File;


/**
 *
 * @author rebeccas
 */
public abstract class BaseDomainInfoObj implements DomainInfoObj {
	final String CONFIG_DOMAIN_XML_FILE =
		UpgradeConstants.AS_CONFIG_DIRECTORY + "/" +
		UpgradeConstants.DOMAIN_XML_FILE;
		
	String installDir = null;  //- user provided path of the domain		
	String domainRoot = null;  //- parent path of the domain
	String domainName = null;  //- basename of domain
	String srvConfigFile = null; //- path to the server domain.xml file		
	String version = null; //- domain version		
	String edition = null; //- domain profile
	String versionEdition = null; //- old way of providing data as one
	
	/**
	 * Each Source and target domains may have
	 * different validation rules.
	 */
	public abstract boolean isValidPath(String s);
	
	/*
	 *  The initial source provided by the user
	 */
	public void setInstallDir(String s){ 
		installDir = s;
		if (s != null){
			domainRoot = extractDomainRoot(s);
			setDomainName(new File(s).getName());
			srvConfigFile = installDir + "/" + CONFIG_DOMAIN_XML_FILE;
		}
	}
	public String getInstallDir(){
		return installDir; 
	}

        // todo: this returns the same data as getInstallDir. should be removed
	public String getDomainDir(){
		return installDir;
	}
		
	public void setDomainName(String d){
		domainName = d;
	}
	public String getDomainName(){
		return domainName;
	}
	
	public String getDomainRoot(){
		return domainRoot;
	}
	
	public String getConfigXMLFile(){
		return srvConfigFile;
	}
		
	public String getVersion(){
		if (version == null){
			getVersionEdition();
		}
		return version;
	}
	
	public String getEdition(){
		if (edition == null){
			getVersionEdition();
		}
		return edition;
	}
	
	/**
	 * Source and target domains may have different
	 * rules for establishing product version-Edition
	 */
	public abstract String getVersionEdition();

	
	
	//=======================================
	protected String extractDomainRoot(String str){
		String s = str;
		String droot;
		//-System.out.println(s.length() + "\ts: " + s);
		int i = s.lastIndexOf("/", s.length()-2);
		switch (i){
			case -1:
				//- path ex:  zoo  /
				droot = s;
				break;
			case 0:
				//- path ex:  /z  ./z
				droot = s.substring(0, 1);
				break;
			default:
				//- path ex: /space/gf/b09a/  /space/as91ee
				droot = s.substring(0, i);
		}
		//-System.out.println("i: " + i  + "\ts: " + droot);
		return droot;
	}
	
	protected void extractVersionAndEdition(String s){
		String [] tmpS = versionEdition.split(s);
		version = tmpS[0];
		edition = tmpS[1];
	}
}
