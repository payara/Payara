/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.nucleus.microprofile.config.source;

import com.sun.enterprise.security.store.DomainScopedPasswordAliasStore;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.internal.api.Globals;

import java.util.Set;
import java.util.HashSet;
import java.util.Objects;

/**
 *
 * @author steve
 */
public class PasswordAliasConfigSource extends PayaraConfigSource {

    private final DomainScopedPasswordAliasStore store;

    public PasswordAliasConfigSource() {
        store = Globals.getDefaultHabitat().getService(DomainScopedPasswordAliasStore.class);
    }

    @Override
    public int getOrdinal() {
        return Integer.parseInt(configService.getMPConfig().getPasswordOrdinality());
    }


    @Override
    public Map<String, String> getProperties() {
        Map<String,String> properties = new HashMap<>();
        store.keys().forEachRemaining(key -> properties.put(key, new String(store.get(key))));
        return properties;
    }

    @Override
    public Set<String> getPropertyNames() {
        Set<String> propertyNames = new HashSet<>();
        store.keys().forEachRemaining(propertyNames::add);
        return propertyNames;
    }


    @Override
    public String getValue(String name) {
        Objects.requireNonNull(name, "Name perameter cannot be null");

        String value = null;

        // Attempt to match literally against password store
        if (store.containsKey(name)) {
            value = new String(store.get(name));
        } else {
            // Check if the property being asked for is in the format ${ALIAS=xxx} and get the password associated with the alias if so
            if (TranslatedConfigView.getAlias(name) != null) {
                try {
                    value = TranslatedConfigView.getRealPasswordFromAlias(name);
                } catch (IllegalArgumentException iae) {
                    Logger.getLogger(PasswordAliasConfigSource.class.getName()).log(Level.FINEST, iae.getMessage());
                } catch (IOException | CertificateException | NoSuchAlgorithmException |
                        UnrecoverableKeyException | KeyStoreException exception) {
                    Logger.getLogger(PasswordAliasConfigSource.class.getName()).log(Level.FINE,
                            "Exception caught reading from Password Alias store", exception);
                }
            }
        }

        return value;
    }

    @Override
    public String getName() {
        return "Password Alias";
    }
}
