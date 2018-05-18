/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.microprofile.openapi.impl.model.info;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class InfoImpl extends ExtensibleImpl implements Info {

    protected String title;
    protected String description;
    protected String termsOfService;
    protected Contact contact;
    protected License license;
    protected String version;

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public Info title(String title) {
        setTitle(title);
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Info description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public String getTermsOfService() {
        return termsOfService;
    }

    @Override
    public void setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
    }

    @Override
    public Info termsOfService(String termsOfService) {
        setTermsOfService(termsOfService);
        return this;
    }

    @Override
    public Contact getContact() {
        return contact;
    }

    @Override
    public void setContact(Contact contact) {
        this.contact = contact;
    }

    @Override
    public Info contact(Contact contact) {
        setContact(contact);
        return this;
    }

    @Override
    public License getLicense() {
        return license;
    }

    @Override
    public void setLicense(License license) {
        this.license = license;
    }

    @Override
    public Info license(License license) {
        setLicense(license);
        return this;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public Info version(String version) {
        setVersion(version);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.info.Info from, Info to, boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setTitle(mergeProperty(to.getTitle(), from.title(), override));
        to.setVersion(mergeProperty(to.getVersion(), from.version(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        to.setTermsOfService(mergeProperty(to.getTermsOfService(), from.termsOfService(), override));
        if (!isAnnotationNull(from.license())) {
            if (to.getLicense() == null) {
                to.setLicense(new LicenseImpl());
            }
            LicenseImpl.merge(from.license(), to.getLicense(), override);
        }
        if (!isAnnotationNull(from.contact())) {
            if (to.getContact() == null) {
                to.setContact(new ContactImpl());
            }
            ContactImpl.merge(from.contact(), to.getContact(), override);
        }
    }

}
