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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2018-2022 Payara Foundation and/or its affiliates
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license

package com.sun.enterprise.iiop.security;

import com.sun.corba.ee.impl.encoding.CDRInputObject;
import com.sun.corba.ee.impl.encoding.CDROutputObject;
import com.sun.corba.ee.impl.encoding.EncapsInputStream;
import com.sun.corba.ee.org.omg.CSIIOP.AS_ContextSec;
import com.sun.corba.ee.org.omg.CSIIOP.CompoundSecMech;
import com.sun.corba.ee.org.omg.CSIIOP.CompoundSecMechList;
import com.sun.corba.ee.org.omg.CSIIOP.CompoundSecMechListHelper;
import com.sun.corba.ee.org.omg.CSIIOP.Confidentiality;
import com.sun.corba.ee.org.omg.CSIIOP.EstablishTrustInClient;
import com.sun.corba.ee.org.omg.CSIIOP.EstablishTrustInTarget;
import com.sun.corba.ee.org.omg.CSIIOP.IdentityAssertion;
import com.sun.corba.ee.org.omg.CSIIOP.Integrity;
import com.sun.corba.ee.org.omg.CSIIOP.SAS_ContextSec;
import com.sun.corba.ee.org.omg.CSIIOP.ServiceConfiguration;
import com.sun.corba.ee.org.omg.CSIIOP.TAG_CSI_SEC_MECH_LIST;
import com.sun.corba.ee.org.omg.CSIIOP.TAG_NULL_TAG;
import com.sun.corba.ee.org.omg.CSIIOP.TAG_TLS_SEC_TRANS;
import com.sun.corba.ee.org.omg.CSIIOP.TLS_SEC_TRANS;
import com.sun.corba.ee.org.omg.CSIIOP.TLS_SEC_TRANSHelper;
import com.sun.corba.ee.org.omg.CSIIOP.TransportAddress;
import com.sun.corba.ee.spi.folb.SocketInfo;
import com.sun.corba.ee.spi.ior.IOR;
import com.sun.corba.ee.spi.ior.TaggedComponent;
import com.sun.corba.ee.spi.ior.iiop.IIOPProfile;
import com.sun.corba.ee.spi.ior.iiop.IIOPProfileTemplate;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbIORConfigurationDescriptor;
import com.sun.enterprise.deployment.MethodPermission;
import com.sun.logging.LogDomains;
import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.glassfish.enterprise.iiop.impl.CSIv2Policy;
import org.glassfish.internal.api.ORBLocator;
import org.glassfish.pfl.basic.func.UnaryFunction;
import org.glassfish.security.common.Role;
import org.ietf.jgss.GSSException;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.IORInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.enterprise.deployment.EjbIORConfigurationDescriptor.NONE;
import static com.sun.enterprise.deployment.EjbIORConfigurationDescriptor.REQUIRED;
import static com.sun.enterprise.deployment.EjbIORConfigurationDescriptor.SUPPORTED;
import static com.sun.enterprise.iiop.security.GSSUtils.GSSUP_MECH_OID;
import static com.sun.enterprise.util.Utility.getLocalAddress;
import static com.sun.enterprise.util.Utility.intToShort;
import static com.sun.logging.LogDomains.SECURITY_LOGGER;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

/**
 * This is the class that manages the CSIV2 tagged component information in the IORs. Note: For
 * supporting FLOB in a cluster/EE mode we need to register the CSIV2TaggedComponentHandlerImpl with
 * the GlassFishORBManager.
 * 
 * @author Vivek Nagar
 * @author Harpreet Singh
 * @author Ken Cavanaugh
 */

public final class CSIV2TaggedComponentInfo {
    public static final int SUPPORTED_IDENTITY_TOKEN_TYPES = 15;

    private static final String DEFAULT_REALM = "default";
    private static final Logger logger = LogDomains.getLogger(CSIV2TaggedComponentInfo.class, SECURITY_LOGGER);
    private static final org.omg.IOP.TaggedComponent NULL_TAGGED_COMPONENT;

    static {
        byte[] b = {};
        NULL_TAGGED_COMPONENT = new org.omg.IOP.TaggedComponent(TAG_NULL_TAG.value, b);
    }

    // Realm name is first picked up from the application object.
    // If the realm is unpopulated here, then we query it from the IORDescriptor(as in for a standalone
    // ejb case).
    // The fallback is "default"

    private ORB orb;
    private int sslMutualAuthPort;
    private GlassFishORBHelper orbHelper;

    public CSIV2TaggedComponentInfo(ORB orb) {
        this.orb = orb;
        orbHelper = Lookups.getGlassFishORBHelper();
    }

    public CSIV2TaggedComponentInfo(ORB orb, int sslMutualAuthPort) {
        this(orb);
        this.sslMutualAuthPort = sslMutualAuthPort;
    }

    public EjbDescriptor getEjbDescriptor(IORInfo iorInfo) {
        CSIv2Policy csiv2Policy = null;
        try {
            csiv2Policy = (CSIv2Policy) iorInfo.get_effective_policy(orbHelper.getCSIv2PolicyType());
        } catch (INV_POLICY ex) {
            logger.log(FINE, "CSIV2TaggedComponentInfo.getEjbDescriptor: CSIv2Policy not present");
        }

        // Add CSIv2 tagged component for this EJB type.
        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "TxSecIORInterceptor.establish_components: CSIv2Policy: " + csiv2Policy);
        }

        EjbDescriptor ejbDesc = null;
        if (csiv2Policy != null) {
            ejbDesc = csiv2Policy.getEjbDescriptor();
        }

        return ejbDesc;
    }

    /**
     * Create the security mechanism list tagged component based on the deployer specified configuration
     * information. This method is on the server side for all ejbs in the non-cluster app server case.
     */
    public org.omg.IOP.TaggedComponent createSecurityTaggedComponent(int sslPort, EjbDescriptor ejbDescriptor) {

        org.omg.IOP.TaggedComponent securityTaggedComponent = null;
        try {
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "IIOP: Creating a Security Tagged Component");
            }

            securityTaggedComponent = createCompoundSecMechListComponent(createCompoundSecMechs(sslPort, ejbDescriptor));
        } catch (Exception e) {
            logger.log(SEVERE, "iiop.createcompund_exception", e);
        }

        return securityTaggedComponent;
    }

    /**
     * Create the CSIv2 tagged component for a clustered app server.
     */
    public org.omg.IOP.TaggedComponent createSecurityTaggedComponent(List<SocketInfo> socketInfos, EjbDescriptor ejbDescriptor) {
        org.omg.IOP.TaggedComponent securityTaggedComponent = null;

        if (ejbDescriptor != null) {
            try {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(FINE, "IIOP: Creating a Security Tagged Component");
                }

                securityTaggedComponent = createCompoundSecMechListComponent(createCompoundSecMechs(socketInfos, ejbDescriptor));
            } catch (Exception e) {
                logger.log(SEVERE, "iiop.createcompund_exception", e);
            }
        }

        return securityTaggedComponent;
    }

    private boolean getBooleanValue(Properties props, String name) {
        return props.getProperty(name, "false").equals("true");
    }

    /**
     * This method is called on the server side for all non-EJB POAs.
     */
    public org.omg.IOP.TaggedComponent createSecurityTaggedComponent(int sslPort) {

        org.omg.IOP.TaggedComponent securityTaggedComponent = null;

        try {
            Properties props = orbHelper.getCSIv2Props();
            boolean sslRequired = getBooleanValue(props, ORBLocator.ORB_SSL_SERVER_REQUIRED);
            boolean clientAuthRequired = getBooleanValue(props, ORBLocator.ORB_CLIENT_AUTH_REQUIRED);

            CompoundSecMech[] mechList = new CompoundSecMech[1];

            org.omg.IOP.TaggedComponent transportMech = createSSLInfo(sslPort, null, sslRequired);

            // Create AS_Context
            AS_ContextSec asContext = createASContextSec(null, DEFAULT_REALM);

            // Create SAS_Context
            SAS_ContextSec sasContext = createSASContextSec(null);

            short targetRequires = (clientAuthRequired ? EstablishTrustInClient.value : 0);

            // Convert Profile.TaggedComponent to org.omg.IOP.TaggedComponent
            mechList[0] = new CompoundSecMech(targetRequires, transportMech, asContext, sasContext);

            securityTaggedComponent = createCompoundSecMechListComponent(mechList);
        } catch (Exception e) {
            logger.log(SEVERE, "iiop.createcompund_exception", e);
        }

        return securityTaggedComponent;
    }

    private org.omg.IOP.TaggedComponent createCompoundSecMechListComponent(CompoundSecMech[] mechList) {
        CDROutputObject out = (CDROutputObject) orb.create_output_stream();
        out.putEndian();

        boolean stateful = false;
        CompoundSecMechListHelper.write(out, new CompoundSecMechList(stateful, mechList));

        return new org.omg.IOP.TaggedComponent(TAG_CSI_SEC_MECH_LIST.value, out.toByteArray());
    }

    private Set<EjbIORConfigurationDescriptor> getIORConfigurationDescriptors(EjbDescriptor ejbDescriptor) {

        if (ejbDescriptor == null) {
            return null;
        }

        Set<EjbIORConfigurationDescriptor> iorDescriptors = ejbDescriptor.getIORConfigurationDescriptors();
        int size = iorDescriptors.size();

        if (size == 0) {

            // No IOR config descriptors:
            // Either none were configured or 1.2.x app.

            // Create an IOR config desc with SSL supported
            EjbIORConfigurationDescriptor iorDescriptor = new EjbIORConfigurationDescriptor();
            iorDescriptor.setIntegrity(SUPPORTED);
            iorDescriptor.setConfidentiality(SUPPORTED);
            iorDescriptor.setEstablishTrustInClient(SUPPORTED);
            iorDescriptors.add(iorDescriptor);

            // Check if method permissions are set on the descriptor.
            // If they are then enable username_password mechanism in as_context
            Set<Role> permissions = ejbDescriptor.getPermissionedRoles();
            if (permissions.size() > 0) {
                if (logger.isLoggable(FINE)) {
                    logger.log(FINE, "IIOP:Application has protected methods");
                }

                iorDescriptor.setAuthMethodRequired(true);
                String realmName = DEFAULT_REALM;

                if (ejbDescriptor.getApplication() != null) {
                    realmName = ejbDescriptor.getApplication().getRealm();
                }

                if (realmName == null) {
                    realmName = DEFAULT_REALM;
                }

                iorDescriptor.setRealmName(realmName);

                // If the EJB contains some methods that don't require authentication, add a descriptor that
                // doesn't require authentication so that lookup can be performed (access checks on protected
                // methods should still happen later, this is simply to allow lookup)
                for (MethodPermission methodPermission : ejbDescriptor.getMethodPermissionsFromDD().keySet()) {
                    if (methodPermission.isUnchecked()) {
                        EjbIORConfigurationDescriptor uncheckedDescriptor = new EjbIORConfigurationDescriptor();
                        uncheckedDescriptor.setIntegrity(SUPPORTED);
                        uncheckedDescriptor.setConfidentiality(SUPPORTED);
                        uncheckedDescriptor.setEstablishTrustInClient(SUPPORTED);
                        uncheckedDescriptor.setRealmName(realmName);
                        iorDescriptors.add(uncheckedDescriptor);
                        break;
                    }
                }
            }
        }

        return iorDescriptors;
    }

    // Type of simple closure used for createCompoundSecMechs
    private interface DescriptorMaker extends UnaryFunction<EjbIORConfigurationDescriptor, org.omg.IOP.TaggedComponent> {
    }

    /**
     * Create the security mechanisms. Only 1 such mechanism is created although the spec allows
     * multiple mechanisms (in decreasing order of preference). Note that creating more than one
     * CompoundSecMech here will cause getSecurityMechanisms to fail, as it supports only one
     * CompoundSecMech.
     */
    private CompoundSecMech[] createCompoundSecMechs(DescriptorMaker maker, EjbDescriptor ejbDescriptor) throws GSSException {

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "IIOP: Creating CompoundSecMech");
        }

        if (ejbDescriptor == null) {
            return null;
        }

        Set<EjbIORConfigurationDescriptor> iorDescriptors = getIORConfigurationDescriptors(ejbDescriptor);

        CompoundSecMech[] mechList = new CompoundSecMech[iorDescriptors.size()];
        Iterator<EjbIORConfigurationDescriptor> itr = iorDescriptors.iterator();

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "IORDescSet SIZE:" + iorDescriptors.size());
        }

        String realmName = DEFAULT_REALM;

        for (int i = 0; i < iorDescriptors.size(); i++) {
            EjbIORConfigurationDescriptor iorDescriptor = itr.next();

            int targetRequires = getTargetRequires(iorDescriptor);
            org.omg.IOP.TaggedComponent comp = maker.evaluate(iorDescriptor);

            if (ejbDescriptor.getApplication() != null) {
                realmName = ejbDescriptor.getApplication().getRealm();
            }

            if (realmName == null) {
                realmName = iorDescriptor.getRealmName();
            }

            if (realmName == null) {
                realmName = DEFAULT_REALM;
            }

            // Create AS_Context
            AS_ContextSec asContext = createASContextSec(iorDescriptor, realmName);

            // Create SAS_Context
            SAS_ContextSec sasContext = createSASContextSec(iorDescriptor);

            // Update the target requires value
            int targ_req = targetRequires | asContext.target_requires | sasContext.target_requires;

            // Convert Profile.TaggedComponent to org.omg.IOP.TaggedComponent
            mechList[i] = new CompoundSecMech((short) targ_req, comp, asContext, sasContext);
        }

        return mechList;
    }

    private CompoundSecMech[] createCompoundSecMechs(final List<SocketInfo> socketInfos, EjbDescriptor ejbDescriptor)
            throws GSSException {

        DescriptorMaker maker = new DescriptorMaker() {
            @Override
            public org.omg.IOP.TaggedComponent evaluate(EjbIORConfigurationDescriptor iorDescriptor) {
                return createSSLInfo(socketInfos, iorDescriptor, false);
            }
        };

        return createCompoundSecMechs(maker, ejbDescriptor);
    }

    private CompoundSecMech[] createCompoundSecMechs(final int sslPort, final EjbDescriptor ejbDescriptor) throws GSSException {

        DescriptorMaker maker = new DescriptorMaker() {
            @Override
            public org.omg.IOP.TaggedComponent evaluate(EjbIORConfigurationDescriptor iorDescriptor) {
                return createSSLInfo(sslPort, iorDescriptor, false);
            }
        };

        return createCompoundSecMechs(maker, ejbDescriptor);
    }

    /**
     * Create the AS layer context within a compound mechanism definition.
     */
    public AS_ContextSec createASContextSec(EjbIORConfigurationDescriptor iorDescriptor, String realmName) throws GSSException {
        int targetSupports = 0;
        int targetRequires = 0;
        byte[] clientAuthenticationMmechanism = {};
        byte[] targetNname = {};
        String authMethod = null;
        boolean authMethodRequired = false;

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "IIOP: Creating AS_Context");
        }

        // If AS_ContextSec is not required to be generated in an IOR,
        // then optimize the code by not generating and filling in fields that are
        // irrelevant.

        if (iorDescriptor != null) {
            authMethod = iorDescriptor.getAuthenticationMethod();
            authMethodRequired = iorDescriptor.isAuthMethodRequired();
        }

        if (authMethod != null && authMethod.equalsIgnoreCase(NONE)) {
            return new AS_ContextSec((short) targetSupports, (short) targetRequires, clientAuthenticationMmechanism, targetNname);
        }

        /**
         * Functionality for Realm Per App Try to get the realm from the descriptor, else fill in default
         */

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "IIOP:AS_Context: Realm Name for login = " + realmName);
        }

        if (realmName == null) {
            realmName = iorDescriptor.getRealmName();
        }

        if (realmName == null) {
            realmName = DEFAULT_REALM;
        }

        targetNname = GSSUtils.createExportedName(GSSUP_MECH_OID, realmName.getBytes());

        targetSupports = EstablishTrustInClient.value;

        if (authMethodRequired) {
            targetRequires = EstablishTrustInClient.value;
        }

        clientAuthenticationMmechanism = GSSUtils.getMechanism();

        return new AS_ContextSec((short) targetSupports, (short) targetRequires, clientAuthenticationMmechanism, targetNname);
    }

    /**
     * Create the SAS layer context within a compound mechanism definition.
     */
    public SAS_ContextSec createSASContextSec(EjbIORConfigurationDescriptor iorDescriptor) throws GSSException {
        int targetSupports = 0; // target_supports = 0 means that target supports ITTAbsent
        int targetRequires = 0;
        ServiceConfiguration[] privilegeAuthorities = new ServiceConfiguration[0];
        String callerPropagation = null;
        byte[][] mechanisms = {};

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "IIOP: Creating SAS_Context");
        }

        // this shall be non-zero if target_supports is non-zero
        int supportedIdentityTokenType = 0;

        if (iorDescriptor != null) {
            callerPropagation = iorDescriptor.getCallerPropagation();
        }

        if (callerPropagation != null && callerPropagation.equalsIgnoreCase(NONE)) {
            return new SAS_ContextSec((short) targetSupports, (short) targetRequires, privilegeAuthorities, mechanisms,
                    supportedIdentityTokenType);
        }

        targetSupports = IdentityAssertion.value;

        byte[] upm = GSSUtils.getMechanism(); // Only username_password mechanism
        mechanisms = new byte[1][upm.length];
        for (int i = 0; i < upm.length; i++) {
            mechanisms[0][i] = upm[i];
        }

        // para 166 of CSIv2 spec says that the bit corresponding to the
        // ITTPrincipalName is non-zero if supported_mechanism has atleast
        // 1 element. Supported_mechanism has the value of GSSUP OID
        if (targetSupports != 0) {
            supportedIdentityTokenType = SUPPORTED_IDENTITY_TOKEN_TYPES;
        }

        return new SAS_ContextSec((short) targetSupports, (short) targetRequires, privilegeAuthorities, mechanisms,
                supportedIdentityTokenType);
    }

    /**
     * Get the value of target_supports for the transport layer.
     */
    public int getTargetSupports(EjbIORConfigurationDescriptor iorDescriptor) {
        if (iorDescriptor == null) {
            return 0;
        }

        int supports = 0;
        String integrity = iorDescriptor.getIntegrity();
        if (!integrity.equalsIgnoreCase(NONE)) {
            supports = supports | Integrity.value;
        }

        if (!iorDescriptor.getConfidentiality().equalsIgnoreCase(NONE)) {
            supports = supports | Confidentiality.value;
        }

        if (!iorDescriptor.getEstablishTrustInTarget().equalsIgnoreCase(NONE)) {
            supports = supports | EstablishTrustInTarget.value;
        }

        if (!iorDescriptor.getEstablishTrustInClient().equalsIgnoreCase(NONE)) {
            supports = supports | EstablishTrustInClient.value;
        }

        return supports;
    }

    /**
     * Get the value of target_requires for the transport layer.
     */
    public int getTargetRequires(EjbIORConfigurationDescriptor iorDescriptor) {
        if (iorDescriptor == null) {
            return 0;
        }

        int requires = 0;

        if (iorDescriptor.getIntegrity().equalsIgnoreCase(REQUIRED)) {
            requires = requires | Integrity.value;
        }

        if (iorDescriptor.getConfidentiality().equalsIgnoreCase(REQUIRED)) {
            requires = requires | Confidentiality.value;
        }

        if (iorDescriptor.getEstablishTrustInTarget().equalsIgnoreCase(REQUIRED)) {
            requires = requires | EstablishTrustInTarget.value;
        }

        if (iorDescriptor.getEstablishTrustInClient().equalsIgnoreCase(REQUIRED)) {
            requires = requires | EstablishTrustInClient.value;
        }

        return requires;
    }

    private int getTargetSupportsDefault(EjbIORConfigurationDescriptor iorDescriptor) {
        if (iorDescriptor == null) {
            return Integrity.value | Confidentiality.value | EstablishTrustInClient.value | EstablishTrustInTarget.value;
        }

        return getTargetSupports(iorDescriptor);
    }

    private int getTargetRequiresDefault(EjbIORConfigurationDescriptor iorDescriptor, boolean sslRequired) {
        int targetRequires = 0;
        if (iorDescriptor == null) {
            if (sslRequired) {
                targetRequires = Integrity.value | Confidentiality.value | EstablishTrustInClient.value;
            }
        } else {
            targetRequires = getTargetRequires(iorDescriptor);
        }

        return targetRequires;
    }

    private org.omg.IOP.TaggedComponent createTlsSecTransComponent(int targetSupports, int targetRequires,
            TransportAddress[] transportAddresses) {

        TLS_SEC_TRANS tls_sec = new TLS_SEC_TRANS((short) targetSupports, (short) targetRequires, transportAddresses);

        CDROutputObject out = (CDROutputObject) orb.create_output_stream();
        out.putEndian();
        TLS_SEC_TRANSHelper.write(out, tls_sec);

        // create new Tagged Component for SSL
        return new org.omg.IOP.TaggedComponent(TAG_TLS_SEC_TRANS.value, out.toByteArray());
    }

    private TransportAddress[] generateTransportAddresses(int sslPort) {
        return new TransportAddress[] { new TransportAddress(getLocalAddress(), intToShort(sslPort)) };
    }

    private TransportAddress[] generateTransportAddresses(List<SocketInfo> socketInfos) {

        TransportAddress[] transportAddresses = new TransportAddress[socketInfos.size()];

        for (int i = 0; i < socketInfos.size(); i++) {
            SocketInfo socketInfo = socketInfos.get(i);
            transportAddresses[i] = new TransportAddress(socketInfo.host(), intToShort(socketInfo.port()));
        }

        return transportAddresses;
    }

    /**
     * Create the SSL tagged component within a compound mechanism definition.
     */
    private org.omg.IOP.TaggedComponent createSSLInfo(int initialSslPort, EjbIORConfigurationDescriptor iorDescriptor,
            boolean sslRequired) {

        int targetSupports = getTargetSupportsDefault(iorDescriptor);
        int targetRequires = getTargetRequiresDefault(iorDescriptor, sslRequired);
        boolean mutualAuthRequired = (iorDescriptor != null)
                && ((targetRequires & EstablishTrustInClient.value) == EstablishTrustInClient.value);
        int sslPort = mutualAuthRequired ? sslMutualAuthPort : initialSslPort;

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "IIOP: Creating Transport Mechanism for sslport " + sslPort);
        }

        /*
         * if both targetSupports and targetRequires are zero, then the mechanism does not support a
         * transport_mechanism and hence a TAG_NULL_TAG must be generated.
         */

        if ((targetSupports | targetRequires) == 0 || sslPort == -1) {
            return NULL_TAGGED_COMPONENT;
        }

        return createTlsSecTransComponent(targetSupports, targetRequires, generateTransportAddresses(sslPort));
    }

    private org.omg.IOP.TaggedComponent createSSLInfo(List<SocketInfo> socketInfos, EjbIORConfigurationDescriptor iorDescriptor,
            boolean sslRequired) {

        int targetSupports = getTargetSupportsDefault(iorDescriptor);
        int targetRequires = getTargetRequiresDefault(iorDescriptor, sslRequired);

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "IIOP: Creating Transport Mechanism for socketInfos " + socketInfos);
        }

        /*
         * if both targetSupports and targetRequires are zero, then the mechanism does not support a
         * transport_mechanism and hence a TAG_NULL_TAG must be generated.
         */

        if ((targetSupports | targetRequires) == 0) {
            return NULL_TAGGED_COMPONENT;
        }

        return createTlsSecTransComponent(targetSupports, targetRequires, generateTransportAddresses(socketInfos));
    }

    /**
     * This method determines if all the mechanisms defined in the CSIV2 CompoundSecMechList structure
     * require protected invocations.
     */
    public boolean allMechanismsRequireSSL(Set iorDescSet) {
        int size = iorDescSet.size();
        if (size == 0) {
            return false;
        }

        Iterator<EjbIORConfigurationDescriptor> itr = iorDescSet.iterator();

        for (int i = 0; i < size; i++) {
            EjbIORConfigurationDescriptor iorDesc = itr.next();
            int target_requires = getTargetRequires(iorDesc);
            if (target_requires == 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get the Compound security mechanism list from the given IOR.
     * 
     * @param the IOR.
     * @return the array of compound security mechanisms.
     */
    public CompoundSecMech[] getSecurityMechanisms(IOR ior) {
        IIOPProfile prof = ior.getProfile();
        IIOPProfileTemplate ptemp = (IIOPProfileTemplate) prof.getTaggedProfileTemplate();
        Iterator<TaggedComponent> itr = ptemp.iteratorById(TAG_CSI_SEC_MECH_LIST.value);

        if (!itr.hasNext()) {
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "IIOP:TAG_CSI_SEC_MECH_LIST tagged component not found");
            }

            return null;
        }

        TaggedComponent tcomp = itr.next();
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Component:" + tcomp);
        }

        if (itr.hasNext()) {
            String msg = "More than one TAG_CSI_SEC_MECH_LIST tagged " + "component found ";
            logger.log(Level.SEVERE, "iiop.many_tagged_component");
            throw new RuntimeException(msg);
        }

        org.omg.IOP.TaggedComponent comp = tcomp.getIOPComponent(orb);
        byte[] b = comp.component_data;
        CDRInputObject in = new EncapsInputStream(orb, b, b.length);
        in.consumeEndian();
        CompoundSecMechList l = CompoundSecMechListHelper.read(in);
        CompoundSecMech[] list = l.mechanism_list;

        return list;
    }

    /**
     * Retrieve the SSL tagged component from the compound security mechanism.
     */
    public TLS_SEC_TRANS getSSLInformation(CompoundSecMech mech) {
        org.omg.IOP.TaggedComponent pcomp = mech.transport_mech;
        TLS_SEC_TRANS ssl = getSSLComponent(pcomp);
        return ssl;
    }

    private TLS_SEC_TRANS getSSLComponent(org.omg.IOP.TaggedComponent comp) {

        TLS_SEC_TRANS ssl = null;

        // a TAG_NULL_TAG implies that SSL is not required
        if (comp.tag == TAG_NULL_TAG.value) {
            ssl = null;
        } else {
            byte[] b = comp.component_data;
            CDRInputObject in = new EncapsInputStream(orb, b, b.length);
            in.consumeEndian();
            ssl = TLS_SEC_TRANSHelper.read(in);
        }

        return ssl;
    }
}
