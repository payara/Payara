/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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

package jaxb1.impl.runtime;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

/**
 * Keeps the information about the grammar as a whole.
 * 
 * This object is immutable and thread-safe.
 *
 * @author
 *  <a href="mailto:kohsuke.kawaguchi@sun.com">Kohsuke KAWAGUCHI</a>
 */
public class GrammarInfoImpl implements GrammarInfo
{
    /**
     * Map from {@link QName}s (root tag names) to {@link Class}es of the
     * content interface that should be instanciated.
     */
    private final Map rootTagMap;
    
    /**
     * Enclosing ObjectFactory class. Used to load resources.
     */
    private final Class objectFactoryClass;
    
    /**
     * Map from {@link Class}es that represent content interfaces
     * to {@link String}s that represent names of the corresponding
     * implementation classes.
     */
    private final Map defaultImplementationMap;
    
    /**
     * ClassLoader that should be used to load impl classes.
     */
    private final ClassLoader classLoader;
    
    public GrammarInfoImpl( Map _rootTagMap, Map _defaultImplementationMap, Class _objectFactoryClass ) {
        this.rootTagMap = _rootTagMap;
        this.defaultImplementationMap = _defaultImplementationMap;
        this.objectFactoryClass = _objectFactoryClass;
        // the assumption is that the content interfaces and their impls
        // are loaded from the same class loader. 
        this.classLoader = objectFactoryClass.getClassLoader(); 
    }
    
    /**
     * @return the name of the content interface that is registered with
     * the specified element name.
     */
    private final Class lookupRootMap( String nsUri, String localName ) {
        // note that the value of rootTagMap could be null.
        QName qn;
        
        qn = new QName(nsUri,localName);
        if(rootTagMap.containsKey(qn))    return (Class)rootTagMap.get(qn);

        qn = new QName(nsUri,"*");
        if(rootTagMap.containsKey(qn))    return (Class)rootTagMap.get(qn);

        qn = new QName("*","*");
        return (Class)rootTagMap.get(qn);
    }
    
    public final Class getRootElement(String namespaceUri, String localName) {
        Class intfCls = lookupRootMap(namespaceUri,localName);
        if(intfCls==null)    return null;
        else                return getDefaultImplementation(intfCls);
    }

    public final UnmarshallingEventHandler createUnmarshaller(
        String namespaceUri, String localName, UnmarshallingContext context ) {
        
        Class impl = getRootElement(namespaceUri,localName);
        if(impl==null)        return null;
        
        try {
            return ((UnmarshallableObject)impl.newInstance()).createUnmarshaller(context);
        } catch (InstantiationException e) {
            throw new InstantiationError(e.toString());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.toString());
        }
    }
    
    public final String[] getProbePoints() {
        List r = new ArrayList();
        for (Iterator itr = rootTagMap.keySet().iterator(); itr.hasNext();) {
            QName qn = (QName) itr.next();
            r.add(qn.getNamespaceURI());
            r.add(qn.getLocalPart());
        }
        return (String[]) r.toArray(new String[r.size()]);
    }
    
    public final boolean recognize( String nsUri, String localName ) {
        return lookupRootMap(nsUri,localName)!=null;
    }
    
    public final Class getDefaultImplementation( Class javaContentInterface ) {
        try {
            // by caching the obtained Class objects.
            String name = (String)defaultImplementationMap.get(javaContentInterface);
            if(name==null)
                return null;
            else
                return Class.forName(name, true, classLoader );
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.toString());
        }
    }

    /**
     * Gets the MSV AGM which can be used to validate XML during
     * marshalling/unmarshalling.
     */
    public final com.sun.msv.grammar.Grammar getGrammar() throws JAXBException {
        try {
            InputStream is = objectFactoryClass.getResourceAsStream("bgm.ser");
            
            if( is==null ) {
                // unable to find bgm.ser
                String name = objectFactoryClass.getName();
                int idx = name.lastIndexOf('.');
                name = '/'+name.substring(0,idx+1).replace('.','/')+"bgm.ser";
                throw new JAXBException(
                    Messages.format( Messages.NO_BGM, name ) );
            }
            
            // deserialize the bgm
            ObjectInputStream ois = new ObjectInputStream( is );
            com.sun.xml.bind.GrammarImpl g = (com.sun.xml.bind.GrammarImpl)ois.readObject();
            ois.close();
            
            g.connect(new com.sun.msv.grammar.Grammar[]{g});    // connect to itself
            
            return g;
        } catch( Exception e ) {
            throw new JAXBException( 
                Messages.format( Messages.UNABLE_TO_READ_BGM ), 
                e );
        }
    }
    
    /**
     * @see com.sun.tools.xjc.runtime.GrammarInfo#castToXMLSerializable(java.lang.Object)
     */
    public XMLSerializable castToXMLSerializable(Object o) {
        if( o instanceof XMLSerializable ) {
             return (XMLSerializable)o;
        } else {
            return null;
        }
    }
    
    /**
     * @see com.sun.tools.xjc.runtime.GrammarInfo#castToValidatableObject(java.lang.Object)
     */
    public ValidatableObject castToValidatableObject(Object o) {
        if( o instanceof ValidatableObject ) {
             return (ValidatableObject)o;
        } else {
            return null;
        }
    }
}
