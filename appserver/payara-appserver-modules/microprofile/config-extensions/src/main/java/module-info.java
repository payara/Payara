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

/**
 * JPMS module descriptor for the Payara MicroProfile Config Extensions module.
 *
 * This module provides additional ConfigSource implementations backed by cloud secret
 * managers (AWS, Azure, GCP, HashiCorp), LDAP, DynamoDB, and TOML files.
 *
 * Config sources are registered via HK2 @Service annotations (not Java ServiceLoader),
 * so no 'provides org.eclipse.microprofile.config.spi.ConfigSource' declaration is needed.
 *
 * OSGi metadata continues to be Bnd-generated. This file adds JPMS named-module semantics
 * while the existing MANIFEST.MF (Bundle-SymbolicName, Export-Package, Import-Package)
 * continues to serve the OSGi runtime — the dual-presence strategy.
 *
 * Automatic-module note: microprofile-config-api, hk2-api, hk2-config, and Payara internal
 * modules appear as automatic modules on the module path with the derived names used here.
 * Named (MR JAR) modules: nimbus-jose-jwt, jersey-client, all jackson-* artifacts.
 */
module fish.payara.microprofile.config.extensions {

    // ── Spec and standard Jakarta APIs ───────────────────────────────────────
    // automatic module: microprofile-config-api-*.jar → org.eclipse.microprofile.config
    requires org.eclipse.microprofile.config;
    // named modules
    requires jakarta.annotation;
    requires jakarta.cdi;
    requires jakarta.inject;
    // jakarta.json-api → jakarta.json  (JSON streaming, used by AWS / GCP sources)
    requires jakarta.json;
    // jakarta.ws.rs-api → jakarta.ws.rs  (JAX-RS client for HTTP-based sources)
    requires jakarta.ws.rs;

    // ── Payara internal modules (automatic: no module-info.java yet) ──────────
    // microprofile-config-service-*.jar → microprofile.config.service
    requires microprofile.config.service;
    // internal-api-*.jar → internal.api  (Globals, ServerEnvironment, etc.)
    requires internal.api;

    // ── HK2 (automatic modules) ───────────────────────────────────────────────
    // hk2-api-*.jar → org.glassfish.hk2.api
    requires org.glassfish.hk2.api;
    // hk2-config-*.jar → hk2.config  (@Configured, ConfigBeanProxy, DomDocument, etc.)
    requires hk2.config;

    // ── GlassFish APIs (automatic) ────────────────────────────────────────────
    // glassfish-api-*.jar → glassfish.api  (ServerEnvironment, AdminCommand wiring)
    requires glassfish.api;

    // ── Third-party named modules (MR JAR = multi-release named module) ───────
    // nimbus-jose-jwt (named MR) → com.nimbusds.jose.jwt  (AWS/OAuth2 JWT/JOSE)
    requires com.nimbusds.jose.jwt;
    // jersey-client (named) → org.glassfish.jersey.core.client
    requires org.glassfish.jersey.core.client;
    // jackson-core (named MR) → com.fasterxml.jackson.core
    requires com.fasterxml.jackson.core;
    // jackson-databind (named MR) → com.fasterxml.jackson.databind
    requires com.fasterxml.jackson.databind;
    // jackson-dataformat-toml (named MR) → com.fasterxml.jackson.dataformat.toml
    requires com.fasterxml.jackson.dataformat.toml;

    // ── GlassFish / Payara admin config framework (automatic) ────────────────
    // config-api-*.jar → config.api  (org.glassfish.config.support annotations on admin commands)
    requires config.api;
    // common-util-*.jar → common.util  (com.sun.enterprise.util used in Set* admin commands)
    requires common.util;
    // jersey-common (named) → org.glassfish.jersey.core.common  (jersey logging in AwsRequestBuilder)
    requires org.glassfish.jersey.core.common;

    // ── Additional Jakarta APIs ───────────────────────────────────────────────
    // jakarta.xml.bind-api (named) → jakarta.xml.bind  (used in AWS/Azure sources)
    requires jakarta.xml.bind;
    // jakarta.validation-api (named) → jakarta.validation  (used in DynamoDB admin commands)
    requires jakarta.validation;

    // ── JDK modules ───────────────────────────────────────────────────────────
    // java.util.logging (Logger, Level) — used across all config source classes
    requires java.logging;
    // java.beans (PropertyChangeEvent etc.) — used in Set* HK2 config admin commands
    requires java.desktop;
    // javax.naming, javax.naming.directory, javax.naming.ldap  (LDAP config source)
    requires java.naming;

    // ── Exported packages ─────────────────────────────────────────────────────
    exports fish.payara.microprofile.config.extensions.aws;
    exports fish.payara.microprofile.config.extensions.aws.client;
    exports fish.payara.microprofile.config.extensions.azure;
    exports fish.payara.microprofile.config.extensions.azure.admin;
    exports fish.payara.microprofile.config.extensions.azure.model;
    exports fish.payara.microprofile.config.extensions.dynamodb;
    exports fish.payara.microprofile.config.extensions.dynamodb.admin;
    exports fish.payara.microprofile.config.extensions.gcp;
    exports fish.payara.microprofile.config.extensions.gcp.model;
    exports fish.payara.microprofile.config.extensions.hashicorp;
    exports fish.payara.microprofile.config.extensions.hashicorp.admin;
    exports fish.payara.microprofile.config.extensions.hashicorp.model;
    exports fish.payara.microprofile.config.extensions.ldap;
    exports fish.payara.microprofile.config.extensions.ldap.admin;
    exports fish.payara.microprofile.config.extensions.oauth;
    exports fish.payara.microprofile.config.extensions.toml;
    exports fish.payara.microprofile.config.extensions.toml.admin;

    // ── Deep opens for runtime framework access ───────────────────────────────
    // HK2 config generator and inhabitant scanner need reflective access to
    // configuration beans and @Service-annotated classes at startup.
    // CDI (Weld) requires opens to create proxies for @Inject injection points.
    // Unqualified opens also satisfy Mockito's byte-buddy subclassing when tests
    // run from the unnamed module (classpath).
    opens fish.payara.microprofile.config.extensions.aws;
    opens fish.payara.microprofile.config.extensions.aws.client;
    opens fish.payara.microprofile.config.extensions.azure;
    opens fish.payara.microprofile.config.extensions.azure.admin;
    opens fish.payara.microprofile.config.extensions.dynamodb;
    opens fish.payara.microprofile.config.extensions.dynamodb.admin;
    opens fish.payara.microprofile.config.extensions.gcp;
    opens fish.payara.microprofile.config.extensions.gcp.model;
    opens fish.payara.microprofile.config.extensions.hashicorp;
    opens fish.payara.microprofile.config.extensions.hashicorp.admin;
    opens fish.payara.microprofile.config.extensions.hashicorp.model;
    opens fish.payara.microprofile.config.extensions.ldap;
    opens fish.payara.microprofile.config.extensions.ldap.admin;
    opens fish.payara.microprofile.config.extensions.oauth;
    opens fish.payara.microprofile.config.extensions.toml;
    opens fish.payara.microprofile.config.extensions.toml.admin;
}
