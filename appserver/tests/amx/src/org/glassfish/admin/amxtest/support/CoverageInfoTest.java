/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.admin.amxtest.support;

import com.sun.appserv.management.ext.coverage.CoverageInfoImpl;

import javax.management.MBeanInfo;

/**
 */
public final class CoverageInfoTest
        extends junit.framework.TestCase {
    public CoverageInfoTest() {
    }


    public CoverageInfoImpl
    create(final MBeanInfo info) {
        return new CoverageInfoImpl(info);
    }


    public void
    testCreate() {
        CoverageInfoImpl impl = create(null);

        // verify that we fail in null MBeanInfo case
        try {
            impl.getNumReadableAttributes();
            assert (false);
        } catch (Exception e) {
        }
        try {
            impl.getNumWriteableAttributes();
            assert (false);
        } catch (Exception e) {
        }
        try {
            impl.getNumWriteableAttributes();
            assert (false);
        } catch (Exception e) {
        }

        /*
        impl = create(getTestMBeanInfo());
        assert (impl.getReadableAttributes() != null);
        assert (impl.getWriteableAttributes() != null);
        assert (impl.getOperations() != null);
        assert impl.getNumReadableAttributes() != 0;
        assert impl.getNumWriteableAttributes() != 0;
        assert impl.getNumOperations() != 0;
        assert impl.getAttributesRead().size() == 0;
        assert impl.getAttributesWritten().size() == 0;
        assert impl.getOperationsInvoked().size() == 0;
        */
    }
    
    
/*



    private MBeanInfo
    getTestMBeanInfo() {
        return MBeanInfoConverter.getInstance().convert(DomainConfig.class, null);
    }


	    public void
    testAttributes()
    {
        final MBeanInfo mbeanInfo    = getTestMBeanInfo();

        final CoverageInfoImpl  impl    = create( mbeanInfo );
        assert( mbeanInfo == impl.getMBeanInfo() );

        final MBeanAttributeInfo[]  attrInfos   = mbeanInfo.getAttributes();

        for( final MBeanAttributeInfo attrInfo : attrInfos )
        {
            final String    name    = attrInfo.getName();

            if ( attrInfo.isReadable() )
            {
                assert( impl.getReadableAttributes().contains( name ) );
                impl.attributeWasRead( name );
                assert( impl.getAttributesRead().contains( name ) );
                assert( ! impl.getAttributesNotRead().contains( name ) );
            }

            if ( attrInfo.isWritable() )
            {
                assert( impl.getWriteableAttributes().contains( name ) );
                impl.attributeWasWritten( name );
                assert( impl.getAttributesWritten().contains( name ) );
                assert( ! impl.getAttributesNotWritten().contains( name ) );
            }
        }

        assert( impl.getAttributesNotRead().size() == 0 );
        assert( impl.getAttributeReadCoverage() == 100 );
        assert( impl.getAttributeGetFailures().size() == 0 );

        assert( impl.getAttributesNotWritten().size() == 0 );
        assert( impl.getAttributeWriteCoverage() == 100 );
        assert( impl.getAttributeSetFailures().size() == 0 );


        final String BOGUS  = "bogus";
        impl.attributeWasRead( BOGUS );
        impl.attributeWasWritten( BOGUS );

        assert( impl.getUnknownAttributes().keySet().contains( BOGUS ) );
        assert( impl.getUnknownAttributes().keySet().size() == 1 );
        assert( impl.getAttributeGetFailures().size() == 0 );
        assert( impl.getAttributeSetFailures().size() == 0 );

        final MBeanAttributeInfo    attr    = attrInfos[ 0 ];
        impl.attributeGetFailure( attr.getName() );
        impl.attributeGetFailure( attr.getName() );
        assert( impl.getAttributeGetFailures().size() == 1 );
        impl.attributeSetFailure( attr.getName() );
        impl.attributeSetFailure( attr.getName() );
        assert( impl.getAttributeSetFailures().size() == 1 );
    }


        public void
    testOperations()
    {
        final MBeanInfo             mbeanInfo        = getTestMBeanInfo();
        final MBeanOperationInfo[]  operationInfos   = mbeanInfo.getOperations();

        final CoverageInfoImpl  impl    = create( mbeanInfo );
        assert( mbeanInfo == impl.getMBeanInfo() );

        //--------------------------------------------------------------------
        // verify that operationWasInvoked() works

        for( final MBeanOperationInfo operationInfo : operationInfos )
        {
            final String    name    = operationInfo.getName();
            final String[]  sig     = JMXUtil.getSignature( operationInfo.getSignature() );

            impl.operationWasInvoked( name, sig );
        }

        assert( impl.getOperationCoverage() == 100 ) :
            "Expected coverage of 100%, got " + impl.getOperationCoverage();

        assert( impl.getUnknownOperations().size() == 0 );
        assert( impl.getOperationsNotInvoked().size() == 0 );
        assert( impl.getInvocationFailures().size() == 0 );
        impl.toString( true );
        impl.toString( false );


        //--------------------------------------------------------------------
        // verify that markAsInvoked() works

        final Set<String>   invoked = impl.getOperationsInvoked();
        impl.clear();
        for( final String op : invoked )
        {
            impl.markAsInvoked( op );
        }

        assert( impl.getOperationCoverage() == 100 ) :
            "Expected coverage of 100%, got " + impl.getOperationCoverage();
        assert( impl.getUnknownOperations().size() == 0 );
        assert( impl.getOperationsNotInvoked().size() == 0 );
        assert( impl.getInvocationFailures().size() == 0 );

        final String DUMMY_OPERATION    = "dummyOperationName";
        impl.operationWasInvoked( DUMMY_OPERATION, null );
        impl.operationWasInvoked( DUMMY_OPERATION, null );
        assert( impl.getUnknownOperations().size() == 1 );
        impl.toString( true );
        impl.toString( false );


        //--------------------------------------------------------------------
        // verify that failures are tracked correctly
        impl.clear();
        for( final MBeanOperationInfo operationInfo : operationInfos )
        {
            final String    name    = operationInfo.getName();
            final String[]  sig     = JMXUtil.getSignature( operationInfo.getSignature() );

            impl.operationWasInvoked( name, sig );
            impl.operationFailed( name, sig );
        }
        assert( impl.getOperationCoverage() == 100 ) :
            "Expected coverage of 100%, got " + impl.getOperationCoverage();
        assert( impl.getUnknownOperations().size() == 0 );
        assert( impl.getOperationsNotInvoked().size() == 0 );
        assert( impl.getOperationsInvoked().size() == operationInfos.length );
        assert( impl.getInvocationFailures().size() == operationInfos.length );
        impl.toString( true );
        impl.toString( false );


        //--------------------------------------------------------------------
        // verify that we can't call operationFailed() on an illegal operation
        try
        {
            impl.operationFailed( "foo", null );
            assert( false ) : "expected failure when calling operationFailed()";
        }
        catch( IllegalArgumentException e )
        {
        }
        impl.toString( true );
        impl.toString( false );
        impl.toString();
    }*/
}























