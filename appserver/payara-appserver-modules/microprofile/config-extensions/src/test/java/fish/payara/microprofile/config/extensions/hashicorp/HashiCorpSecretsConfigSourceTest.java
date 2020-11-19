package fish.payara.microprofile.config.extensions.hashicorp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HashiCorpSecretsConfigSourceTest {

    private static final String FAKE_ENDPOINT = "http://fake-endpoint";
    private static final String FAKE_TOKEN = "FAKE_TOKEN";

    @Mock
    private Client client;

    @InjectMocks
    private HashiCorpSecretsConfigSource configSource = new HashiCorpSecretsConfigSource();
    private static HashiCorpSecretsConfigSourceConfiguration config;
    private static Response fakeResponse;

    @Before
    public void initMocks() {
        // Create fake config that returns the fake endpoint address
        config = mock(HashiCorpSecretsConfigSourceConfiguration.class);
        when(config.getVaultAddress()).thenReturn(FAKE_ENDPOINT);
        when(config.getPath()).thenReturn("/secret/my-secret");

        // Configure the vault token
        configSource.hashiCorpVaultToken = FAKE_TOKEN;

        // Create fake web target to return the expected response
        final WebTarget fakeTarget = mock(WebTarget.class);
        when(client.target(config.getVaultAddress() + "/v1" + config.getPath())).thenReturn(fakeTarget);
        final Invocation.Builder fakeBuilder = mock(Invocation.Builder.class);
        when(fakeTarget.request()).thenReturn(fakeBuilder);
        when(fakeBuilder.accept(MediaType.APPLICATION_JSON)).thenReturn(fakeBuilder);
        when(fakeBuilder.header("Authorization", "Bearer " + FAKE_TOKEN)).thenReturn(fakeBuilder);
        fakeResponse = mock(Response.class);
        when(fakeBuilder.get()).thenReturn(fakeResponse);
        when(fakeResponse.getStatus()).thenReturn(200);

    }

    @Test
    public void testApiVersion1() {
        final String apiVersion1GetResult = "{\"request_id\":\"666f6576-8e81-5938-fcc8-bc8fe914c219\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":36000,\"data\":{\"key\":\"value\"},\"wrap_info\":null,\"warnings\":null,\"auth\":null}";
        when(fakeResponse.getEntity()).thenReturn(new ByteArrayInputStream(apiVersion1GetResult.getBytes()));
        when(config.getApiVersion()).thenReturn("1");
        configSource.setConfiguration(config);
        assertEquals("Incorrect property value", "value", configSource.getValue("key"));
    }

    @Test
    public void testApiVersion2() {
        final String apiVersion2GetResult = "{\"request_id\":\"666f6576-8e81-5938-fcc8-bc8fe914c219\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":36000,\"data\": {\"data\": {\"key\": \"value\",\"secretkey\": \"secretvalue\"}},\"wrap_info\":null,\"warnings\":null,\"auth\":null}";
        when(fakeResponse.getEntity()).thenReturn(new ByteArrayInputStream(apiVersion2GetResult.getBytes()));
        when(config.getApiVersion()).thenReturn("2");
        configSource.setConfiguration(config);
        assertEquals("Incorrect property value", "value", configSource.getValue("key"));
    }
}
