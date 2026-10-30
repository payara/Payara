/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2024] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.spi;

import java.util.List;

import jakarta.validation.constraints.Min;

import fish.payara.nucleus.microprofile.config.source.DirConfigSource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;

/**
 * The configuration that configures the semantics of the MP {@link Config} implementation.
 *
 * First of all this is the ordinality for the different types of {@link ConfigSource}s.
 * The source with the highest ordinality takes precedence.
 *
 * @since 4.1.2.173
 * @author Steve Millidge (Payara Foundation)
 */
@Configured(name="microprofile-config")
public interface MicroprofileConfigConfiguration extends ConfigExtension {

    @Attribute(defaultValue = DirConfigSource.DEFAULT_DIR, dataType = String.class)
    String getSecretDir();
    void setSecretDir(String directory);
    
    @Attribute(defaultValue = "90", dataType = Integer.class)
    String getSecretDirOrdinality();
    void setSecretDirOrdinality(String message);

    @Attribute(defaultValue = "95", dataType = Integer.class)
    String getTomlOrdinality();
    void setTomlOrdinality(String value);

    @Attribute(defaultValue = "105", dataType = Integer.class)
    String getPasswordOrdinality();
    void setPasswordOrdinality(String message);
    
    @Attribute(defaultValue = "110", dataType = Integer.class)
    String getDomainOrdinality();
    void setDomainOrdinality(String message);
    
    @Attribute(defaultValue = "115", dataType = Integer.class)
    String getJndiOrdinality();
    void setJndiOrdinality(String message);

    @Attribute(defaultValue = "120", dataType = Integer.class)
    String getConfigOrdinality();
    void setConfigOrdinality(String message);
    
    @Attribute(defaultValue = "130", dataType = Integer.class)
    String getServerOrdinality();
    void setServerOrdinality(String message);

    @Attribute(defaultValue = "140", dataType = Integer.class)
    String getApplicationOrdinality();
    void setApplicationOrdinality(String message);

    @Attribute(defaultValue = "150", dataType = Integer.class)
    String getModuleOrdinality();
    void setModuleOrdinality(String message);

    @Attribute(defaultValue = "160", dataType = Integer.class)
    String getClusterOrdinality();
    void setClusterOrdinality(String message);
    
    @Attribute(defaultValue = "170", dataType = Integer.class)
    String getPayaraExpressionPropertiesOrdinality();
    void setPayaraExpressionPropertiesOrdinality(String message);

    @Attribute(defaultValue = "180", dataType = Integer.class)
    String getCloudOrdinality();
    void setCloudOrdinality(String value);
    
    @Attribute(defaultValue = "190", dataType = Integer.class)
    String getJdbcOrdinality();
    void setJdbcOrdinality(String message);

    @Attribute(defaultValue = "200", dataType = Integer.class)
    String getLdapOrdinality();
    void setLdapOrdinality(String message);

    /**
     * @return number of seconds any MP {@link Config} is cached. That means changes to value as provided by a
     *         {@link ConfigSource} do become visible after a maximum of this duration. When set to zero or less caching
     *         is disabled.
     */
    @Min(0)
    @Attribute(defaultValue = "60", dataType = Integer.class)
    String getCacheDurationSeconds();
    void setCacheDurationSeconds(String cacheDurationSeconds);

    @Element("*")
    List<ConfigSourceConfiguration> getConfigSourceConfigurationList();

    @DuckTyped
    <T extends ConfigSourceConfiguration> T getConfigSourceConfigurationByType(Class<T> type);

    class Duck {
        public static <T extends ConfigSourceConfiguration> T getConfigSourceConfigurationByType(MicroprofileConfigConfiguration config, Class<T> type) {
            for (ConfigSourceConfiguration configSourceConfiguration : config.getConfigSourceConfigurationList()) {
                try {
                    return type.cast(configSourceConfiguration);
                } catch (Exception e) {
                    // Do nothing
                }
            }
            return null;
        }
    }
}
