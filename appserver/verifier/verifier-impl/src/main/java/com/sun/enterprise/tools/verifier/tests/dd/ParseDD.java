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

package com.sun.enterprise.tools.verifier.tests.dd;

import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.StringManagerHelper;
import com.sun.enterprise.tools.verifier.tests.VerifierTest;
import com.sun.enterprise.tools.verifier.util.LogDomains;
import com.sun.enterprise.tools.verifier.util.XMLValidationHandler;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>
 * This test parses the deployment descriptor using a SAX parser to
 * avoid the dependency on the DOL
 * <p>
 *
 * @author  Sheetal Vartak
 * @version
 */

public class ParseDD extends VerifierTest {
    DocumentBuilder builder = null;
    Result result;
    private com.sun.enterprise.util.LocalStringManagerImpl smh;
    boolean oneFailed = false;
    private static String EJB = "EJB Deployment Descriptor";
    private static String APPCLIENT = "App Client Deployment Descriptor";
    private static String CONNECTOR = "Connector Deployment Descriptor";
    private static String WEB = "Web Deployment Descriptor";

    // Logger to log messages
    private static Logger logger = LogDomains.getLogger(LogDomains.AVK_VERIFIER_LOGGER);


    /**
      * <p>
      * This test parses the deployment descriptor using a SAX parser to
      * avoid the dependency on the DOL
      * <p>
      */

    public ParseDD () {
	result = getInitializedResult();
	smh = StringManagerHelper.getLocalStringsManager();
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	try {
	     factory.setAttribute("http://apache.org/xml/features/allow-java-encodings", new Boolean(true)); 
	    builder = factory.newDocumentBuilder();
	    EntityResolver dh = new XMLValidationHandler(false);
	    builder.setEntityResolver(dh);
	} catch (ParserConfigurationException e) {
	    logger.log(Level.SEVERE, getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".Exception",
			   "Exception : {0}",
			   new Object[] {e.toString()}));
	}catch (Exception e) {
            logger.log(Level.SEVERE,getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".Exception",
			   "Exception : {0}",
			   new Object[] {e.toString()}));
	}
    }


    /**
     * Parse validate the XML file
     *
     * @param source Input XML to be checked
     *
     */
    public Result validateConnectorDescriptor(InputStream source)
    {
	Document document = null;
	result = getInitializedResult();
	result.setComponentName(EJB);
	NodeList nodeList = null;
	try {
	    if (source == null) {
		logger.log(Level.SEVERE,getClass().getName() + ".srcnull");
		result.failed
		    (smh.getLocalString
		     (getClass().getName() + ".NoIO",
		      "no InputStream found",
		      new Object[] {}));
		return result;
	    }
	    else {
		document = builder.parse(source);

		//license-required
		nodeList = document.getElementsByTagName("license-required");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("true")
			    && !value.equals("false")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedLicense",
				  "[Connector] license-required cannot be {0}. It has to be either true or false.",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedLicense",
				  "PASSED[Connector]  : license-required is {0}.",
				  new Object[] {value}));
			}
		    }
		}

		//reauthentication-support
		nodeList = document.getElementsByTagName("reauthentication-support");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("true")
			    && !value.equals("false")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedReauthenticationSupport",
				  "[Connector] reauthentication-support cannot be {0}. It has to be either true or false.",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedReauthenticationSupport",
				  "PASSED [Connector] : reauthentication-support is {0}.",
				  new Object[] {value}));
			}
		    }
		}

		//transaction-support
		nodeList = document.getElementsByTagName("transaction-support");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("NoTransaction")
			    && !value.equals("LocalTransaction")
			    && !value.equals("XATransaction")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedTransactionSupport",
				  "[Connector]transaction-support cannot be {0}. It has to be either NoTransaction or LocalTransaction or XATransaction.",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedTransactionSupport",
				  "PASSED [Connector] : transaction-support is {0}.",
				  new Object[] {value}));
			}
		    }
		}
		if (result.getStatus() != Result.FAILED) {
		    result.setStatus(Result.PASSED);
		}
		return result;
	    }
	} catch (IOException e) {
            logger.log(Level.SEVERE,getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed
		(smh.getLocalString
		 (getClass().getName() + ".Exception",
		  "Exception : {0}",
		  new Object[] {e.toString()}));
	    return result;
	} catch (SAXException e) {
            logger.log(Level.SEVERE,getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed
		(smh.getLocalString
		 (getClass().getName() + ".Exception",
		  "Exception : {0}",
		  new Object[] {e.toString()}));
	    return result;
	} catch (Exception e) {
           logger.log(Level.SEVERE,getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed
		(smh.getLocalString
		 (getClass().getName() + ".Exception",
		  "Exception : {0}",
		  new Object[] {e.toString()}));
	    return result;
	}
    }

    /**
     * Parse validate the XML file
     *
     * @param source Input XML to be checked
     *
     */
    public Result validateAppClientDescriptor(InputStream source)
    {
	Document document = null;
	result = getInitializedResult();
	result.setComponentName(EJB);
	NodeList nodeList = null;
	try {
	    if (source == null) {
		logger.log(Level.SEVERE,getClass().getName() + ".srcnull");
		result.failed
		    (smh.getLocalString
		     (getClass().getName() + ".NoIO",
		      "no InputStream found",
		      new Object[] {}));
		return result;
	    }
	    else {
		document = builder.parse(source);

		//ejb-ref-type
		nodeList = document.getElementsByTagName("ejb-ref-type");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String ejbRefTypeValue = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!ejbRefTypeValue.equals(EjbSessionDescriptor.TYPE)
			    && !ejbRefTypeValue.equals(EjbEntityDescriptor.TYPE)) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedAppClientEjbRefType",
				  "[App Client] ejb-ref-type cannot be {0}. It has to be either Entity or Session.",
				  new Object[] {ejbRefTypeValue}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedAppClientEjbRefType",
				  "PASSED [App Client] : ejb-ref-type is {0}.",
				  new Object[] {ejbRefTypeValue}));
			}
		    }
		}

		//res-auth
		nodeList = document.getElementsByTagName("res-auth");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("Application")
			    && !value.equals("Container")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedAppClientResAuth",
				  "[App Client] res-auth cannot be {0}. It has to be either Application or Container",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedAppClientResAuth",
				  "PASSED [App Client] : res-auth is {0}.",
				  new Object[] {value}));
			}
		    }
		}

		//res-sharing-scope
		nodeList = document.getElementsByTagName("res-sharing-scope");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("Shareable")
			    && !value.equals("Unshareable")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedAppClientResSharingScope",
				  "[App Client] res-sharing-scope cannot be {0}. It has to be either Shareable or Unshareable",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedAppClientResSharingScope",
				  "PASSED [App Client] : res-sharing-scope is {0}.",
				  new Object[] {value}));
			}
		    }
		}

		if (result.getStatus() != Result.FAILED) {
		    result.setStatus(Result.PASSED);
		}
		return result;
	    }
	} catch (IOException e) {
	    logger.log(Level.SEVERE, getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed
		(smh.getLocalString
		 (getClass().getName() + ".Exception",
		  "Exception : {0}",
		  new Object[] {e.toString()}));
	    return result;
	} catch (SAXException e) {
           logger.log(Level.SEVERE,getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed
		(smh.getLocalString
		 (getClass().getName() + ".Exception",
		  "Exception : {0}",
		  new Object[] {e.toString()}));
	    return result;
	} catch (Exception e) {
           logger.log(Level.SEVERE,getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed
		(smh.getLocalString
		 (getClass().getName() + ".Exception",
		  "Exception : {0}",
		  new Object[] {e.toString()}));
	    return result;
	}
    }

    /**
     * Parse validate the XML file
     *
     * @param source Input XML to be checked
     *
     */
    public Result validateWebDescriptor(InputStream source)
    {
	Document document = null;
	result = getInitializedResult();
	result.setComponentName(EJB);
	NodeList nodeList = null;
	try {
	    if (source == null) {
		logger.log(Level.SEVERE,getClass().getName() + ".srcnull");
		result.failed
		    (smh.getLocalString
		     (getClass().getName() + ".NoIO",
		      "no InputStream found",
		      new Object[] {}));
		return result;
	    }
	    else {
		document = builder.parse(source);

		//ejb-ref-type
		nodeList = document.getElementsByTagName("ejb-ref-type");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String ejbRefTypeValue = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!ejbRefTypeValue.equals(EjbSessionDescriptor.TYPE)
			    && !ejbRefTypeValue.equals(EjbEntityDescriptor.TYPE)) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedWebEjbRefType",
				  "[Web] ejb-ref-type cannot be {0}. It has to be either Entity or Session.",
				  new Object[] {ejbRefTypeValue}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedWebEjbRefType",
				  "PASSED [Web] : ejb-ref-type is {0}.",
				  new Object[] {ejbRefTypeValue}));
			}
		    }
		}

		//res-auth
		nodeList = document.getElementsByTagName("res-auth");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("Application")
			    && !value.equals("Container")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedWebResAuth",
				  "[Web] res-auth cannot be {0}. It has to be either Application or Container",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedWebResAuth",
				  "PASSED[Web]  : res-auth is {0}.",
				  new Object[] {value}));
			}
		    }
		}

		//res-sharing-scope
		nodeList = document.getElementsByTagName("res-sharing-scope");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("Shareable")
			    && !value.equals("Unshareable")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedWebResSharingScope",
				  "[Web] res-sharing-scope cannot be {0}. It has to be either Shareable or Unshareable",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedWebResSharingScope",
				  "PASSED [Web] : res-sharing-scope is {0}.",
				  new Object[] {value}));
			}
		    }
		}

		if (result.getStatus() != Result.FAILED) {
		    result.setStatus(Result.PASSED);
		}
		return result;
	    }
	} catch (IOException e) {
	    logger.log(Level.SEVERE, getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed
		(smh.getLocalString
		 (getClass().getName() + ".Exception",
		  "Exception : {0}",
		  new Object[] {e.toString()}));
	    return result;
	} catch (SAXException e) {
	    logger.log(Level.SEVERE, getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed
		(smh.getLocalString
		 (getClass().getName() + ".Exception",
		  "Exception : {0}",
		  new Object[] {e.toString()}));
	    return result;
	} catch (Exception e) {
	    logger.log(Level.SEVERE, getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed
		(smh.getLocalString
		 (getClass().getName() + ".Exception",
		  "Exception : {0}",
		  new Object[] {e.toString()}));
	    return result;
	}
    }


    /**
     * Parse validate the XML file
     *
     * @param source Input XML to be checked
     *
     */
    public Result validateEJBDescriptor(InputStream source)
    {
	Document document = null;
	result = getInitializedResult();
	result.setComponentName(EJB);
	NodeList nodeList = null;
	try {
	    if (source == null) {
		logger.log(Level.SEVERE, getClass().getName() + ".srcnull");
		result.failed
		    (smh.getLocalString
		     (getClass().getName() + ".NoIO",
		      "no InputStream found",
		      new Object[] {}));
		return result;
	    }
	    else {
		document = builder.parse(source);

		checkInterfacePairs(document, "remote", "home");
		checkInterfacePairs(document, "local", "local-home");

		//session-type
		nodeList = document.getElementsByTagName("session-type");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String sessionTypeValue = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!sessionTypeValue.equals(EjbSessionDescriptor.STATELESS)
			    && !sessionTypeValue.equals(EjbSessionDescriptor.STATEFUL)) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedSession",
				  "session-type cannot be {0}. It has to be either Stateless or Stateful.",
				  new Object[] {sessionTypeValue}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedSession",
				  "PASSED : session-type is {0}.",
				  new Object[] {sessionTypeValue}));
			}

		    }
		}

		//ejb-ref-type
		nodeList = document.getElementsByTagName("ejb-ref-type");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String ejbRefTypeValue = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!ejbRefTypeValue.equals(EjbSessionDescriptor.TYPE)
			    && !ejbRefTypeValue.equals(EjbEntityDescriptor.TYPE)) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedEjbRefType",
				  "[EJB] ejb-ref-type cannot be {0}. It has to be either Entity or Session.",
				  new Object[] {ejbRefTypeValue}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedEjbRefType",
				  "PASSED [EJB] : ejb-ref-type is {0}.",
				  new Object[] {ejbRefTypeValue}));
			}

		    }
		}

		//multiplicity
		nodeList = document.getElementsByTagName("multiplicity");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String multiplicityValue = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!multiplicityValue.equals("One")
			    && !multiplicityValue.equals("Many")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedMultiplicity",
				  "[EJB] multiplicity cannot be {0}. It has to be either One or Many",
				  new Object[] {multiplicityValue}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedMultiplicity",
				  "PASSED [EJB] : multiplicity is {0}.",
				  new Object[] {multiplicityValue}));
			}

		    }
		}

		//cmp-version
		nodeList = document.getElementsByTagName("cmp-version");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("1.x")
			    && !value.equals("2.x")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedVersion",
				  "[EJB] version cannot be {0}. It has to be either 1.x or 2.x",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedVersion",
				  "PASSED [EJB] : version is {0}.",
				  new Object[] {value}));
			}

		    }
		}

		//destination-type
		nodeList = document.getElementsByTagName("destination-type");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("javax.jms.Queue")
			    && !value.equals("javax.jms.Topic")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedDestinationType",
				  "[EJB] destination-type cannot be {0}. It has to be either javax.jms.Topic or javax.jms.Queue",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedDestinationType",
				  "PASSED [EJB] : destination-type is {0}.",
				  new Object[] {value}));
			}
		    }
		}
		//method-intf
		nodeList = document.getElementsByTagName("method-intf");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("Home")
			    && !value.equals("Remote")
			    && !value.equals("LocalHome")
			    && !value.equals("Local")
			    && !value.equals("ServiceEndpoint")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedMethodIntf",
				  "[EJB] method-intf cannot be [ {0} ]. It has to be either Local, Remote, LocalHome or Home or ServiceEndpoint",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedMethodIntf",
				  "PASSED [EJB] : method-intf is {0}.",
				  new Object[] {value}));
			}
		    }
		}
		//persistence-type
		nodeList = document.getElementsByTagName("persistence-type");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("Bean")
			    && !value.equals("Container")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedPersistenceType",
				  "[EJB] persistence-type cannot be {0}. It has to be either Bean or Container",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedPersistenceType",
				  "PASSED [EJB] : persistence-type is {0}.",
				  new Object[] {value}));
			}
		    }
		}

		//reentrant
		nodeList = document.getElementsByTagName("reentrant");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("True")
			    && !value.equals("False")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedReentrant",
				  "[EJB] reentrant cannot be {0}. It has to be either True or False",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedReentrant",
				  "PASSED [EJB] : reentrant is {0}.",
				  new Object[] {value}));
			}
		    }
		}

		//res-auth
		nodeList = document.getElementsByTagName("res-auth");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("Application")
			    && !value.equals("Container")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedEjbResAuth",
				  "[EJB] res-auth cannot be {0}. It has to be either Application or Container",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedEjbResAuth",
				  "PASSED [EJB] : res-auth is {0}.",
				  new Object[] {value}));
			}
		    }
		}

		//result-type-mapping
		nodeList = document.getElementsByTagName("result-type-mapping");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("Local")
			    && !value.equals("Remote")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedResultTypeMapping",
				  "[EJB] result-type-mapping cannot be {0}. It has to be either Remote or Local",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedResultTypeMapping",
				  "PASSED [EJB] : result-type-mapping is {0}.",
				  new Object[] {value}));
			}
		    }
		}

		//subscription-durability
		nodeList = document.getElementsByTagName("subscription-durability");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("Durable")
			    && !value.equals("NonDurable")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedSubscriptionDurability",
				  "[EJB] subscription-durability cannot be {0}. It has to be either Durable or NonDurable",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedSubscriptionDurability",
				  "PASSED [EJB] : subscription-durability is {0}.",
				  new Object[] {value}));
			}
		    }
		}
		//trans-attribute
		nodeList = document.getElementsByTagName("trans-attribute");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("NotSupported")
			    && !value.equals("Supports")
			    && !value.equals("Required")
			    && !value.equals("RequiresNew")
			    && !value.equals("Mandatory")
			    && !value.equals("Never")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedTransAttribute",
				  "[EJB] trans-attribute cannot be {0}. It has to be either NotSupported or Supports or Required or RequiresNew or Mandatory or Never.",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedTransAttribute",
				  "PASSED [EJB]: trans-attribute is {0}.",
				  new Object[] {value}));
			}
		    }
		}

		//transaction-type
		nodeList = document.getElementsByTagName("transaction-type");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("Bean")
			    && !value.equals("Container")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedTransactionType",
				  "[EJB] transaction-type cannot be {0}. It has to be either Bean or Container",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedTransactionType",
				  "PASSED [EJB]: transaction-type is {0}.",
				  new Object[] {value}));
			}
		    }
		}

		//acknowledge-mode
		nodeList = document.getElementsByTagName("acknowledge-mode");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("Auto-acknowledge")
			    && !value.equals("Dups-ok-acknowledge")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedAcknowledgeMode",
				  "[EJB] acknowledge-mode cannot be {0}. It has to be either True or False",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedAcknowledgeMode",
				  "PASSED [EJB]: acknowledge-mode is {0}.",
				  new Object[] {value}));
			}
		    }
		}

		//res-sharing-scope
		nodeList = document.getElementsByTagName("res-sharing-scope");
		if (nodeList != null) {
		    for (int i = 0; i < nodeList.getLength(); i++) {
			String value = ((Text)nodeList.item(i).getFirstChild()).getNodeValue();
			if (!value.equals("Shareable")
			    && !value.equals("Unshareable")) {
			    result.failed
				(smh.getLocalString
				 (getClass().getName() + ".failedEjbResSharingScope",
				  "[EJB] res-sharing-scope cannot be {0}. It has to be either Shareable or Unshareable",
				  new Object[] {value}));
			    oneFailed = true;
			}
			else {
			    result.addGoodDetails
				(smh.getLocalString
				 (getClass().getName() + ".passedEjbResSharingScope",
				  "PASSED [EJB] : res-sharing-scope is {0}.",
				  new Object[] {value}));
			}
		    }
		}


		if (result.getStatus() != Result.FAILED) {
		    result.setStatus(Result.PASSED);
		}
		return result;
	    }
	} catch (IOException e) {
	    logger.log(Level.SEVERE, getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed
		(smh.getLocalString
		 (getClass().getName() + ".Exception",
		  "Exception : {0}",
		  new Object[] {e.toString()}));
	    return result;
	} catch (SAXException e) {
	    logger.log(Level.SEVERE,getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed
		(smh.getLocalString
		 (getClass().getName() + ".Exception",
		  "Exception : {0}",
		  new Object[] {e.toString()}));
	    return result;
	} catch (Exception e) {
	    logger.log(Level.SEVERE, getClass().getName() + ".Exception",
				new Object[] {e.toString()});
	    result.failed
		(smh.getLocalString
		 (getClass().getName() + ".Exception",
		  "Exception : {0}",
		  new Object[] {e.toString()}));
	    return result;
	}
    }

    public void checkInterfacePairs(Document document, String intf1, String intf2) {
        //check if "local" and "local-home" are both present
        //similarly for "remote" and "remote-home"

        int length1 = 0;
	int length2 = 0;
        NodeList nodeList = document.getElementsByTagName(intf1.trim());
	if (nodeList != null) {
	    length1 = nodeList.getLength();

	}
	nodeList = document.getElementsByTagName(intf2.trim());
	if (nodeList != null) {
	    length2 = nodeList.getLength();

	}
	
	if (length1 == length2) {
	    if (length1 != 0) {
	        result.addGoodDetails
		  (smh.getLocalString
		   (getClass().getName() + ".passedPairs",
		    "PASSED [EJB] : [ {0} ] and [ {1} ] tags present.",
		    new Object[] {intf1, intf2}));
	    }
	} else {
	    result.failed
	      (smh.getLocalString
	       (getClass().getName() + ".failedPairs",
		"FAILED [EJB] : Either one of the [ {0} ] : [ {1} ] tag pair is not present.",
		new Object[] {intf1, intf2}));
	}
	
    }

}
