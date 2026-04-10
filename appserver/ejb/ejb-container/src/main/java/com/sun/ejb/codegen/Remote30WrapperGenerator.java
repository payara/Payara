/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2025] [Payara Foundation and/or its affiliates]

package com.sun.ejb.codegen;

import com.sun.ejb.containers.InternalEJBContainerException;
import com.sun.ejb.containers.InternalRemoteException;
import com.sun.ejb.containers.RemoteBusinessWrapperBase;
import com.sun.enterprise.util.LocalStringManagerImpl;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.EJBException;
import jakarta.ejb.EJBObject;
import jakarta.ejb.EJBTransactionRequiredException;
import jakarta.ejb.EJBTransactionRolledbackException;
import jakarta.ejb.NoSuchEJBException;
import jakarta.transaction.TransactionRequiredException;
import jakarta.transaction.TransactionRolledbackException;
import java.lang.reflect.Method;
import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.glassfish.pfl.dynamic.codegen.spi.Expression;
import org.glassfish.pfl.dynamic.codegen.spi.Type;
import org.omg.CORBA.SystemException;

import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._String;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._arg;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._assign;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._body;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._call;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._cast;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._catch;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._class;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._constructor;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._data;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._define;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._end;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._expr;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._method;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._new;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._return;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._s;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._super;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._t;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._throw;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._try;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._v;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._void;

/**
 * 
 */
public class Remote30WrapperGenerator extends Generator {

    private static final LocalStringManagerImpl localStrings = 
            new LocalStringManagerImpl(Remote30WrapperGenerator.class);
    private static final Logger logger = Logger.getLogger(Remote30WrapperGenerator.class.getName());

    private final String remoteInterfaceName;
    private final Class<?> businessInterface;
    private final String remoteClientClassName;
    private final String remoteClientPackageName;
    private final String remoteClientSimpleName;
    private final Method[] methodsToGenerate;

    /**
     * Adds _Wrapper to the original name.
     *
     * @param businessIntf full class name
     */
    public static String getGeneratedRemoteWrapperName(final String businessIntf) {
        final String packageName = getPackageName(businessIntf);
        final String simpleName = getBaseName(businessIntf);
        final String generatedSimpleName = "_" + simpleName + "_Wrapper";
        return packageName == null ? generatedSimpleName : packageName + "." + generatedSimpleName;
    }

    /**
     * Construct the Wrapper generator with the specified deployment
     * descriptor and class loader.
     *
     * @throws GeneratorException
     */
    public Remote30WrapperGenerator(final ClassLoader classLoader, final String businessIntfName,
                                    final String remoteInterfaceName)
            throws GeneratorException {
        super(classLoader);
        this.remoteInterfaceName = remoteInterfaceName;

        try {
            this.businessInterface = classLoader.loadClass(businessIntfName);
        } catch (final ClassNotFoundException ex) {
            throw new GeneratorException(localStrings.getLocalString("generator.remote_interface_not_found",
                    "Business interface " + businessIntfName + " not found "));
        }

        if (EJBObject.class.isAssignableFrom(businessInterface)) {
            throw new GeneratorException("Invalid Remote Business Interface " + businessInterface
                    + ". A Remote Business interface MUST not extend jakarta.ejb.EJBObject.");
        }

        remoteClientClassName = getGeneratedRemoteWrapperName(businessIntfName);
        remoteClientPackageName = getPackageName(remoteClientClassName);
        remoteClientSimpleName = getBaseName(remoteClientClassName);

        methodsToGenerate = removeRedundantMethods(businessInterface.getMethods());

        // NOTE : no need to remove ejb object methods because EJBObject
        // is only visible through the RemoteHome view.
    }
    
    @Override
    protected String getPackageName() {
        return this.remoteClientPackageName;
    }

    @Override
    public String getGeneratedClassName() {
        return remoteClientClassName;
    }

    @Override
    public Class<?> getAnchorClass() {
        return businessInterface;
    }
    
    @Override
    public void defineClassBody() {
        _class(PUBLIC, remoteClientSimpleName,
                _t(RemoteBusinessWrapperBase.class.getName()),
                _t(businessInterface.getName()));

        _data(PRIVATE, _t(remoteInterfaceName), "delegate_");

        _constructor(PUBLIC);
        _arg(_t(remoteInterfaceName), "stub");
        _arg(_String(), "busIntf");

        _body();
        _expr(_super(_s(_void(), _t(Remote.class.getName()), _String()),
                _v("stub"), _v("busIntf")));
        _assign(_v("delegate_"), _v("stub"));
        _end();

        for (final Method method : methodsToGenerate) {
            printMethodImpl(method);
        }

        _end();
    }
    
    private void printMethodImpl(final Method m) {
        final List<Type> exceptionList = new LinkedList<>();
        for (final Class<?> exception : m.getExceptionTypes()) {
            exceptionList.add(Type.type(exception));
        }

        final Type returnType = Type.type(m.getReturnType());
        _method(PUBLIC, returnType, m.getName(), exceptionList);

        int i = 0;
        final List<Type> expressionListTypes = new LinkedList<>();
        final List<Expression> expressionList = new LinkedList<>();
        for (final Class<?> param : m.getParameterTypes()) {
            final String paramName = "param" + i;
            _arg(Type.type(param), paramName);
            i++;
            expressionListTypes.add(Type.type(param));
            expressionList.add(_v(paramName));
        }

        _body();

        _try();

        if (m.getReturnType() == void.class) {
            _expr(
                    _call(_v("delegate_"), m.getName(), _s(returnType, expressionListTypes), expressionList));
        } else {
            _return(
                    _call(_v("delegate_"), m.getName(), _s(returnType, expressionListTypes), expressionList));
        }

        final boolean doExceptionTranslation = !Remote.class.isAssignableFrom(businessInterface);
        if (doExceptionTranslation) {
            _catch(_t(TransactionRolledbackException.class.getName()), "trex");

            _define(_t(RuntimeException.class.getName()), "r",
                    _new(_t(EJBTransactionRolledbackException.class.getName()), _s(_void())));
            _expr(
                    _call(
                            _v("r"), "initCause",
                            _s(_t(Throwable.class.getName()), _t(Throwable.class.getName())),
                            _v("trex")
                    )
            );
            _throw(_v("r"));

            _catch(_t(TransactionRequiredException.class.getName()), "treqex");

            _define(_t(RuntimeException.class.getName()), "r",
                    _new(_t(EJBTransactionRequiredException.class.getName()), _s(_void())));

            _expr(
                    _call(
                            _v("r"), "initCause",
                            _s(_t(Throwable.class.getName()), _t(Throwable.class.getName())),
                            _v("treqex")
                    )
            );
            _throw(_v("r"));

            _catch(_t(NoSuchObjectException.class.getName()), "nsoe");

            _define(_t(RuntimeException.class.getName()), "r",
                    _new(_t(NoSuchEJBException.class.getName()), _s(_void())));
            _expr(
                    _call(
                            _v("r"), "initCause",
                            _s(_t(Throwable.class.getName()), _t(Throwable.class.getName())),
                            _v("nsoe")
                    )
            );
            _throw(_v("r"));

            _catch(_t(AccessException.class.getName()), "accex");

            _define(_t(RuntimeException.class.getName()), "r",
                    _new(_t(EJBAccessException.class.getName()), _s(_void())));
            _expr(
                    _call(
                            _v("r"), "initCause",
                            _s(_t(Throwable.class.getName()), _t(Throwable.class.getName())),
                            _v("accex")
                    )
            );
            _throw(_v("r"));

            _catch(_t(InternalEJBContainerException.class.getName()), "iejbcEx");

            _throw(
                    _cast(
                            _t(EJBException.class.getName()),
                            _call(_v("iejbcEx"), "getCause", _s(_t(Throwable.class.getName())))
                    )
            );


            _catch(_t(RemoteException.class.getName()), "re");

            _throw( _new( _t(EJBException.class.getName()), _s(_void(), _t(Exception.class.getName())), _v("re")));

            _catch( _t(SystemException.class.getName()), "corbaSysEx");
            _define( _t(RuntimeException.class.getName()), "r", _new( _t(EJBException.class.getName()), _s(_void())));
            _expr(
                    _call(
                            _v("r"), "initCause",
                            _s(_t(Throwable.class.getName()), _t(Throwable.class.getName())),
                            _v("corbaSysEx")
                    )
            );
            _throw(_v("r"));

            _end();

        } else {
            _catch(_t(InternalEJBContainerException.class.getName()), "iejbcEx");
            _throw(
                    _new( _t(InternalRemoteException.class.getName()),
                            _s(_void(), _t(InternalEJBContainerException.class.getName())),
                            _v("iejbcEx"))
            );
            _end();
        }

        _end();
    }

}
