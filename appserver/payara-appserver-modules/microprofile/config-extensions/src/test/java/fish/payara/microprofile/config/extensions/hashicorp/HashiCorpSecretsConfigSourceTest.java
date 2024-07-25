/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.config.extensions.hashicorp;

import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HashiCorpSecretsConfigSourceTest {
    private static final String FAKE_TOKEN = "FAKE_TOKEN";
    private static final String FAKE_SECRETS_ENGINE_PATH = "KV";
    private static final String FAKE_SECRETS_PATH = "mySecrets";
    private static final String FAKE_VAULT_ADDRESS = "http://127.0.0.1:8200";

    @Mock
    private Client client;

    @InjectMocks
    private HashiCorpSecretsConfigSource configSource = new HashiCorpSecretsConfigSource();
    private static Response fakeResponse;
    private static WebTarget fakeTarget;
    private static MicroprofileConfigConfiguration fakeMPConfig;

    @Before
    public void initMocks() {
        // Configure the vault token
        configSource.hashiCorpVaultToken = FAKE_TOKEN;
        configSource.secretsEnginePath = FAKE_SECRETS_ENGINE_PATH;
        configSource.secretsPath = FAKE_SECRETS_PATH;
        configSource.vaultAddress = FAKE_VAULT_ADDRESS;

        // Create fake web target to return the expected response
        fakeTarget = mock(WebTarget.class);
        //when(client.target(config.getVaultAddress() + "/v1" + config.getPath())).thenReturn(fakeTarget);
        final Invocation.Builder fakeBuilder = mock(Invocation.Builder.class);
        when(fakeTarget.request()).thenReturn(fakeBuilder);
        when(fakeBuilder.accept(MediaType.APPLICATION_JSON)).thenReturn(fakeBuilder);
        when(fakeBuilder.header("Authorization", "Bearer " + FAKE_TOKEN)).thenReturn(fakeBuilder);
        fakeResponse = mock(Response.class);
        when(fakeBuilder.get()).thenReturn(fakeResponse);
        when(fakeResponse.getStatus()).thenReturn(200);
        
        fakeMPConfig = mock(MicroprofileConfigConfiguration.class);
        when(fakeMPConfig.getCloudOrdinality()).thenReturn("180");
        configSource.mpconfig = fakeMPConfig;

    }

    @Test
    public void testApiVersion1() {
        configSource.apiVersion = 1;
        when(client.target(FAKE_VAULT_ADDRESS + "/v1/" + FAKE_SECRETS_ENGINE_PATH + "/" + FAKE_SECRETS_PATH)).thenReturn(fakeTarget);
        final String apiVersion1GetResult = "{\"request_id\":\"666f6576-8e81-5938-fcc8-bc8fe914c219\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":36000,\"data\":{\"key\":\"value\"},\"wrap_info\":null,\"warnings\":null,\"auth\":null}";
        when(fakeResponse.getEntity()).thenReturn(new ByteArrayInputStream(apiVersion1GetResult.getBytes()));
        assertEquals("Incorrect property value", "value", configSource.getValue("key"));
    }

    @Test
    public void testApiVersion2() {
        configSource.apiVersion = 2;
        when(client.target(FAKE_VAULT_ADDRESS + "/v1/" + FAKE_SECRETS_ENGINE_PATH + "/data/" + FAKE_SECRETS_PATH)).thenReturn(fakeTarget);
        final String apiVersion2GetResult = "{\"request_id\":\"666f6576-8e81-5938-fcc8-bc8fe914c219\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":36000,\"data\": {\"data\": {\"key\": \"value\",\"secretkey\": \"secretvalue\"}},\"wrap_info\":null,\"warnings\":null,\"auth\":null}";
        when(fakeResponse.getEntity()).thenReturn(new ByteArrayInputStream(apiVersion2GetResult.getBytes()));;
        assertEquals("Incorrect property value", "value", configSource.getValue("key"));
    }
    
    @Test
    public void testOrdinal() {
        assertEquals(180, configSource.getOrdinal());
    }
}
