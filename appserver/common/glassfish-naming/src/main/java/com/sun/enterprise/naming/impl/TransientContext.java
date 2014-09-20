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

import javax.naming.*;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import static com.sun.enterprise.naming.util.LogFacade.logger;

/**
 * Class to implement multiple level of subcontexts in SerialContext. To use this
 * class a new object of class InitialContext (env) should be instantiated.
 * The env i.e the Environment is initialised with SerialInitContextFactory
 * An example for using this is in /test/subcontext
 */
public class TransientContext implements Context, Serializable {
    Hashtable myEnv;
    private Map<String,Object> bindings = new HashMap<String,Object>();
    static NameParser myParser = new SerialNameParser();

    // Issue 7067: lots of lookup failures in a heavily concurrent client.
    // So add a read/write lock, which allows unlimited concurrent readers,
    // and only imposes a global lock on relatively infrequent updates.
    private static final ReadWriteLock lock = new ReentrantReadWriteLock() ;

    public TransientContext() {
    }

    /**
     * Create a subcontext with the specified name.
     *
     * @return the created subcontext.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public Context createSubcontext(String name) throws NamingException {
        return drillDownAndCreateSubcontext(name);
    }

    /**
     * Create a subcontext with the specified name.
     *
     * @return the created subcontext.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public Context createSubcontext(Name name) throws NamingException {
        return createSubcontext(name.toString());
    }

    /**
     * Destroy the subcontext with the specified name.
     *
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void destroySubcontext(String name) throws NamingException {
        drillDownAndDestroySubcontext(name);
    }

    /**
     * Destroy the subcontext with the specified name.
     *
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void destroySubcontext(Name name) throws NamingException {
        destroySubcontext(name.toString());
    }

    /**
     * Handles making nested subcontexts
     * i.e. if you want abcd/efg/hij. It will go  subcontext efg in abcd
     * (if not present already - it will create it) and then
     * make subcontext hij
     *
     * @return the created subcontext.
     * @throws NamingException if there is a Naming exception
     */
    private Context drillDownAndCreateSubcontext(String name)
            throws NamingException {
        lock.writeLock().lock() ;
        try {
            Name n = new CompositeName(name);
            if (n.size() <= 1) { // bottom
                if (bindings.containsKey(name)) {
                    throw new NameAlreadyBoundException("Subcontext " +
                            name + " already present");
                }

                TransientContext ctx = null;
                ctx = new TransientContext();
                bindings.put(name, ctx);
                return ctx;
            } else {
                String suffix = n.getSuffix(1).toString();
                Context retCtx, ctx; // the created context
                try {
                    ctx = resolveContext(n.get(0));
                } catch (NameNotFoundException e) {
                    ctx = new TransientContext();
                }
                retCtx = ctx.createSubcontext(suffix);
                bindings.put(n.get(0), ctx);
                return retCtx;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Handles deleting nested subcontexts
     * i.e. if you want delete abcd/efg/hij. It will go  subcontext efg in abcd
     * it will delete it) and then delete subcontext hij
     *
     * @throws NamingException if there is a naming exception
     */
    private void drillDownAndDestroySubcontext(String name)
            throws NamingException {
        lock.writeLock().lock();
        try {
            Name n = new CompositeName(name);
            if (n.size() < 1) {
                throw new InvalidNameException("Cannot destoy empty subcontext");
            }
            if (n.size() == 1) { // bottom
                if (bindings.containsKey(name)) {
                    bindings.remove(name);
                } else {
                    throw new NameNotFoundException("Subcontext: " + name +
                            " not found");
                }
            } else {
                String suffix = n.getSuffix(1).toString();
                Context ctx; // the context to drill down from
                ctx = resolveContext(n.get(0));
                ctx.destroySubcontext(suffix);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Lookup the specified name.
     *
     * @return the object or context bound to the name.
     * @throws NamingException          if there is a naming exception.
     * @throws java.rmi.RemoteException if there is an RMI exception.
     */
    @Override
    public Object lookup(String name) throws NamingException {
        lock.readLock().lock() ;
        try {
            Name n = new CompositeName(name);
            if (n.size() < 1) {
                throw new InvalidNameException("Cannot bind empty name");
            }

            if (n.size() == 1) { // bottom
                return doLookup(n.toString());
            } else {
                String suffix = n.getSuffix(1).toString();
                TransientContext ctx = resolveContext(n.get(0));
                return ctx.lookup(suffix);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Lookup the specified name.
     *
     * @return the object or context bound to the name.
     * @throws NamingException          if there is a naming exception.
     * @throws java.rmi.RemoteException if there is an RMI exception.
     */
    @Override
    public Object lookup(Name name) throws NamingException {
        return lookup(name.toString());
    }

    /**
     * Lookup the specified name in the current objects hashtable.
     *
     * @return the object or context bound to the name.
     * @throws NamingException          if there is a naming exception.
     * @throws java.rmi.RemoteException if there is an RMI exception.
     */
    private Object doLookup(String name) throws NamingException {
        Object answer = bindings.get(name);
        if (answer == null) {
            throw new NameNotFoundException(name + " not found");
        }
        return answer;
    }

    /**
     * Bind the object to the specified name.
     *
     * @throws NamingException          if there is a naming exception.
     * @throws java.rmi.RemoteException if there is an RMI exception.
     */
    @Override
    public void bind(String name, Object obj) throws NamingException {
        lock.writeLock().lock();
        try {
            Name n = new CompositeName(name);
            if (n.size() < 1) {
                throw new InvalidNameException("Cannot bind empty name");
            }
            if (n.size() == 1) { // bottom
                doBindOrRebind(n.toString(), obj, false);
            } else {
                String suffix = n.getSuffix(1).toString();
                Context ctx;
                try {
                    ctx = resolveContext(n.get(0));
                } catch (NameNotFoundException e) {
                    ctx = createSubcontext(n.get(0));
                }
                ctx.bind(suffix, obj);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Bind the object to the specified name.
     *
     * @throws NamingException          if there is a naming exception.
     * @throws java.rmi.RemoteException if there is an RMI exception.
     */
    @Override
    public void bind(Name name, Object obj)
            throws NamingException {
        bind(name.toString(), obj);
    }

    /**
     * Finds out if the subcontext specified is present in the current context
     *
     * @throws NamingException          if there is a naming exception
     * @throws java.rmi.RemoteException if there is a RMI exception
     */
    /* Finds if the name searched for is a type of context or anyother type of 
     * object.
     */
    private TransientContext resolveContext(String s) throws NamingException {
        //TransientContext ctx = (TransientContext) bindings.get(s);
        TransientContext ctx;
        Object obj = bindings.get(s);
        if (obj == null) {
            throw new NameNotFoundException(s);
        }
        if (obj instanceof TransientContext) {
            ctx = (TransientContext) obj;
        } else {
            throw new NameAlreadyBoundException(s);
        }
        return ctx;
    }

    /**
     * Binds or rebinds the object specified by name
     *
     * @throws NamingException          if there is a naming exception
     * @throws java.rmi.RemoteException if there is a RMI exception
     */
    private void doBindOrRebind(String name, Object obj, boolean rebind)
            throws NamingException {
        if (name.equals("")) {
            throw new InvalidNameException("Cannot bind empty name");
        }
        if (!rebind) {
            if (bindings.get(name) != null) {
                throw new NameAlreadyBoundException(
                        "Use rebind to override");
            }
        }
        bindings.put(name, obj);
    }


    /**
     * Rebinds the object specified by name
     *
     * @throws NamingException          if there is a naming exception
     * @throws java.rmi.RemoteException if there is a RMI exception
     */
    @Override
    public void rebind(String name, Object obj)
            throws NamingException {
        lock.writeLock().lock();
        try {
            Name n = new CompositeName(name);
            if (n.size() < 1) {
                throw new InvalidNameException("Cannot bind empty name");
            }
            if (n.size() == 1) { // bottom
                doBindOrRebind(n.toString(), obj, true);
            } else {
                String suffix = n.getSuffix(1).toString();
                Context ctx = null;
                try {
                    ctx = resolveContext(n.get(0));
                    ctx.rebind(suffix, obj);
                } catch (NameNotFoundException e) {
                    ctx = createSubcontext(n.get(0));
                    ctx.rebind(suffix, obj);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Binds or rebinds the object specified by name
     *
     * @throws NamingException          if there is a naming exception
     * @throws java.rmi.RemoteException if there is a RMI exception
     */
    @Override
    public void rebind(Name name, Object obj)
            throws NamingException {
        rebind(name.toString(), obj);
    }

    /**
     * Unbinds the object specified by name. Traverses down the context tree
     * and unbinds the object if required.
     *
     * @throws NamingException          if there is a naming exception
     * @throws java.rmi.RemoteException if there is a RMI exception
     */
    private void doUnbind(String name) throws NamingException {
        if (name.equals("")) {
            throw new InvalidNameException("Cannot unbind empty name");
        }
        // After checking javadoc of Context.unbind(),I think we need not throw
        // NameNotFoundException here.
        /*if (bindings.get(name) == null) {
            throw new NameNotFoundException(
                    "Cannot find name to unbind");
        }*/
        bindings.remove(name);
    }

    /**
     * Unbinds the object specified by name. Calls itself recursively to
     * traverse down the context tree and unbind the object.
     *
     * @throws NamingException          if there is a naming exception
     * @throws java.rmi.RemoteException if there is a RMI exception
     */
    @Override
    public void unbind(String name) throws NamingException {
        lock.writeLock().lock();
        try {
            Name n = new CompositeName(name);
            if (n.size() < 1) {
                throw new InvalidNameException("Cannot unbind empty name");
            }
            if (n.size() == 1) { // bottom
                doUnbind(n.toString());
            } else {
                String suffix = n.getSuffix(1).toString();
                TransientContext ctx = resolveContext(n.get(0));
                ctx.unbind(suffix);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Unbinds the object specified by name
     *
     * @throws NamingException          if there is a naming exception
     * @throws java.rmi.RemoteException if there is a RMI exception
     */
    @Override
    public void unbind(Name name)
            throws NamingException {
        unbind(name.toString());
    }

    /**
     * Rename the object specified by oldname to newname
     *
     * @throws NamingException          if there is a naming exception
     * @throws java.rmi.RemoteException if there is a RMI exception
     */
    @Override
    public void rename(Name oldname, Name newname) throws NamingException {
        rename(oldname.toString(), newname.toString());
    }

    /**
     * Rename the object specified by oldname to newname
     *
     * @throws NamingException          if there is a naming exception
     * @throws java.rmi.RemoteException if there is a RMI exception
     */
    @Override
    public void rename(String oldname, String newname)
            throws NamingException {
        if (oldname.equals("") || newname.equals("")) {
            throw new InvalidNameException("Cannot rename empty name");
        }

        lock.writeLock().lock() ;
        try {
            // Check if new name exists
            if (bindings.get(newname) != null) {
                throw new NameAlreadyBoundException(newname +
                        " is already bound");
            }

            // Check if old name is bound
            Object oldBinding = bindings.remove(oldname);
            if (oldBinding == null) {
                throw new NameNotFoundException(oldname + " not bound");
            }

            bindings.put(newname, oldBinding);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * list the objects stored by the current context
     *
     * @throws NamingException          if there is a naming exception
     * @throws java.rmi.RemoteException if there is a RMI exception
     */
    public Hashtable list() {
        lock.readLock().lock();
        try {
            return new Hashtable(bindings);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * List the objects specified by name.
     *
     * @throws NamingException          if there is a naming exception
     * @throws java.rmi.RemoteException if there is a RMI exception
     */
    public Hashtable listContext(String name) throws NamingException {
        lock.readLock().lock();
        try {
            if (logger.isLoggable(Level.FINE)) {
                print(bindings);
            }
            if (name.equals("")) {
                return new Hashtable(bindings);
            }

            Object target = lookup(name);
            if (target instanceof TransientContext) {
                return ((TransientContext) target).listContext("");
            }
            throw new NotContextException(name + " cannot be listed");
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * List the objects specified by name.
     *
     * @throws NamingException if there is a naming exception
     */
    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return list(name.toString());
    }

    /**
     * List the objects specified by name.
     *
     * @throws NamingException if there is a naming exception
     */
    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        lock.readLock().lock() ;
        try {
            if (logger.isLoggable(Level.FINE)) {
                print(bindings);
            }
            if (name.equals("")) {
                return new RepNames<NameClassPair>(new Hashtable(bindings));
            }

            Object target = lookup(name);
            if (target instanceof Context) {
                return ((Context) target).list("");
            }
            throw new NotContextException(name + " cannot be listed");
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * List the bindings of objects present in name.
     *
     * @throws NamingException if there is a naming exception
     */
    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        lock.readLock().lock() ;
        try {
            if (name.equals("")) {
                return new RepBindings<Binding>(new Hashtable(bindings));
            }

            Object target = lookup(name);
            if (target instanceof Context) {
                return ((Context) target).listBindings("");
            }
            throw new NotContextException(name + " cannot be listed");
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * List the binding of objects specified by name.
     *
     * @throws NamingException if there is a naming exception
     */
    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return listBindings(name.toString());
    }

    /**
     * Lookup the name.
     *
     * @throws NamingException if there is a naming exception
     */
    @Override
    public Object lookupLink(String name) throws NamingException {
        // This flat context does not treat links specially
        return lookup(name);
    }

    /**
     * Lookup name.
     *
     * @throws NamingException if there is a naming exception
     */
    @Override
    public Object lookupLink(Name name) throws NamingException {
        // Flat namespace; no federation; just call string version
        return lookupLink(name.toString());
    }

    /**
     * List the NameParser specified by name.
     *
     * @throws NamingException if there is a naming exception
     */
    @Override
    public NameParser getNameParser(String name) throws NamingException {
        return myParser;
    }

    /**
     * List the NameParser specified by name.
     *
     * @throws NamingException if there is a naming exception
     */
    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        // Flat namespace; no federation; just call string version
        return getNameParser(name.toString());
    }

    /**
     * Compose a new name specified by name and prefix.
     *
     * @return null
     * @throws NamingException if there is a naming exception
     */
    @Override
    public String composeName(String name, String prefix)
            throws NamingException {
        return null;
    }

    /**
     * Compose a new name specified by name and prefix.
     *
     * @return Name result of the concatenation
     * @throws NamingException if there is a naming exception
     */
    @Override
    public Name composeName(Name name, Name prefix)
            throws NamingException {
        Name result = (Name) (prefix.clone());
        result.addAll(name);
        return result;
    }

    /**
     * Add the property name and value to the environment.
     *
     * @throws NamingException if there is a naming exception
     */
    @Override
    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException {
        lock.writeLock().lock() ;
        try {
            if (myEnv == null) {
                myEnv = new Hashtable(5, 0.75f);
            }
            return myEnv.put(propName, propVal);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove property from the environment.
     *
     * @throws NamingException if there is a naming exception
     */
    @Override
    public Object removeFromEnvironment(String propName)
            throws NamingException {
        lock.writeLock().lock() ;
        try {
            if (myEnv == null) {
                return null;
            }
            return myEnv.remove(propName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * List the current environment.
     *
     * @throws NamingException if there is a naming exception
     */
    @Override
    public Hashtable getEnvironment() throws NamingException {
        lock.writeLock().lock() ;
        try {
            if (myEnv == null) {
                // Must return non-null
                myEnv = new Hashtable(3, 0.75f);
            }
            return myEnv;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Invalidate the current environment.
     */
    @Override
    public void close() throws NamingException {
        myEnv = null;
    }

    /**
     * Operation not supported.
     */
    @Override
    public String getNameInNamespace() throws NamingException {
        throw new OperationNotSupportedException("getNameInNamespace() " +
                "not implemented");
    }

    /**
     * Print the current hashtable.  Should only be invoked for FINE Level logging.
     */
    private static void print(Map<String,Object> ht) {
        for (Map.Entry<String, Object> entry : ht.entrySet()) {
            Object value = entry.getValue();
            logger.log(Level.FINE, "[{0}, {1}:{2}]",
                    new Object[]{entry.getKey(), value, value.getClass().getName()});
            // END OF IASRI 4660742
        }
    }

    // Class for enumerating name/class pairs
    static class RepNames<T> implements NamingEnumeration<T> {
        private Map<String,String> nameToClassName =
            new HashMap<String,String>() ;
        private Iterator<String> iter ;

        RepNames(Hashtable bindings) {
            Set<String> names = new HashSet<String>() ;
            for (Object str : bindings.keySet()) {
                names.add( (String)str ) ;
            }

            iter = names.iterator() ;

            for (Object obj : bindings.entrySet() ) {
                Map.Entry entry = (Map.Entry)obj ;
                nameToClassName.put( (String)entry.getKey(),
                    entry.getValue().getClass().getName()) ;
            }
        }

        @Override
        public boolean hasMoreElements() {
            return iter.hasNext();
        }

        @Override
        public boolean hasMore() throws NamingException {
            return hasMoreElements();
        }

        @Override
        public T nextElement() {
            if (iter.hasNext()) {
                String name = iter.next();
                String className = nameToClassName.get(name) ;
                return (T) (new NameClassPair(name, className));
            } else {
                return null;
            }
        }

        @Override
        public T next() throws NamingException {
            return nextElement();
        }

        @Override
        public void close() {
            //no-op since no steps needed to free up resources
        }
    }

    // Class for enumerating bmesindings
    static class RepBindings<T> implements NamingEnumeration<T> {
        Enumeration names;
        Hashtable bindings;

        RepBindings(Hashtable bindings) {
            this.bindings = bindings;
            this.names = bindings.keys();
        }

        @Override
        public boolean hasMoreElements() {
            return names.hasMoreElements();
        }

        @Override
        public boolean hasMore() throws NamingException {
            return hasMoreElements();
        }

        @Override
        public T nextElement() {
            if (hasMoreElements()) {
                String name = (String) names.nextElement();
                return (T) (new Binding(name, bindings.get(name)));
            } else {
                return null;
            }
        }

        @Override
        public T next() throws NamingException {
            return nextElement();
        }

        @Override
        public void close() {
            //no-op since no steps needed to free up resources
        }
    }
}










