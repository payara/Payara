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

package javax.enterprise.deploy.model;

/**
 * An interface for beans that represent a fragment of a 
 * standard deployment descriptor.  A link is provided to 
 * the Java EE application that includes this bean.
 */
public interface DDBean 
{
    
    /**
     * Returns the original xpath string provided by the DConfigBean.
     * @return The XPath of this Bean.
     */    
    public String getXpath();
    
    /**
     * Returns the XML text for by this bean.
     * @return The XML text for this Bean.
     */    
	public String getText();

    /**
     * Returns a tool-specific reference for attribute ID on an 
     * element in the deployment descriptor.  This attribute is 
     * defined for J2EE 1.2 and 1.3 components.
     * @return The XML text for this Bean or 'null' if
     *            no attribute was specifed with the tag.
     */    
	public String getId();
   
   /**
    * Return the root element for this DDBean.
    * @return The DDBeanRoot at the root of this DDBean
    * tree.
    */   
   public DDBeanRoot getRoot();
      
   /**
    * Return a list of DDBeans based upon the XPath.
    * @param xpath An XPath string referring to a location in the
    * same deployment descriptor as this standard bean.
    * @return a list of DDBeans or 'null' if no matching XML data is
    *            found. 
    */   
   public DDBean[] getChildBean(String xpath);
   
   /**
    * Return a list of text values for a given XPath in the
    * deployment descriptor.
    * @param xpath An XPath.
    * @return The list text values for this XPath or 'null'
    *     if no matching XML data is found.
    */   
   public String[] getText(String xpath);
   
   /**
    * Register a listener for a specific XPath.
    *
    * @param xpath The XPath this listener is to be registered for.
    * @param xpl The listener object.
    */
   public void addXpathListener(String xpath, XpathListener xpl);

   /**
    * Unregister a listener for a specific XPath.
    *
    * @param xpath The XPath from which this listener is to be
    *          unregistered.
    * @param xpl The listener object.
    */
   public void removeXpathListener(String xpath, XpathListener xpl);

    /** 
     * Returns the list of attribute names associated with the XML element. 
     *
     * @return a list of attribute names on this element.  Null 
     * is returned if there are no attributes. 
     */ 
   public String[] getAttributeNames(); 

    /**
     * Returns the string value of the named attribute. 
     *
     * @return a the value of the attribute.  Null is returned 
     * if there is no such attribute. 
     */ 
   public String getAttributeValue(String attrName); 
}

