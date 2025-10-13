/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2019] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.servermgmt.stringsubs.impl.algorithm;

/**
 * Performs string substitution for the given input.  Substitution process look
 * for the matching input in the given {@link RadixTree} and replaced the string
 * with the corresponding matching value.
 *
 * @see {@link RadixTree}
 */
class RadixTreeSubstitution {
    /** {@link RadixTree} used sub. */
    private RadixTree tree;

    /** Buffer to store the current processing characters, reset when match found
     *  for the processed character. */
    private StringBuilder processedChars;

    /** Reference to the currently processing node.*/
    private RadixTreeNode currentNode;

    /** No of matched character in currently processing node. */
    private int nodeMatchedChars;

    /** Last matched node value. */
    private String lastMatchedValue;

    /** Buffer to store the characters need to re-process from root. */
    private StringBuilder reProcessChars;

    /**
     * Construct {@link RadixTreeSubstitution} for the given {@link RadixTree}.
     * @param tree
     */
    RadixTreeSubstitution(RadixTree tree) {
        if (tree == null) {
            throw new IllegalArgumentException("Invalid tree.");
        }
        this.tree = tree;
        processedChars = new StringBuilder();
        currentNode = tree.getRootNode();
    }

    /**
     * Perform substitution by allowing continuous character feeding. Once
     * the character sequence matched completely to {@link RadixTree} node
     * and no further/extended match is available then the output is returned
     * and the values reset to re-look the new input from root node.
     * <p>
     * Method maintains the processed characters, currently processing
     * node and other parameters require in substitution.
     * </p>
     * <p>
     * <b>NOTE:</b> A <code>null</code> input signify the end of the processing.
     * </p>
     * @param c Input character to match with the node key.
     * @return
     * <li>Value of the matching node, if no further match available.</li>
     * <li>Return the string of processed characters if no matching node found
     *   or the node value is null.</li>
     * <li><code>null</code> if waiting for more input char.</li>
     */
    String substitute(Character c) {
        StringBuilder outputBuffer = null;
        boolean finalCall = (c == null);
        do {
            if (c != null) {
                if (reProcessChars != null && reProcessChars.length() > 0) {
                    c = reProcessChars.charAt(0);
                    reProcessChars.delete(0, 1);
                }
                String nodeKey = currentNode.getKey();
                if (nodeMatchedChars < nodeKey.length()) {
                    if (c == nodeKey.charAt(nodeMatchedChars)) {
                        processedChars.append(c);
                        nodeMatchedChars++;
                        continue;
                    }
                } else {
                    if (currentNode.getValue() != null) {
                        lastMatchedValue = currentNode.getValue();
                        processedChars.delete(0, processedChars.length());
                    }
                    RadixTreeNode childNode =  currentNode.getChildNode(c);
                    if (childNode != null) {
                        processedChars.append(c);
                        currentNode = childNode;
                        nodeMatchedChars = 1;
                        continue;
                    }
                }
            } else if(currentNode.getValue() != null && nodeMatchedChars == currentNode.getKey().length()) {
                lastMatchedValue = currentNode.getValue();
                processedChars.delete(0, processedChars.length());
            }

            if (outputBuffer == null) {
                outputBuffer = new StringBuilder();
            }
            // write to the output buffer.
            if (lastMatchedValue != null) {
                outputBuffer.append(lastMatchedValue);
                if (c != null) {
                    processedChars.append(c);
                }
                lastMatchedValue = null;
            } else {
                // If no match found than append the first character and start fresh from second character.
                if (processedChars.length() > 0) {
                    outputBuffer.append(processedChars.charAt(0));
                    processedChars.delete(0, 1);
                    if (c != null) {
                        processedChars.append(c);
                    }
                } else {
                    if (c != null) {
                        outputBuffer.append(c);
                    }
                    processedChars.delete(0, processedChars.length());
                }
            }
            if (processedChars.length() > 0) {
                if (reProcessChars == null) {
                    reProcessChars = new StringBuilder(processedChars);
                } else {
                    processedChars.append(reProcessChars);
                    reProcessChars.delete(0, reProcessChars.length());
                    reProcessChars.append(processedChars);
                }
                c = reProcessChars.charAt(0);
                processedChars.delete(0, processedChars.length());
            }
            currentNode = tree.getRootNode();
            nodeMatchedChars = 0;
        } while (reProcessChars != null && reProcessChars.length() > 0);

        //Append the last process character sequence.
        if (finalCall) {
            if (nodeMatchedChars == currentNode.getKey().length() && currentNode.getValue() != null) {
                outputBuffer.append(currentNode.getValue());
            } else {
                outputBuffer.append(currentNode.getKey().substring(0, nodeMatchedChars));
            }
            processedChars.delete(0, processedChars.length());
            currentNode = tree.getRootNode();
            nodeMatchedChars = 0;
            lastMatchedValue = null;
        }
        return outputBuffer == null || outputBuffer.toString().isEmpty() ? null : outputBuffer.toString();
    }
}