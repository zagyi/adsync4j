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
 ***************************************************************************** */
package org.adsync4j.unboundid;

import org.adsync4j.LdapClientException;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnboundIDLdapClientForTesting extends UnboundIDLdapClient {

    private final static Pattern DELETED_OBJECTS_CONTAINER_PATTERN = Pattern.compile("<WKGUID=[^,]*(.*)>");
    private final static String DELETED_OBJECTS_CONTAINER_RDN = "CN=Deleted Objects";

    public UnboundIDLdapClientForTesting(PagingUnboundIDConnectionFactory connectionFactory) throws LdapClientException {
        super(connectionFactory);
    }

    @Nonnull
    @Override
    public Iterable<UUID> searchDeleted(String deletedObjectsContainer, String filter) throws LdapClientException {
        String normalDeletedObjectsContainerDN = translateADSpecificContainerDN(deletedObjectsContainer);
        return super.searchDeleted(normalDeletedObjectsContainerDN, filter);
    }

    private String translateADSpecificContainerDN(String deletedObjectsContainer) {
        Matcher matcher = DELETED_OBJECTS_CONTAINER_PATTERN.matcher(deletedObjectsContainer);
        if (matcher.matches()) {
            String rootDN = matcher.group(1);
            return DELETED_OBJECTS_CONTAINER_RDN + rootDN;
        } else {
            throw new AssertionError();
        }
    }
}
