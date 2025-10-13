/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright [2016-2025] [Payara Foundation and/or affiliates]

package com.sun.enterprise.admin.servermgmt.cli;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.enterprise.admin.servermgmt.pe.PEDomainsManager;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.HostAndPort;

import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * The change-master-password command for the DAS. This is a hidden command
 * which is called from change-master-password command.
 *
 * @author Bhakti Mehta
 */
@Service(name = "_change-master-password-das")
@PerLookup
public class ChangeMasterPasswordCommandDAS extends LocalDomainCommand {

    @Param(name = "domain", primary = true, optional = true)
    protected String domainName0;

    @Param(name = "savemasterpassword", optional = true, defaultValue = "false")
    protected boolean savemp;

    @Param(name = "masterpasswordlocation", optional = true)
    private String mpLocation;

    private static final LocalStringsImpl STRINGS = new LocalStringsImpl(ChangeMasterPasswordCommandDAS.class);

    @Override
    protected void validate() throws CommandException {
        String dName;
        if (domainName0 != null) {
            dName = domainName0;
        } else {
            dName = getDomainName();
        }
        setDomainName(dName);
        super.validate();
    }

    @Override
    protected int executeCommand() throws CommandException {

        try {
            HostAndPort adminAddress = getAdminAddress();
            if (isRunning(adminAddress.getHost(), adminAddress.getPort()))
                throw new CommandException(STRINGS.get("domain.is.running", getDomainName(), getDomainRootDir()));
            DomainConfig domainConfig = new DomainConfig(getDomainName(), getDomainsDir().getAbsolutePath());
            PEDomainsManager manager = new PEDomainsManager();
            String mp = super.readFromMasterPasswordFile();
            if (mp == null) {
                mp = passwords.get("AS_ADMIN_MASTERPASSWORD");
                if (mp == null) {
                    char[] mpCharArr = super.readPassword(STRINGS.get("current.mp"));
                    mp = mpCharArr != null ? new String(mpCharArr) : null;
                }
            }
            if (mp == null)
                throw new CommandException(STRINGS.get("no.console"));
            if (!super.verifyMasterPassword(mp))
                throw new CommandException(STRINGS.get("incorrect.mp"));
            char[] nmpCharArr = getPassword("newmasterpassword", STRINGS.get("new.mp"), STRINGS.get("new.mp.again"),
                    true);
            String nmp = nmpCharArr != null ? new String(nmpCharArr) : null;
            if (nmp == null)
                throw new CommandException(STRINGS.get("no.console"));

            // keytool requires password at least 6 characters
            if (nmp.length() < 6) {
                throw new CommandException(STRINGS.get("password.too.short"));
            }

            domainConfig.put(DomainConfig.K_MASTER_PASSWORD, mp);
            domainConfig.put(DomainConfig.K_NEW_MASTER_PASSWORD, nmp);
            domainConfig.put(DomainConfig.K_SAVE_MASTER_PASSWORD, savemp);
            if (savemp && mpLocation != null) {
                domainConfig.put(DomainConfig.K_MASTER_PASSWORD_LOCATION, mpLocation);
            }
            manager.changeMasterPassword(domainConfig);

            try {
                if (dataGridEncryptionEnabled()) {
                    logger.warning(
                            "Data grid encryption is enabled - " + "you will need to regenerate the encryption key");
                }
            } catch (IOException | XMLStreamException exception) {
                logger.warning("Could not determine if data grid encryption is enabled - "
                        + "you will need to regenerate the encryption key if it is");
            }

            try {
                checkAdditionalTrustAndKeyStores();
            } catch (IOException | XMLStreamException exception) {
                logger.warning("Could not determine if there were additional Key Stores or Trust stores, if the master-password has been updated, the password for the additional stores need updating in order to continue using them.");
            }

            return 0;
        } catch (Exception e) {
            throw new CommandException(e.getMessage(), e);
        }
    }

}
