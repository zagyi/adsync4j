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

import com.unboundid.ldap.listener.InMemoryRequestHandler;
import com.unboundid.ldap.listener.LDAPListenerClientConnection;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.util.Debug;

import java.util.Map;

public class CustomRootDSEInMemoryRequestHandler extends InMemoryRequestHandler {

    private InMemoryDirectoryServerConfigWithRootDSEAttributes _config;

    public CustomRootDSEInMemoryRequestHandler(InMemoryDirectoryServerConfigWithRootDSEAttributes config) throws LDAPException {
        super(config);
        _config = config;
    }

    public CustomRootDSEInMemoryRequestHandler(
            CustomRootDSEInMemoryRequestHandler parent, LDAPListenerClientConnection connection)
    {
        super(parent, connection);
        _config = parent._config;

    }

    @Override
    public InMemoryRequestHandler newInstance(LDAPListenerClientConnection connection) throws LDAPException {
        return new CustomRootDSEInMemoryRequestHandler(this, connection);
    }

    @Override
    protected ReadOnlyEntry generateRootDSE() {
        ReadOnlyEntry rootDSEFromSuper = super.generateRootDSE();
        Entry rootDSE = new Entry(rootDSEFromSuper.getDN(), getSchema(), rootDSEFromSuper.getAttributes());
        addExtendedRootDSEAttributes(rootDSE);
        return new ReadOnlyEntry(rootDSE);
    }

    private void addExtendedRootDSEAttributes(Entry rootDSEEntry) {
        if (_config.getRootDSEAttributes() != null) {
            for (Map.Entry<String, String> rootDSEAttribute : _config.getRootDSEAttributes().entrySet()) {
                String attributeName = rootDSEAttribute.getKey();
                String attributeValue = rootDSEAttribute.getValue();

                rootDSEEntry.addAttribute(attributeName, attributeValue);

                Debug.getLogger().info(String.format("Added extended Root DSE attribute: %s = %s", attributeName, attributeValue));
            }
        }
    }
}
