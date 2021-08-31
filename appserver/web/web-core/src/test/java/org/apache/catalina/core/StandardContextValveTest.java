package org.apache.catalina.core;

import junit.framework.TestCase;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.web.valve.GlassFishValve;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StandardContextValveTest extends TestCase {

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private HttpServletResponse httpServletResponse;

    @InjectMocks
    private StandardContextValve standardContextValve = new StandardContextValve();

    @Test
    public void preventAccessToRestrictedDirectoryWithEmptyContextRoot() throws IOException, ServletException {
        DataChunk dataChunk = DataChunk.newInstance();
        dataChunk.setString("WEB-INF/web.xml");

        when(httpRequest.getCheckRestrictedResources()).thenReturn(true);
        when(httpRequest.getRequestPathMB()).thenReturn(dataChunk);
        when(httpResponse.getResponse()).thenReturn(httpServletResponse);

        int pipelineResult = standardContextValve.invoke(httpRequest, httpResponse);

        assertEquals(GlassFishValve.END_PIPELINE, pipelineResult);
        verify(httpRequest, times(1)).getCheckRestrictedResources();
        verify(httpRequest, times(1)).getRequestPathMB();
        verify(httpResponse, times(1)).getResponse();
        verify(httpServletResponse, times(1)).sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}