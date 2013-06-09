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
package org.adsync4j.impl;

import org.adsync4j.DomainControllerAffiliation;

import java.util.List;
import java.util.UUID;

class ImmutableDomainControllerAffiliation implements DomainControllerAffiliation {

    final String protocol;
    final String host;
    final int port;
    final String bindUser;
    final String bindPassword;
    final String rootDN;

    final UUID invocationId;
    final Long highestCommittedUSN;
    final String syncBaseDN;
    final List<String> attributesToSync;
    final String searchFilter;
    final String searchDeletedObjectsFilter;

    ImmutableDomainControllerAffiliation(DomainControllerAffiliation affiliation) {
        protocol = affiliation.getProtocol();
        host = affiliation.getHost();
        port = affiliation.getPort();
        bindUser = affiliation.getBindUser();
        bindPassword = affiliation.getBindPassword();
        rootDN = affiliation.getRootDN();

        invocationId = affiliation.getInvocationId();
        highestCommittedUSN = affiliation.getHighestCommittedUSN();
        syncBaseDN = affiliation.getSyncBaseDN();
        attributesToSync = affiliation.getAttributesToSync();
        searchFilter = affiliation.getSearchFilter();
        searchDeletedObjectsFilter = affiliation.getSearchDeletedObjectsFilter();
    }

    @Override
    public UUID getInvocationId() {
        return invocationId;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getBindUser() {
        return bindUser;
    }

    @Override
    public String getBindPassword() {
        return bindPassword;
    }

    @Override
    public String getRootDN() {
        return rootDN;
    }

    @Override
    public Long getHighestCommittedUSN() {
        return highestCommittedUSN;
    }

    @Override
    public String getSyncBaseDN() {
        return syncBaseDN;
    }

    @Override
    public List<String> getAttributesToSync() {
        return attributesToSync;
    }

    @Override
    public String getSearchFilter() {
        return searchFilter;
    }

    @Override
    public String getSearchDeletedObjectsFilter() {
        return searchDeletedObjectsFilter;
    }
}
