/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.mbeans;


import org.apache.catalina.*;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.modeler.ManagedBean;
import org.apache.tomcat.util.modeler.Registry;
import org.glassfish.web.valve.GlassFishValve;

import javax.management.*;
import javax.management.modelmbean.ModelMBean;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Public utility methods in support of the server side MBeans implementation.
 *
 * @author Craig R. McClanahan
 * @author Amy Roh
 * @version $Revision: 1.5 $ $Date: 2006/11/10 18:12:35 $
 */

public class MBeanUtils {

    private static Logger log = Logger.getLogger(MBeanUtils.class.getName());

    // ------------------------------------------------------- Static Variables


    /**
     * The set of exceptions to the normal rules used by
     * <code>createManagedBean()</code>.  The first element of each pair
     * is a class name, and the second element is the managed bean name.
     */
    private static String exceptions[][] = {
        { "org.apache.catalina.core.StandardDefaultContext",
          "DefaultContext" },
    };


    // --------------------------------------------------------- Static Methods

    /**
     * Translates a string into x-www-form-urlencoded format
     *
     * @param t string to be encoded
     * @return encoded string
     */
    private static final String encodeStr(String t) {
   
        return URLEncoder.encode(t);

    }


    /**
     * Create and return the name of the <code>ManagedBean</code> that
     * corresponds to this Catalina component.
     *
     * @param component The component for which to create a name
     */
    static String createManagedName(Object component) {

        // Deal with exceptions to the standard rule
        String className = component.getClass().getName();
        for (int i = 0; i < exceptions.length; i++) {
            if (className.equals(exceptions[i][0])) {
                return (exceptions[i][1]);
            }
        }

        // Perform the standard transformation
        int period = className.lastIndexOf('.');
        if (period >= 0)
            className = className.substring(period + 1);
        return (className);

    }


    /**
     * Create an <code>ObjectName</code> for this
     * <code>Context</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param context The Context to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    static ObjectName createObjectName(String domain,
                                              Context context)
        throws MalformedObjectNameException {

        ObjectName name = null;
        Host host = (Host)context.getParent();
        String path = context.getPath();
        if (path.length() < 1)
            path = "/";
        // FIXME 
        name = new ObjectName(domain + ":j2eeType=WebModule,name=//" +
                              host.getName()+ path +
                              ",J2EEApplication=none,J2EEServer=none");
    
        return (name);

    }

    
    /**
     * Create an <code>ObjectName</code> for this
     * <code>Service</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param context The ContextEnvironment to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    public static ObjectName createObjectName(String domain,
                                              ContextEnvironment environment)
        throws MalformedObjectNameException {

        ObjectName name = null;
        Object container = 
                environment.getNamingResources().getContainer();
        if (container instanceof Server) {
            name = new ObjectName(domain + ":type=Environment" + 
                        ",resourcetype=Global,name=" + environment.getName());
        } else if (container instanceof Context) {        
            String path = ((Context)container).getPath();
            if (path.length() < 1)
                path = "/";
            Host host = (Host) ((Context)container).getParent();
            name = new ObjectName(domain + ":type=Environment" + 
                        ",resourcetype=Context,path=" + path + 
                        ",host=" + host.getName() +
                        ",name=" + environment.getName());
        } else if (container instanceof DefaultContext) {
            container = ((DefaultContext)container).getParent();
            if (container instanceof Host) {
                Host host = (Host) container;
                name = new ObjectName(domain + ":type=Environment" + 
                        ",resourcetype=HostDefaultContext,host=" + host.getName() +
                        ",name=" + environment.getName());
            } else if (container instanceof Engine) {
                name = new ObjectName(domain + ":type=Environment" + 
                        ",resourcetype=ServiceDefaultContext,name=" + environment.getName());
            }
        }
        
        return (name);

    }
    
    
    /**
     * Create an <code>ObjectName</code> for this
     * <code>ContextResource</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param resource The ContextResource to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    public static ObjectName createObjectName(String domain,
                                              ContextResource resource)
        throws MalformedObjectNameException {

        ObjectName name = null;
        String encodedResourceName = encodeStr(resource.getName());
        Object container = 
                resource.getNamingResources().getContainer();
        if (container instanceof Server) {        
            name = new ObjectName(domain + ":type=Resource" +
                        ",resourcetype=Global,class=" + resource.getType() + 
                        ",name=" + encodedResourceName);
        } else if (container instanceof Context) {                    
            String path = ((Context)container).getPath();
            if (path.length() < 1)
                path = "/";
            Host host = (Host) ((Context)container).getParent();
            name = new ObjectName(domain + ":type=Resource" +
                        ",resourcetype=Context,path=" + path + 
                        ",host=" + host.getName() +
                        ",class=" + resource.getType() +
                        ",name=" + encodedResourceName);
        } else if (container instanceof DefaultContext) {            
            container = ((DefaultContext)container).getParent();
            if (container instanceof Host) {
                Host host = (Host) container;
                name = new ObjectName(domain + ":type=Resource" + 
                        ",resourcetype=HostDefaultContext,host=" + host.getName() +
                        ",class=" + resource.getType() +
                        ",name=" + encodedResourceName);
            } else if (container instanceof Engine) {
                name = new ObjectName(domain + ":type=Resource" + 
                        ",resourcetype=ServiceDefaultContext,class=" + resource.getType() +
                        ",name=" + encodedResourceName);
            }
        }
        
        return (name);

    }
  
    
     /**
     * Create an <code>ObjectName</code> for this
     * <code>ContextResourceLink</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param resourceLink The ContextResourceLink to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    public static ObjectName createObjectName(String domain,
                                              ContextResourceLink resourceLink)
        throws MalformedObjectNameException {

        ObjectName name = null;
        String encodedResourceLinkName = encodeStr(resourceLink.getName());        
        Object container = 
                resourceLink.getNamingResources().getContainer();
        if (container instanceof Server) {        
            name = new ObjectName(domain + ":type=ResourceLink" +
                        ",resourcetype=Global" + 
                        ",name=" + encodedResourceLinkName);
        } else if (container instanceof Context) {                    
            String path = ((Context)container).getPath();
            if (path.length() < 1)
                path = "/";
            Host host = (Host) ((Context)container).getParent();
            name = new ObjectName(domain + ":type=ResourceLink" +
                        ",resourcetype=Context,path=" + path + 
                        ",host=" + host.getName() +
                        ",name=" + encodedResourceLinkName);
        } else if (container instanceof DefaultContext) {            
            container = ((DefaultContext)container).getParent();
            if (container instanceof Host) {
                Host host = (Host) container;
                name = new ObjectName(domain + ":type=ResourceLink" + 
                        ",resourcetype=HostDefaultContext,host=" + host.getName() +
                        ",name=" + encodedResourceLinkName);
            } else if (container instanceof Engine) {
                name = new ObjectName(domain + ":type=ResourceLink" + 
                        ",resourcetype=ServiceDefaultContext,name=" + encodedResourceLinkName);
            }
        }
        
        return (name);

    }
    
    
    /**
     * Create an <code>ObjectName</code> for this
     * <code>DefaultContext</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param context The DefaultContext to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    static ObjectName createObjectName(String domain,
                                              DefaultContext context)
        throws MalformedObjectNameException {

        ObjectName name = null;
        Container container = context.getParent();
        if (container instanceof Host) {
            Host host = (Host) container;
            name = new ObjectName(domain + ":type=DefaultContext,host=" +
                              host.getName());
        } else if (container instanceof Engine) {
            name = new ObjectName(domain + ":type=DefaultContext");
        }

        return (name);

    }

    /**
     * Create an <code>ObjectName</code> for this
     * <code>Engine</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param engine The Engine to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    static ObjectName createObjectName(String domain,
                                              Engine engine)
        throws MalformedObjectNameException {

        ObjectName name = null;
        name = new ObjectName(domain + ":type=Engine");
        return (name);

    }


    /**
     * Create an <code>ObjectName</code> for this
     * <code>Host</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param host The Host to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    static ObjectName createObjectName(String domain,
                                       Host host)
        throws MalformedObjectNameException {

        ObjectName name = null;
        name = new ObjectName(domain + ":type=Host,host=" +
                              host.getName());
        return (name);

    }


    /**
     * Create an <code>ObjectName</code> for this
     * <code>Loader</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param loader The Loader to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    static ObjectName createObjectName(String domain,
                                       Loader loader)
        throws MalformedObjectNameException {

        ObjectName name = null;
        Container container = loader.getContainer();

        if (container instanceof Engine) {
            name = new ObjectName(domain + ":type=Loader");
        } else if (container instanceof Host) {
            name = new ObjectName(domain + ":type=Loader,host=" +
                              container.getName());
        } else if (container instanceof Context) {
            String path = ((Context)container).getPath();
            if (path.length() < 1) {
                path = "/";
            }
            Host host = (Host) container.getParent();
            name = new ObjectName(domain + ":type=Loader,path=" + path +
                              ",host=" + host.getName());
        } else if (container == null) {
            // What is that ???
            DefaultContext defaultContext = loader.getDefaultContext();
            if (defaultContext != null) {
                Container parent = defaultContext.getParent();
                if (parent instanceof Engine) {
                    name = new ObjectName(domain + ":type=DefaultLoader");
                } else if (parent instanceof Host) {
                    name = new ObjectName(domain + ":type=DefaultLoader,host=" +
                            parent.getName());
                }
            }
        }

        return (name);

    }


    /**
     * Create an <code>ObjectName</code> for this
     * <code>Logger</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param logger The Logger to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    static ObjectName createObjectName(String domain,
                                       org.apache.catalina.Logger logger)
        throws MalformedObjectNameException {

        ObjectName name = null;
        Container container = logger.getContainer();

        if (container instanceof Engine) {
            name = new ObjectName(domain + ":type=Logger");
        } else if (container instanceof Host) {
            name = new ObjectName(domain + ":type=Logger,host=" +
                              container.getName());
        } else if (container instanceof Context) {
            String path = ((Context)container).getPath();
            if (path.length() < 1) {
                path = "/";
            }
            Host host = (Host) container.getParent();
            name = new ObjectName(domain + ":type=Logger,path=" + path +
                              ",host=" + host.getName());
        }

        return (name);

    }


    /**
     * Create an <code>ObjectName</code> for this
     * <code>Manager</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param manager The Manager to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    static ObjectName createObjectName(String domain,
                                              Manager manager)
        throws MalformedObjectNameException {

        ObjectName name = null;
        Container container = manager.getContainer();

        if (container instanceof Engine) {
            name = new ObjectName(domain + ":type=Manager");
        } else if (container instanceof Host) {
            name = new ObjectName(domain + ":type=Manager,host=" +
                              container.getName());
        } else if (container instanceof Context) {
            String path = ((Context)container).getPath();
            if (path.length() < 1) {
                path = "/";
            }
            Host host = (Host) container.getParent();
            name = new ObjectName(domain + ":type=Manager,path=" + path +
                              ",host=" + host.getName());
        } else if (container == null) {
            DefaultContext defaultContext = manager.getDefaultContext();
            if (defaultContext != null) {
                Container parent = defaultContext.getParent();
                if (parent instanceof Engine) {
                    name = new ObjectName(domain + ":type=DefaultManager");
                } else if (parent instanceof Host) {
                    name = new ObjectName(domain + ":type=DefaultManager,host=" +
                            parent.getName());
                }
            }
        }

        return (name);

    }
    
    
    /**
     * Create an <code>ObjectName</code> for this
     * <code>Server</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param resources The NamingResources to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    static ObjectName createObjectName(String domain,
                                              NamingResources resources)
        throws MalformedObjectNameException {

        ObjectName name = null;
        Object container = resources.getContainer();        
        if (container instanceof Server) {        
            name = new ObjectName(domain + ":type=NamingResources" + 
                        ",resourcetype=Global");
        } else if (container instanceof Context) {        
            String path = ((Context)container).getPath();
            if (path.length() < 1)
                path = "/";
            Host host = (Host) ((Context)container).getParent();
            name = new ObjectName(domain + ":type=NamingResources" + 
                        ",resourcetype=Context,path=" + path + 
                        ",host=" + host.getName());
        } else if (container instanceof DefaultContext) {
            container = ((DefaultContext)container).getParent();
            if (container instanceof Host) {
                Host host = (Host) container;
                name = new ObjectName(domain + ":type=NamingResources" + 
                        ",resourcetype=HostDefaultContext,host=" + host.getName());
            } else if (container instanceof Engine) {
                name = new ObjectName(domain + ":type=NamingResources" + 
                        ",resourcetype=ServiceDefaultContext");
            }
        }
        
        return (name);

    }

    
    /**
     * Create an <code>ObjectName</code> for this
     * <code>Realm</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param realm The Realm to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    static ObjectName createObjectName(String domain,
                                              Realm realm)
        throws MalformedObjectNameException {

        ObjectName name = null;
        Container container = realm.getContainer();

        if (container instanceof Engine) {
            name = new ObjectName(domain + ":type=Realm");
        } else if (container instanceof Host) {
            name = new ObjectName(domain + ":type=Realm,host=" +
                              container.getName());
        } else if (container instanceof Context) {
            String path = ((Context)container).getPath();
            if (path.length() < 1) {
                path = "/";
            }
            Host host = (Host) container.getParent();
            name = new ObjectName(domain + ":type=Realm,path=" + path +
                              ",host=" + host.getName());
        }

        return (name);

    }


    /**
     * Create an <code>ObjectName</code> for this
     * <code>Server</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param server The Server to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    static ObjectName createObjectName(String domain,
                                              Server server)
        throws MalformedObjectNameException {

        ObjectName name = null;
        name = new ObjectName(domain + ":type=Server");
        return (name);

    }


    /**
     * Create an <code>ObjectName</code> for this
     * <code>Service</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param service The Service to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    static ObjectName createObjectName(String domain,
                                              Service service)
        throws MalformedObjectNameException {

        ObjectName name = null;
        name = new ObjectName(domain + ":type=Service,serviceName=" + 
                            service.getName());
        return (name);

    }


    /**
     * Create an <code>ObjectName</code> for this
     * <code>Valve</code> object.
     *
     * @param domain Domain in which this name is to be created
     * @param valve The Valve to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    static ObjectName createObjectName(String domain,
                                       GlassFishValve valve)
        throws MalformedObjectNameException {
        if( valve instanceof ValveBase ) {
            ObjectName name=((ValveBase)valve).getObjectName();
            if( name != null )
                return name;
        }

        ObjectName name = null;
        Container container = null;
        String className=valve.getClass().getName();
        int period = className.lastIndexOf('.');
        if (period >= 0)
            className = className.substring(period + 1);
        if( valve instanceof Contained ) {
            container = ((Contained)valve).getContainer();
        }
        if( container == null ) {
            throw new MalformedObjectNameException(
                               "Cannot create mbean for non-contained valve " +
                               valve);
        }        
        if (container instanceof Engine) {
            String local="";
            int seq = getSeq(local);
            String ext="";
            if( seq > 0 ) {
                ext=",seq=" + seq;
            }
            name = new ObjectName(domain + ":type=Valve,name=" + className + 
                                    ext + local );
        } else if (container instanceof Host) {
            String local=",host=" +container.getName();
            int seq = getSeq(local);
            String ext="";
            if( seq > 0 ) {
                ext=",seq=" + seq;
            }
            name = new ObjectName(domain + ":type=Valve,name=" + className + 
                                    ext + local );
        } else if (container instanceof Context) {
            String path = ((Context)container).getPath();
            if (path.length() < 1) {
                path = "/";
            }
            Host host = (Host) container.getParent();
            String local=",path=" + path + ",host=" +
                    host.getName();
            int seq = getSeq(local);
            String ext="";
            if( seq > 0 ) {
                ext=",seq=" + seq;
            }
            name = new ObjectName(domain + ":type=Valve,name=" + className + 
                                    ext + local );
        }

        return (name);

    }

    static Hashtable<String, int[]> seq=new Hashtable<String, int[]>();
    static int getSeq( String key ) {
        int i[]=seq.get( key );
        if (i == null ) {
            i=new int[1];
            i[0]=0;
            seq.put( key, i);
        } else {
            i[0]++;
        }
        return i[0];
    }


}
