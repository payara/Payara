/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.naming.impl;

import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ProcessEnvironment.ProcessType;
import org.glassfish.api.naming.ComponentNamingUtil;
import org.glassfish.internal.api.Globals;
import org.glassfish.hk2.api.ServiceLocator;

import javax.naming.*;
import java.util.Hashtable;
import java.util.logging.Level;

import static com.sun.enterprise.naming.util.LogFacade.logger;

/**
 * This class is a context implementation for the java:comp namespace.
 * The context determines the component id from the invocation manager
 * of the component that is invoking the method and then looks up the
 * object in that component's local namespace.
 */
public final class JavaURLContext implements Context, Cloneable {
    private static GlassfishNamingManagerImpl namingManager;

    private Hashtable myEnv;
    //private Context ctx; XXX not needed ?
    private String myName = "";

    private SerialContext serialContext = null;

    /**
     * Create a context with the specified environment.
     */
    public JavaURLContext(Hashtable environment) throws NamingException {
        myEnv = (environment != null) ? (Hashtable) (environment.clone())
                : null;
    }

    /**
     * Create a context with the specified name+environment.
     * Called only from GlassfishNamingManager.
     */
    public JavaURLContext(String name, Hashtable env)
            throws NamingException {
        this(env);
        this.myName = name;
    }

    /**
     * this constructor is called from SerialContext class
     */
    public JavaURLContext(Hashtable env, SerialContext serialContext)
            throws NamingException {
        this(env);
        this.serialContext = serialContext;
    }

    public JavaURLContext( JavaURLContext ctx, SerialContext sctx ) {
        this.myName = ctx.myName ;
        this.myEnv = ctx.myEnv ;
        this.serialContext = sctx ;
    }

    static void setNamingManager(GlassfishNamingManagerImpl mgr) {
        namingManager = mgr;
    }

    /**
     * add SerialContext to preserve stickiness to cloned instance
     * why clone() : to avoid the case of multiple threads modifying
     * the context returned for ctx.lookup("java:com/env/ejb")
     */
    /**  STICKY context not enabled
    public JavaURLContext addStickyContext(SerialContext serialContext)
            throws NamingException {
        try {
            JavaURLContext jCtx = (JavaURLContext) this.clone();
            jCtx.serialContext = serialContext;
            return jCtx;
        } catch (java.lang.CloneNotSupportedException ex) {
            NamingException ne = new NamingException(
                    "problem with cloning javaURLContext instance");
            ne.initCause(ex);
            throw ne;
        }
    }
    **/

    /**
     * Lookup an object in the serial context.
     *
     * @return the object that is being looked up.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public Object lookup(String name) throws NamingException {
        if (name.equals("")) {
            /**
             * javadocs for Context.lookup: If name is empty, returns a new
             * instance of this context (which represents the same naming
             * context as this context, but its environment may be modified
             * independently and it may be accessed concurrently).
             */
            return new JavaURLContext(myName, myEnv);
        }

        String fullName = name;
        if (!myName.equals("")) {
            if (myName.equals("java:"))
                fullName = myName + name;
            else
                fullName = myName + "/" + name;
        }

        try {
            Object obj = null;
            // If we know for sure it's an entry within an environment namespace
            if (isLookingUpEnv(fullName)) {
                // refers to a dependency defined by the application
                obj = namingManager.lookup(fullName, serialContext);
            } else {
                // It's either an application-defined dependency in a java:
                // namespace or a special EE platform object.
                // Check for EE platform objects first to prevent overriding.  
                obj = NamedNamingObjectManager.tryNamedProxies(name);
                if (obj == null) {
                    // try GlassfishNamingManager
                    obj = namingManager.lookup(fullName, serialContext);
                }
            }

            if( obj == null ) {
                throw new NamingException("No object found for " + name);
            }

            return obj;
        } catch (NamingException ex) {

            ServiceLocator services = Globals.getDefaultHabitat();
            ProcessEnvironment processEnv = services.getService(ProcessEnvironment.class);
            if( fullName.startsWith("java:app/") &&
                processEnv.getProcessType() == ProcessType.ACC ) {

                // This could either be an attempt by an app client to access a portable
                // remote session bean JNDI name via the java:app namespace or a lookup of
                // an application-defined java:app environment dependency.  Try them in
                // that order.

                Context ic = namingManager.getInitialContext();
                String appName = (String) namingManager.getInitialContext().lookup("java:app/AppName");

                Object obj = null;

                if (!fullName.startsWith("java:app/env/")
                    || !"java:app/env".equals(fullName)) {
                    try {

                        // Translate the java:app name into the equivalent java:global name so that
                        // the lookup will be resolved by the server.
                        String newPrefix = "java:global/" + appName + "/";

                        int javaAppLength = "java:app/".length();
                        String globalLookup = newPrefix + fullName.substring(javaAppLength);

                        obj = ic.lookup(globalLookup);

                    } catch(NamingException javaappenvne) {
                        logger.log(Level.FINE, "Trying global version of java:app ejb lookup", javaappenvne);
                    }
                }

                if( obj == null ) {
                   ComponentNamingUtil compNamingUtil = services.getService(ComponentNamingUtil.class);
                   String internalGlobalJavaAppName =
                    compNamingUtil.composeInternalGlobalJavaAppName(appName, fullName);

                    obj = ic.lookup(internalGlobalJavaAppName);
                }

                if( obj == null ) {
                    throw new NamingException("No object found for " + name);
                }

                return obj;
               
            }

            throw ex;
        } catch (Exception ex) {
            throw (NamingException) (new NameNotFoundException(
                    "No object bound for " + fullName)).initCause(ex);
        }
    }

    /**
     * Lookup a name in either the cosnaming or serial context.
     *
     * @return the object that is being looked up.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public Object lookup(Name name) throws NamingException {
        // Flat namespace; no federation; just call string version
        return lookup(name.toString());
    }

    /**
     * Bind an object in the namespace. Binds the reference to the
     * actual object in either the cosnaming or serial context.
     *
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void bind(String name, Object obj) throws NamingException {
        throw new NamingException("java:comp namespace cannot be modified");
    }

    /**
     * Bind an object in the namespace. Binds the reference to the
     * actual object in either the cosnaming or serial context.
     *
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void bind(Name name, Object obj) throws NamingException {
        throw new NamingException("java:comp namespace cannot be modified");
    }

    /**
     * Rebind an object in the namespace. Rebinds the reference to the
     * actual object in either the cosnaming or serial context.
     *
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void rebind(String name, Object obj) throws NamingException {
        throw new NamingException("java:comp namespace cannot be modified");
    }

    /**
     * Rebind an object in the namespace. Rebinds the reference to the
     * actual object in either the cosnaming or serial context.
     *
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        throw new NamingException("java:comp namespace cannot be modified");
    }

    /**
     * Unbind an object from the namespace.
     *
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void unbind(String name) throws NamingException {
        throw new NamingException("java:comp namespace cannot be modified");
    }

    /**
     * Unbind an object from the namespace.
     *
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void unbind(Name name) throws NamingException {
        throw new NamingException("java:comp namespace cannot be modified");
    }

    /**
     * The rename operation is not supported by this context. It throws
     * an OperationNotSupportedException.
     */
    @Override
    public void rename(String oldname, String newname) throws NamingException {
        throw new NamingException("java:comp namespace cannot be modified");
    }

    /**
     * The rename operation is not supported by this context. It throws
     * an OperationNotSupportedException.
     */
    @Override
    public void rename(Name oldname, Name newname)
            throws NamingException {
        throw new NamingException("java:comp namespace cannot be modified");
    }

    /**
     * The destroySubcontext operation is not supported by this context.
     * It throws an OperationNotSupportedException.
     */
    @Override
    public void destroySubcontext(String name) throws NamingException {
        throw new NamingException("java:comp namespace cannot be modified");
    }

    /**
     * The destroySubcontext operation is not supported by this context.
     * It throws an OperationNotSupportedException.
     */
    @Override
    public void destroySubcontext(Name name) throws NamingException {
        throw new NamingException("java:comp namespace cannot be modified");
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        throw new NamingException("java:comp namespace cannot be modified");
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        throw new NamingException("java:comp namespace cannot be modified");
    }


    /**
     * Lists the contents of a context or subcontext. The operation is
     * delegated to the serial context.
     *
     * @return an enumeration of the contents of the context.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public NamingEnumeration<NameClassPair> list(String name)
            throws NamingException {
        if (name.equals("")) {
            // listing this context
            if (namingManager == null)
                throw new NamingException();
            return namingManager.list(myName);
        }

        // Check if 'name' names a context
        Object target = lookup(name);
        if (target instanceof Context) {
            return ((Context) target).list("");
        }
        throw new NotContextException(name + " cannot be listed");
    }

    /**
     * Lists the contents of a context or subcontext. The operation is
     * delegated to the serial context.
     *
     * @return an enumeration of the contents of the context.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public NamingEnumeration<NameClassPair> list(Name name)
            throws NamingException {
        // Flat namespace; no federation; just call string version
        return list(name.toString());
    }

    /**
     * Lists the bindings of a context or subcontext. The operation is
     * delegated to the serial context.
     *
     * @return an enumeration of the bindings of the context.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public NamingEnumeration<Binding> listBindings(String name)
            throws NamingException {
        if (name.equals("")) {
            // listing this context
            if (namingManager == null)
                throw new NamingException();
            return namingManager.listBindings(myName);
        }

        // Perhaps 'name' names a context
        Object target = lookup(name);
        if (target instanceof Context) {
            return ((Context) target).listBindings("");
        }
        throw new NotContextException(name + " cannot be listed");
    }

    /**
     * Lists the bindings of a context or subcontext. The operation is
     * delegated to the serial context.
     *
     * @return an enumeration of the bindings of the context.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public NamingEnumeration<Binding> listBindings(Name name)
            throws NamingException {
        // Flat namespace; no federation; just call string version
        return listBindings(name.toString());
    }

    /**
     * This context does not treat links specially. A lookup operation is
     * performed.
     */
    @Override
    public Object lookupLink(String name) throws NamingException {
        // This flat context does not treat links specially
        return lookup(name);
    }

    /**
     * This context does not treat links specially. A lookup operation is
     * performed.
     */
    @Override
    public Object lookupLink(Name name) throws NamingException {
        // Flat namespace; no federation; just call string version
        return lookupLink(name.toString());
    }

    /**
     * Return the name parser for the specified name.
     *
     * @return the NameParser instance.
     * @throws NamingException if there is an exception.
     */
    @Override
    public NameParser getNameParser(String name)
            throws NamingException {
        if (namingManager == null)
            throw new NamingException();
        return namingManager.getNameParser();
    }

    /**
     * Return the name parser for the specified name.
     *
     * @return the NameParser instance.
     * @throws NamingException if there is an exception.
     */
    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        // Flat namespace; no federation; just call string version
        return getNameParser(name.toString());
    }

    @Override
    public String composeName(String name, String prefix)
            throws NamingException {
        Name result = composeName(new CompositeName(name),
                new CompositeName(prefix));
        return result.toString();
    }

    @Override
    public Name composeName(Name name, Name prefix)
            throws NamingException {
        Name result = (Name) (prefix.clone());
        result.addAll(name);
        return result;
    }

    /**
     * Add a property to the environment.
     */
    @Override
    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException {
        if (myEnv == null) {
            myEnv = new Hashtable(5, 0.75f);
        }
        return myEnv.put(propName, propVal);
    }

    /**
     * Remove a property from the environment.
     */
    @Override
    public Object removeFromEnvironment(String propName)
            throws NamingException {
        if (myEnv == null) {
            return null;
        }
        return myEnv.remove(propName);
    }

    /**
     * Get the context's environment.
     */
    @Override
    public Hashtable getEnvironment() throws NamingException {
        if (myEnv == null) {
            // Must return non-null
            myEnv = new Hashtable(3, 0.75f);
        }
        return myEnv;
    }

    /**
     * New JNDI 1.2 operation.
     */
    @Override
    public void close() throws NamingException {
        myEnv = null;
    }

    /**
     * Return the name of this context within the namespace.  The name
     * can be passed as an argument to (new InitialContext()).lookup()
     * to reproduce this context.
     */
    @Override
    public String getNameInNamespace() throws NamingException {
        return myName;
    }
    

  private boolean isLookingUpEnv(String fullName) {
    boolean result = false;
    if (fullName.startsWith("java:comp/env/")
        || fullName.startsWith("java:module/env/")
        || fullName.startsWith("java:app/env/")) {
      result = true;
    } else if ("java:comp/env".equals(fullName)
        || "java:module/env".equals(fullName)
        || "java:app/env".equals(fullName)) {
      result = true;
    }
    return result;
  }

}


