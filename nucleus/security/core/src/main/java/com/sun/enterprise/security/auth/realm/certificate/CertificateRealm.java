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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.auth.realm.certificate;

import com.sun.enterprise.security.BaseRealm;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.auth.login.DistinguishedPrincipalCredential;
import com.sun.enterprise.security.auth.login.common.LoginException;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.util.Utility;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.x500.X500Principal;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.glassfish.security.common.Group;
import org.jvnet.hk2.annotations.Service;
import fish.payara.security.api.ClientCertificateValidator;
import java.security.cert.CertificateException;
import org.glassfish.grizzly.utils.Holder.LazyHolder;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.JavaEEContextUtil;
import org.glassfish.internal.api.JavaEEContextUtil.Context;

/**
 * Realm wrapper for supporting certificate authentication.
 * <P>
 * The certificate realm provides the security-service functionality needed to process a client-cert
 * authentication.
 * <P>
 * Since the SSL processing, and client certificate verification is done by NSS,
 * no authentication is actually done by this realm. It only serves the purpose of being registered
 * as the certificate handler realm and to service group membership requests during web container
 * role checks.
 * <P>
 * There is no JAAS LoginModule corresponding to the certificate realm, therefore this realm does
 * not require the jaas-context configuration parameter to be set. The purpose of a JAAS LoginModule
 * is to implement the actual authentication processing, which for the case of this certificate
 * realm is already done by the time execution gets to Java.
 * <P>
 * The certificate realm needs the following properties in its configuration: None.
 * <P>
 * The following optional properties can also be specified:
 * <ul>
 * <li>assign-groups - a comma-separated list of group names which will be assigned to all users who
 * present a cryptographically valid certificate.
 * <li>{@value #COMMON_NAME_AS_PRINCIPAL_NAME} - if true, the CN from the client certificate will be
 * used as a name of the principal
 * <li>{@value #DN_PARTS_USED_FOR_GROUPS} a comma-separated list of {@link OID} names whose values
 * in certificate's distinguished name will be used as a group names.
 * </ul>
 */
@Service
public final class CertificateRealm extends BaseRealm {

    private static final String COMMON_NAME_AS_PRINCIPAL_NAME = "common-name-as-principal-name";
    private static final String DN_PARTS_USED_FOR_GROUPS = "dn-parts-used-for-groups";

    /** Descriptive string of the authentication type of this realm. */
    public static final String AUTH_TYPE = "certificate";

    private boolean doValidation;
    private final Map<ClassLoader,
            // ServiceLoader keeps a reference to ClassLoader, so it has
            // to be explicitly weak as well
            WeakReference<ServiceLoader<ClientCertificateValidator>>> clientCertificateValidatorMap
            = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<ClassLoader, Set<ClientCertificateValidator>> injectedClasses
            = Collections.synchronizedMap(new WeakHashMap<>());
    private final LazyHolder<JavaEEContextUtil> ctxUtil = LazyHolder.lazyHolder(() ->
                    Globals.getDefaultHabitat().getService(JavaEEContextUtil.class));

    @Override
    protected void init(Properties props) throws BadRealmException, NoSuchRealmException {
        super.init(props);

        final String jaasCtx = props.getProperty(JAAS_CONTEXT_PARAM);
        setProperty(JAAS_CONTEXT_PARAM, jaasCtx);

        final String useCommonName = props.getProperty(COMMON_NAME_AS_PRINCIPAL_NAME);
        setProperty(COMMON_NAME_AS_PRINCIPAL_NAME, useCommonName);

        final String dnPartsForGroup = props.getProperty(DN_PARTS_USED_FOR_GROUPS);
        setProperty(DN_PARTS_USED_FOR_GROUPS, dnPartsForGroup);

        String validationCheckProperty = props.getProperty("validation-check");
        doValidation = validationCheckProperty != null && Boolean.parseBoolean(validationCheckProperty);
    }

    /**
     * Returns a short (preferably less than fifteen characters) description of the kind of authentication which is
     * supported by this realm.
     *
     * @return Description of the kind of authentication that is directly supported by this realm.
     */
    @Override
    public String getAuthType() {
        return AUTH_TYPE;
    }

    /**
     * WARN: does not have access to user's certificate, so it does not return groups based on certificate.
     *
     * @return enumeration of group names assigned to all users authenticated by this realm.
     */
    @Override
    public Enumeration<String> getGroupNames(String username) {
        String[] groups = addAssignGroups(null);
        if (groups == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(Arrays.asList(groups));
    }


    /**
     * @param subject The Subject object for the authentication request.
     * @param principal The Principal object from the user certificate.
     * @param componentId
     * @return principal's name
     * @throws java.security.cert.CertificateException
     */
    public String authenticate(Subject subject, X500Principal principal, String componentId) throws CertificateException {
        X509Certificate certificate = getCertificateFromSubject(subject, principal);
        if (doValidation) {
            certificate.checkValidity();
        }
        validateSubjectViaAPI(subject, principal, certificate, componentId);
        _logger.finest(() -> String.format("authenticate(subject=%s, principal=%s)", subject, principal));

        final LdapName dn = getLdapName(principal);
        _logger.log(Level.FINE, "dn={0}", dn);
        final String principalName = getPrincipalName(dn);
        _logger.log(Level.FINE, "Certificate realm is setting up security context for principal: {0}", principalName);

        final Enumeration<String> defaultGroups = getGroupNames(principalName);
        final Set<Principal> principalSet = subject.getPrincipals();
        while (defaultGroups.hasMoreElements()) {
            principalSet.add(new Group(defaultGroups.nextElement()));
        }
        final Set<Group> groupsFromDN = getGroupNamesFromDN(dn);
        principalSet.addAll(groupsFromDN);
        _logger.log(Level.FINE, "principalSet: {0}", principalSet);

        if (!subject.getPrincipals().isEmpty()) {
            subject.getPublicCredentials().add(new DistinguishedPrincipalCredential(principal));
        }

        // Making authentication final - setting the authenticated caller name
        // in the security context
        SecurityContext.setCurrent(new SecurityContext(principalName, subject));
        return principalName;
    }

    private void validateSubjectViaAPI(Subject subject, X500Principal principal,
            X509Certificate certificate, String componentId) throws LoginException {
        List<ClientCertificateValidator> validatorList = Collections.emptyList();
        try {
            validatorList = loadValidatorClasses();
        } catch (Throwable exc) {
            _logger.log(Level.WARNING, "Exception while loading certificate validation class", exc);
            clientCertificateValidatorMap.remove(Utility.getClassLoader());
        }

        boolean failed = false;
        if (!validatorList.isEmpty() && componentId != null) {
            try (Context context = ctxUtil.get().fromComponentId(componentId).pushContext()) {
                failed = validatorList.stream().anyMatch(validator
                        -> !inject(validator, context).isValid(subject, principal, certificate));
            } catch (Exception exc) {
                _logger.log(Level.WARNING, "Exception from certificate validation class", exc);
                clientCertificateValidatorMap.remove(Utility.getClassLoader());
            }

            if (failed) {
                throw new LoginException("Certificate Validation Failed via API");
            }
        } else if (componentId == null) {
            _logger.severe("No Jakarta EE Component available for Certificate Validation");
        }
    }

    private ClientCertificateValidator inject(ClientCertificateValidator validator, Context context) {
        Set<ClientCertificateValidator> injectedSet = injectedClasses.computeIfAbsent(Utility.getClassLoader(),
                key -> Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>())));
        if (injectedSet.add(validator)) {
            return context.inject(validator, false);
        } else {
            return validator;
        }
    }

    private List<ClientCertificateValidator> loadValidatorClasses() {
        AtomicReference<ServiceLoader<ClientCertificateValidator>> serviceLoader = new AtomicReference<>();
        clientCertificateValidatorMap.compute(Utility.getClassLoader(), (cl, weak) -> {
                            serviceLoader.set(weak != null ? weak.get() : null);
                            if (serviceLoader.get() == null) {
                                serviceLoader.set(ServiceLoader.load(ClientCertificateValidator.class));
                                return new WeakReference<>(serviceLoader.get());
                            } else {
                                return weak;
                            }
                        });
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(serviceLoader.get().iterator(),
                Spliterator.ORDERED), false).collect(Collectors.toList());
    }

    private X509Certificate getCertificateFromSubject(Subject subject, X500Principal principal) {
        return subject.getPublicCredentials(List.class).stream()
                .flatMap(Collection<?>::stream)
                .filter(X509Certificate.class::isInstance)
                .map(X509Certificate.class::cast)
                .filter(cert -> principal.equals(cert.getIssuerX500Principal()))
                .findAny().get();
    }

    private LdapName getLdapName(final X500Principal principal) {
        try {
            return new LdapName(principal.getName(X500Principal.RFC2253, OID.getOIDMap()));
        } catch (InvalidNameException e) {
            throw new IllegalStateException("Exception extracting DN from principal:\n" + principal, e);
        }
    }


    private String getPrincipalName(final LdapName distinguishedName) {
        if (Boolean.parseBoolean(getProperty(COMMON_NAME_AS_PRINCIPAL_NAME))) {
            return distinguishedName.getRdns().stream().filter(rdn -> rdn.getType().equalsIgnoreCase(OID.CN.getName()))
                .findFirst() //
                .orElseThrow(() -> new IllegalStateException(
                    "common-name-as-principal-name set to true, but no CN present in " + distinguishedName))
                .getValue().toString();
        }
        return distinguishedName.toString();
    }


    private Set<Group> getGroupNamesFromDN(final LdapName distinguishedName) {
        _logger.log(Level.FINE, "getGroupNamesFromDN(distinguishedName={0})", distinguishedName);
        final String dnPartsForGroups = getProperty(DN_PARTS_USED_FOR_GROUPS);
        if (dnPartsForGroups == null) {
            return Collections.emptySet();
        }
        final Set<String> oidNames = OID.toOIDS(dnPartsForGroups.split(GROUPS_SEP)).stream().map(OID::getName)
            .collect(Collectors.toSet());
        final Function<Rdn, Group> rdnToGroup = rdn -> new Group(rdn.getValue().toString());
        return distinguishedName.getRdns().stream().filter(rdn -> oidNames.contains(rdn.getType())).map(rdnToGroup)
            .collect(Collectors.toSet());
    }

    /**
     * <p>
     * A <code>LoginModule</code> for <code>CertificateRealm</code> can instantiate and pass a
     * <code>AppContextCallback</code> to <code>handle</code> method of the passed <code>CallbackHandler</code> to retrieve
     * the application name information.
     */
    public final static class AppContextCallback implements Callback {

        private String moduleID;

        /**
         * Get the fully qualified module name. The module name consists of the application name (if not a singleton) followed
         * by a '#' and the name of the module.
         *
         * <p>
         *
         * @return the application name.
         */
        public String getModuleID() {
            return moduleID;
        }

        /**
         * Set the fully qualified module name. The module name consists of the application name (if not a singleton) followed
         * by a '#' and the name of the module.
         *
         * @param moduleID
         */
        public void setModuleID(String moduleID) {
            this.moduleID = moduleID;
        }
    }
}
