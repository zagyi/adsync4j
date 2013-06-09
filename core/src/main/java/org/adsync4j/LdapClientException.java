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
package org.adsync4j;

import javax.annotation.Nullable;

public class LdapClientException extends RuntimeException {

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
}
