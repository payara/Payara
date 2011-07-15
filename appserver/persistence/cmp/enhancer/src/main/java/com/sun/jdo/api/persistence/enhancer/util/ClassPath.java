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

import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;


/**
 * ClassPath provides class file lookup according to a classpath 
 * specification.
 */

public class ClassPath {
  /* The entire class path specification */
  private String theClassPathSpec;

  /* Linked list of class path elements */
  private ClassPathElement theClassPath;

  /**
   * Construct a class path from the input String argument.
   * The path is expected to be in the form appropriate for the current
   * execution environment.
   */
  public ClassPath(String path) {
    theClassPathSpec = path;
    parsePath();
  }

  /**
   * locate a class file given a fully qualified class name
   */
  public ClassFileSource findClass(String className) {
    return findClass(className, theClassPath);
  }

  /**
   * locate a class file given a fully qualified class name
   * starting at the specified class path element
   */
  static ClassFileSource findClass(String className, ClassPathElement path) {
    for (ClassPathElement e = path; e != null; e = e.next()) {
      ClassFileSource source = e.sourceOf(className);
      if (source != null) {
	source.setSourceElement(e);
	return source;
      }
    }

    return null;
  }

  /**
   * Return a file name which might reasonably identify a file containing
   * the specified class.  The name is a "./" relative path.
   */
  public static String fileNameOf(String className, char separator) {
    StringBuffer path = new StringBuffer();
    StringTokenizer parser = new StringTokenizer(className, "./", false);//NOI18N
    for (boolean first = true; parser.hasMoreElements(); first = false) {
      if (!first)
	path.append(separator);
      path.append(parser.nextToken());
    }
    path.append(".class");//NOI18N
    return path.toString();
  }


  /**
   * Return a file name which might reasonably identify a file containing
   * the specified class.  The name is a "./" relative path.
   */
  public static String fileNameOf(String className) {
    return fileNameOf(className, File.separatorChar);
  }


  /**
   * Return a file name which might reasonably identify a file containing
   * the specified class in a zip file.
   */
  public static String zipFileNameOf(String className) {
    return fileNameOf(className, '/');
  }


  /**
   * Return the vm class name which corresponds to the input file name.
   * The file name is expected to be a "./" relative path.
   * Returns null if the file name doesn't end in ".class"
   */
  public static String classNameOf(String fileName) {
    int fnlen = fileName.length();
    if (fnlen > 6 && fileName.regionMatches(true, fnlen - 6, ".class", 0, 6)) {//NOI18N
      /* the file name ends with .class */
      fileName = fileName.substring(0, fileName.length()-6);
      StringBuffer className = new StringBuffer();
      StringTokenizer parser = new StringTokenizer(fileName, "\\/", false);//NOI18N
      for (boolean first = true; parser.hasMoreElements(); first = false) {
	if (!first)
	  className.append('/');
	className.append(parser.nextToken());
      }
      return className.toString();
    }
    return null;
  }

  /**
   * Remove any class path elements which match directory
   */
  public boolean remove(File directory) {
    boolean matched = false;
    ClassPathElement firstElement = theClassPath;
    ClassPathElement prevElement = null;
    for (ClassPathElement cpe = firstElement; cpe != null; cpe = cpe.next()) {
      if (cpe.matches(directory)) {
	matched = true;
	if (prevElement == null)
	  firstElement = cpe.next();
	else
	  prevElement.setNext(cpe.next());
      } else {
	prevElement = cpe;
      }
    }
    theClassPath = firstElement;
    return matched;
  }

  /**
   * Append a directory to the classpath.
   */
  public void append(File directory) {
    append(ClassPathElement.create(directory.getPath()));
  }

  /**
   * Append a class path element to the classpath.
   */
  public void append(ClassPathElement anElement) {
    if (theClassPath == null)
      theClassPath = anElement;
    else
      theClassPath.append(anElement);
  }

  /**
   * Return an enumeration of all of the class files in the specified 
   * package in this class path.
   * @param packageName specifies the VM format package name 
   *    to which class files must belong.
   * @return an Enumeration of the VM format class names which
   *    can be found.  Note that the Enumeration value is of type String
   *    and duplicate entries may be returned as the result of finding
   *    a class through more than one class path element.  Note also
   *    that the class name returned might not correspond the the
   *    name of the class in the file.
   */
  public Enumeration classesInPackage(String packageName) {
    return new ClassPackageEnumeration(this, packageName);
  }

  /* package local accessors */
  ClassPathElement getPathElements() {
    return theClassPath;
  }

  /* private accessors */

  private void parsePath() {
    StringTokenizer parser = 
      new StringTokenizer(theClassPathSpec,
			  java.io.File.pathSeparator,
			  false /* dont return delimiters */
			  );
    
    ClassPathElement lastElement = null;
    while (parser.hasMoreElements()) {
      ClassPathElement anElement = ClassPathElement.create(parser.nextToken());

      if (lastElement == null)
	theClassPath = anElement;
      else
	lastElement.append(anElement);

      lastElement = anElement;
    }
  }

}

/**
 * An enumeration class which returns the names of the classes which
 * can be found in a class path
 */

class ClassPackageEnumeration implements Enumeration {
  /* The next class path element to look for matches in once
     the current enumeration is complete */
  private ClassPathElement nextClassPathElement; 

  /* The package name */
  private String thePackageName;

  /* The enumeration of matching class names in the current class path
     element */
  private Enumeration currentElementEnumeration;

  /**
   * Construct a ClassPackageEnumeration.
   * @param classPath The class path in which to search for classes.
   * @param packageName The VM name of the package in which to search.
   */
  ClassPackageEnumeration(ClassPath classPath, String packageName) {
    nextClassPathElement = classPath.getPathElements();
    thePackageName = packageName;
  }

  public boolean hasMoreElements() {
    while ((currentElementEnumeration == null ||
	    !currentElementEnumeration.hasMoreElements()) &&
	   nextClassPathElement != null) {
      currentElementEnumeration = 
	nextClassPathElement.classesInPackage(thePackageName);
      nextClassPathElement = nextClassPathElement.next();
    }

    return (currentElementEnumeration != null &&
	    currentElementEnumeration.hasMoreElements());
  }

  public Object nextElement() {
    if (hasMoreElements())
      return currentElementEnumeration.nextElement();

    throw new NoSuchElementException();
  }
}

