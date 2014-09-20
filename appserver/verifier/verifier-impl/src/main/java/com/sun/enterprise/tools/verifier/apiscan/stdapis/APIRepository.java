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
 * APIHelper.java
 *
 * Created on September 22, 2004, 10:23 AM
 */

package com.sun.enterprise.tools.verifier.apiscan.stdapis;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class represents a repository of APIs. It is backed by a XML file
 * with a predefined schema. This class is resoponsible for parsing the
 * XML file and getting populated by the information available in that file.
 * In verifier, this class is used in conjunction with an XML file where all
 * the standard APIs are listed along with their respective version number.
 * Refer to standard-apis.xml file to see how we list APIs in that file.
 * 
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class APIRepository {
    private static Logger logger = Logger.getLogger("apiscan.stdapis"); // NOI18N
    private static String myClsName = "APIRepository"; // NOI18N
    private HashMap<String, API> apis = new HashMap<String, API>();
    //singleton
    private static APIRepository me;

    /**
     * Return the singleton instance.
     */
    public static APIRepository Instance() {
        assert(me != null);
        return me;
    }

    /**
     * Initialize the singleton instance.
     *
     * @param fileName An XML file which contains the details of APIs.
     */
    public synchronized static void Initialize(String fileName)
            throws Exception {
        logger.logp(Level.FINE, myClsName, "Initialize", fileName); // NOI18N
        //Pl refer to bug#6174887
//        assert(me==null);
//        if(me==null){
        me = new APIRepository(fileName);
//        }else throw new RuntimeException("Already Initialized");
    }

    /**
     * Initialize the singleton instance.
     *
     * @param is InputStream for an XML file which contains the details of APIs.
     */
    public synchronized static void Initialize(InputStream is)
            throws Exception {
        logger.logp(Level.FINE, myClsName, "Initialize", is.toString()); // NOI18N
        //Pl refer to bug#6174887
//        assert(me==null);
//        if(me==null){
        me = new APIRepository(is);
//        }else throw new RuntimeException("Already Initialized");
    }

    /**
     * This method is used to find out if a particular class is part of 
     * a standard API or not. e.g. to find out if an EJB 2.0 application is 
     * allowed to use javax.ejb.Timer.class, call this method as 
     * <blockquote><pre>
     * isClassPartOf("javax.ejb.Timer","ejb_jar_2.0")
     * </pre></blockquote>
     *
     * @param class_name name of the class (in external class name format)
     * @param api_name_version is the name of the API along with version. It
     * must already be defined in the XML file.
     * 
     * @return true iff the given class is part of this API.
     */
    public boolean isClassPartOf(String class_name, String api_name_version) {
        if (getClassesFor(api_name_version).contains(class_name)) {
            return true;
        } else if (getPackagesFor(api_name_version).
                contains(getPackageName(class_name))) {
            return true;
        } else {
            for (String pattern : getPatternsFor(api_name_version)) {
                if (class_name.startsWith(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * This method is used to find out if a particular package is part of 
     * a standard API or not. e.g. to find out if an appclient (v 1.4) is 
     * allowed to import javax.persistence.* , call this method as 
     * <blockquote><pre>
     * isPackagePartOf("javax.persistence","appclient_1.4")
     * </pre></blockquote>
     *
     * @param pkg_name name of the package
     * @param api_name_version is the name of the API along with version. It
     * must already be defined in the XML file.
     * 
     * @return true iff the given package is part of this API.
     */
    public boolean isPackagePartOf(String pkg_name, String api_name_version) {
        if (getPackagesFor(api_name_version).contains(pkg_name)) {
            return true;
        } else {
            for (String pattern : getPatternsFor(api_name_version)) {
                if (pkg_name.startsWith(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected Collection<String> getPackagesFor(String api_name_version) {
        return ((API) apis.get(api_name_version)).getPackages();
    }

    protected Collection<String> getPatternsFor(String api_name_version) {
        return ((API) apis.get(api_name_version)).getPatterns();
    }

    protected Collection<String> getClassesFor(String api_name_version) {
        return ((API) apis.get(api_name_version)).getClasses();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Iterator i = apis.values().iterator(); i.hasNext();) {
            sb.append("\n").append(i.next().toString()); // NOI18N
        }
        return sb.toString();
    }

    private APIRepository(String fileName) throws Exception {
        logger.entering(myClsName, "init<>", fileName); // NOI18N
        final File file = new File(fileName);
        Document d = getDocumentBuilder().parse(file);
        traverseTree(d.getDocumentElement());
    }
    
    private APIRepository(InputStream is) throws Exception {
        logger.entering(myClsName, "init<>", is.toString()); // NOI18N
        Document d = getDocumentBuilder().parse(is);
        traverseTree(d.getDocumentElement());
    }

    private DocumentBuilder getDocumentBuilder() throws Exception {
        DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
        bf.setValidating(false);
        bf.setIgnoringComments(false);
        bf.setIgnoringElementContentWhitespace(true);
        bf.setCoalescing(true);
        bf.setNamespaceAware(true);
        bf.setAttribute(
                "http://java.sun.com/xml/jaxp/properties/schemaLanguage", // NOI18N
                "http://www.w3.org/2001/XMLSchema"); // NOI18N
        DocumentBuilder builder = bf.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler() {
            public void error(SAXParseException e) throws SAXException {
                throw e;
            }
        });
        return builder;
    }

    private void traverseTree(Node node) {
        if (node == null) {
            return;
        }
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element e = (Element) node;
            String tagName = e.getTagName();
            if (tagName.equals("api")) { // NOI18N
                API a = new API(e);
                apis.put(a.name_version, a);
            }
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                traverseTree(childNodes.item(i));
            }
        }
    }

    //return java.util for java.util.Map$Entry
    private static String getPackageName(String externalClassName) {
        int idx = externalClassName.lastIndexOf('.');
        if (idx != -1) {
            return externalClassName.substring(0, idx);
        } else
            return "";
    }

    class APIRef {
        private String api_name_version;

        public APIRef(Element node) {
            if (node.getTagName().equals("api_ref")) { // NOI18N
                api_name_version = node.getAttribute("api_name_version"); // NOI18N
            } else
                throw new IllegalArgumentException(node.toString());
        }

        public APIRef(String api_name_version) {
            this.api_name_version = api_name_version;
        }

        public API deref() {
            API result = (API) apis.get(api_name_version);
            if (result == null)
                throw new NullPointerException(
                        "No API with name_version [" + api_name_version + "]"); // NOI18N
            return result;
        }
    }

    class API {
        private String name_version;
        private ArrayList<APIRef> apiRefs = new ArrayList<APIRef>();
        ArrayList<String> packages = new ArrayList<String>(), patterns = new ArrayList<String>(), classes = new ArrayList<String>();

        public API(Element node) {
            if (node.getTagName().equals("api")) { // NOI18N
                name_version = node.getAttribute("name_version"); // NOI18N
                NodeList refNodes = node.getElementsByTagName("api_ref"); // NOI18N
                for (int loopIndex = 0;
                     loopIndex < refNodes.getLength(); loopIndex++) {
                    apiRefs.add(new APIRef((Element) refNodes.item(loopIndex)));
                }
                NodeList pkgsNodes = node.getElementsByTagName("packages"); // NOI18N
                for (int i = 0; i < pkgsNodes.getLength(); ++i) {
                    Element pkgsNode = (Element) pkgsNodes.item(i);
                    NodeList children = pkgsNode.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        Node next = children.item(j);
                        if (next.getNodeType() == Node.TEXT_NODE) {
                            String names = next.getNodeValue().trim();
                            for (StringTokenizer st = new StringTokenizer(
                                    names);
                                 st.hasMoreTokens();) {
                                packages.add(st.nextToken());
                            }
                        }
                    }
                }
                NodeList patternsNodes = node.getElementsByTagName("patterns"); // NOI18N
                for (int i = 0; i < patternsNodes.getLength(); ++i) {
                    Element patternsNode = (Element) patternsNodes.item(i);
                    NodeList children = patternsNode.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        Node next = children.item(j);
                        if (next.getNodeType() == Node.TEXT_NODE) {
                            String names = next.getNodeValue().trim();
                            for (StringTokenizer st = new StringTokenizer(
                                    names);
                                 st.hasMoreTokens();) {
                                patterns.add(st.nextToken());
                            }
                        }
                    }
                }
                NodeList classesNodes = node.getElementsByTagName("classes"); // NOI18N
                for (int i = 0; i < classesNodes.getLength(); ++i) {
                    Element classesNode = (Element) classesNodes.item(i);
                    String package_name = classesNode.getAttribute("package") // NOI18N
                            .trim();
                    NodeList children = classesNode.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        Node next = children.item(j);
                        if (next.getNodeType() == Node.TEXT_NODE) {
                            String names = next.getNodeValue().trim();
                            for (StringTokenizer st = new StringTokenizer(
                                    names);
                                 st.hasMoreTokens();) {
                                String clsName = package_name + "." + // NOI18N
                                        st.nextToken();
                                classes.add(clsName);
                            }
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException(node.toString());
            }
        }//constructor

        public Collection<String> getPackages() {
            ArrayList<String> results = new ArrayList<String>();
            for (Iterator i = apiRefs.iterator(); i.hasNext();) {
                results.addAll(((APIRef) i.next()).deref().getPackages());
            }
            results.addAll(packages);
            return results;
        }

        public Collection<String> getPatterns() {
            ArrayList<String> results = new ArrayList<String>();
            for (Iterator i = apiRefs.iterator(); i.hasNext();) {
                results.addAll(((APIRef) i.next()).deref().getPatterns());
            }
            results.addAll(patterns);
            return results;
        }

        public Collection<String> getClasses() {
            ArrayList<String> results = new ArrayList<String>();
            for (Iterator i = apiRefs.iterator(); i.hasNext();) {
                results.addAll(((APIRef) i.next()).deref().getClasses());
            }
            results.addAll(classes);
            return results;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("<api name_version=\"" + name_version + "\">"); // NOI18N
            sb.append("\n\t<classes>"); // NOI18N
            for (Iterator i = getClasses().iterator(); i.hasNext();) sb.append(
                    "\n\t\t") // NOI18N
                    .append(i.next().toString());
            sb.append("\n\t</classes>"); // NOI18N
            sb.append("\n\t<packages>"); // NOI18N
            for (Iterator i = getPackages().iterator(); i.hasNext();) sb.append(
                    "\n\t\t") // NOI18N
                    .append(i.next().toString());
            sb.append("\n\t</packages>"); // NOI18N
            sb.append("\n\t<patterns>"); // NOI18N
            for (Iterator i = getPatterns().iterator(); i.hasNext();) sb.append(
                    "\n\t\t") // NOI18N
                    .append(i.next().toString());
            sb.append("\n\t</patterns>"); // NOI18N
            sb.append("\n</api>"); // NOI18N
            return sb.toString();
        }
    }//class API

    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
        }
        try {
            APIRepository.Initialize(args[0]);
            APIRepository apiRep = APIRepository.Instance();
            switch(args.length) {
                case 1:
                    System.out.println(apiRep);
                    break;
                case 2:
                    System.out.println(apiRep.apis.get(args[1]));
                    break;
                case 3:
                    System.out.println(apiRep.isClassPartOf(args[2], args[1]));
                    break;
                default:
                    usage();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void usage(){
        System.out.println(
                "Usage: java " + APIRepository.class.getName() + // NOI18N
                " <file_name> [api_name_version] [class_name]"); // NOI18N
        System.out.println("\nExamples:\n"); // NOI18N
        System.out.println(
                "java " + APIRepository.class.getName() + // NOI18N
                " src/standard-apis.xml ejb_jar_2.0 javax.ejb.Timer"); // NOI18N
        System.out.println("The above command prints true if javax.ejb.Timer is part of ejb_api_2.0 API.\n"); // NOI18N
        System.out.println(
                "java " + APIRepository.class.getName() + // NOI18N
                " src/standard-apis.xml ejb_jar_2.0"); // NOI18N
        System.out.println("The above command prints details about all classes and packages for ejb_api_2.0 API.\n"); // NOI18N
        System.out.println(
                "java " + APIRepository.class.getName() + // NOI18N
                " src/standard-apis.xml"); // NOI18N
        System.out.println("The above command prints details about all APIs.\n"); // NOI18N
        System.exit(1);
    }
}//class APIRespository
