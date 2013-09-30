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
package org.adsync4j.testutils.ldap;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryRequestHandler;
import com.unboundid.ldap.sdk.LDAPException;

public class CustomInMemoryDirectoryServer extends InMemoryDirectoryServer {

    public CustomInMemoryDirectoryServer(InMemoryDirectoryServerConfigWithRootDSEAttributes config) throws LDAPException {
        super(config);
    }

    @Override
    protected InMemoryRequestHandler createLDAPListenerRequestHandler(
            InMemoryDirectoryServerConfig config) throws LDAPException
    {
        if (config instanceof InMemoryDirectoryServerConfigWithRootDSEAttributes) {
            return new CustomRootDSEInMemoryRequestHandler((InMemoryDirectoryServerConfigWithRootDSEAttributes) config);
        } else {
            throw new IllegalArgumentException(
                    "Required config type is " + InMemoryDirectoryServerConfigWithRootDSEAttributes.class.getName() +
                    ", but received an instance of type " + config.getClass().getName());
        }
    }
}
