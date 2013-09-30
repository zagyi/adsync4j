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
package org.adsync4j.api;

import org.adsync4j.spi.EntryProcessor;

/**
 * Main service interface of ADSync4J.
 * <p/>
 * Implementations have to consult a {@link org.adsync4j.spi.DomainControllerAffiliation DomainControllerAffiliation} record in
 * order to obtain details on the source and the scope of the synchronization operations. Implementations must also make sure
 * upon completion of the synchronization operation that the used {@link org.adsync4j.spi.DomainControllerAffiliation
 * DomainControllerAffiliation} record is updated with the highest committed Update Sequence Number and the Invocation ID
 * of the domain controller.
 *
 * @param <LDAP_ATTRIBUTE> An LDAP SDK specific attribute type determined by the {@link org.adsync4j.spi.LdapClient LdapClient}
 *                         implementation in use. For more on this, refer to the documentation of {@link
 *                         org.adsync4j.spi.LdapClient LdapClient} and {@link org.adsync4j.spi.LdapAttributeResolver
 *                         LdapAttributeResolver}.
 */
public interface ActiveDirectorySyncService<LDAP_ATTRIBUTE> {

    /**
     * Performs a full synchronization that retrieves all entries currently found in the synchronization scope in Active
     * Directory. Entries are delivered one-by-one to the caller by iteratively invoking {@link EntryProcessor#processNew
     * processNew()} on the provided {@link EntryProcessor}.
     *
     * @param entryProcessor {@link EntryProcessor} implementation provided by the caller in order to receive
     *                       the synchronized entries.
     * @return The current highest committed Update Sequence Number on the server side that represents the point of time from
     *         which the next incremental synchronization will have to retrieve changes from Active Directory.
     * @throws LdapClientException in case a problem is encountered during communication with Active Directory.
     */
    long fullSync(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor) throws LdapClientException;


    /**
     * Performs an incremental synchronization that only retrieves the entries created/changed/deleted after the point of time
     * represented by the highest committed Update Sequence Number that has been recorded by the last synchronization. Entries
     * are delivered one-by-one to the caller by iteratively invoking the corresponding methods of the provided
     * {@link EntryProcessor}.
     *
     * @param entryProcessor {@link EntryProcessor} implementation provided by the caller in order to receive the synchronized
     *                       entries.
     * @return The current highest committed Update Sequence Number on the server side that represents the point of time from
     *         which the next incremental synchronization will have to retrieve changes from Active Directory.
     * @throws LdapClientException in case a problem is encountered during communication with Active Directory.
     */
    long incrementalSync(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor) throws LdapClientException;
}