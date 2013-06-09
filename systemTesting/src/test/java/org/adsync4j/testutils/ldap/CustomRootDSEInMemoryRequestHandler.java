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

import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryRequestHandler;
import com.unboundid.ldap.listener.LDAPListenerClientConnection;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.util.Debug;

import java.util.Map;

public class CustomRootDSEInMemoryRequestHandler extends InMemoryRequestHandler {

    private static final String AD_ROOT_DSE_PROPERTY_PREFIX = "adRootDse.";

    public CustomRootDSEInMemoryRequestHandler(InMemoryDirectoryServerConfig config) throws LDAPException {
        super(config);
    }

    public CustomRootDSEInMemoryRequestHandler(InMemoryRequestHandler parent, LDAPListenerClientConnection connection) {
        super(parent, connection);
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
        for (Map.Entry<Object, Object> systemProperty : System.getProperties().entrySet()) {
            String propertyName = (String) systemProperty.getKey();
            String propertyValue = (String) systemProperty.getValue();
            if (propertyName.startsWith(AD_ROOT_DSE_PROPERTY_PREFIX)) {
                addExtendedRootDSEAttribute(
                        rootDSEEntry, propertyName.substring(AD_ROOT_DSE_PROPERTY_PREFIX.length()), propertyValue);
            }
        }
    }

    private void addExtendedRootDSEAttribute(Entry rootDSEEntry, String propertyName, String propertyValue) {
        rootDSEEntry.addAttribute(propertyName, propertyValue);
        Debug.getLogger().info(String.format("Added extended Root DSE attribute: %s = %s", propertyName, propertyValue));
    }
}
