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

import com.unboundid.ldap.sdk.*;

/**
 * Interface that declares an additional {@link PagingLdapConnection#search(SearchRequest, int) paging search} operation on top
 * of the available methods in {@link LDAPInterface}.
 */
public interface PagingLdapConnection extends LDAPInterface, PagingLdapSearcher {
}
