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

    private static final String GET_RESULT = "{\"request_id\":\"666f6576-8e81-5938-fcc8-bc8fe914c219\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":36000,\"data\":{\"key\":\"value\"},\"wrap_info\":null,\"warnings\":null,\"auth\":null}";

    @Mock
    private Client client;

    @InjectMocks
    private HashiCorpSecretsConfigSource configSource = new HashiCorpSecretsConfigSource();

    @Before
    public void initMocks() {
        // Create fake config that returns the fake endpoint address
        final HashiCorpSecretsConfigSourceConfiguration config = mock(HashiCorpSecretsConfigSourceConfiguration.class);
        when(config.getVaultAddress()).thenReturn(FAKE_ENDPOINT);
        configSource.setConfiguration(config);

        // Configure the vault token
        configSource.hashiCorpVaultToken = FAKE_TOKEN;

        // Create fake web target to return the expected response
        final WebTarget fakeTarget = mock(WebTarget.class);
        when(client.target(FAKE_ENDPOINT)).thenReturn(fakeTarget);
        final Invocation.Builder fakeBuilder = mock(Invocation.Builder.class);
        when(fakeTarget.request()).thenReturn(fakeBuilder);
        when(fakeBuilder.accept(MediaType.APPLICATION_JSON)).thenReturn(fakeBuilder);
        when(fakeBuilder.header("Authorization", "Bearer " + FAKE_TOKEN)).thenReturn(fakeBuilder);
        final Response fakeResponse = mock(Response.class);
        when(fakeBuilder.get()).thenReturn(fakeResponse);
        when(fakeResponse.getStatus()).thenReturn(200);
        when(fakeResponse.getEntity()).thenReturn(new ByteArrayInputStream(GET_RESULT.getBytes()));
    }

    @Test
    public void test() {
        assertEquals("Incorrect property value", "value", configSource.getValue("key"));
    }
    
}
