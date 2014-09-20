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
 * VerifierTest.java
 *
 * Created on September 20, 2000, 4:18 PM
 */

package com.sun.enterprise.tools.verifier.tests;

import java.lang.reflect.Method;
import java.io.File;
import java.util.jar.JarFile;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URI;

import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.StringManagerHelper;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.DocumentType;
import com.sun.org.apache.xpath.internal.XPathAPI;
import com.sun.org.apache.xpath.internal.NodeSet;
import com.sun.org.apache.xml.internal.utils.PrefixResolver;
import com.sun.enterprise.tools.verifier.XpathPrefixResolver;
import com.sun.enterprise.tools.verifier.util.LogDomains;
import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.org.apache.xpath.internal.objects.XObject;

/**
 * Superclass for all tests developped for the Verifier Harness
 * Contains convenience methods and fields
 *
 * @author  Jerome Dochez
 * @version 
 */
abstract public class VerifierTest extends Object {
    // variables ensuring that result details are added only once
    private boolean addedError   = false;
    private boolean addedGood    = false;
    private boolean addedNa      = false;
    private boolean addedWarning = false;
    protected Logger logger = LogDomains.getLogger(LogDomains.AVK_VERIFIER_LOGGER);


   /**
     * <p>
     * Are we in debug mode 
     * </p>
     */
    protected final boolean debug = Verifier.isDebug();

    /**
     * <p>
     * helper property to get to the localized strings
     * </p>
     */
    protected static final com.sun.enterprise.util.LocalStringManagerImpl smh =
	StringManagerHelper.getLocalStringsManager();

    private VerifierTestContext context = null;

    /**
     * This method sets the Context object
     */
    public void setVerifierContext(VerifierTestContext context) {
	this.context = context;
    }
    
    /**
     * This method provides the Context object
     */
    public VerifierTestContext getVerifierContext() {
	return context;
    }
    

    /**
     * <p>
     * Common traces and initialization of the result object that will
     * hold the result of the assertion tested by this verifier test
     * </p>
     * 
     * @return the initialized Result object
     */     
    protected Result getInitializedResult() {

        logger.log(Level.FINE, "which.class.called.string",
                new Object[] {getClass()}) ;
        Result result = new Result();
        String version = "";
        // This is only needed because of ParseDD test which runs before
        // context is set in VerifierTest object, else we shall get a NPE.
        String compName = "";
        if (context!=null) {
            version = context.getSchemaVersion();
            compName = context.getComponentNameConstructor().toString();
        } else {
            logger.fine(getClass().getName() + " context is null.");
        }
        result.init(getClass(), version, compName);
        logger.log(Level.FINE, "test.string.assertion", new Object[] {result.getAssertion()});
        return result;
    }

    /**
     * <p>
     * check if the class is a sublcass or the class itself of the passed class name
     * </p>
     * 
     * @param subClass class object to test if it a subclass 
     * @param superClassName class name for the superclass
     * @return true if the <code>Class</code> is a subclass or the class itself
     * of the class name 
     */
    public static boolean isSubclassOf(Class subClass, String superClassName) {
        
        
        if (subClass==null || superClassName==null) {
            return false;
        }
        
        Class c = subClass;
        
	do {
            if (c.getName().equals(superClassName)) {
                return true;
            }
	    Class[] interfaces = c.getInterfaces();
            for (int i=0; i<interfaces.length;i++) {
                if (interfaces[i].getName().equals(superClassName)) {
		    return true;
                }
		else {
		    if (isSubclassOf(interfaces[i], superClassName)) {
			return true;
		    }
		}
            }
            c = c.getSuperclass(); 	             
	} while (c!=null);
        return false;
    }
    
    /**
     * <p>
     * Test if a class or its superclasses implements an interface
     * </p>
     * 
     * @param c is the class to test 
     * @param interfaceName is the interface we test for implementation
     * @return true if the class or superclasses implements the interface
     */
    public static boolean isImplementorOf(Class<?> c, String interfaceName) {
        
        if (c==null || interfaceName==null) 
            return false;

        // try this first because the code in the rest of the method
        // is buggy 
        try {
          Class<?> intf = Class.forName(interfaceName);
          if (intf.isAssignableFrom(c)) 
             return true;
          else
             return false;
         }catch(Exception e) { }
        
	do {
	    if (isSubclassOf(c, interfaceName)) {
		return true;
	    }
	    // get the list of implemented interfaces
            Class[] interfaces = c.getInterfaces();
            for (int i=0; i<interfaces.length;i++) {
		if (isSubclassOf(interfaces[i], interfaceName)) {
		    return true;
                }
            }
            // we haven't found for this implementation, look in the superclass
            c = c.getSuperclass();
        } while (c != null);
	return false;        
    }    

    /**
     * <p>
     * utility method to return a method if it is implemented by a class or one
     * of its superclass irrespective of the method being public, private or protected
     * </p>
     * 
     * @param clazz the class used to look up the method
     * @param methodName the method name
     * @param parmTypes the parameters 
     * @return instanceof the <code>Method</code> if implemented
     */
    public static Method getDeclaredMethod(Class clazz, String methodName, Class[] parmTypes)
    {
        Method m=null;
        Class c = clazz;
        do {
            try {
                m = clazz.getDeclaredMethod(methodName, parmTypes);
            } catch(NoSuchMethodException nsme) {
            } catch(SecurityException se) {
            }
            c = c.getSuperclass();
        } while (m != null && c != null);            
        return m;
    }
    
    /**
     * <p>
     * utility method to return a method if ig is implemented by a class or one
     * of its superclass and it is defined as public
     * </p>
     * 
     * @param clazz the class used to look up the method
     * @param methodName the method name
     * @param parmTypes the parameters 
     * @return instanceof the <code>Method</code> if implemented
     */
    public static Method getMethod(Class clazz, String methodName, Class[] parmTypes)
    {
        Method m=null;
        Class c = clazz;
        do {
            try {
                m = clazz.getMethod(methodName, parmTypes);
            } catch(NoSuchMethodException nsme) {
            } catch(SecurityException se) {
            }
            c = c.getSuperclass();
        } while (m != null && c != null);            
        return m;
    }
    
    /**
     * <p>
     * verify that a class or one of its superclass is implementing an interface
     * </p>
     *
     * @param clazz the class to test for the implementation of the interface
     * @param interfaceName the name of the interface that should be implementad
     * @param result where to put the result
     * 
     */
    public static boolean testImplementationOf(Class clazz, String interfaceName, Result result) 
    {        
        if (isImplementorOf(clazz, interfaceName)) {
            result.passed(smh.getLocalString
	        ("com.sun.enterprise.tools.verifier.tests.VerifierTest.interfaceimplementation.passed", 
                "The class [ {0} ] implements the [ {1} ] interface",
                new Object[] {clazz.getName(), interfaceName}));    
            return true;
        } else {
            result.failed(smh.getLocalString
	        ("com.sun.enterprise.tools.verifier.tests.VerifierTest.interfaceimplementation.failed", 
                "Error: The class [ {0} ] does not implement the [ {1} ] interface",
                new Object[] {clazz.getName(), interfaceName}));        
            return false;
        }
    }    
    
    /** 
     * <p>
     * Test for a file existence in the archive file
     * </p>
     *
     * @param uri The archive to look in
     * @param fileName The file Name to look for
     * @param fileID The archive file name
     * @param result where to place the result
     */
    public static void testFileExistence(String uri, String fileName, String fileID, Result result) {
        
       FileArchive arch=null;
//       ZipEntry ze=null;
       JarFile jarFile=null;
       
       if (fileName == null || fileName.length()==0) {
	    result.notApplicable(smh.getLocalString
    	        ("com.sun.enterprise.tools.verifier.tests.VerifierTest.fileexistence.notApplicable",
                 "No {0} defined in deployment descriptors",
                 new Object[] {fileID}));                        
            return;
        }
//        if (file==null){
            try{
                arch = new FileArchive();
                arch.open(URI.create(uri));
            }catch(Exception e){}   
//        }else{
//            try {
//                jarFile = new JarFile(file);
//            } catch (java.io.IOException ioe) {
//                Verifier.debug(ioe);
//                result.failed(smh.getLocalString
//                        ("com.sun.enterprise.tools.verifier.tests.VerifierTest.fileexistence.failed",
//                         "Error:  {0} [ {1} ] not found in the archive",
//                         new Object[] {fileID, fileName}));      
//                         return;
//             }
//         }
        try{
//            if (file!=null){
//            ze = jarFile.getEntry(fileName);
//            if (ze == null) {
//                result.failed(smh.getLocalString
//                    ("com.sun.enterprise.tools.verifier.tests.VerifierTest.fileexistence.failed",
//                    "Error: {0} [ {1} ] not found in the archive",
//                    new Object[] {fileID, fileName}));                           
//            }else{
//                result.passed(smh.getLocalString
//                ("com.sun.enterprise.tools.verifier.tests.VerifierTest.fileexistence.passed",
//                "{0} [ {1} ] found in the archive",
//                new Object[] {fileID, fileName}));                           
//            }
//        }
//        else{
            File urif = new File(new File(arch.getURI()), fileName);
            if(urif.exists()){
                result.passed(smh.getLocalString
                ("com.sun.enterprise.tools.verifier.tests.VerifierTest.fileexistence.passed",
                "{0} [ {1} ] found in the archive",
                new Object[] {fileID, fileName}));                           
            }else{
                result.warning(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.VerifierTest.fileexistence.warning",
                    "{0} [ {1} ] not found in the archive",
                    new Object[] {fileID, fileName}));                           
            }
                urif = null;
//        }
        
        if (jarFile!=null)
                        jarFile.close();
        }catch(Exception ex){}
    
    }
    
   /**
     * <p>
     * test if a method throws a particular exception
     * </p>
     * 
     * @param method the method to test
     * @param exception the exception we are looking for
     * @return true if the method actually throw the exception
     */
    public static boolean methodThrowException(Method method, String exception) {
        
        Class[] exceptions = method.getExceptionTypes();
        for (int i=0;i<exceptions.length;i++) {
            if (isSubclassOf(exceptions[i], exception))
                return true;            
        }
        return false;
    }

    /*
     *getXPathValueForNonRuntime(String xpath)
     *   return String - is the value of the element specified in the xpath.
     */
    public String getXPathValueForNonRuntime(String xpath){
        try{
            String value = null;
            Document d = getVerifierContext().getDocument();
            if (d==null) return null;
            XObject result = XPathAPI.eval(d, xpath, 
                             (PrefixResolver)new XpathPrefixResolver (d));
            NodeList nl = result.nodelist();
            for(int i=0; i<nl.getLength();i++){
                Node n = ((Node)nl.item(i)).getFirstChild();
                if (n==null) return null;
                value = n.getNodeValue();
            }
            return value;
        }catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    /*
     *getXPathValue(String xpath)
     *   return String - is the value of the element specified in the xpath.
     */
    public String getXPathValue(String xpath){
        try{
            String value = null;
            Document d = getVerifierContext().getRuntimeDocument();
            if (d==null) return null;
            NodeList nl = XPathAPI.selectNodeList(d, xpath);
            for(int i=0; i<nl.getLength();i++){
                Node n = ((Node)nl.item(i)).getFirstChild();
                if (n==null) return null;
                value = n.getNodeValue();
            }
            return value;
        }catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    /*
     * getCountNodeSet( String xpath)
     *   return the number of nodes for the specified xpath
     */
    public int getCountNodeSet (String xpath){
        try{
//            int value = -1;
            Document d = getVerifierContext().getRuntimeDocument();
            if (d==null)
                return -1;
            NodeSet ns = new NodeSet(XPathAPI.selectNodeList(d, xpath));
            return ns.getLength();
        }catch(Exception ex){
            ex.printStackTrace();
            return -1;
        }
    }

    public int getNonRuntimeCountNodeSet(String xpath){
        try{
//            int value = -1;
            Document d = getVerifierContext().getDocument();
            if (d==null)
                return -1;
            XObject result = XPathAPI.eval(d, xpath,
                             (PrefixResolver)new XpathPrefixResolver (d));
            NodeList nl = result.nodelist();
            NodeSet ns = new NodeSet(nl);
            return ns.getLength();
        }catch(Exception ex){
            ex.printStackTrace();
            return -1;
        }
    }

    /*
     *getRuntimeSpecVersion()
     *   return Float Spec-version
     */
    public Float getRuntimeSpecVersion(){
        String docType = null;
        String versionStr = null;
        Float versionFloat=null;
        try{
            DocumentType dt = getVerifierContext().getRuntimeDocument().getDoctype();
            if (dt==null) return null;
            docType = dt.getPublicId();
            StringTokenizer st = new StringTokenizer(docType, "//");
            while (st.hasMoreElements()) {
                String tmp = st.nextToken();
                if (tmp.startsWith("DTD")) {
                // this is the string we are interested in
                    StringTokenizer versionST = new StringTokenizer(tmp);
                    while (versionST.hasMoreElements()) {
                        versionStr = versionST.nextToken();
                        try {
                            versionFloat = Float.valueOf(versionStr);
                        } catch(NumberFormatException nfe) {
                        // ignore, this is just the other info of the publicID
                        }
                    }
                }
            }
            return versionFloat;
        }catch(Exception ex){
            //ex.printStackTrace();
            return null;
        }
    }

    /**
     * If the param string is of primitive type this method return that class
     * representation of the primitive type
     * @param param
     * @return Class representaion of the primitive type
     */
    public Class checkIfPrimitive(String param) {

        if (param.equals("int"))
            return int.class;
        if (param.equals("boolean"))
            return boolean.class;
        if (param.equals("float"))
            return float.class;
        if (param.equals("double"))
            return(double.class);
        if (param.equals("byte"))
            return byte.class;
        if (param.equals("long"))
            return long.class;
        if (param.equals("char"))
            return char.class;
        if (param.equals("short"))
            return short.class;
        if (param.equals("void"))
            return void.class;

        return null;
    }

    /** These methods are used to add details to the result. Since this
     *  Class is the base class of all the Ejb Tests, all the tests
     *  can use these methods. Currently only runtime tests are using them.
     */
    
    protected void addGoodDetails(Result result, ComponentNameConstructor compName) {
        // make sure that this message is added only once
        if(addedGood) return;
        addedGood = true;
	    result.addGoodDetails(smh.getLocalString("tests.componentNameConstructor",
				                                 "For [ {0} ]",
				                                 new Object[] {compName.toString()}));
    }
    
    protected void addErrorDetails(Result result, ComponentNameConstructor compName) {
        // make sure that this message is added only once
        if(addedError) return;
        addedError = true;
	    result.addErrorDetails(smh.getLocalString("tests.componentNameConstructor",
				                                  "For [ {0} ]",
				                                  new Object[] {compName.toString()}));
    }
    
    protected void addWarningDetails(Result result, ComponentNameConstructor compName) {
        // make sure that this message is added only once
        if(addedWarning) return;
        addedWarning = true;
	    result.addWarningDetails(smh.getLocalString("tests.componentNameConstructor",
				                                    "For [ {0} ]",
				                                    new Object[] {compName.toString()}));
    }
    
    protected void addNaDetails(Result result, ComponentNameConstructor compName) {
        // make sure that this message is added only once
        if(addedNa) return;
        addedNa = true;
	    result.addNaDetails(smh.getLocalString("tests.componentNameConstructor",
				                               "For [ {0} ]",
				                               new Object[] {compName.toString()}));
    }
}
