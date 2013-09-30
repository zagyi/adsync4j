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

import java.util.Map;

public class InMemoryActiveDirectoryMockFactoryBean extends EmbeddedLdapServerFactoryBean {

    private Map<String, String> _rootDSEAttributes;

    @Override
    protected EmbeddedUnboundIDLdapServer createEmbeddedUnboundIDLdapServer() {
        InMemoryActiveDirectoryMock activeDirectoryMock = new InMemoryActiveDirectoryMock();
        activeDirectoryMock.setRootDSEAttributes(_rootDSEAttributes);
        return activeDirectoryMock;
    }

    public void setRootDSEAttributes(Map<String, String> rootDSEAttributes) {
        _rootDSEAttributes = rootDSEAttributes;
    }
}
