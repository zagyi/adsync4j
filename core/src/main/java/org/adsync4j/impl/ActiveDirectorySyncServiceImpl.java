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
 *******************************************************************************/
package org.adsync4j.impl;

import org.adsync4j.api.ActiveDirectorySyncService;
import org.adsync4j.api.InitialFullSyncRequiredException;
import org.adsync4j.api.InvocationIdMismatchException;
import org.adsync4j.api.LdapClientException;
import org.adsync4j.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.AbstractList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.adsync4j.impl.ActiveDirectorySyncServiceImpl.ActiveDirectoryAttribute.*;
import static org.adsync4j.impl.UUIDUtils.bytesToUUID;

/**
 * Implementation of the main service interface of ADSync4J.
 * <p/>
 * In order to start synchronizing content, clients need to create a {@link org.adsync4j.spi.DomainControllerAffiliation} (DCA)
 * for every domain controller they want to synchronize with, and each DCA requires a dedicated {@link
 * ActiveDirectorySyncServiceImpl} instance. When creating a service instance, the DCA is not passed directly,
 * but specified indirectly through a key and a repository of DCAs. This indirection relieves clients of the
 * responsibility to update the highest committed Update Sequence Number in the DCA and to persist the updated record,
 * which is crucial for the correct functioning of synchronization operations.
 * <p/>
 * This class defines a number of type parameters which are required, so that clients can:
 * <ul>
 * <li>provide their own implementation of the {@link org.adsync4j.spi.DomainControllerAffiliation} interface (e.g. a JPA
 * entity),</li>
 * <li>provide their own implementation of the {@link org.adsync4j.spi.DCARepository} interface (e.g. a JPA repository),</li>
 * <li>freely choose an arbitrary key type for the repository,</li>
 * <li>pick an LDAP SDK to use for implementing the {@link org.adsync4j.spi.LdapClient} interface.</li>
 * </ul>
 * <p/>
 * <b>Important!</b>
 * The provided {@link DCARepository} implementation must persist DCAs in the same physical database that stores the
 * synchronized entries. This is necessary in order to ensure the consistency between the DCA and the synchronized data even if
 * the database fails and has to be restored from a backup. Failing to do so will result in the need for a full
 * re-synchronization after the database is restored.
 * <p/>
 * This class is NOT thread-safe.
 *
 * @param <DCA_KEY>        Type of the key used in the provided DCA repository.
 * @param <DCA_IMPL>       The implementation class of {@link org.adsync4j.spi.DomainControllerAffiliation}, the instances
 *                         of which are stored in the provided DCA repository.
 * @param <LDAP_ATTRIBUTE> The LDAP attribute type (determined by the {@link org.adsync4j.spi.LdapClient} implementation in use).
 */
@NotThreadSafe
public class ActiveDirectorySyncServiceImpl<DCA_KEY, DCA_IMPL extends DomainControllerAffiliation, LDAP_ATTRIBUTE>
        implements ActiveDirectorySyncService<LDAP_ATTRIBUTE> {

    private final static Logger LOG = LoggerFactory.getLogger(ActiveDirectorySyncServiceImpl.class);

    protected final DCA_KEY _dcaKey;
    protected final DCARepository<DCA_KEY, DCA_IMPL> _affiliationRepository;
    protected final LdapClient<LDAP_ATTRIBUTE> _ldapClient;
    protected final LdapAttributeResolver<LDAP_ATTRIBUTE> _attributeResolver;

    protected DCA_IMPL _dcAffiliation;

    /**
     * Internal interface with two implementations encapsulating the logic of the full and incremental synchronization
     * operations. Not to be directly used by clients.
     *
     * @param <LDAP_ATTRIBUTE>
     */
    protected interface SyncOperation<LDAP_ATTRIBUTE> {
        void execute(long remoteHighestCommittedUSN, EntryProcessor<LDAP_ATTRIBUTE> entryProcessor);
    }

    /**
     * Internal enum listing a number of attribute names defined in Active Directory that the synchronization service uses.
     */
    protected enum ActiveDirectoryAttribute {
        /**
         * Attribute contained in a directory entry that is pointed to by the 'dsServiceName' attribute of the root DSE. It's
         * value is basically the Domain Controller's identifier.
         *
         * @see DomainControllerAffiliation#getInvocationId()
         */
        INVOCATION_ID("invocationID"),

        /**
         * An attribute of the root DSE the value of which is the distinguished name of the DS Service entry. That entry
         * contains (among other attributes) the Domain Controller's Invocation ID.
         *
         * @see ActiveDirectoryAttribute#INVOCATION_ID
         */
        DS_SERVICE_NAME("dsServiceName"),

        /**
         * Attribute of the root DSE storing the highest Update Sequence Number committed locally by the domain controller.
         */
        HIGHEST_COMMITTED_USN("highestCommittedUSN"),

        /**
         * Attribute maintained in every entry in Active Directory recording the Sequence Number of the transaction that most
         * recently changed the entry.
         */
        USN_CHANGED("uSNChanged"),

        /**
         * Attribute stored in every entry in Active Directory recording the Sequence Number of the transaction that created the
         * entry.
         */
        USN_CREATED("uSNCreated");

        private final String _name;

        private ActiveDirectoryAttribute(String name) {
            _name = name;
        }

        /**
         * @return The attribute name as used in the Active Directory schema.
         */
        public String key() {
            return _name;
        }

        public String toString() {
            return _name;
        }
    }

    /**
     * Constructs a synchronization service instance that is dedicated to work with the specific
     * {@link DomainControllerAffiliation} record loaded from the provided {@link DCARepository} using the given {@code dcaKey}.
     * This indirection makes it possible to relieve clients of the responsibility to update the highest committed Update
     * Sequence Number in the DCA and to persist the updated record, which is crucial for the correct functioning of
     * synchronization operations.
     * <p/>
     * <b>Important!</b>
     * The provided {@link DCARepository} implementation must persist DCAs in the same physical database that stores the
     * synchronized entries. This is necessary in order to ensure the consistency between the DCA and the synchronized data
     * even if the database fails and has to be restored from a backup. Failing to do so will result in the need for a full
     * re-synchronization after the database is restored.
     *
     * @param dcaKey                Key of the {@link DomainControllerAffiliation} based on which this service instance
     *                              has to perform the synchronization operations. The passed {@code affiliationRepository} must
     *                              contain a DCA assigned to this key.
     * @param affiliationRepository Repository managing {@link DomainControllerAffiliation} entities.
     * @param ldapClient            {@link LdapClient} used by the service to communicate with Active Directory.
     */
    public ActiveDirectorySyncServiceImpl(
            DCA_KEY dcaKey,
            DCARepository<DCA_KEY, DCA_IMPL> affiliationRepository,
            LdapClient<LDAP_ATTRIBUTE> ldapClient)
    {
        _dcaKey = dcaKey;
        _affiliationRepository = affiliationRepository;
        _ldapClient = ldapClient;
        _attributeResolver = _ldapClient.getAttributeResolver();
    }

    /**
     * Performs a full synchronization that retrieves all entries currently found in the synchronization scope in Active
     * Directory. Entries are delivered one-by-one to the caller by iteratively invoking {@link EntryProcessor#processNew
     * processNew()} on the provided {@link EntryProcessor}.
     * <p/>
     * It ensures that the {@link DomainControllerAffiliation} this service instance uses is {@link DCARepository#save saved}
     * after updating the current highest committed Update Sequence Number and the Invocation ID it contains.
     *
     * @param entryProcessor {@link EntryProcessor} implementation provided by the caller in order to receive
     *                       the synchronized entries.
     * @return The current highest committed Update Sequence Number on the server side that represents the point of time from
     *         which the next incremental synchronization will have to retrieve changes from Active Directory.
     * @throws LdapClientException in case a problem is encountered during communication with Active Directory.
     */
    @Override
    public long fullSync(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor) {
        return doSync(entryProcessor, new SyncOperation<LDAP_ATTRIBUTE>() {
            @Override
            public void execute(long remoteHighestCommittedUSN, EntryProcessor<LDAP_ATTRIBUTE> entryProcessor) {
                String filter = getFilterWithUpperBoundUSN(_dcAffiliation.getSearchFilter(), remoteHighestCommittedUSN);

                Iterable<LDAP_ATTRIBUTE[]> searchResult = _ldapClient.search(
                        _dcAffiliation.getSyncBaseDN(), filter, _dcAffiliation.getAttributesToSync());

                for (LDAP_ATTRIBUTE[] entryAttributes : searchResult) {
                    entryProcessor.processNew(asList(entryAttributes));
                }

                _dcAffiliation.setInvocationId(retrieveInvocationId());
            }
        });
    }

    /**
     * Performs an incremental synchronization that only retrieves the entries created/changed/deleted after the point of time
     * represented by the highest committed Update Sequence Number that has been recorded by the last synchronization. Entries
     * are delivered one-by-one to the caller by iteratively invoking the corresponding methods of the provided
     * {@link EntryProcessor}.
     * <p/>
     * It ensures that the {@link DomainControllerAffiliation} this service instance uses is {@link DCARepository#save saved}
     * after updating the current highest committed Update Sequence Number and the Invocation ID it contains.
     *
     * @param entryProcessor {@link EntryProcessor} implementation provided by the caller in order to receive the synchronized
     *                       entries.
     * @return The current highest committed Update Sequence Number on the server side that represents the point of time from
     *         which the next incremental synchronization will have to retrieve changes from Active Directory.
     * @throws LdapClientException in case a problem is encountered during communication with Active Directory.
     */
    @Override
    public long incrementalSync(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor) {
        return doSync(entryProcessor, new SyncOperation<LDAP_ATTRIBUTE>() {
            @Override
            public void execute(long remoteHighestCommittedUSN, EntryProcessor<LDAP_ATTRIBUTE> entryProcessor) {
                assertIncrementalSyncIsPossible();
                queryChangedAndNewEntries(entryProcessor, remoteHighestCommittedUSN);
                queryDeletedEntries(entryProcessor, remoteHighestCommittedUSN);
            }
        });
    }

    /**
     * Template method that implements a common frame of logic which has to be executed regardless of the specific sync
     * operation (full or incremental) actually being performed.
     * <p/>
     * This template makes sure that the highest committed USN is always retrieved from the server as the first step,
     * and it's always {@link DomainControllerAffiliation#setHighestCommittedUSN set on the DCA} (which also gets {@link
     * DCARepository#save persisted}) as the last step.
     *
     * @param entryProcessor Call-back object implemented by the client.
     * @param syncOperation  Function object encapsulating the behavior of the specific sync operation to be performed.
     * @return The highest committed USN retrieved from the server at the beginning of the method.
     */
    private long doSync(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor, SyncOperation<LDAP_ATTRIBUTE> syncOperation) {
        reloadAffiliation();
        long remoteHighestCommittedUSN = retrieveRemoteHighestCommittedUSN();

        // delegate to the specific sync operation
        try {
            syncOperation.execute(remoteHighestCommittedUSN, entryProcessor);
        } finally {
            _ldapClient.closeConnection();
        }

        _dcAffiliation.setHighestCommittedUSN(remoteHighestCommittedUSN);
        _dcAffiliation = _affiliationRepository.save(_dcAffiliation);
        LOG.debug("Updated Domain Controller Affiliation record: {}", _dcAffiliation);

        return remoteHighestCommittedUSN;
    }

    void reloadAffiliation() {
        _dcAffiliation = _affiliationRepository.load(_dcaKey);
        LOG.debug("Loaded Domain Controller Affiliation record: {}", _dcAffiliation);
        if (_dcAffiliation == null) {
            throw new IllegalArgumentException(
                    "The specified Domain Controller Affiliation record is not found in the repository. Requested key was: " +
                    _dcaKey);
        }
    }

    /**
     * Helper method that checks if properties of the DCA that this service instance holds enable an incremental sync.<br>
     * Specifically it checks if
     * <ul>
     * <li>the Invocation ID stored in the DCA matches the ID retrieved from the server</li>
     * <li>the DCA contains an Update Sequence Number starting from which the incremental synchronization needs to
     * retrieve changes</li>
     * </ul>
     */
    void assertIncrementalSyncIsPossible() {
        UUID expectedInvocationId = _dcAffiliation.getInvocationId();
        Long lastSeenHighestCommittedUSN = _dcAffiliation.getHighestCommittedUSN();

        boolean isAnyAffiliationDetailMissing = expectedInvocationId == null || lastSeenHighestCommittedUSN == null;

        if (isAnyAffiliationDetailMissing) {
            throw new InitialFullSyncRequiredException();
        }

        UUID actualInvocationId = retrieveInvocationId();
        if (!actualInvocationId.equals(expectedInvocationId)) {
            throw new InvocationIdMismatchException(expectedInvocationId, actualInvocationId);
        }
    }

    /**
     * Performs an LDAP search that retrieves the list of entries that have been changed or created since the last
     * synchronization, and iteratively invokes the appropriate method of the provided call-back object with these entries.
     * <p/>
     * It includes the {@link ActiveDirectoryAttribute#USN_CREATED USN_CREATED} attribute in the search request,
     * so that changed and newly created entries can be distinguished.
     *
     * @param entryProcessor Call-back object implemented by the client.
     * @param upperBoundUSN  The USN read at the start of synchronization. Marks the point of time until which changed/new
     *                       entries should be retrieved.
     */
    protected void queryChangedAndNewEntries(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor, long upperBoundUSN) {
        String filter = getFilterWithLowerAndUpperBoundUSN(_dcAffiliation.getSearchFilter(), upperBoundUSN);

        List<String> attributes = new OnePlusListList<>(USN_CREATED.key(), _dcAffiliation.getAttributesToSync());
        Iterable<LDAP_ATTRIBUTE[]> searchResult = _ldapClient.search(_dcAffiliation.getSyncBaseDN(), filter, attributes);

        for (LDAP_ATTRIBUTE[] entry : searchResult) {
            feedEntryProcessor(entryProcessor, entry);
        }
    }

    /**
     * A list that is composed of one single element followed by a list of other elements.
     */
    private static class OnePlusListList<T> extends AbstractList<T> {
        private final T _one;
        private final List<T> _list;

        private OnePlusListList(T one, java.util.List<T> list) {
            _one = one;
            _list = list;
        }

        @Override
        public T get(int index) { return index == 0 ? _one : _list.get(index - 1); }

        @Override
        public int size() { return _list.size() + 1; }
    }

    /**
     * Helper method that dispatches the entry either to the {@link EntryProcessor#processNew processNew()} or to the {@link
     * EntryProcessor#processChanged processChanged()} call-back method of the provided {@link EntryProcessor}.
     * <p/>
     * It is expected that the provided attribute array contains the {@link ActiveDirectoryAttribute#USN_CREATED USN_CREATED}
     * attribute in its first position based on which this method decides if the entry is new or changed.
     * <p/>
     * The dispatched entry will not contain this first attribute, as it's not useful for the client.
     *
     * @param entryProcessor Call-back object implemented by the client.
     * @param entry          Attribute array representing the entry.
     */
    private void feedEntryProcessor(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor, LDAP_ATTRIBUTE[] entry) {
        List<LDAP_ATTRIBUTE> entryWithoutUsnCreatedAttribute = asList(entry).subList(1, entry.length);
        if (isNewEntry(entry)) {
            entryProcessor.processNew(entryWithoutUsnCreatedAttribute);
        } else {
            entryProcessor.processChanged(entryWithoutUsnCreatedAttribute);
        }
    }

    /**
     * Helper method that tells if the passed entry is new by comparing its first attribute (that is expected to be the {@link
     * ActiveDirectoryAttribute#USN_CREATED USN_CREATED} attribute) with the highest committed USN recorded in the DCA.
     *
     * @param entry Attribute array representing the entry.
     * @return True if the entry is new, or false otherwise.
     */
    private boolean isNewEntry(LDAP_ATTRIBUTE[] entry) {
        LDAP_ATTRIBUTE usnCreatedAttribute = entry[0];
        Long usnCreated = _attributeResolver.getAsLong(usnCreatedAttribute);
        return
                usnCreated != null &&
                usnCreated > _dcAffiliation.getHighestCommittedUSN();
    }

    /**
     * Performs an LDAP search that retrieves the ID of every entry that has been deleted since the last synchronization,
     * and iteratively invokes the {@link EntryProcessor#processDeleted processDeleted()} method of the provided call-back
     * object passing these IDs.
     *
     * @param entryProcessor Call-back object implemented by the client.
     * @param upperBoundUSN  The USN read at the start of synchronization. Marks the point of time until which deleted
     *                       entries should be retrieved.
     */
    protected void queryDeletedEntries(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor, long upperBoundUSN) {
        String filter = getFilterWithLowerAndUpperBoundUSN(_dcAffiliation.getSearchDeletedObjectsFilter(), upperBoundUSN);

        Iterable<UUID> deletedObjectIds = _ldapClient.searchDeleted(_dcAffiliation.getRootDN(), filter);

        for (UUID uuid : deletedObjectIds) {
            entryProcessor.processDeleted(uuid);
        }
    }

    /**
     * Retrieves the Invocation ID from Active Directory.
     *
     * @return The current Invocation ID identifying the affiliated domain controller.
     */
    protected UUID retrieveInvocationId() {
        LDAP_ATTRIBUTE dsServiceDNAttribute = _ldapClient.getRootDSEAttribute(DS_SERVICE_NAME.key());
        String dsServiceDN = _attributeResolver.getAsString(dsServiceDNAttribute);

        LdapClientException.throwIfNull(dsServiceDN,
                "Invalid %s attribute encountered: %s", DS_SERVICE_NAME.key(), String.valueOf(dsServiceDNAttribute));

        LDAP_ATTRIBUTE invocationIdAttribute = _ldapClient.getEntryAttribute(dsServiceDN, INVOCATION_ID.key());

        UUID invocationId = bytesToUUID(_attributeResolver.getAsByteArray(invocationIdAttribute));
        LdapClientException.throwIfNull(invocationId,
                "Invalid Update Sequence Number encountered: %s.", String.valueOf(invocationIdAttribute));

        return invocationId;
    }

    /**
     * Retrieves the current highest Update Sequence Number that has been committed up to this point in the database of Active
     * Directory.
     *
     * @return The current highest committed Update Sequence Number.
     */
    protected long retrieveRemoteHighestCommittedUSN() {
        LDAP_ATTRIBUTE hcusnAttribute = _ldapClient.getRootDSEAttribute(HIGHEST_COMMITTED_USN.key());
        Long hcusn = _attributeResolver.getAsLong(hcusnAttribute);
        LdapClientException.throwIfNull(hcusn,
                "Invalid Update Sequence Number encountered: %s.", String.valueOf(hcusnAttribute));
//      noinspection ConstantConditions
        return hcusn;
    }

    /**
     * Combines the provided LDAP filter expression with a lower and upper limit on the {@link
     * ActiveDirectoryAttribute#USN_CHANGED USN_CHANGED} attribute. The highest committed USN stored in the DCA becomes the lower
     * limit, while the upper limit is the second argument.
     * <p/>
     * Called when compiling the filter for an incremental synchronization.
     *
     * @param filter        The LDAP filter expression to complete.
     * @param upperBoundUSN Value for the upper limit of the {@link ActiveDirectoryAttribute#USN_CHANGED USN_CHANGED} attribute.
     * @return The provided LDAP filter combined with lower and upper bounds on the {@link ActiveDirectoryAttribute#USN_CHANGED
     *         USN_CHANGED} attribute.
     */
    protected String getFilterWithLowerAndUpperBoundUSN(String filter, long upperBoundUSN) {
        long lowerBoundUSN = _dcAffiliation.getHighestCommittedUSN();
        String lowerBoundUSNFilter = USN_CHANGED + ">=" + lowerBoundUSN;
        String upperBoundUSNFilter = USN_CHANGED + "<=" + upperBoundUSN;
        return and(filter, lowerBoundUSNFilter, upperBoundUSNFilter);
    }

    /**
     * Combines the provided LDAP filter expression with an upper limit on the {@link ActiveDirectoryAttribute#USN_CHANGED
     * USN_CHANGED} attribute.
     * <p/>
     * Called when compiling the filter for a full synchronization.
     *
     * @param filter        The LDAP filter expression to complete.
     * @param upperBoundUSN Value for the upper limit of the {@link ActiveDirectoryAttribute#USN_CHANGED USN_CHANGED} attribute.
     * @return The provided LDAP filter combined with an upper bound on the {@link ActiveDirectoryAttribute#USN_CHANGED
     *         USN_CHANGED} attribute.
     */
    protected String getFilterWithUpperBoundUSN(String filter, long upperBoundUSN) {
        String usnUpperBoundFilter = USN_CHANGED + "<=" + upperBoundUSN;
        return and(filter, usnUpperBoundFilter);
    }

    /**
     * Combines the provided LDAP filter expressions into one single expression using the logical AND operator.
     *
     * @return A new LDAP filter expression that combines all input filters with the logical AND operator.
     */
    protected static String and(String... filters) {
        StringBuilder result = new StringBuilder("(&");
        for (String filter : filters) {
            result.append(ensureWrappedInParenthesis(filter));
        }
        return result.append(')').toString();
    }

    /**
     * Wraps the provided filter expression in parenthesis, unless it's already wrapped.
     *
     * @param filter An LDAP filter expression.
     * @return A {@link StringBuilder} containing the input filter wrapped in parenthesis if it was not wrapped before,
     *         otherwise the original filter expression is returned.
     */
    private static StringBuilder ensureWrappedInParenthesis(String filter) {
        StringBuilder result = new StringBuilder(filter.length() + 2);
        if (filter.startsWith("(")) {
            return result.append(filter);
        } else {
            return result
                    .append('(')
                    .append(filter)
                    .append(')');
        }
    }
}
