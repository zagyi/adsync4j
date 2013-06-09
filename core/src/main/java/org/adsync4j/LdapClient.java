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
package org.adsync4j;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;

public interface LdapClient<T_ATTRIBUTE> {

    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final String SHOW_DELETED_CONTROL_OID = "1.2.840.113556.1.4.417";
    public static final String OBJECT_GUID = "objectGUID";

    @Nonnull
    T_ATTRIBUTE getRootDSEAttribute(String attributeName) throws LdapClientException;

    @Nonnull
    T_ATTRIBUTE getEntryAttribute(String entryDN, String attribute) throws LdapClientException;

    @Nonnull
    Iterable<T_ATTRIBUTE[]> search(String searchBaseDN, String filter, Collection<String> attributes) throws LdapClientException;

    @Nonnull
    Iterable<UUID> searchDeleted(String deletedObjectsContainer, String filter) throws LdapClientException;

    @Nonnull
    LdapAttributeResolver<T_ATTRIBUTE> getAttributeResolver();
}


