/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.web.ResourceReference;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * This descriptor represents a dependency on a resource.
 * @author Danny Coward
 */

public class ResourceReferenceDescriptor extends EnvironmentProperty 
	implements NamedDescriptor, ResourceReference {

    static private final int NULL_HASH_CODE = Integer.valueOf(1).hashCode();

    /** 
     * For database resources, this says the application will log in.
     */
    public static final String APPLICATION_AUTHORIZATION = "Application";
    /** 
     * For database resources this says the container will log in.
     */
    public static final String CONTAINER_AUTHORIZATION = "Container";

    // res-sharing-scope values
    public static final String RESOURCE_SHAREABLE = "Shareable";
    public static final String RESOURCE_UNSHAREABLE = "Unshareable";
    
    private static final String URL_RESOURCE_TYPE = "java.net.URL";
    
    //START OF IASRI 4633229
    private static final String CONNECTOR_RESOURCE_TYPE = "javax.resource.cci.ConnectionFactory";    
    //END OF IASRI 4633229
    private static final String MAIL_RESOURCE_TYPE = "javax.mail.Session";    

    // start IASRI 4734197
    private static final String JDBC_RESOURCE_TYPE = "javax.sql.DataSource";    
    // end IASRI 4734197

    private static final String ORB_RESOURCE_TYPE = "org.omg.CORBA.ORB";

    private static final String WEBSERVICE_CONTEXT_TYPE =
        "javax.xml.ws.WebServiceContext";

    // change field name from type to rType since it's error-prone 
    // to use the same field name as its super class
    private String rType;

    private ResourcePrincipal resourcePrincipal = null;

    // XXX - MailConfiguration is saved and returned, but no one ever seems
    // to use the value that's saved.  Should probably just remove this.
    private MailConfiguration mailConfiguration;

    private String authorization;
    private DataSource dataSource;
    private String sharingScope;
    
    private List runtimeProps=null;
    
    // for cmp-resource type
    boolean createTablesAtDeploy=false;
    boolean dropTablesAtUndeploy=false;
    String databaseVendorName = null;
    Properties schemaGeneratorProperties = null;

    // START OF IASRI 4718559
    private static final LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(ResourceReferenceDescriptor.class);
    // END OF IASRI 4718559    
    
    /**
     * Construct a resource reference with the given name, description 
     * and type.
     * @param the name of the reference
     * @param the description
     * @param the type of the resource reference.
     */
    public ResourceReferenceDescriptor(String name, String description, 
					String type) {
	super(name, "", description);
	rType = type;
    }
    
    /**
     * Default constructor.
     */
    public ResourceReferenceDescriptor() {
    }
    
   // Create logger object per Java SDK 1.4 to log messages
    // introduced Santanu De, Sun Microsystems, March 2002

    static final Logger _logger = DOLUtils.getDefaultLogger();
    

    /**
     * Return the JNDI name of this resource reference.
     * @return the JNDI name of the resource reference.
     */
    public String getJndiName() {
        String jndiName = super.getValue();
        if (jndiName != null  && ! jndiName.equals("")) {
            return jndiName;
        }
        if (mappedName != null && ! mappedName.equals("")) { 
            return mappedName;
        }
        return lookupName;
    }
    
    /** 
     * Set the JNDI name of this resource reference.
     * @param the JNDI name of the resource reference.
     */
    public void setJndiName(String jndiName) {
	super.setValue(jndiName);
    }

    public String getInjectResourceType() {
        return rType;
    }

    public void setInjectResourceType(String resourceType) {
        rType = resourceType;
    }
   
    /**
     * Has the sharing scope been set?
     * @return true if the sharing scope has been set
     */
    public boolean hasSharingScope() {
        return this.sharingScope != null;
    }

    /**
     * Return the res-sharing-scope of this resource reference.
     * @return the sharing scope.
     */
    public String getSharingScope() {
	if ( sharingScope == null ) {
	    return RESOURCE_SHAREABLE;
	}
	return sharingScope;
    }
    
    /** 
     * Set the res-sharing-scope of this resource reference.
     * @param the sharing scope.
     */
    public void setSharingScope(String ss) {
	sharingScope = ss;
    }


    /**
     * Does this resource references have a JNDI name.
     * @return true if the resource reference has a JNDI name, false otherwise
     */
    public boolean isResolved() {
	return true;
    }
    
    /**
     * Has the authorization type been set?
     * @return true if the authorization type has been set
     */
    public boolean hasAuthorization() {
        return this.authorization != null;
    }

    /**
     * Return true of this resource reference is expecting the container 
     * to authorize the resource.
     * @return true if authorization is container managed.
     */
    public boolean isContainerAuthorization() {
	return this.getAuthorization().equals(CONTAINER_AUTHORIZATION);
    }
    
    /**
     * Return the authorization type of this resource. The default value
     * is APPLICATION_AUTHORIZATION
     * @return the authorization type of the resource.
     */
    public String getAuthorization() {
	if (this.authorization == null) {
	    this.authorization = APPLICATION_AUTHORIZATION;
	}
	return this.authorization;
    }

    /** 
     * Sets the authorization type of this resource. 
     * @param the authorization type.
     */    
    public void setAuthorization(String authorization) {
	this.authorization = authorization;
    }

    /** 
     * Return the type of the resource.
     * @return the type of the resource.
     */
    @Override
    public String getType() {
        return rType;
    }
    
    /** 
     * Sets the type of this resource.
     * @param the type of the resource.
     */
    @Override
    public void setType(String type) {
        rType = type;
    }	

    /**
     * Lookup the datasource from the namespace based on the JNDI name.
     * @return the data source
     */
    public DataSource getJDBCDataSource()
    {
	if ( dataSource == null ) {
	    try {
		// Get JDBC DataSource for database 
		javax.naming.Context ctx = new javax.naming.InitialContext();
		// cache the datasource to avoid JNDI lookup overheads
		dataSource = (DataSource)ctx.lookup(getJndiName());
	    } catch ( Exception ex ) { }
	}
	return dataSource;
    }

    public boolean isWebServiceContext() {
        return this.getType().equals(WEBSERVICE_CONTEXT_TYPE);
    }
    
    public boolean isORB() {
    	return this.getType().equals(ORB_RESOURCE_TYPE);
    }

    /**
     * Return true if this resource is to a JavaMail session object.
     * @return true if the resource is a JavaMail session object.
     */
    public boolean isMailResource() {
        //START OF IASRI 4650786
//	return (this.getMailConfiguration() != null);
        return this.getType().equals(MAIL_RESOURCE_TYPE);
        //END OF IASRI 4650786
    }

    // start IASRI 4734197
    /**
     * @return true if the resource is a jdbc DataSource object.
     */
    public boolean isJDBCResource() {
        return this.getType().equals(JDBC_RESOURCE_TYPE);
    }
    // end IASRI 4734197


    /**
     * Return true if this resource is a URL object.
     * @return true if the resource is a URL object, false otherwise.
     */
    public boolean isURLResource() {
        return (this.getType() != null && this.getType().equals(URL_RESOURCE_TYPE));
    }

    /**
     * Return true if this resource is a JMS connection factory.
     * @return true if the resource is a JMS connection factory, false 
     * otherwise.
     */
    public boolean isJMSConnectionFactory() {
        String myType = this.getType();
        return 
            ( myType.equals("javax.jms.QueueConnectionFactory") ||
              myType.equals("javax.jms.TopicConnectionFactory") );
    }
    
    /**
     * Return the identity used to authorize this resource.
     * @return the principal.
     */
    public ResourcePrincipal getResourcePrincipal() {
	return this.resourcePrincipal;
    }
    
    /**
     * Sets the identity used to authorize this resource.
     * @param the principal.
     */
    public void setResourcePrincipal(ResourcePrincipal resourcePrincipal) {
	this.resourcePrincipal = resourcePrincipal;
    }
    
    /**
     * Sets the mail configuration information for this reference.
     * @param the mail configuration object.
     */
    public void setMailConfiguration(MailConfiguration mailConfiguration) {
	this.mailConfiguration = mailConfiguration;
    }
    
    /**
     * Add a new runtime property to this cmp resource
     */
     public void addProperty(NameValuePairDescriptor newProp) {
	 if (runtimeProps==null) {
	     runtimeProps = new ArrayList();
	 }
	 runtimeProps.add(newProp);
     }
     
     /**
      * @return the runtime properties for this cmp resource
      */
     public Iterator getProperties() {
	 if (runtimeProps==null) {
	     return null;
	 }
	 return runtimeProps.iterator();
     }
    
    /**
     * Return the mail configuration details of thsi resource or null.
     * @return the mail configuration object.
     */
    public MailConfiguration getMailConfiguration() {
	return this.mailConfiguration;
    }
    
    /** 
     * @return true if automatic creation of tables for the CMP Beans is 
     * done at deployment time
     */
    public boolean isCreateTablesAtDeploy() {
        return createTablesAtDeploy;
    }
    
    /**
     * Sets whether if automatic creation of tables for the CMP Beans is 
     * done at deployment time
     */
    public void setCreateTablesAtDeploy(boolean createTablesAtDeploy) {
        this.createTablesAtDeploy = createTablesAtDeploy;
    }
    
    /** 
     * @return true if automatic creation of tables for the CMP Beans is 
     * done at deployment time
     */
    public boolean isDropTablesAtUndeploy() {
        return dropTablesAtUndeploy;
    }
    
    /**
     * Sets whether if automatic creation of tables for the CMP Beans is 
     * done at deployment time
     */
    public void setDropTablesAtUndeploy(boolean dropTablesAtUndeploy) {
        this.dropTablesAtUndeploy = dropTablesAtUndeploy;
    }
    
    /**
     * @return the database vendor name
     */
    public String getDatabaseVendorName() {
        return databaseVendorName;
    }
    
    /**
     * Sets the database vendor name
     */
    public void setDatabaseVendorName(String vendorName) {
        this.databaseVendorName = vendorName;
    }
    
    /**
     * @return the override properties for the schema generation
     */
    public Properties getSchemaGeneratorProperties() {
        return schemaGeneratorProperties;
    }
    
    /**
     * Sets the override properties for the schema generation
     */
    public void setSchemaGeneratorProperties(Properties props) {
        schemaGeneratorProperties = props;
    }
    
    /**
     * Equality on name. 
     */
    public boolean equals(Object object) {
	if (object instanceof ResourceReference) {
	    ResourceReference resourceReference = (ResourceReference) object;
	    return resourceReference.getName().equals(this.getName());
	}
	return false;
    }
    
    public int hashCode() {
        int result = NULL_HASH_CODE;
        String name = getName();
        if (name != null) {
            result += name.hashCode();
        }
        return result;
    }

    /** 
     * Returns a formatted string representing my state.
     */
    public void print(StringBuffer toStringBuffer) {
        StringBuffer sb = toStringBuffer;
        sb.append("Res-Ref-Env-Property: ");
        sb.append(super.getName());
        sb.append("@");
        sb.append(getType());
        sb.append("@");
        sb.append(getDescription());
	if (this.isResolved()) {
	    sb.append(" resolved as: jndi: ");
            sb.append(getJndiName());
            sb.append("@res principal: ");
            sb.append(getResourcePrincipal());
            sb.append("@mail: ");
            sb.append(getMailConfiguration());
	}
        if (runtimeProps!=null) {
            for (Iterator itr = runtimeProps.iterator();itr.hasNext();) {
                sb.append("\nPropery : ");
                sb.append(itr.next());
            }
        } else {
            sb.append("\nNo Runtime properties");
        }
        sb.append("\nDatabase Vendor : " + databaseVendorName);
        sb.append("\nCreate Tables at Deploy : " + createTablesAtDeploy);
        sb.append("\nDelete Tables at Undeploy : " + dropTablesAtUndeploy);
        
        if (schemaGeneratorProperties!=null) {
            sb.append("\nSchema Generator Properties : ");
            sb.append(schemaGeneratorProperties);
        }
                
    }
   //START OF IASRI 4633229
    /**
     * Return true if this resource is a CCI connection factory.
     * @return true if the resource is a CCI connection factory, false 
     * otherwise.
     */
    public boolean isResourceConnectionFactory () {
        return this.getType().equals(CONNECTOR_RESOURCE_TYPE);
    }    
    //END OF IASRI 4633229

    // START OF IASRI 4718559, 4729298
    /** 
    ** checks the given class type. throws an IllegalArgumentException 
    ** if bounds checking
    ** if the class of type "type" does not exist
    */
    public void checkType() {
        if (rType == null) {
	    if (this.isBoundsChecking()) {
	        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptiontypenotallowedpropertytype", 
                "{0} is not an allowed property value type", 
                new Object[] {"null"}));
            }
        }
	if (rType != null) {
	    Class typeClass = null;
	    // is it loadable ?
	    try {
	        // Bug fix 4850684: for resource-refs that are user-defined classes,
	        // the classloader used to load them cannot be the one associated 
	        // with the application deployed, since the classloader instance
	        // would have no idea about classes not included in app
	        // for e.g connector module with res-ref that points to the
	        // ConnectionFactory class of a resource adapter
	      
	        typeClass = Class.forName(rType, true,
					  Thread.currentThread().getContextClassLoader());
		
	    } catch (Throwable t) {
	        if (this.isBoundsChecking()) {
		    throw new IllegalArgumentException(localStrings.getLocalString(
                  "enterprise.deployment.exceptiontypenotallowedpropertytype", 
                  "{0} is not an allowed property value type", 
                  new Object[] {rType}));
		} else {
		    return;
		}
	    }
        }
    }
    // END OF IASRI 4718559, 4729298

    public boolean isConflict(ResourceReferenceDescriptor other) {
        return (getName().equals(other.getName())) &&
            (!(DOLUtils.equals(getType(), other.getType()) &&
                getAuthorization().equals(other.getAuthorization()) &&
                getSharingScope().equals(other.getSharingScope())
                ) ||
            isConflictResourceGroup(other));
    }
}
