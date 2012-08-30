/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Configuration;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.admin.rest.provider.RestModelProvider;

import org.glassfish.admin.rest.readers.RestModelReader;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientFactory;
import org.glassfish.jersey.client.filter.CsrfProtectionFilter;
import org.glassfish.jersey.jettison.JettisonBinder;
import org.glassfish.jersey.media.multipart.MultiPartClientBinder;

/**
 * This class wraps the Client returned by JerseyClientFactory. Using this class allows us to encapsulate many of the
 * client configuration concerns, such as registering the <code>CsrfProtectionFilter</code>.
 * @author jdlee
 */
public class RestClient implements Client {
    protected Client realClient;

    public RestClient() {
        this(new HashMap<String, String>());
    }

    /**
     * Create the client, as well as registering a <code>ClientRequestFilter</code> that adds the specified headers to
     * each request.
     * @param headers
     */
    public RestClient(final Map<String, String> headers) {
        realClient = JerseyClientFactory.newClient(new ClientConfig().
                binders(new MultiPartClientBinder(), new JettisonBinder()));
        realClient.configuration().register(new CsrfProtectionFilter());
        realClient.configuration().register(RestModelReader.class);
        realClient.configuration().register(RestModelProvider.class);
        realClient.configuration().register(new ClientRequestFilter() {

            @Override
            public void filter(ClientRequestContext rc) throws IOException {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    rc.getHeaders().add(entry.getKey(), entry.getValue());
                }
            }

        });
    }

    @Override
    public void close() {
        realClient.close();
    }

    @Override
    public Configuration configuration() {
        return realClient.configuration();
    }

    @Override
    public WebTarget target(String uri) throws IllegalArgumentException, NullPointerException {
        return realClient.target(uri);
    }

    @Override
    public WebTarget target(URI uri) throws NullPointerException {
        return realClient.target(uri);
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) throws NullPointerException {
        return realClient.target(uriBuilder);
    }

    @Override
    public WebTarget target(Link link) throws NullPointerException {
        return realClient.target(link);
    }

    @Override
    public Invocation invocation(Link link) throws NullPointerException, IllegalArgumentException {
        return realClient.invocation(link);
    }

    @Override
    public Invocation invocation(Link link, Entity<?> entity) throws NullPointerException, IllegalArgumentException {
        return realClient.invocation(link, entity);
    }
}
