/*******************************************************************************
 * ADSync4J (https://github.com/zagyi/adsync4j)
 *
 * Copyright (c) 2013 Balazs Zagyvai
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Balazs Zagyvai
 ******************************************************************************/
package org.adsync4j.api;

import javax.annotation.Nullable;

/**
 * Unchecked exception thrown on error conditions when communicating with an LDAP (Active Directory) server.
 */
public class LdapClientException extends RuntimeException {

    /**
     * Shortcut/convenience method used to throw an {@link LdapClientException} in case the passed reference is {@code null}.
     * Clients using this method avoid typing the repetitive, boilerplate code:
     * {@code if (x==null) {throw new LdapClientException(...)}}.
     *
     * @param referenceToCheck  Reference that must not be null.
     * @param messageFormat     Defines the exception's message, will be passed to {@link String#format(String, Object...)}.
     * @param messageParameters Defines the exception's message, will be passed to {@link String#format(String, Object...)}.
     * @throws LdapClientException in case the {@code referenceToCheck} parameter is {@code null}.
     */
    public static void throwIfNull(
            @Nullable Object referenceToCheck, String messageFormat, String... messageParameters) throws LdapClientException
    {
        if (referenceToCheck == null) {
            throw new LdapClientException(String.format(messageFormat, (Object[]) messageParameters));
        }
    }

    public LdapClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public LdapClientException(String message) {
        super(message);
    }

    public LdapClientException(Throwable e) {
        super(e);
    }

    public LdapClientException() {
    }
}
