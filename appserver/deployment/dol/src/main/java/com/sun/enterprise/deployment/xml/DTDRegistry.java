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
// Portions Copyright [2018] Payara Foundation and/or affiliates

// START OF IASRI 4661135

package com.sun.enterprise.deployment.xml;

public final class DTDRegistry {

     public static final String Package = "com.sun.enterprise.deployment.xml";

     public static final String DTD_LOCATION= System.getProperty("com.sun.aas.installRoot") + java.io.File.separator + "lib" + java.io.File.separator + "dtds";
     
     public static final String SCHEMA_LOCATION=System.getProperty("com.sun.aas.installRoot") + java.io.File.separator + "lib" + java.io.File.separator + "schemas";

     // Standard DTDs
    
     public static final String APPLICATION_13_DTD_PUBLIC_ID =
         "-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN";
     public static final String APPLICATION_13_DTD_SYSTEM_ID =
        "http://java.sun.com/dtd/application_1_3.dtd";

     public static final String APPLICATION_12_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD J2EE Application 1.2//EN";
     public static final String APPLICATION_12_DTD_SYSTEM_ID =
        "http://java.sun.com/dtd/application_1_2.dtd";

     public static final String EJBJAR_20_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN";
     public static final String EJBJAR_20_DTD_SYSTEM_ID =
        "http://java.sun.com/dtd/ejb-jar_2_0.dtd";

    public static final String EJBJAR_11_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";
    public static final String EJBJAR_11_DTD_SYSTEM_ID =
        "http://java.sun.com/dtd/ejb-jar_1_1.dtd";

    public static final String APPCLIENT_13_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.3//EN";
    public static final String APPCLIENT_13_DTD_SYSTEM_ID =
        "http://java.sun.com/dtd/application-client_1_3.dtd";

    public static final String APPCLIENT_12_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.2//EN";
    public static final String APPCLIENT_12_DTD_SYSTEM_ID =
        "http://java.sun.com/dtd/application-client_1_2.dtd";

    public static final String CONNECTOR_10_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Connector 1.0//EN";
    public static final String CONNECTOR_10_DTD_SYSTEM_ID =
        "http://java.sun.com/dtd/connector_1_0.dtd";

    public static final String WEBAPP_23_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";
    public static final String WEBAPP_23_DTD_SYSTEM_ID =
        "http://java.sun.com/dtd/web-app_2_3.dtd";

    public static final String WEBAPP_22_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";
    public static final String WEBAPP_22_DTD_SYSTEM_ID =
        "http://java.sun.com/dtd/web-app_2_2.dtd";

    public static final String TAGLIB_12_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.2//EN";
    public static final String TAGLIB_12_DTD_SYSTEM_ID =
        "http://java.sun.com/dtd/web-jsptaglibrary_1_2.dtd";

    public static final String TAGLIB_11_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.1//EN";
    public static final String TAGLIB_11_DTD_SYSTEM_ID =
        "http://java.sun.com/dtd/web-jsptaglibrary_1_1.dtd";


    //SunONE specific dtds

    /**
     * Application: Sun ONE App Server specific dtd info.
     */
    public static final String SUN_APPLICATION_130_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 7.0 J2EE Application 1.3//EN";
    public static final String SUN_APPLICATION_130_DTD_SYSTEM_ID =
        "http://www.sun.com/software/sunone/appserver/dtds/sun-application_1_3-0.dtd";
    public static final String SUN_APPLICATION_140beta_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 8.0 J2EE Application 1.4//EN";
    public static final String SUN_APPLICATION_140beta_DTD_SYSTEM_ID =
        "http://www.sun.com/software/sunone/appserver/dtds/sun-application_1_4-0.dtd";    
    public static final String SUN_APPLICATION_140_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 8.0 J2EE Application 1.4//EN";
    public static final String SUN_APPLICATION_140_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-application_1_4-0.dtd";    
    public static final String SUN_APPLICATION_141_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 8.1 J2EE Application 1.4//EN";
    public static final String SUN_APPLICATION_141_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-application_1_4-0.dtd";    
    public static final String SUN_APPLICATION_500_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 9.0 Java EE Application 5.0//EN";
    public static final String SUN_APPLICATION_500_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-application_5_0-0.dtd";        
    public static final String SUN_APPLICATION_600_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD GlassFish Application Server 3.0 Java EE Application 6.0//EN";
    public static final String SUN_APPLICATION_600_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-application_6_0-0.dtd"; 

    /**
     * EJB: Sun ONE App Server specific dtd info.
     */
    public static final String SUN_EJBJAR_200_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 7.0 EJB 2.0//EN";
    public static final String SUN_EJBJAR_200_DTD_SYSTEM_ID =
        "http://www.sun.com/software/sunone/appserver/dtds/sun-ejb-jar_2_0-0.dtd";
    public static final String SUN_EJBJAR_201_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 7.1 EJB 2.0//EN";
    public static final String SUN_EJBJAR_201_DTD_SYSTEM_ID =
        "http://www.sun.com/software/sunone/appserver/dtds/sun-ejb-jar_2_0-1.dtd";
    public static final String SUN_EJBJAR_210beta_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 8.0 EJB 2.1//EN";
    public static final String SUN_EJBJAR_210beta_DTD_SYSTEM_ID =
        "http://www.sun.com/software/sunone/appserver/dtds/sun-ejb-jar_2_1-0.dtd";    
    public static final String SUN_EJBJAR_210_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 8.0 EJB 2.1//EN";
    public static final String SUN_EJBJAR_210_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-ejb-jar_2_1-0.dtd";    
    public static final String SUN_EJBJAR_211_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 8.1 EJB 2.1//EN";
    public static final String SUN_EJBJAR_211_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-ejb-jar_2_1-1.dtd";    
    public static final String SUN_EJBJAR_300_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 9.0 EJB 3.0//EN";
    public static final String SUN_EJBJAR_300_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-ejb-jar_3_0-0.dtd";
    
    public static final String SUN_EJBJAR_310_DTD_PUBLIC_ID =
           "-//Sun Microsystems, Inc.//DTD GlassFish Application Server 3.0 EJB 3.1//EN";
    public static final String SUN_EJBJAR_310_DTD_SYSTEM_ID =
           "http://www.sun.com/software/appserver/dtds/sun-ejb-jar_3_1-0.dtd";


    /**
     * Application Client: Sun ONE App Server specific dtd info.
     */
    public static final String SUN_APPCLIENT_130_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 7.0 Application Client 1.3//EN";
    public static final String SUN_APPCLIENT_130_DTD_SYSTEM_ID =
        "http://www.sun.com/software/sunone/appserver/dtds/sun-application-client_1_3-0.dtd";
    public static final String SUN_APPCLIENT_140beta_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 8.0 Application Client 1.4//EN";
    public static final String SUN_APPCLIENT_140beta_DTD_SYSTEM_ID =
        "http://www.sun.com/software/sunone/appserver/dtds/sun-application-client_1_4-0.dtd";    
    public static final String SUN_APPCLIENT_140_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 8.0 Application Client 1.4//EN";
    public static final String SUN_APPCLIENT_140_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-application-client_1_4-0.dtd";    
    public static final String SUN_APPCLIENT_141_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 8.1 Application Client 1.4//EN";
    public static final String SUN_APPCLIENT_141_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-application-client_1_4-1.dtd";
    public static final String SUN_APPCLIENT_500_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 9.0 Application Client 5.0//EN";
    public static final String SUN_APPCLIENT_500_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-application-client_5_0-0.dtd";
    public static final String SUN_APPCLIENT_600_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD GlassFish Application Server 3.0 Application Client 6.0//EN";
    public static final String SUN_APPCLIENT_600_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-application-client_6_0-0.dtd";

    /**
     * Connectors: Sun ONE App Server specific dtd info.
     */
    public static final String SUN_CONNECTOR_100_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 7.0 Connector 1.0//EN";
    public static final String SUN_CONNECTOR_100_DTD_SYSTEM_ID =
        "http://www.sun.com/software/sunone/appserver/dtds/sun-connector_1_0-0.dtd";

    /**
     * Web: Sun ONE App Server specific dtd info.
     */
    public static final String SUN_WEBAPP_230_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 7.0 Servlet 2.3//EN";
    public static final String SUN_WEBAPP_230_DTD_SYSTEM_ID =
        "http://www.sun.com/software/sunone/appserver/dtds/sun-web-app_2_3-0.dtd";

    public static final String SUN_WEBAPP_231_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 7.1 Servlet 2.3//EN";
    public static final String SUN_WEBAPP_231_DTD_SYSTEM_ID =
        "http://www.sun.com/software/sunone/appserver/dtds/sun-web-app_2_3-1.dtd";

    public static final String SUN_WEBAPP_240beta_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 8.0 Servlet 2.4//EN";	
    public static final String SUN_WEBAPP_240beta_DTD_SYSTEM_ID =
        "http://www.sun.com/software/sunone/appserver/dtds/sun-web-app_2_4-0.dtd";	

    public static final String SUN_WEBAPP_240_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 8.0 Servlet 2.4//EN";	
    public static final String SUN_WEBAPP_240_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-web-app_2_4-0.dtd";

    public static final String SUN_WEBAPP_241_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 8.1 Servlet 2.4//EN";	
    public static final String SUN_WEBAPP_241_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-web-app_2_4-1.dtd";

    public static final String SUN_WEBAPP_250_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 9.0 Servlet 2.5//EN";	
    public static final String SUN_WEBAPP_250_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-web-app_2_5-0.dtd";

    public static final String SUN_WEBAPP_300_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD GlassFish Application Server 3.0 Servlet 3.0//EN";
    public static final String SUN_WEBAPP_300_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-web-app_3_0-0.dtd";
    
    /**
     * Application Client Container: Sun ONE App Server specific dtd info.
     */
    public static final String SUN_CLIENTCONTAINER_700_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 7.0 Application Client Container 1.0//EN";
    public static final String SUN_CLIENTCONTAINER_700_DTD_SYSTEM_ID =
        "http://www.sun.com/software/sunone/appserver/dtds/sun-application-client-container_1_0.dtd";

    //4690447-adding it for sun-cmp-mapping.xml
    /**
     * EJB CMP Mapping : Sun ONE App Server specific dtd info.
     */
    public static final String SUN_CMP_MAPPING_700_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 7.0 OR Mapping //EN";
    public static final String SUN_CMP_MAPPING_700_DTD_SYSTEM_ID =
        "http://www.sun.com/software/sunone/appserver/dtds/sun-cmp-mapping_1_0.dtd";

    public static final String SUN_CMP_MAPPING_800_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 8.0 OR Mapping//EN";
    public static final String SUN_CMP_MAPPING_800_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-cmp-mapping_1_1.dtd";

    public static final String SUN_CMP_MAPPING_810_DTD_PUBLIC_ID =
        "-//Sun Microsystems, Inc.//DTD Application Server 8.1 OR Mapping//EN";
    public static final String SUN_CMP_MAPPING_810_DTD_SYSTEM_ID =
        "http://www.sun.com/software/appserver/dtds/sun-cmp-mapping_1_2.dtd";

    public static final String GF_WEBAPP_301_DTD_PUBLIC_ID = 
        "-//GlassFish.org//DTD GlassFish Application Server 3.1 Servlet 3.0//EN";
    public static final String GF_WEBAPP_301_DTD_SYSTEM_ID = 
        "http://glassfish.org/dtds/glassfish-web-app_3_0-1.dtd";

    public static final String GF_APPLICATION_601_DTD_PUBLIC_ID =
        "-//GlassFish.org//DTD GlassFish Application Server 3.1 Java EE Application 6.0//EN";
    public static final String GF_APPLICATION_601_DTD_SYSTEM_ID =
        "http://glassfish.org/dtds/glassfish-application_6_0-1.dtd";

    public static final String GF_APPCLIENT_602_DTD_PUBLIC_ID =
        "-//GlassFish.org//DTD GlassFish Application Server 3.1 Java EE Application Client 6.0//EN";
    public static final String GF_APPCLIENT_602_DTD_SYSTEM_ID =
        "http://glassfish.org/dtds/glassfish-application-client_6_0-2.dtd";

    public static final String GF_EJBJAR_311_DTD_PUBLIC_ID =
        "-//GlassFish.org//DTD GlassFish Application Server 3.1 EJB 3.1//EN";
    public static final String GF_EJBJAR_311_DTD_SYSTEM_ID =
        "http://glassfish.org/dtds/glassfish-ejb-jar_3_1-1.dtd";
    
    
    // Payara DTDs
    
    public static final String PAYARA_WEBAPP_4_DTD_PUBLIC_ID = "-//Payara.fish//DTD Payara Server 4 Servlet 3.0//EN";
    public static final String PAYARA_WEBAPP_4_DTD_SYSTEM_ID = "https://docs.payara.fish/schemas/payara-web-app_4.dtd";
    
}

// END OF IASRI 4661135
