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

package com.sun.jdo.api.persistence.enhancer.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

/**
 * Abstract base class for components of a class path.
 */

abstract class ClassPathElement {

  /* A link to the next element in the path */
  private ClassPathElement next;

  /* package local accessors */

  /**
   * If this class path element resolves the class, return a 
   * ClassFileSource for the class.
   */
  public abstract ClassFileSource sourceOf(String className);

  /**
   * Return an enumeration of all of the class files in the specified 
   * package in this class path element.
   *
   * @param packageName specifies the VM format package name 
   *    to which class files must belong.
   * @returns an Enumeration of the VM format class names which
   *    can be found.  The return value may be null if the class
   *    path element is not valid.
   */
  public abstract Enumeration classesInPackage(String packageName);

  /**
   * Check to see if this ClassPathElement is a directory matching
   * the directory passed in.
   */
  abstract boolean matches(File directory);

  /**
   * Return the next class path element in the chain 
   */
  ClassPathElement next() {
    return this.next;
  }

  /**
   * Set the the next class path element in the chain 
   */
  void setNext(ClassPathElement next) {
    this.next = next;
  }

  /**
   * Construct a class path element 
   */
  ClassPathElement() {
  }

  /**
   * Create an appropriate type of class path element based on the
   * path element name.
   */
  static ClassPathElement create(String elementSpec) {
    File element = new File(elementSpec);
    if (!element.isDirectory() &&
	looksLikeZipName(elementSpec))
      return new ZipFileClassPathElement(element);
    else
      return new DirectoryClassPathElement(element);
  }

  /**
   * Append a class path element to the chain
   */
  void append(ClassPathElement another) {
    ClassPathElement e = this;
    while (e.next() != null)
      e = e.next();
    e.next = another;
  }

  /**
   * Check whether the String "looks" like a zip file name.
   */
  static protected boolean looksLikeZipName(String fname) {
    return (fname.length() > 4 &&
	    (fname.regionMatches(true, fname.length() - 4, ".zip", 0, 4) ||//NOI18N
	     fname.regionMatches(true, fname.length() - 4, ".jar", 0, 4)));//NOI18N
  }

}

/**
 * DirectoryClassPathElement represents a component of a class path
 * which is believe to be a directory.
 */

class DirectoryClassPathElement extends ClassPathElement {

  private File directory;

  private boolean exists;

  /* package local accessors */

  /**
   * If this class path element resolves the class, return a 
   * ClassFileSource for the class.
   */
  public ClassFileSource sourceOf(String className) {
    File f = fileOf(className);
    if (f != null && f.exists()) {
      return new ClassFileSource(className, f);
    }
    return null;
  }

  public Enumeration classesInPackage(String packageName) {
    if (!exists)
      return null;

    return new DirectoryClassPackageEnumerator(directory, packageName);
  }

  boolean matches(File matchDirectory) {
    String dir = FilePath.canonicalize(directory);
    String matchDir = FilePath.canonicalize(matchDirectory);
    return FilePath.canonicalNamesEqual(dir, matchDir);
  }

  /**
   * Construct a class path element 
   */
  DirectoryClassPathElement(File dirSpec) {
    directory = dirSpec;
    checkValid();
  }

  /* private methods */

  /**
   * fileOf Return a File object which might reasonably contain the
   * specified class.  The contents may or may not be valid.
   */
  private File fileOf(String className) {
    if (exists && directory.isDirectory()) {
      StringBuffer newPath = new StringBuffer(directory.getPath());
      if (newPath.charAt(newPath.length() - 1) != File.separatorChar)
	newPath.append(File.separatorChar);
      newPath.append(ClassPath.fileNameOf(className));

      File f = new File(newPath.toString());
      if (f.isFile())
	return f;
    }

    return null;
  }

  /**
   * Is this class path element valid?  That is, does the directory
   * exist with the specified name?
   */
  private boolean isValid() {
    return exists;
  }

  private void checkValid() {
    exists = directory.isDirectory();
  }
}

/**
 * ZipFileClassPathElement represents a component of a class path
 * which is believe to be a zip file containing classes.
 */

class ZipFileClassPathElement extends ClassPathElement {

  private File zipFileElement;
  private ZipFile zipFile;

  /* package local accessors */

  /**
   * If this class path element resolves the class, return a 
   * ClassFileSource for the class.
   */
  public ClassFileSource sourceOf(String className) {
    if (zipFile != null) {
      ZipEntry entry =
	zipFile.getEntry(ClassPath.zipFileNameOf(className));
      if (entry != null) {
	return new ClassFileSource(className, zipFile);
      }
    }
    return null;
  }

  public Enumeration classesInPackage(String packageName) {
    if (zipFile == null)
      return null;

    return new ZipFileClassPackageEnumerator(zipFile, packageName);
  }

  boolean matches(File directory) {
    return false;
  }

  /**
   * Construct a zip file class path element 
   */
  ZipFileClassPathElement(File elementSpec) {
    zipFileElement = elementSpec;
    checkValid();
  }

  /* private methods */

  private void checkValid() {
    if (looksLikeZipName(zipFileElement.getPath()) &&
	zipFileElement.isFile()) {
      try {
	zipFile = ZipFileRegistry.openZipFile(zipFileElement);
      } catch (IOException e) {
	System.err.println("IO exception while reading " +
			   zipFileElement.getPath());
	zipFile = null;
      }
    }
  }
}


/**
 * An enumeration class which returns the names of the classes which
 * can be found relative to a particular directory.
 */

class DirectoryClassPackageEnumerator 
  implements Enumeration, FilenameFilter {

  private String[] matches;
  private int nextMatch = -1;
  String searchPackage;

  /**
   * Constructor 
   * @param directory The directory to be used as the root of the
   *   package structure.
   * @param packageName The name of the package to search (in VM form).
   */
  DirectoryClassPackageEnumerator(File directory, String packageName) {
    searchPackage = packageName;
    String packageDirName = directory.getPath() + File.separator + 
      packageName.replace('/', File.separatorChar);

    File packageDir = new File(packageDirName);
    if (packageDir.isDirectory()) {
      matches = packageDir.list(this);
      if (matches != null && matches.length > 0)
	nextMatch = 0;
    }
  }

  public boolean hasMoreElements() {
    return (nextMatch >= 0);
  }

  public Object nextElement() {
    if (!hasMoreElements())
      throw new NoSuchElementException();
    String next = matches[nextMatch++];
    if (nextMatch >= matches.length)
      nextMatch = -1;
    return ClassPath.classNameOf(searchPackage + "/" + next);//NOI18N
  }

  /**
   * Check whether the file name is valid.
   * Needed for FilenameFilter implementation.
   */

  public boolean accept(File dir, String name) {
    int nameLength = name.length();
    boolean isOk = (nameLength > 6 &&
		    name.regionMatches(true, nameLength - 6, ".class", 0, 6));//NOI18N
    return isOk;
  }

}

/**
 * An enumeration class which returns the names of the classes which
 * can be found within a zip file.
 */

class ZipFileClassPackageEnumerator implements Enumeration {
  Enumeration zipFileEntries;
  ZipEntry nextEntry;
  String packageName;

  ZipFileClassPackageEnumerator(ZipFile zipFile, String packageName) {
    zipFileEntries = zipFile.entries();
    this.packageName = packageName;
  }

  public boolean hasMoreElements() {
    while (nextEntry == null && zipFileEntries != null &&
	   zipFileEntries.hasMoreElements()) {
      ZipEntry ent = (ZipEntry) zipFileEntries.nextElement();
      String memName = ent.getName();
      int memNameLength = memName.length();
      int packageNameLength = packageName.length();

      /* Check that the package name is a prefix of the member name.
	 Note that we rely here on the fact that zip file have a separator
	 character identical to the VM package separator */

      if (memNameLength > packageNameLength + 1 &&
	  memName.regionMatches(false, 0, packageName, 
				0, packageNameLength) &&
	  memName.charAt(packageNameLength) == '/') {
	if (memName.indexOf('/', packageNameLength+1) == -1) {
	  boolean isOk =
	    (memNameLength > packageNameLength+7 &&
	     memName.regionMatches(true, memNameLength - 6, ".class", 0, 6));//NOI18N
	  if (isOk)
	    nextEntry = ent;
	}
      }
    }
    return nextEntry != null;
  }

  public Object nextElement() {
    if (!hasMoreElements())
      throw new NoSuchElementException();
    String className = nextEntry.getName();
    nextEntry = null;
    return ClassPath.classNameOf(className);
  }
}
