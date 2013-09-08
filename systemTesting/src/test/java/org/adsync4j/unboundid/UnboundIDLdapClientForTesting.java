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

public class UnboundIDLdapClientForTesting extends UnboundIDLdapClient {

    private final static String DELETED_OBJECTS_CONTAINER_RDN = "CN=Deleted Objects,";

    public UnboundIDLdapClientForTesting(PagingUnboundIDConnectionFactory connectionFactory) throws LdapClientException {
        super(connectionFactory);
    }

    /**
     * The implementation in the super-class passes the root DN as search base to Active Directory which will automatically
     * look up the Deleted Objects Container when it encounters the {@link org.adsync4j.LdapClient#SHOW_DELETED_CONTROL_OID
     * show deleted} request control. Since the in-memory AD mock server doesn't implement this logic, we must compensate for
     * it on the client side by specifying the deleted objects container as the base of the search operation.
     */
    @Nonnull
    @Override
    public Iterable<UUID> searchDeleted(String rootDN, String filter) throws LdapClientException {
        return super.searchDeleted(DELETED_OBJECTS_CONTAINER_RDN + rootDN, filter);
    }
}
