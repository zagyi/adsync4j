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
package org.adsync4j.spi;

import java.util.List;
import java.util.UUID;

/**
 * A call-back interface through which clients of ADSync4J can obtain the new/changed/deleted LDAP entries during
 * synchronization.
 * <p/>
 * In case of a full synchronization, all entries are reported as new.
 *
 * @param <LDAP_ATTRIBUTE> The LDAP attribute type determined by the {@link LdapClient} implementation in use.
 */
public interface EntryProcessor<LDAP_ATTRIBUTE> {

    /**
     * Call-back method invoked during a full or incremental synchronization.
     *
     * @param entry The list of attributes of a new entry. It's guaranteed that this list contains the same number of
     *              attribute values in the same order as it's determined by the {@link
     *              DomainControllerAffiliation#getAttributesToSync() attributesToSync} property of the affiliation record
     *              which was used for synchronization. Note that some of the values might be {@code null} in case the
     *              corresponding attribute is not present on the entry.
     */
    void processNew(List<LDAP_ATTRIBUTE> entry);

    /**
     * Call-back method invoked during an incremental synchronization.
     *
     * @param entry The list of attributes of a changed entry. It's guaranteed that this list contains the same number of
     *              attribute values in the same order as it's determined by the {@link
     *              DomainControllerAffiliation#getAttributesToSync() attributesToSync} property of the affiliation record
     *              which was used for synchronization. Note that some of the values might be {@code null} in case the
     *              corresponding attribute is not present on the entry.
     */
    void processChanged(List<LDAP_ATTRIBUTE> entry);

    /**
     * Call-back method invoked during an incremental synchronization.
     *
     * @param entryId The GUID of an entry that has been deleted since the last synchronization.
     */
    void processDeleted(UUID entryId);
}
