/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions Copyright [2016-2017] [Payara Foundation]
package fish.payara.arquillian.container.payara.clientutils;

/**
 * @author Z.Paulovics
 */
public class PayaraClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Construct a new instance with the supplied message
     */
    public PayaraClientException() {
        super();
    }

    /**
     * Construct a new instance with the supplied message
     *
     * @param message
     *     the message
     */
    public PayaraClientException(String message) {
        super(message);
    }

    /**
     * Construct a new instance with the supplied message and cause
     *
     * @param message
     *     the message
     * @param cause
     *     the Throwable that caused the exception to be thrown
     */
    public PayaraClientException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a new instance with the supplied cause
     *
     * @param cause
     *     the Throwable that caused the exception to be thrown
     */
    public PayaraClientException(Throwable cause) {
        super(cause);
    }
}
