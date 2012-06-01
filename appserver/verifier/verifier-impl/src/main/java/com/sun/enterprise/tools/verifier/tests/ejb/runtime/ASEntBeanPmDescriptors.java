/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.tests.ejb.runtime;

import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

/** enterprise-beans
 *    pm-descriptors ?
 *        pm-descriptor
 *            pm-identifier [String]
 *            pm-version [String]
 *            pm-config ? [String]
 *            pm-class-generator ? 2.0 / pm-class-generator 2.1 [String]
 *            pm-mapping-factory ? [String]
 *        pm-inuse
 *            pm-identifier [String]
 *            pm-version [String]
 *
 * The pm-descriptors element contains one or more pm-descriptor elements
 * The pm-descriptor describes the properties for the persistence
 * manager associated with the entity bean.
 * The pm-identifier and pm-version fields are required and should not be null.
 * the pm-config should be a valid ias-cmp-mapping descriptor
 *
 * The pm-inuse identifies the persistence manager in use at a particular time.
 * The pm-identifier and pm-version should be from the pm-descriptor
 * element.
 * @author Irfan Ahmed
 */
public class ASEntBeanPmDescriptors extends EjbTest implements EjbCheck {
    private boolean oneFailed=false;//4698035
    private boolean oneWarning=false;
    ComponentNameConstructor compName = null;

    
    public Result check(EjbDescriptor descriptor) {
	Result result = getInitializedResult();
	compName = getVerifierContext().getComponentNameConstructor();
//        String value=null;
        int count = 0;
        try{
            count = getCountNodeSet("sun-ejb-jar/enterprise-beans/pm-descriptors");
            if (count>0){
                Map<String, String> pmIdVer = new HashMap<String, String>();
                count = getCountNodeSet("sun-ejb-jar/enterprise-beans/pm-descriptors/pm-descriptor");
                if (count>0){
                    for(int i=0;i<count;i++){
                        testPmDescriptor(i, result, pmIdVer, descriptor);
                    }
                    count = getCountNodeSet("sun-ejb-jar/enterprise-beans/pm-descriptors/pm-inuse");
                    if (count >0){
                        String pmIdentifier = getXPathValue("sun-ejb-jar/enterprise-beans/pm-descriptors/pm-inuse/pm-identifier");
                        String pmVersion = getXPathValue("sun-ejb-jar/enterprise-beans/pm-descriptors/pm-inuse/pm-version");
                        if (pmIdentifier!=null){
                            if (pmVersion!=null){
                                if(pmIdVer.containsKey(pmIdentifier)){
                                    result.addGoodDetails(smh.getLocalString
                                        ("tests.componentNameConstructor",
                                        "For [ {0} ]",
                                        new Object[] {compName.toString()}));
                                    result.passed(smh.getLocalString(getClass().getName()+".passed",
                                        "PASSED [AS-EJB pm-inuse] : pm-identifier {0} is valid"
                                        ,new Object[]{pmIdentifier}));
                                    String testVersion = (String)pmIdVer.get(pmIdentifier);
                                    if(testVersion.equals(pmVersion))
                                    {
                                        result.addGoodDetails(smh.getLocalString
                                            ("tests.componentNameConstructor",
                                            "For [ {0} ]",
                                            new Object[] {compName.toString()}));
                                        result.passed(smh.getLocalString(getClass().getName()+".passed1",
                                            "PASSED [AS-EJB pm-inuse] : pm-version {0} is valid", 
                                            new Object[]{pmVersion}));
                                    }else{
                                        // <addition> srini@sun.com Bug: 4698038
                                        //result.failed(smh.getLocalString(getClass().getName()+".failed",
                                        //  "FAILED [AS-EJB pm-inuse] : pm-version {0} for pm-identifier {0} not defined in pm-descriptors"
                                        //, new Object[]{pmVersion, pmIdentifier}));
                                        result.addErrorDetails(smh.getLocalString
		                            ("tests.componentNameConstructor",
				            "For [ {0} ]",
				            new Object[] {compName.toString()}));
                                        result.failed(smh.getLocalString(getClass().getName()+".failed5",
                                            "FAILED [AS-EJB pm-inuse] : pm-version {0} for pm-identifier {1} not defined in pm-descriptors"
                                            , new Object[]{pmVersion, pmIdentifier}));
                                        // </addition> Bug: 4698038
                                        oneFailed=true;
                                    }
                                }else{
                                    result.addErrorDetails(smh.getLocalString
				        ("tests.componentNameConstructor",
				        "For [ {0} ]",
				        new Object[] {compName.toString()}));
                                    result.failed(smh.getLocalString(getClass().getName()+".failed1",
                                        "FAILED [AS-EJB pm-inuse] : pm-identifier {0} is not defined in pm-descriptors"
                                        , new Object[]{pmIdentifier}));
                                    oneFailed=true;
                                }
                            }else{
                                result.addErrorDetails(smh.getLocalString
				    ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
                                result.failed(smh.getLocalString(getClass().getName()+".failed12",
                                    "FAILED [AS-EJB pm-inuse] : pm-version {0} is not defined in pm-inuse"
                                    , new Object[]{pmIdentifier}));
                                oneFailed=true;
                            }
                        }else{
                                result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
                                result.failed(smh.getLocalString(getClass().getName()+".failed14",
                                    "FAILED [AS-EJB pm-inuse] : pm-identifier {0} is not defined in pm-inuse"
                                    , new Object[]{pmIdentifier}));
                                oneFailed=true;
                        }
                    }else{
                        result.addErrorDetails(smh.getLocalString
			            ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
                        result.failed(smh.getLocalString(getClass().getName()+".failed10",
                            "FAILED [AS-EJB pm-descriptors] : pm-inuse {0} is not defined in pm-descriptors"));
                        oneFailed=true;
                    }
                }else{
                    result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
			"For [ {0} ]",
			new Object[] {compName.toString()}));
                    result.failed(smh.getLocalString(getClass().getName()+".failed11",
                        "FAILED [AS-EJB pm-descriptors] : pm-descriptor is not defined in pm-descriptors"));
                    oneFailed=true;
                }
            }else{
                result.addNaDetails(smh.getLocalString
		    ("tests.componentNameConstructor",
		    "For [ {0} ]",
		    new Object[] {compName.toString()}));
                result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable",
                    "NOT APPLICABLE [AS-EJB enterprise-beans] : pm-descriptors Element not defined"));
            }
            if(oneFailed)
                result.setStatus(Result.FAILED);
            else if(oneWarning)
                result.setStatus(Result.WARNING);
        }catch(Exception ex){
            result.addErrorDetails(smh.getLocalString
                ("tests.componentNameConstructor",
		"For [ {0} ]",
		new Object[] {compName.toString()}));
            result.failed(smh.getLocalString(getClass().getName()+".notRun",
                "NOT RUN [AS-EJB] Could not create descriptor Object."));
        }
        return result;
    }
    
    /**
     * @param i int
     * @param result Result
     * @param idVerMap Map  
     * @param descriptor EjbDescriptor */    
    protected void testPmDescriptor(int i, Result result, Map<String, String> idVerMap, EjbDescriptor descriptor){
        try{
            String value = null;
            //pm-identifier
            String pmIdentifier = getXPathValue("sun-ejb-jar/enterprise-beans/pm-descriptors/pm-descriptor[\""+i+"\"]/pm-identifier");
            if(pmIdentifier==null || pmIdentifier.length()==0){
                result.addErrorDetails(smh.getLocalString
                    ("tests.componentNameConstructor",
		    "For [ {0} ]",
		    new Object[] {compName.toString()}));
                result.failed(smh.getLocalString(getClass().getName()+".failed2",
                    "FAILED [AS-EJB pm-descriptor] : pm-identifier cannot be an empty string"));
                oneFailed=true;
            }else{
                result.addGoodDetails(smh.getLocalString
                    ("tests.componentNameConstructor",
                    "For [ {0} ]",
                    new Object[] {compName.toString()}));
                result.passed(smh.getLocalString(getClass().getName()+".passed2",
                    "PASSED [AS-EJB pm-descriptor] : pm-identifier is {0}",
                    new Object[]{pmIdentifier}));
            }

            //pm-version
            String pmVersion = getXPathValue("sun-ejb-jar/enterprise-beans/pm-descriptors/pm-descriptor[\""+i+"\"]/pm-version");
            if(pmVersion==null || pmVersion.length()==0){
                result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
			"For [ {0} ]",
			new Object[] {compName.toString()}));
                result.failed(smh.getLocalString(getClass().getName()+".failed3",
                    "FAILED [AS-EJB pm-descritor] : pm-version cannot be an empty string"));
                oneFailed=true;
            }else{
                result.addGoodDetails(smh.getLocalString
                    ("tests.componentNameConstructor",
                    "For [ {0} ]",
                    new Object[] {compName.toString()}));
                result.passed(smh.getLocalString(getClass().getName()+".passed3",
                    "PASSED [AS-EJB pm-descriptor] : pm-version is {0}",
                    new Object[]{pmVersion}));
            }

            if (pmIdentifier!=null && pmVersion!=null)
                idVerMap.put(pmIdentifier,pmVersion);

            //pm-config
            value = getXPathValue("sun-ejb-jar/enterprise-beans/pm-descriptors/pm-descriptor[\""+i+"\"]/pm-config");
            if(value!=null){
                //////  //4698035
                if(value.length()==0){
                    result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
			            "For [ {0} ]",
			            new Object[] {compName.toString()}));
                    oneFailed = true;
                    result.failed(smh.getLocalString(getClass().getName()+".failed4",
                        "FAILED [AS-EJB pm-descritor] : pm-config cannot be an empty string"));
                }else{
//                    File f = Verifier.getArchiveFile(descriptor.getEjbBundleDescriptor().getModuleDescriptor().getArchiveUri());
                    JarFile jarFile = null;
                    InputStream deploymentEntry=null;
                    try{
//                        if (f==null){
                            String uri = getAbstractArchiveUri(descriptor);
                            try {
                                FileArchive arch = new FileArchive();
                                arch.open(uri);
                                deploymentEntry = arch.getEntry(value);
                            }catch (Exception e) { }
//                        }else{
//                            try{
//                            jarFile = new JarFile(f);
//                            ZipEntry deploymentEntry1 = jarFile.getEntry(value);
//                            if (deploymentEntry1 != null)
//                                deploymentEntry = jarFile.getInputStream(deploymentEntry1);
//                            }catch (Exception e) { }
//                        }

                        if(deploymentEntry !=null){
                            result.addGoodDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                            result.passed(smh.getLocalString(getClass().getName()+".passed4",
                                "PASSED [AS-EJB pm-descriptor] : pm-config is {0}",
                                new Object[]{value}));
                        }else{
                            result.addWarningDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                            oneWarning=true;
                            result.warning(smh.getLocalString(getClass().getName()+".warning3",
                                "WARNING [AS-EJB pm-descriptor] : config file {0} pointed in pm-config is not present in the jar file",
                                new Object[]{value}));
                        }
                    }catch(Exception e){}
                    finally{
                        try {
                            if (jarFile != null)
                                jarFile.close();
                            if (deploymentEntry != null)
                                deploymentEntry.close();
                        }catch (Exception x) {}
                    }

                }
                /////////
            }

            //pm-class-generator
            value = getXPathValue("sun-ejb-jar/enterprise-beans/pm-descriptors/pm-descriptor[\""+i+"\"]/pm-class-generator");
            if (value==null || value.length()==0 ){
                //check for spec version accordingly give error or warning
                Float specVer = getRuntimeSpecVersion();
		        if ((Float.compare(specVer.floatValue(), (new Float("2.1")).floatValue()) >= 0)) {
                    oneFailed=true;
                    result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
			            "For [ {0} ]",
			            new Object[] {compName.toString()}));
                    result.failed(smh.getLocalString(getClass().getName()+".failed13",
                        "FAILED [AS-EJB pm-descriptor] : pm-class-generator cannot be empty."));
                }
            }else{
                if(value.trim().indexOf(" ") != -1){
                    oneFailed=true;
                    result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
			            "For [ {0} ]",
			            new Object[] {compName.toString()}));
                    result.failed(smh.getLocalString(getClass().getName()+".failed7",
                        "FAILED [AS-EJB pm-descriptor] : pm-class-generator class name is invalid"));
                }else{
                    result.addGoodDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                        "For [ {0} ]",
                        new Object[] {compName.toString()}));
                    result.passed(smh.getLocalString(getClass().getName()+".passed5",
                        "PASSED [AS-EJB pm-descriptor] : pm-class-generator is {0}",
                        new Object[]{value}));
                }
            }

            //pm-mapping-factory
            value = getXPathValue("sun-ejb-jar/enterprise-beans/pm-descriptors/pm-descriptor[\""+i+"\"]/pm-mapping-factory");
            if (value != null){
                if(value.trim().indexOf(" ") != -1){
                    result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
			            "For [ {0} ]",
			            new Object[] {compName.toString()}));
                    oneFailed=true;
                    result.failed(smh.getLocalString(getClass().getName()+".failed8",
                        "FAILED [AS-EJB pm-descriptor] : pm-mapping-factory class name is invalid"));
                }else{
                    if(value.trim().length()==0){
                        result.addErrorDetails(smh.getLocalString
                            ("tests.componentNameConstructor",
			                "For [ {0} ]",
			                new Object[] {compName.toString()}));
                        oneFailed=true;
                        result.failed(smh.getLocalString(getClass().getName()+".failed6",
                            "FAILED [AS-EJB pm-descritor] : pm-pm-mapping-factory cannot be an empty string"));
                    }else{
                        result.addGoodDetails(smh.getLocalString
                            ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[] {compName.toString()}));
                        result.passed(smh.getLocalString(getClass().getName()+".passed6",
                            "PASSED [AS-EJB pm-descriptor] : pm-mapping-factory is {0}",
                            new Object[]{value}));
                    }
                }
            }
        }catch(RuntimeException ex){
            oneFailed = true;
            result.failed(smh.getLocalString(getClass().getName()+".notRun",
                "NOT RUN [AS-EJB] Could not create descriptor Object."));
        }finally{
        }
    }
}
