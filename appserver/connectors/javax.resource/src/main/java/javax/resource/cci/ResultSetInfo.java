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

package javax.resource.cci;


import javax.resource.ResourceException;

/** 
 * The interface <code>javax.resource.cci.ResultSetInfo</code> provides
 * information on the support provided for ResultSet by a connected 
 * EIS instance. A component calls the method 
 * <code>Connection.getResultInfo</code> to get the ResultSetInfo instance. 
 * 
 * <p>A CCI implementation is not required to support 
 * <code>javax.resource.cci.ResultSetInfo</code> interface. The 
 * implementation of this interface is provided only if the CCI 
 * supports the ResultSet facility.
 * 
 * @version     0.9
 * @author      Rahul Sharma
 * @see         javax.resource.cci.Connection
 * @see         java.sql.ResultSet
 * @see         javax.resource.cci.ConnectionMetaData
 */

public interface ResultSetInfo {
  
  /** 
   * Indicates whether or not a visible row update can be detected 
   * by calling the method <code>ResultSet.rowUpdated</code>.
   *
   * @param   type    type of the ResultSet i.e. ResultSet.TYPE_XXX
   * @return          true if changes are detected by the result set 
   *                  type; false otherwise
   * @see     java.sql.ResultSet#rowUpdated
   * @throws  ResourceException   Failed to get the information
   */
  public 
  boolean updatesAreDetected(int type)  throws ResourceException;

  /** 
   * Indicates whether or not a visible row insert can be detected
   * by calling ResultSet.rowInserted.
   *
   * @param   type    type of the ResultSet i.e. ResultSet.TYPE_XXX
   * @return          true if changes are detected by the result set 
   *                  type; false otherwise
   * @see     java.sql.ResultSet#rowInserted
   * @throws  ResourceException   Failed to get the information
   */
  public 
  boolean insertsAreDetected(int type)  throws ResourceException;
	
  /** 
   * Indicates whether or not a visible row delete can be detected by
   * calling ResultSet.rowDeleted.  If deletesAreDetected
   * returns false, then deleted rows are removed from the ResultSet.
   *
   * @param   type    type of the ResultSet i.e. ResultSet.TYPE_XXX
   * @return          true if changes are detected by the result set 
   *                  type; false otherwise
   * @see     java.sql.ResultSet#rowDeleted
   * @throws  ResourceException   Failed to get the information
   */
  public
  boolean deletesAreDetected(int type)  throws ResourceException;
  
  /** 
   * Indicates whether or not a resource adapter supports a type
   * of ResultSet.
   *     
   * @param   type  type of the ResultSet i.e. ResultSet.TYPE_XXX
   * @return        true if ResultSet type supported; false otherwise
   * @throws  ResourceException   Failed to get the information
   */
  public 
  boolean supportsResultSetType(int type) throws ResourceException;

  /** 
   * Indicates whether or not a resource adapter supports the 
   * concurrency type in combination with the given ResultSet type/
   *
   * @param   type        type of the ResultSet i.e. ResultSet.TYPE_XXX
   * @param   concurrency ResultSet concurrency type defined in
   *                      java.sql.ResultSet
   * @return  true if the specified combination supported; false otherwise
   * @throws  ResourceException   Failed to get the information
   */
  public 
  boolean supportsResultTypeConcurrency(int type,
					int concurrency)  throws ResourceException;


  /** 
   * Indicates whether updates made by others are visible.
   *
   * @param    type       type of the ResultSet i.e. ResultSet.TYPE_XXX
   * @return              true if updates by others are visible for the
   *                      ResultSet type; false otherwise
   * @throws  ResourceException   Failed to get the information
   */
  public
  boolean othersUpdatesAreVisible(int type)  throws ResourceException;

  /**
   * Indicates whether deletes made by others are visible.
   *
   * @param    type       type of the ResultSet i.e. ResultSet.TYPE_XXX
   * @return              true if deletes by others are visible for the
   *                      ResultSet type; false otherwise
   * @throws  ResourceException   Failed to get the information
   */
  public
  boolean othersDeletesAreVisible(int type)  throws ResourceException;
    
  /**
   * Indicates whether inserts made by others are visible.
   *
   * @param    type       type of the ResultSet i.e. ResultSet.TYPE_XXX
   * @return              true if inserts by others are visible for the
   *                      ResultSet type; false otherwise
   * @throws  ResourceException   Failed to get the information
   */
  public
  boolean othersInsertsAreVisible(int type) throws ResourceException;


  /**
   * Indicates whether a ResultSet's own updates are visible.
   *
   * @param    type       type of the ResultSet i.e. ResultSet.TYPE_XXX
   * @return              true if updates are visible for the ResultSet
   *                      type; false otherwise
   * @throws  ResourceException   Failed to get the information
   */
  public
  boolean ownUpdatesAreVisible(int type) throws ResourceException;

  /** 
   * Indicates whether a ResultSet's own inserts are visible.
   *
   * @param    type       type of the ResultSet i.e. ResultSet.TYPE_XXX
   * @return              true if inserts are visible for the ResultSet
   *                      type; false otherwise
   * @throws  ResourceException   Failed to get the information
   */  
  public 
  boolean ownInsertsAreVisible(int type) throws ResourceException;

  /**
   * Indicates whether a ResultSet's own deletes are visible.
   *
   * @param    type       type of the ResultSet i.e. ResultSet.TYPE_XXX
   * @return              true if inserts are visible for the ResultSet
   *                      type; false otherwise
   * @throws  ResourceException   Failed to get the information
   */  
  public 
  boolean ownDeletesAreVisible(int type) throws ResourceException;

}
