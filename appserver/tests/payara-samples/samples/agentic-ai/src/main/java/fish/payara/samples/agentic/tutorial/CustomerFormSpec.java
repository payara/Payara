/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.agentic.tutorial;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Supplies the sample's form: a Customer registration form to contract Azul
 * Payara Server. Single source of truth for both the rendered form and the
 * generated tutorial.
 */
@ApplicationScoped
public class CustomerFormSpec {

    private final FormSpec spec = new FormSpec(
            "Contract Azul Payara Server",
            "Tell us about your organisation and your runtime needs; our team will get back to you.",
            List.of(
                    new FieldSpec("firstName", "First name", "text", true,
                            List.of(), "Given name of the main contact."),
                    new FieldSpec("lastName", "Last name", "text", true,
                            List.of(), "Family name of the main contact."),
                    new FieldSpec("businessEmail", "Business email", "email", true,
                            List.of(), "Work email we will use to follow up; personal inboxes are discouraged."),
                    new FieldSpec("company", "Company", "text", true,
                            List.of(), "Legal or trading name of the organisation."),
                    new FieldSpec("jobTitle", "Job title", "text", false,
                            List.of(), "Helps us route the enquiry to the right specialist."),
                    new FieldSpec("country", "Country", "select", true,
                            List.of("United States", "United Kingdom", "Germany", "Brazil",
                                    "India", "Japan", "Other"),
                            "Used for regional licensing and the correct sales contact."),
                    new FieldSpec("phone", "Phone", "tel", false,
                            List.of(), "Optional direct line for a faster conversation."),
                    new FieldSpec("message", "Requirements / How can we help?", "textarea", true,
                            List.of(), "Number of instances, environments, and any support expectations.")
            ));

    public FormSpec spec() {
        return spec;
    }
}
