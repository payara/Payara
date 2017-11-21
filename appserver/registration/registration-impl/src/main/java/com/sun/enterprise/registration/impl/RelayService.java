/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2016 Oracle and/or its affiliates. All rights reserved.
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


package com.sun.enterprise.registration.impl;
import java.util.*;
import java.io.*;
import java.text.*;

import com.sun.enterprise.registration.RegistrationException;
import com.sun.enterprise.registration.impl.environment.EnvironmentInformation;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Formatter;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class RelayService {

    private static final Logger logger = RegistrationLogger.getLogger();
    private static final String ENV_TOKEN   =   "@@@ENVIRONMENT@@@";
    private static final String TAG_TOKEN   =   "@@@SERVICE_TAGS@@@";
    private static final String PRODUCTNAME_TOKEN =   "@@@PRODUCTNAME@@@";
    private static final String TEMPLATE_FILE = "com/sun/enterprise/registration/impl/relay-template.html";
    private static final String STRING_TOKEN =   "@@@STRING.";
    private static final String END_TOKEN =   "@@@";

    private RepositoryManager rm;
    private ResourceBundle bundle;

    public RelayService(File repositoryFile) throws RegistrationException {
        rm = new RepositoryManager(repositoryFile);
        // make sure runtime values are generated in RepositoryManager
        rm.updateRuntimeValues();
    }

    public RelayService(String repositoryFile) throws RegistrationException {
        this(new File(repositoryFile));
    }

    public void generateRegistrationPage(String outputFile) throws Exception {
        generateRegistrationPage(outputFile, Locale.getDefault());
    }
    
    public void generateRegistrationPage(String outputFile, Locale locale) throws Exception {
        bundle = getResourceBundle(locale);
        InputStream is = getClass().getClassLoader().getResourceAsStream(TEMPLATE_FILE);
        if (is == null)
            throw new RegistrationException("Template file [" + TEMPLATE_FILE + "] not found");

        List<ServiceTag> serviceTags = rm.getServiceTags();
        StringBuilder productName = new StringBuilder();
        for (ServiceTag tag : serviceTags) {
            if (productName.length() > 0)
                productName = productName.append(" + ");
            productName = productName.append(tag.getSource());
        }
        
        String tags = getHtml(serviceTags);
        String env = getEnvironmentInformation();

        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            bw = new BufferedWriter(new FileWriter(outputFile));

            String line;
            while ((line = br.readLine())!= null) {
                if (line.indexOf(ENV_TOKEN) >= 0)
                    line = line.replaceAll(ENV_TOKEN, env);
                if (line.indexOf(TAG_TOKEN) >= 0)
                    line = line.replaceAll(TAG_TOKEN, tags);
                if (line.indexOf(PRODUCTNAME_TOKEN) >= 0)
                    line = line.replaceAll(PRODUCTNAME_TOKEN, productName.toString());
                line = replaceStringTokens(line);
                bw.write(line);
                bw.newLine();
            }
            bw.flush();
        } 
        finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ioex) {}
            }

            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ioex) {}
            }
        }
    }

    private String replaceStringTokens(String line) {
        int start = 0, end = 0;
        StringBuffer buf = new StringBuffer("");

        while (start != -1) {
            // Find start of token
            start = line.indexOf(STRING_TOKEN, end);
            if (start != -1) {
                // copy the stuff before the start
                buf.append(line.substring(end, start));

                // Move past the @@@
                start += STRING_TOKEN.length();

                // Find end of token
                end = line.indexOf(END_TOKEN, start);
                if (end != -1) {
                    try {
                        // Copy the token value to the buffer
                        buf.append(
                                bundle.getString(line.substring(start, end)));
                    } catch (MissingResourceException ex) {
                        // Unable to find the resource, so we don't do anything
                        buf.append(STRING_TOKEN + line.substring(start, end) + END_TOKEN);
                    }

                    // Move past the %%%
                    end += END_TOKEN.length();
                } else {
                    // Add back the %%% because we didn't find a matching end
                    buf.append(END_TOKEN);

                    // Reset end so we can copy the remainder of the text
                    end = start;
                }
            }
        }

        // Copy the remainder of the text
        buf.append(line.substring(end));

        // Return the new String
        return buf.toString();

    }

    /**
     * <p> This method returns the resource bundle for localized Strings </p>
     *
     * @param    locale    The Locale to be used.
     */
    private ResourceBundle getResourceBundle(Locale locale) {
        return ResourceBundle.getBundle(
            "com.sun.enterprise.registration.impl.LocalStrings", locale);
    }


    private String getHtml(List<ServiceTag> serviceTags) {
        if (serviceTags.isEmpty()) {
            logger.log(Level.WARNING, "No tags found");
            return "";
        }
        StringBuilder tags = new StringBuilder();
        for (ServiceTag serviceTag : serviceTags) {
            tags.append(getHtml(serviceTag));
        }
        return tags.toString();
    }


    private String  getEnvironmentInformation() throws RegistrationException {
        StringBuilder html = new StringBuilder();
        EnvironmentInformation se = new EnvironmentInformation();

        Formatter fmt = new Formatter(html);

        html.append("<environment>");
        fmt.format("<hostname>%s</hostname>", se.getHostname());
        fmt.format("<hostId>%s</hostId>",se.getHostId());
        fmt.format("<osName>%s</osName>",se.getOsName());
        fmt.format("<osVersion>%s</osVersion>",se.getOsVersion());
        fmt.format("<osArchitecture>%s</osArchitecture>",se.getOsArchitecture());
        fmt.format("<systemModel>%s</systemModel>",se.getSystemModel());
        fmt.format("<systemManufacturer>%s</systemManufacturer>",se.getSystemManufacturer());
        fmt.format("<cpuManufacturer>%s</cpuManufacturer>",se.getCpuManufacturer());
        fmt.format("<serialNumber>%s</serialNumber>",se.getSerialNumber());

        addNumericTag(fmt, "physmem", se.getPhysMem());
        html.append("<cpuinfo>");
        addNumericTag(fmt, "sockets", se.getSockets());
        addNumericTag(fmt, "cores", se.getCores());
        addNumericTag(fmt, "virtcpus", se.getVirtCpus());
        fmt.format("<name>%s</name>", se.getCpuName());
        addNumericTag(fmt, "clockrate", se.getClockRate());
        html.append("</cpuinfo>");
        html.append("</environment>");
        return html.toString();
    }

    private void addNumericTag(Formatter fmt, String tag, String value) {
       try {
            int i = Integer.parseInt(value);
            fmt.format("<" + tag + ">%s</" + tag + ">", i);
        } catch (Exception ex) {
            //ignore.
        }
    }
    
    private String getHtml(ServiceTag tag) {
        StringBuilder html = new StringBuilder();
        Formatter fmt = new Formatter(html);
        fmt.format("<service_tag>");
        fmt.format("<instance_urn>%s</instance_urn>",tag.getInstanceURN());
        fmt.format("<product_name>%s</product_name>",tag.getProductName());
        fmt.format("<product_version>%s</product_version>",tag.getProductVersion());
        fmt.format("<product_urn>%s</product_urn>",tag.getProductURN());
        fmt.format("<product_parent_urn/>");
        fmt.format("<product_parent>%s</product_parent>",tag.getProductParent());
        fmt.format("<product_defined_inst_id>%s</product_defined_inst_id>",tag.getProductDefinedInstID());
        fmt.format("<product_vendor>%s</product_vendor>",tag.getProductVendor());
        fmt.format("<platform_arch>%s</platform_arch>",tag.getPlatformArch());
        fmt.format("<timestamp>%s</timestamp>", (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")).format(new Date()));
        fmt.format("<container>%s</container>",tag.getContainer());
        fmt.format("<source>%s</source>",tag.getSource());
        fmt.format("<installer_uid>-1</installer_uid>");

//        fmt.format("<installer_uid>%s</installer_uid>",tag.getInstallerUID());
        fmt.format("</service_tag>");

        return html.toString();
    }
}
