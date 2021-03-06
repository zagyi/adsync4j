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

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResultEntry;

/**
 * Interface that adds a paged search method to {@link UnboundIDLdapConnection}.
 */
public interface PagingLdapConnection extends UnboundIDLdapConnection {

    /**
     * Processes the provided search request making sure that results are retrieved in pages from the LDAP server behind the
     * scenes. The return type cannot be a more specific collection type, because the total number of entries matching the
     * search request is not known in advance.
     *
     * @param searchRequest The search request to be processed.
     * @param pageSize      Number of search result entries the server is allowed to return in a single page.
     * @return An {@link Iterable} through which callers can iterate over the <b>entire</b> result set (as opposed to entries
     *         of a single page) without having to be concerned about the fact that entries are fetched by pages behind the
     *         scenes.
     * @throws com.unboundid.ldap.sdk.LDAPException
     *
     */
    Iterable<SearchResultEntry> search(SearchRequest searchRequest, int pageSize) throws LDAPException;
}
