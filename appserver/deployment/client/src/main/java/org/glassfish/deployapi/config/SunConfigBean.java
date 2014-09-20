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

package org.glassfish.deployapi.config;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.enterprise.deploy.model.DDBean;
import javax.enterprise.deploy.model.XpathEvent;
import javax.enterprise.deploy.spi.DConfigBean;
import javax.enterprise.deploy.spi.DConfigBeanRoot;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;
import javax.enterprise.deploy.model.XpathListener;

import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * Superclass for all the ConfigBeans. ConfigBeans are organized with 
 * a parent-child relationship. The parent defines xPaths and their 
 * mapping to child beans and return this mapping from the 
 * getXPathToBeanMapping method. Each bean is associated with a 
 * DOL descriptor (virtual field for this class) accessible through the 
 * getDescriptor() call. 
 *
 * @author Jerome Dochez
 */
public abstract class SunConfigBean implements DConfigBean, XpathListener {
    
    // Parent of this config bean. All config beans but root beans 
    // have a parent config bean    
    private SunConfigBean parent=null;
    
    // DDBean associated with this config beans used to get the 
    // xpath this config beans is representing. We do not use 
    // the DDBean to extract the Standard DDs since it is initialized
    // at the DConfigBeanRoot level
    protected DDBean ddBean;
    
    // propery change event support
    protected PropertyChangeSupport propertyChange = new PropertyChangeSupport(this);
    
    protected static final LocalStringManagerImpl localStrings =
	  new LocalStringManagerImpl(SunConfigBean.class);
    
    /**
     * Return the JavaBean containing the server-specific deployment 
     * configuration information based upon the XML data provided 
     * by the DDBean.
     *
     * @return The DConfigBean to display the server-specific properties 
     *         for the standard bean.
     * @param bean The DDBean containing the XML data to be 
     *        evaluated.
     * @throws ConfigurationException reports errors in generating
     *           a configuration bean. This DDBean is considered
     *           undeployable to this server until this exception
     *           is resolved.  
     *           A suitably descriptive message is required so the user 
     *           can diagnose the error.
     */    
    public DConfigBean getDConfigBean(DDBean bean) 
               throws ConfigurationException {
                   
        Map mapping = getXPathToBeanMapping();
        if (mapping==null) {
            return null;
        }
        if (mapping.containsKey(bean.getXpath())) {
            Class c = (Class) mapping.get(bean.getXpath());
            try {
                Object o = c.newInstance();
                if (o instanceof SunConfigBean) {
                    SunConfigBean child = (SunConfigBean) o;
                    child.setParent(this);
                    child.setDDBean(bean);
                    return child;
                } 
            } catch(Exception e) {
                Logger.getAnonymousLogger().log(Level.WARNING, "Error occurred", e);  
                throw new ConfigurationException(e.getMessage());
            }
            
        } 
        return null;
    }
    
    /**
    * Return the JavaBean containing the deployment
    * descriptor XML text associated with this DConfigBean.
    * @return The bean class containing the XML text for
    *       this DConfigBean.
    */   
    public DDBean getDDBean() {
        return ddBean;
    }
   
    /**
     * we are being set a new DDBean, we need to reevaluate 
     * ourself. 
     *
     * @param DDBean is the new standard DDBean container
     */
    protected void setDDBean(DDBean ddBean)  throws Exception {
        this.ddBean = ddBean;
        process();
    }
       
    /**
     * A notification that the DDBean provided in the
     * event has changed and this bean or its child beans need
     * to reevaluate themselves.
     *
     * @param event an event containing a reference to the
     *        DDBean which has changed.
     */
    public void notifyDDChange(XpathEvent xpathEvent) {
    }
    
    /**
     * Remove a child DConfigBean from this bean.
     *
     * @param bean The child DConfigBean to be removed.
     * @throws BeanNotFoundException the bean provided
     *         is not in the child list of this bean.
     */
    public void removeDConfigBean(DConfigBean dConfigBean) throws javax.enterprise.deploy.spi.exceptions.BeanNotFoundException {
    }
    
    /**
     * Register a property listener for this bean.
     * @param pcl PropertyChangeListener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
    }
    
   /** 
    * Unregister a property listener for this bean.
    * @param pcl Listener to remove.
    */
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
    }
    
    /**
     * Notification of change from the standard DDBean
     *
     * @param the change event 
     */
    public void fireXpathEvent(XpathEvent xpe) {
        if (getParent()!=null) {
            getParent().fireXpathEvent(xpe);
            return;
        }        
    }
    
    /**
     * Return a list of XPaths designating the deployment descriptor
     * information this DConfigBean requires.
     *
     * A given server vendor will need to specify some server-specific
     * information.  Each String returned by this method is an XPath
     * describing a certain portion of the standard deployment descriptor
     * for which there is corresponding server-specific configuration.
     *
     * @return a list of XPath Strings representing XML data to be retrieved
     *        or 'null' if there are none.
     */
    public String[] getXpaths() {
        Map mapping = getXPathToBeanMapping();
        if (mapping==null) {
            return null;
        }
        
        Set keys = mapping.keySet();        
        String[] xPaths = new String[keys.size()];
        int i=0;
        for (Iterator itr=keys.iterator(); itr.hasNext();) {
            String s = (String) itr.next();
            xPaths[i++]=s;
        }
        return xPaths;
    }    
    
    
    /*
     * set the parent config bean for this config bean
     */
    protected void setParent(SunConfigBean parent) {
        this.parent = parent;
    }
    
    /*
     * @return the parent for this config bean
     */
    public SunConfigBean getParent() {
        return parent;
    }
    
    
    /**
     * Convenience method extract node value from the passed xml fragment. 
     *
     * @param key the xml tag name
     * @param xml fragment to extract the tag value
     * @return the xml tag value
     */
    public static String extractTextFromXML(String key, String xmlFragment) {
       // should we use a parser... seems heavy, we'll see if string parsing is enough
        
       // pass opening tag
       String openingTag = "<" + key + ">";
       xmlFragment = xmlFragment.substring(xmlFragment.indexOf(openingTag)+openingTag.length());
       return xmlFragment.substring(0, xmlFragment.indexOf("</" + key + ">"));       
    }
        
    /**
     * @return the mapping from xpaths to child config beans where
     * the map keys are the xpaths and the values are the class object
     * for the child config beans
     */
    protected abstract Map getXPathToBeanMapping();
    
    /**
     * evaluate a standard bean
     */
    protected abstract void process() throws Exception;
    
    /**
     * @return the associated DOL Descriptor for this config beans
     */
    public abstract Object getDescriptor();
    
    /**
     * @return the ConfigBeanRoot for this config bean
     */
    protected DConfigBeanRoot getDConfigBeanRoot() {
        if (parent!=null) {
            return parent.getDConfigBeanRoot();
        } 
        return null;
    }
        
    /**
     * @return a meaningful string about myself
     */
    public String toString() {
        String s = "DConfigBean";
        s = s + "\nDConfigBeanRoot = " + getDConfigBeanRoot();
        s = s + "\nParent = " + parent;
        s = s + "\nXPath = " + ddBean.getXpath();
        return s;
    }
}
