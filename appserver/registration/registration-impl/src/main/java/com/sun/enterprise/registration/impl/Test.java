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

package com.sun.enterprise.registration.impl;

import com.sun.enterprise.registration.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Properties;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;

public class Test {
    
    /** Creates a new instance of Test */
    public Test() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            RelayService relay = new RelayService("/servicetag-registry.xml");
            relay.generateRegistrationPage("/test.html");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

/*
    public static void testRegistration() {
        try {
            System.out.println("registering...");
            Object[] params = { getRepositoryFile(), "glassfish:test" };
            RegistrationServiceConfig config =
                    new RegistrationServiceConfig("com.sun.enterprise.registration.SysnetRegistrationService",
                        params);
            RegistrationService regService =
                    RegistrationServiceFactory.getInstance().getRegistrationService(config);

            List<RegistrationDescriptor> l = regService.getRegistrationDescriptors();
            for (Iterator<RegistrationDescriptor> it = l.iterator(); 
                    it.hasNext();) {
                RegistrationDescriptor ri = it.next();
                System.out.println(ri.getProductName() + " " + ri.getInstanceURN());
            }
            HashMap map = new HashMap();
            map.put(RegistrationAccount.USERID, "replaceme");
            map.put(RegistrationAccount.PASSWORD, "replaceme");

            Object[] accountParams = { map };
            RegistrationAccountConfig accountConfig =
                    new RegistrationAccountConfig("com.sun.enterprise.registration.impl.SOAccount",
                        accountParams);
            
            RegistrationAccount account = 
                RegistrationAccountFactory.getInstance().getRegistrationAccount(accountConfig);

//            RegistrationHelper regHelper = new RegistrationHelper(
  //                  "webcache.east.sun.com", 8080);
            System.out.println("registering...");
            if (!regService.isRegistrationEnabled())
                return;
            regService.register(account);
            System.out.println("Registered Successfully");
        } catch(ConnectException cex) {
            System.out.println("Connection Exception");            
            cex.printStackTrace();
        } catch(RegistrationException ex) {
            ex.printStackTrace();        
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static void testAccountValidation() {
        try {
//            Object[] params = { getRepositoryFile(), "webcache.east.sun.com", new Integer(8080) };
           Object[] params = { getRepositoryFile()};
            
            RegistrationServiceConfig config =
                    new RegistrationServiceConfig("com.sun.enterprise.registration.impl.SysnetRegistrationService",
                        params);
            RegistrationService regService =
                    RegistrationServiceFactory.getInstance().getRegistrationService(config);

            HashMap map = new HashMap();
            map.put(RegistrationAccount.USERID, "replaceme");
            map.put(RegistrationAccount.PASSWORD, "replaceme");
            Object[] accountParams = { map };
            RegistrationAccountConfig accountConfig =
                    new RegistrationAccountConfig("com.sun.enterprise.registration.impl.SOAccount",
                        accountParams);
            
        
            RegistrationAccount account = 
                RegistrationAccountFactory.getInstance().getRegistrationAccount(accountConfig);
            System.out.println("Is Account Valid? : " +
                    regService.isRegistrationAccountValid(account));
        } catch(Exception ex) {
            ex.printStackTrace();
        }
            
                
    }


    public static void testSOA() {
        try {
//            RegistrationHelper regHelper = new RegistrationHelper(
  //                  "webcache.east.sun.com", 8080);
            
            
            System.out.println("creating SOA...");
            HashMap<String,String> map = new HashMap<String,String>();
            map.put(SOAccount.USERID, "aateaaaaaaaaaaaaaaaaaaaa");
            map.put(SOAccount.PASSWORD, "testpass");
            map.put(SOAccount.EMAIL, "email@email.com");
            map.put(SOAccount.COUNTRY, "United States"); 
            SOAccount soa = new SOAccount(map);
            regHelper.createSunOnlineAccount(soa);
            System.out.println("Created Successfully");
        } catch(Exception ex) {
            ex.getMessage();
            ex.printStackTrace();
        }
    }
    */
    
    public static void testServiceTags() throws RegistrationException {
        Properties data = getServiceTagProps();

        ServiceTag st = new ServiceTag(data);
        RepositoryManager rm = 
                new RepositoryManager(getRepositoryFile());
        System.out.println("Initial repository contents:");
        rm.write(System.out);
        try {
            rm.add(st);
            System.out.println("Service tag added");
            List<ServiceTag> list = rm.getServiceTags();
            System.out.println("List of service tags:");
            for (ServiceTag x : list) {
                System.out.println(x.toString());
            }
            try {
                rm.add(st);
            } catch (RegistrationException e) {
                System.err.println("Attempt to add duplicate correctly rejected");
            }                
            System.out.println("--- end of list of service tags");
            rm.write(System.out);
            System.out.println("going to remove " + st.getSvcTag().getInstanceURN());
            rm.remove(st);
            System.out.println("Service tag removed");
            rm.write(System.out);
            try {
                rm.remove(st);
                System.out.println("Attempt to remove again succeeded; should have failed");
            } catch (RegistrationException e) {
                System.err.println("Attempt to remove correctly rejected");
            }
            rm.add(st);

            System.out.println("Setting status to not registered");
            rm.setRegistrationStatus(st, ServiceTag.RegistrationStatus.NOT_REGISTERED);
            rm.write(System.out);
            
            System.out.println("Setting status to not registered");
            rm.setRegistrationStatus(st, ServiceTag.RegistrationStatus.NOT_REGISTERED);
            rm.write(System.out);
            
            System.out.println("Setting status to transfered");
            rm.setStatus(st, ServiceTag.Status.TRANSFERRED);
            rm.write(System.out);

            System.out.println("Setting status to not transfered");
            rm.setStatus(st, ServiceTag.Status.NOT_TRANSFERRED);
            rm.write(System.out);
            
            rm = new RepositoryManager(
                    getRepositoryFile());
            System.out.println("Setting status to dont ask for regn");
            rm.setRegistrationReminder(RegistrationService.RegistrationReminder.REMIND_LATER);            
            rm.write(System.out);

            System.out.println(rm.getRegistrationStatus());

            testTransferManager(getRepositoryFile());
            testRMRegistration();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void testTransferManager(File repositoryFile) throws RegistrationException {
            /*
             * Try forwarding to SysNet's local repository.
             */
             System.out.println("Testing transfer of local tags to SysNet");
             SysnetTransferManager tm = 
                new SysnetTransferManager(repositoryFile);
             int count = tm.transferServiceTags();
             System.out.println("Transferred " + count + " service tags");
             RepositoryManager rm = new RepositoryManager(repositoryFile);
             rm.write(System.out);


    }
    private static void testRMRegistration() throws RegistrationException {
        RepositoryManager rm = new 
                RepositoryManager(getRepositoryFile());
        System.out.println("Initial repository contents:");
        rm.write(System.out);

        System.out.println("Setting registration status to NOT REGISTERED");
        rm.setRegistrationStatus(RegistrationService.RegistrationStatus.NOT_REGISTERED);
        rm.write(System.out);
        
        System.out.println("Setting reg. status to REGISTERED");
        //rm.setRegistrationStatus(RegistrationService.RegistrationStatus.REGISTERED);
        rm.write(System.out);
    } 
    
    

    private static Properties getServiceTagProps() {
        
        Properties data = new Properties();

        data.put(ServiceTag.PRODUCT_NAME, "AppServer");
        data.put(ServiceTag.PRODUCT_VERSION, "9.1");
        data.put(ServiceTag.PRODUCT_URN, "urn:uuid:5005588c-36f3-11d6-9cec-fc96f718e113");
        data.put(ServiceTag.PRODUCT_PARENT_URN, "urn:uuid:product-parent");
        data.put(ServiceTag.PRODUCT_PARENT, "ProductParent");
        data.put(ServiceTag.PRODUCT_DEFINED_INST_ID, "installDir=/tmp");
        data.put(ServiceTag.PRODUCT_VENDOR, "Sun Micosystems");
        data.put(ServiceTag.CONTAINER, "Global");
        data.put(ServiceTag.SOURCE, "Test");
        data.put(ServiceTag.PRODUCT_DEFINED_INST_ID, "installDir=/tmp");
        data.put(ServiceTag.PRODUCT_URN, "urn:uuid:5005588c-36f3-11d6-9cec-fc96f718e113");
        data.put(ServiceTag.PRODUCT_VENDOR, "ACME Software, Inc.");
        data.put(ServiceTag.PRODUCT_PARENT_URN, "urn:something");
        data.put(ServiceTag.INSTANCE_URN, "urn:st:" + UUID.randomUUID().toString());
        return data;        
    }
    
/*    
    public static void testBadConnectionWithSOA() {
        try {
            RegistrationHelper regHelper = new RegistrationHelper();
                    //"webcache.east.sun.com", 8080);
            System.out.println("creating SOA...");
            HashMap<String,String> map = new HashMap<String,String>();
            map.put(SOAccount.USERID, "testuser1223sdsdsdsd");
            map.put(SOAccount.PASSWORD, "testpass");
            map.put(SOAccount.EMAIL, "email.com");
            map.put(SOAccount.COUNTRY, "United States"); 
            SOAccount soa = new SOAccount(map);
            regHelper.createSunOnlineAccount(soa);
            System.out.println("Created Successfully - should have thrown error");
        } catch(Exception ex) {
            ex.getMessage();
            ex.printStackTrace();
        }                
    }
    
    public static void testBadConnectionWithRegistration() {
        try {
            RegistrationHelper regHelper = new RegistrationHelper();
                    //"webcache.east.sun.com", 8080);
            System.out.println("registering...");
            regHelper.register("testABCDEFGHIJK", "testpass");
            System.out.println("Registered Successfully");
        } catch(ConnectException cex) {
            System.out.println("Connection Exception");            
            cex.printStackTrace();
        } catch(RegistrationException ex) {
            ex.printStackTrace();        
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void testDuplicateSOA() {
        testSOA();
        testSOA(); // should see an error here
    }
    
    public static void testDuplicateRegistration() {
        testRegistration();
        testRegistration(); // should see an error here
    }
    */
    
    private static File getRepositoryFile() {
        return new File("test.xml");
    }
}
