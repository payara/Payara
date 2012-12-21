/*
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 */
package org.glassfish.security.services.impl.authorization;

import org.glassfish.security.services.api.authorization.*;
import org.glassfish.security.services.api.common.Attribute;
import org.glassfish.security.services.impl.common.AttributeImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.security.auth.Subject;
import java.net.URI;
import java.security.AllPermission;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.*;
import static org.glassfish.security.services.impl.authorization.AuthorizationServiceImpl.InitializationState.*;

/**
 * @see AuthorizationServiceImpl
 */
public class AuthorizationServiceImplTest {
    private AuthorizationServiceImpl impl;


    @Before
    public void setUp() throws Exception {
        impl = new AuthorizationServiceImpl();
    }

    @After
    public void tearDown() throws Exception {
        impl = null;
    }

    @Test
    public void testInitialize() throws Exception {

        assertSame( "NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState() );

        try {
            impl.initialize(null);
            fail( "Expected initialize to fail." );
        } catch ( RuntimeException e ) {
        }

        assertSame( "FAILED_INIT", FAILED_INIT, impl.getInitializationState() );
        assertNotNull( "getReasonInitializationFailed", impl.getReasonInitializationFailed() );
    }

    @Test
    public void testIsPermissionGranted() throws Exception {

        assertSame( "NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState() );

        // Does not require service initialization
        impl.isPermissionGranted( new Subject(), new AllPermission() );
    }

    @Test
    public void testIsAuthorized() throws Exception {

        assertSame( "NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState() );

        try {
            impl.isAuthorized(
                new Subject(),
                new URI( "admin:///accounts/account/myaccount" ),
                "update" );
            fail( "Expected fail not initialized." );
        } catch ( RuntimeException e ) {
        }

        assertSame("NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState());
        assertNotNull( "getReasonInitializationFailed", impl.getReasonInitializationFailed() );
    }

    @Test
    public void testGetAuthorizationDecision() throws Exception {

        assertSame( "NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState() );

        try {
            impl.getAuthorizationDecision(
                new AzSubjectImpl( new Subject() ),
                new AzResourceImpl( new URI( "admin:///accounts/account/myaccount" ) ),
                new AzActionImpl( "update" ) );
            fail( "Expected fail not initialized." );
        } catch ( RuntimeException e ) {
        }

        assertSame("NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState());
        assertNotNull( "getReasonInitializationFailed", impl.getReasonInitializationFailed() );
    }

    @Test
    public void testMakeAzSubject() throws Exception {
        try {
            impl.makeAzSubject(null);
            fail( "Expected fail with null." );
        } catch ( IllegalArgumentException e ) {
        }

        Subject s = new Subject();
        assertSame("Subject", s, impl.makeAzSubject(s).getSubject());
    }

    @Test
    public void testMakeAzResource() throws Exception {
        try {
            impl.makeAzResource(null);
            fail( "Expected fail with null." );
        } catch ( IllegalArgumentException e ) {
        }

        URI u = new URI( "admin:///" );
        assertSame("URI", u, impl.makeAzResource(u).getUri());
    }

    @Test
    public void testMakeAzAction() throws Exception {
        AzAction azAction;

        azAction = impl.makeAzAction( null );
        assertNotNull( azAction );
        assertNull( "Null", azAction.getAction() );


        String action = "update";
        azAction = impl.makeAzAction( action );
        assertNotNull(azAction);
        assertEquals("action", action, azAction.getAction());
    }

    @Test
    public void testFindOrCreateDeploymentContext() throws Exception {

        assertSame( "NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState() );

        try {
            impl.findOrCreateDeploymentContext("foo");
            fail( "Expected fail not initialized." );
        } catch ( RuntimeException e ) {
        }

        assertSame("NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState());
        assertNotNull("getReasonInitializationFailed", impl.getReasonInitializationFailed());
    }

    @Test
    public void testAttributeResolvers() throws Exception {
        assertEquals( "initial", 0 , impl.getAttributeResolvers().size() );

        final AzAttributeResolver testAr1 = new TestAttributeResolver( new AttributeImpl("1") );
        final AzAttributeResolver testAr2 = new TestAttributeResolver( new AttributeImpl("2") );

        assertTrue( "append 1", impl.appendAttributeResolver( testAr1 ) );
        assertFalse( "append 1", impl.appendAttributeResolver( testAr1 ) );
        assertTrue( "append 2", impl.appendAttributeResolver( testAr2 ) );
        assertFalse( "append 2", impl.appendAttributeResolver( testAr2 ) );

        List<AzAttributeResolver> arList = impl.getAttributeResolvers();
        assertEquals( "size after append", 2, arList.size());
        assertEquals( "append 1", "1", arList.get(0).resolve(null,null,null).getName() );
        assertEquals( "append 2", "2", arList.get(1).resolve(null,null,null).getName() );

        final AzAttributeResolver testAr3 = new TestAttributeResolver( new AttributeImpl("3") );
        final AzAttributeResolver testAr4 = new TestAttributeResolver( new AttributeImpl("4") );
        List<AzAttributeResolver> tempList = new ArrayList<AzAttributeResolver>();
        tempList.add(testAr3);
        tempList.add(testAr4);
        impl.setAttributeResolvers( tempList );

        List<AzAttributeResolver> arList2 = impl.getAttributeResolvers();
        assertEquals("after get list 2", 2, arList2.size());
        assertEquals("append 3", "3", arList2.get(0).resolve(null, null, null).getName());
        assertEquals( "append 4", "4", arList2.get(1).resolve(null,null,null).getName() );

        assertTrue( "removeAllAttributeResolvers", impl.removeAllAttributeResolvers() );
        assertFalse( "removeAllAttributeResolvers", impl.removeAllAttributeResolvers() );

        assertEquals( "final", 0 , impl.getAttributeResolvers().size() );
    }


    /**
     * Fake test class
     */
    private static class TestAttributeResolver implements AzAttributeResolver {

        final Attribute attr;

        private TestAttributeResolver(Attribute attr) {
            this.attr = attr;
        }

        @Override
        public Attribute resolve(
            String attributeName,
            AzAttributes collection,
            AzEnvironment environment) {
            return this.attr;
        }
    }
}
