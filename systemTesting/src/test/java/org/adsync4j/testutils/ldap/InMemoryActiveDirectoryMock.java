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
import com.unboundid.ldap.sdk.LDAPException;

import java.util.Map;

public class InMemoryActiveDirectoryMock extends EmbeddedUnboundIDLdapServer {

    private Map<String, String> _rootDSEAttributes;

    @Override
    protected InMemoryDirectoryServer createInMemoryDirectoryServer(
            InMemoryDirectoryServerConfig config) throws LDAPException
    {
        InMemoryDirectoryServerConfigWithRootDSEAttributes  configWithRootDSEAttributes = new
                InMemoryDirectoryServerConfigWithRootDSEAttributes(config);

        configWithRootDSEAttributes.setRootDSEAttributes(_rootDSEAttributes);

        return new CustomInMemoryDirectoryServer(configWithRootDSEAttributes);
    }

    public void setRootDSEAttributes(Map<String, String> rootDSEAttributes) {
        _rootDSEAttributes = rootDSEAttributes;
    }
}
