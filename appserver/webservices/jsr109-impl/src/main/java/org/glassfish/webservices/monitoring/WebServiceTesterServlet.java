/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
 * WebServiceTesterServlet.java
 *
 * Created on August 6, 2004, 9:14 AM
 */

package org.glassfish.webservices.monitoring;

import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.tools.ws.spi.WSToolsObjectFactory;
import com.sun.xml.bind.api.JAXBRIContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.webservices.WebServiceContractImpl;
import com.sun.enterprise.module.*;
import org.glassfish.webservices.LogUtils;

/**
 * This servlet is responsible for testing web-services.
 *
 * @author Jerome Dochez
 */
public class WebServiceTesterServlet extends HttpServlet {

    private final WebServiceEndpoint svcEP;
    private static final Logger logger = LogUtils.getLogger();

    private static final Hashtable<String, Class> gsiClasses = new Hashtable<String, Class>();
    private static final Hashtable<String, Object> ports = new Hashtable<String, Object>();
    // resources...
    private static final LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(WebServiceTesterServlet.class);        
        
    public static void invoke(HttpServletRequest request, 
            HttpServletResponse response, WebServiceEndpoint endpoint) {
                    
        try {
            WebServiceTesterServlet servlet = new WebServiceTesterServlet(endpoint);            
            
            response.setCharacterEncoding("UTF-8");
            if (request.getMethod().equalsIgnoreCase("GET")) {
                servlet.doGet(request, response);
            } else {
                servlet.doPost(request, response);
            }
        } catch(Exception e) {
            try {     
                PrintWriter out = response.getWriter();
                out.print("<HTML lang=" + Locale.getDefault().getLanguage() + "><HEAD><TITLE>"+
                        localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.methodInvocationException",
                           "Method invocation exception")+"</TITLE></HEAD>");
                out.print("<H3>" + 
                            localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.ExceptionDetails",
                           "Exceptions details : {0}", 
                           new Object[] {e.getMessage()}) + "</H3>");                        
                out.print("<HR>");
                e.printStackTrace(out);
                out.print("<HR>");                                
                out.print("</HTML>");
                out.close();
            } catch(Exception ex) {};            
        }
    }    
    
    
    /**
     * Creates a new instance of WebServiceTesterServlet
     * @param ep endpoint to monitor
     */
    public WebServiceTesterServlet(WebServiceEndpoint ep) {
        svcEP = ep;
    }
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) 
        throws ServletException, IOException 
    {
        res.setContentType("text/html");
        res.setHeader("pragma", "no-cache");
        PrintWriter out = res.getWriter();
        String requestURL = req.getRequestURL().toString();

        Endpoint myEndpoint;
        if(svcEP.implementedByWebComponent()) {
            myEndpoint = WebServiceEngineImpl.getInstance().getEndpoint(req.getServletPath());
        } else {
            myEndpoint = WebServiceEngineImpl.getInstance().getEndpoint(req.getRequestURI());
        }
        String seiClassName = myEndpoint.getDescriptor().getServiceEndpointInterface();
        ClassLoader testerCL = svcEP.getWebService().getBundleDescriptor().getClassLoader();
        if (testerCL != null ){
            Thread.currentThread().setContextClassLoader(testerCL);
        }

        
        // For now support Tester servlet for JAXWS based services only
        try {
            Class seiClass = Thread.currentThread().getContextClassLoader().loadClass(seiClassName);
            if(seiClass.getAnnotation(javax.jws.WebService.class) == null) {
                testerNotSupportedError(myEndpoint.getDescriptor().getServiceName(), out);
                return;
            }
        } catch (ClassNotFoundException clnfEx) {
            classNotAccessibleError(Thread.currentThread().getContextClassLoader(), 
                    seiClassName, out);
            return;
        }
        
        initializePort(req,res);
        Class clientSEI = gsiClasses.get(requestURL);
        
        out.print("<HTML lang=" + Locale.getDefault().getLanguage() + "><HEAD><TITLE>"+
                            myEndpoint.getDescriptor().getServiceName().getLocalPart()+" "+
                            localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.title",
                           "Web Service Tester") + "</TITLE></HEAD>");
        out.print("<BODY><H1>"+
                            myEndpoint.getDescriptor().getServiceName().getLocalPart()+" "+
                            localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.title",
                           "Web Service Tester") + "</H1>");

        
        // Microsoft Internet Explorer does not handle <BUTTON> properly
        boolean isInternetExplorer=false;
        String userAgent = req.getHeader("user-agent");
        if (userAgent!=null) {
            isInternetExplorer=userAgent.indexOf("MSIE")!=-1;
        }
        StringBuffer sb = new StringBuffer(URLDecoder.decode(requestURL));
        sb.append("?WSDL");
        
        out.print("<br>");
        out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.line1",
                           "This form will allow you to test your web service implementation (<A HREF=\"{0}\" title=\"WSDL file describing {1} web service\">WSDL File</A>)",
                           sb.toString(), myEndpoint.getDescriptor().getServiceName().getLocalPart()));
        out.print("<hr>");
        out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.line2",
                           "To invoke an operation, fill the method parameter(s) input boxes and click on the button labeled with the method name."));
        out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.methods",
                           "<H3>Methods :</H3>"));
        
        Method[] methods = clientSEI.getMethods();
        
        for (Method m : methods) {
            out.print("<FORM METHOD=\"POST\">");
            out.print(m.toString());
            out.print("<BR>");
            out.print(localStrings.getLocalString(
                        "enterprise.webservice.monitoring.formSubmit",
                        "<INPUT TYPE=SUBMIT NAME=action title=\"Invoke {0} operation\" value={0}>",
                        m.getName()));

            out.print(" (");
            Class[] parameters = m.getParameterTypes();
            for (int i=0;i<parameters.length;i++) {
                out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.formInput",
                           "<INPUT TYPE=TEXT NAME=PARAM{0}{1} title=\"{0} parameter of type {2}\">",
                           m.getName(), i, parameters[i].getName()));
                if (i!=parameters.length-1)
                    out.print(",");
            }
            out.print(")");
            out.print("<BR>");
            out.print("<HR>");
            out.print("</FORM></BODY></HTML>");
        }
        out.close();
    }
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) 
        throws ServletException, IOException 
    {
        res.setContentType("text/html");
        res.setHeader("pragma", "no-cache");
        PrintWriter out = res.getWriter();
        String requestURL = req.getRequestURL().toString();
        
        out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.postTitle",
                           "<HTML lang={0}><HEAD><TITLE>Method invocation trace</TITLE></HEAD>",
                            Locale.getDefault().getLanguage()));
                
        String operationName = req.getParameter("action");

        try {
            Endpoint myEndpoint;
            if(svcEP.implementedByWebComponent()) {
                myEndpoint = WebServiceEngineImpl.getInstance().getEndpoint(req.getServletPath());
            } else {
                myEndpoint = WebServiceEngineImpl.getInstance().getEndpoint(req.getRequestURI());
            }


            Class clientSEI = gsiClasses.get(requestURL);
            if (clientSEI==null) {
                initializePort(req,res);
                clientSEI = gsiClasses.get(requestURL);
            }
            Object port = ports.get(requestURL);
            
            // find the right method...
            Method[] methods = clientSEI.getMethods();
            Method toInvoke = null;
            for (Method m : methods) {
                if (String.valueOf(m.getName()).equals(operationName)) {
                    toInvoke = m;
                }
            }            
            
            if (toInvoke==null) {
                out.print("cannot  \"action\" request parameter method");
            } else {
                out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.methodInvocation",
                           "<H2><A> {0} </A> Method invocation</H2><BR><HR>", 
                           new Object[] {toInvoke.getName()}));
                
                // register ourselves to receive the SOAP messages...
                MessageListenerImpl listener = new MessageListenerImpl();
                myEndpoint.addListener(listener);

                Class[] parameterTypes = toInvoke.getParameterTypes();
                Object[] parameterValues= new Object[parameterTypes.length];
                out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.methodTrace",
                           "<h4>Method parameter(s)</h4>"));
                out.print("<table border=\"1\">");
                out.print("<tr>");
                out.print("<th>Type</th>");
                out.print("<th>Value</th>");
                out.print("</tr>");
                for (int i=0;i<parameterTypes.length;i++) {
                    out.print("<tr>");
                    String webValue = req.getParameter("PARAM"+
                            toInvoke.getName()+i);
                    out.print("<td>" + parameterTypes[i].getName()+"</td>");
                    out.print("<td><pre>" + encodeHTML(webValue) + "</pre></td>");
                    parameterValues[i] = convertWebParam(parameterTypes[i],webValue);
                    out.print("</tr>");
                }
                out.print("</table>");
                out.print("<HR>");
                out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.methodReturn",
                           "<h4>Method returned</h4>")
                           +toInvoke.getReturnType().getName() + " : \"<b>" 
                        + encodeHTML(toInvoke.invoke(port, parameterValues).toString())+"</b>\"");
                out.print("<HR>");
                if (listener.getRequest() != null) {
                    // let's print the SOAP request
                    out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.soapReq",
                           "<h4>SOAP Request</h4>"));
                    dumpMessage(listener.getRequest(), out);
                }
                if (toInvoke.getAnnotation(javax.jws.Oneway.class) == null &&
                        listener.getRespose() != null) {
                    // let's print the SOAP request
                    out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.soapResp",
                           "<h4>SOAP Response</h4>"));
                    dumpMessage(listener.getRespose(), out);
                }
                myEndpoint.removeListener(listener);
            } 
        
        } catch(Throwable e) {
            out.print(localStrings.getLocalString(
                       "enterprise.webservice.monitoring.serviceExceptionError",
                       "<H2>Service invocation threw an exception with message : {0}; Refer to the server log for more details</H2><BR><HR>", 
                       new Object[] {e.getMessage()}));
            throw new ServletException(e);
        } 
               
        out.print("</HTML>");
        out.close();
    }
    
    private void dumpMessage(MessageTrace message, PrintWriter out) throws Exception {
                            
        /*String xsl = "<?xml version=\"1.0\"?>"+
        "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"+
        "<xsl:template match=\"/\">" +
        "<xsl:for-each select=\"*\">" +
        "&lt;<xsl:value-of select=\".\"/>&gt;" +
        "</xsl:for-each>" +
        "</xsl:template>" +
        "</xsl:stylesheet>";*/
        
        // now transform it...
        ByteArrayInputStream bais = new ByteArrayInputStream(message.getMessage(true).getBytes());
        StreamSource ss = new StreamSource(bais);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult sr = new StreamResult(baos);
        
        TransformerFactory factory =  TransformerFactory.newInstance();        
        Transformer transformer = factory.newTransformer();
        
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(ss, sr);
        
        
        out.print("<HR><blockquote><pre xml:lang>");
        out.write(encodeHTML(baos.toString()));
        
        out.print("</pre></blockquote><HR>");

    }    
    
    private Object convertWebParam(Class targetParamType, String webValue) {
        
        Object convertedValue = null;
        if (webValue==null || webValue.length()==0) {
            return null;
        }
        if (String.class.equals(targetParamType)) {
             convertedValue = webValue;
        } else {
            try {
                if (int.class.equals(targetParamType) || 
                        Integer.class.equals(targetParamType)) {
                    convertedValue = Integer.valueOf(webValue);
                }
                if (boolean.class.equals(targetParamType) ||
                        Boolean.class.equals(targetParamType)) {
                    convertedValue =  Boolean.valueOf(webValue);
                }
                if (char.class.equals(targetParamType) || 
                        (Character.class.equals(targetParamType))) {
                    convertedValue = webValue.charAt(0);
                }                
                
                if (long.class.equals(targetParamType) || 
                        (Long.class.equals(targetParamType))) {
                    convertedValue =  Long.valueOf(webValue);
                }
                if (float.class.equals(targetParamType) || 
                        (Float.class.equals(targetParamType))) {
                    convertedValue = Float.valueOf(webValue);
                }  
                if (double.class.equals(targetParamType) || 
                        (Double.class.equals(targetParamType))) {
                    convertedValue =  Double.valueOf(webValue);
                }  
                if (byte.class.equals(targetParamType) || 
                        (Byte.class.equals(targetParamType))) {
                    convertedValue = Byte.valueOf(webValue);
                }   
                if (short.class.equals(targetParamType) || 
                        (Short.class.equals(targetParamType))) {
                    convertedValue = new Short(webValue);
                }     
                if (StringBuffer.class.equals(targetParamType)) {
                    convertedValue = new StringBuffer(webValue);
                }                   
            } catch(NumberFormatException nfe) {
                System.out.println("Cannot convert " + webValue + " in " + targetParamType);
            }
        } 
        return convertedValue;
    }
    
    private void classNotAccessibleError(ClassLoader cl, String className, PrintWriter out) {
        
        out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.CNFTitle",
                           "<HTML lang={0}><HEAD><TITLE>Method invocation exception</TITLE></HEAD>",
                            Locale.getDefault().getLanguage()));
        if (cl==null) {
            out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.CNFServerError",
                           "<H3>Internal server error, debugging is not available</H3>"));
        } else {
            out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.CNFerror2",
                           "<H3>Cannot load class {0} - Verify class presence in bundle</H3>", 
                           new Object[] {className}));
        }
        out.print("<HR>");
        out.print("</HTML>");
        out.close();          
    }
    
    private void testerNotSupportedError(QName svcName, PrintWriter out) {
        
        out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.TesterNSTitle",
                           "<HTML lang={0}><HEAD><TITLE>Tester feature not supported</TITLE></HEAD>",
                            Locale.getDefault().getLanguage()));
        out.print("<BODY>");
        out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.TesterNSerror2",
                           "Service {0} looks like a JAXRPC based webservice.", 
                           new Object[] {svcName}));
        out.print("<br><br>");
        out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.TesterNSdetail",
                           "Please note that the tester feature is supported for JAXWS based webservices only"));
        out.print("</BODY>");
        out.print("</HTML>");
        out.close();          
    }

     private void wsImportError(URL wsdlUrl, PrintWriter out) {

        out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.WsImportError",
                           "<HTML lang={0}><HEAD><TITLE>WsImport error for the the following wsdl</TITLE></HEAD>",
                            Locale.getDefault().getLanguage()));
        out.print("<BODY>");
        out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.WsImportError2",
                           "Error generating artifacts for the following WSDL {0}",
                           new Object[] {wsdlUrl}));
        out.print("<br><br>");
         out.print(localStrings.getLocalString(
		    	   "enterprise.webservice.monitoring.WsImportError3",
                           "Possible causes can be" +
                                   "invoking https when the application is not configured for security",
                           new Object[] {wsdlUrl}));

        out.print("</BODY>");
        out.print("</HTML>");
        out.close();
    }

    private void initializePort(HttpServletRequest req,HttpServletResponse res)
        throws ServletException, IOException {

        String requestURL = req.getRequestURL().toString();
        Endpoint myEndpoint;
        if(svcEP.implementedByWebComponent()) {
            myEndpoint = WebServiceEngineImpl.getInstance().getEndpoint(req.getServletPath());
        } else {
            myEndpoint = WebServiceEngineImpl.getInstance().getEndpoint(req.getRequestURI());
        }
        
        // get our service qname
        QName serviceName =
                new QName(myEndpoint.getDescriptor().getWsdlPort().getNamespaceURI(),
                myEndpoint.getDescriptor().getWebService().getName());
        
        // construct the WSDL http url
        StringBuffer sb = new StringBuffer(URLDecoder.decode(requestURL));
        sb.append("?WSDL");
        
        URL[] urls = new URL[1];
        String classesDir;
        try {
            URL wsdlUrl = new URL(sb.toString());
            // create client artifacts
            classesDir = wsImport(wsdlUrl);
            if (classesDir == null) {
                wsImportError(wsdlUrl,res.getWriter());
                return;
            }
            urls[0] = (new File(classesDir)).toURL();
        } catch(MalformedURLException mue) {
            throw new ServletException(mue);
        }
        
        // we need a class loader to load the just created client artifacts. Save the current classloader
        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        // we now create a new classloader using the parent class loader
        // which should be the shared application classloader. I don't want
        // the application class loader to not get into client/server
        // classes clashes.
        // the immediate classloader is the WebApp classloader, its parent is the 
        // application classloader, we want the parent of that one
        ClassLoader testerCL = new URLClassLoader(urls, currentLoader.getParent());
        try {
            Thread.currentThread().setContextClassLoader(testerCL);
            String serviceClassName = getServiceClass(
                    JAXBRIContext.mangleNameToClassName(serviceName.getLocalPart()),
                    classesDir);
            if (serviceClassName==null) {
                throw new RuntimeException("Service Class not generated as expected");
            }
            
            Class serviceClass = testerCL.loadClass(serviceClassName);
            Service service = Service.create(new URL(sb.toString()), serviceName);            
            if (service==null) {
                throw new RuntimeException("Cannot load Service");
            }
            
            // find the right port... for this look at the @WebService annotation in SEI and get the portName
            String portClassName = getPortClass(myEndpoint, serviceClass);
            if (portClassName==null) {
                throw new RuntimeException("Cannot find the correct port class.");
            }            
            
            Class portClass = testerCL.loadClass(portClassName);
            Object port = service.getPort(myEndpoint.getDescriptor().getWsdlPort(), portClass);
            if (port==null) {
                throw new RuntimeException("Cannot find the correct port class.");
            }

            ports.put(requestURL, port);
            gsiClasses.put(requestURL, portClass);

        } catch(Exception e) {
            throw new ServletException(e);
        } finally {
            // restore the class loader
            Thread.currentThread().setContextClassLoader(currentLoader);
            // delete client artifacts, everything should be loaded or it failed
            deleteDir(new File(classesDir));
        }
    }
    
    private String wsImport(URL wsdlLocation) throws IOException {

        File classesDir = new File(System.getProperty("java.io.tmpdir"));

        // create a dumy file to have a unique temporary name for a directory
        classesDir = File.createTempFile("jax-ws", "tester", classesDir);
        if (!classesDir.delete()) {
            logger.log(Level.WARNING, LogUtils.DELETE_DIR_FAILED, classesDir);
        }
        if (!classesDir.mkdirs()) {
            logger.log(Level.SEVERE, LogUtils.CREATE_DIR_FAILED, classesDir);
        }

        String[] wsimportArgs = new String[8];
        wsimportArgs[0]="-d";
        wsimportArgs[1]=classesDir.getAbsolutePath();
        wsimportArgs[2]="-keep";
        wsimportArgs[3]=wsdlLocation.toExternalForm();
        wsimportArgs[4]="-Xendorsed";
        wsimportArgs[5]="-target";
        wsimportArgs[6]="2.1";
        wsimportArgs[7]="-extension";
        WSToolsObjectFactory tools = WSToolsObjectFactory.newInstance();
        logger.log(Level.INFO, LogUtils.WSIMPORT_INVOKE, wsdlLocation);
        boolean success = tools.wsimport(System.out, wsimportArgs);

        if (success) {
            logger.log(Level.INFO, LogUtils.WSIMPORT_OK);
        } else {
            logger.log(Level.SEVERE, LogUtils.WSIMPORT_FAILED);
            return null;
        }
        return classesDir.getAbsolutePath();
    }
    
    private String getServiceClass(String serviceClassName, String classesDirPath) {
        //wsimport generated Service Class always is serviceQName.getLocalPart()+".class"
        // So just look for this class and return that class
        File classesDir = new File(classesDirPath);
        if (!classesDir.exists()) {
            return null;            
        }
        List<File> mycoll = getListOfFiles(classesDir);
        File[] classes = mycoll.toArray(new File[mycoll.size()]);
        String resolvedServiceClass = null;
        String svcClass = null;
        for (File f : classes) {            
            if (f.getName().endsWith(serviceClassName+"_Service.class")){
                resolvedServiceClass = f.getAbsolutePath().substring(classesDirPath.length()+1);
                
            } else {
                if (f.getName().endsWith(serviceClassName+".class")){
                    svcClass = f.getAbsolutePath().substring(classesDirPath.length()+1);
                  
                }
            }         
        }
        //Incase there is a clash JAXWS resolves the serviceClass
        //to serviceName_Service.class Use the first if it is present
        //Fix for issue 3403
        if ( resolvedServiceClass != null){
            svcClass = resolvedServiceClass;
        }    
        if (svcClass != null) {
            svcClass = svcClass.substring(0, svcClass.indexOf(".class"));
            return svcClass.replaceAll("\\"+File.separator,".");
        } else {
            return null;        
        }
    }

    private String getPortClass(Endpoint ep, Class serviceClass)
                                            throws Exception {
        
        for(Method m : serviceClass.getMethods()) {
            WebEndpoint webEP = (WebEndpoint) 
                m.getAnnotation(WebEndpoint.class);
            if(webEP == null || webEP.name() == null ||
                    webEP.name().length() == 0) {
                continue;
            }
            String getPortMethodName = "get" + 
                    JAXBRIContext.mangleNameToClassName(webEP.name());
            Method getPortMethod = 
                    serviceClass.getMethod(getPortMethodName, (Class[])null);
            return getPortMethod.getReturnType().getName();
        }
        return null;
    }
    
    private List<File> getListOfFiles(File path) {
        
        File[] files = path.listFiles();
        List<File> result = new ArrayList<File>();
        for (File f : files) {
            if (f.isDirectory()) {
                result.addAll(getListOfFiles(f));
            } else {
                result.add(f);                
            }
        }
        return result;
    }
    
    private void deleteDir(File path) {

        if (path.exists() && path.isFile()) {
            File[] files = path.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDir(f);
                }
                assert f.delete();
            }
            assert path.delete();
        }
    }

    private String encodeHTML(String html) {
        return html.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }
}
