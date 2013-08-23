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
package org.adsync4j.impl

import org.adsync4j.DomainControllerAffiliation
import org.adsync4j.EntryProcessor
import org.adsync4j.LdapAttributeResolver
import org.adsync4j.LdapClient
import org.adsync4j.testutils.TestUtils
import spock.lang.Specification

import static org.adsync4j.impl.ActiveDirectorySyncServiceImpl.ActiveDirectoryAttribute.*
import static org.adsync4j.impl.ActiveDirectorySyncServiceImpl.and

class ActiveDirectorySyncServiceImplSpec extends Specification {

    LdapAttributeResolver<String> attributeResolver = [
            getAsString: { it as String },
            getAsLong: { Long.valueOf(it) },
            getAsByteArray: { TestUtils.uuidToBytArray(it) },
            getAsStringList: { [it] }
    ] as LdapAttributeResolver

    EntryProcessor entryProcessor = Mock(EntryProcessor)
    LdapClient<String> ldapClient = Mock(LdapClient)

    Spec spec = new Spec()

    def 'and() produces correct ldap expression'() {
        expect:
        and('a', 'b') == '(&(a)(b))'
        and('a', 'b', 'c') == '(&(a)(b)(c))'
    }

    def 'prevent incremental sync if no local InvocationId is stored'() {
        given:
        spec.with {
            localInvocationId = null
        }

        ActiveDirectorySyncServiceImpl service = buildService(spec)

        expect:
        !service.isIncrementalSyncPossible()
    }

    def 'prevent incremental sync if no local Highest Committed USN is stored'() {
        given:
        spec.with {
            localHighestCommittedUSN = null
        }

        ActiveDirectorySyncServiceImpl service = buildService(spec)

        expect:
        !service.isIncrementalSyncPossible()
    }

    def 'prevent incremental sync if local InvocationId does not match the remote one'() {
        given:
        spec.with {
            misMatchingLocalAndRemoteInvocationId()
        }

        ActiveDirectorySyncServiceImpl service = buildService(spec)

        when:
        def isIncrementalSyncPossible = service.isIncrementalSyncPossible()

        then:
        !isIncrementalSyncPossible

        interaction {
            invocationIdIsRetrieved()
        }
    }

    def 'allow incremental sync if local InvocationId matches the remote one'() {
        given:
        // local and remote invocationIds are the same by default in the spec
        ActiveDirectorySyncServiceImpl service = buildService(spec)

        when:
        def isIncrementalSyncPossible = service.isIncrementalSyncPossible()

        then:
        isIncrementalSyncPossible

        interaction {
            invocationIdIsRetrieved()
        }
    }

    def invocationIdIsRetrieved() {
        String dsServiceDn = 'dsServiceDN'
        1 * ldapClient.getRootDSEAttribute(DS_SERVICE_NAME.key()) >> dsServiceDn
        1 * ldapClient.getEntryAttribute(dsServiceDn, INVOCATION_ID.key()) >> spec.remoteInvocationId
    }

    def 'full synchronization'() {
        given:
        spec.with {
            numOfNewEntriesOnServer = 2
        }

        ActiveDirectorySyncServiceImpl service = buildService(spec)

        when:
        def newLocalHighestCommittedUSN = service.fullSync(entryProcessor)

        then: 'retrieve the remote highestCommittedUSN to include it in the search filter'
        1 * ldapClient.getRootDSEAttribute(HIGHEST_COMMITTED_USN.key()) >> spec.remoteHighestCommittedUSN

        then: 'invoke search with the appropriate filter'
        1 * ldapClient.search(spec.syncBaseDN, spec.fullSyncFilter, spec.attributesToSync) >> spec.searchResults

        then: 'submit entries to the entryProcessor'
        1 * entryProcessor.processNew(spec.searchResults[0])
        1 * entryProcessor.processNew(spec.searchResults[1])

        and: 'assert that the caller gets the new highest committed USN'
        spec.remoteHighestCommittedUSN == newLocalHighestCommittedUSN.toString()
    }

    def 'incremental synchronization'() {
        given:
        spec.with {
            uSNCreatedIsIncludedInSearchResults()
            numOfNewEntriesOnServer = 1
            numOfUpdatedEntriesOnServer = 1
            numOfDeletedEntriesOnServer = 2
        }

        ActiveDirectorySyncServiceImpl service = buildService(spec)

        when:
        def newLocalHighestCommittedUSN = service.incrementalSync(entryProcessor)

        then: 'retrieve the remote highestCommittedUSN to include it in the search filter'
        1 * ldapClient.getRootDSEAttribute(HIGHEST_COMMITTED_USN.key()) >> spec.remoteHighestCommittedUSN

        then: 'invoke search for new/updated entries using the appropriate filter and attribute list'
        1 * ldapClient.search(* _) >> { searchBaseDN, filter, attributes ->
            assert searchBaseDN == spec.syncBaseDN
            assert filter == spec.incrementalSyncFilter
            assert attributes as List == [USN_CREATED.key(), * spec.attributesToSync]
            spec.searchResults
        }

        and: 'submit mapped entries to be processed'
        1 * entryProcessor.processNew(spec.searchResultsWithOutUSNCreated[0] as List)
        1 * entryProcessor.processChanged(spec.searchResultsWithOutUSNCreated[1] as List)

        then: 'invoke search for deleted entries using the appropriate filter'
        1 * ldapClient.getEntryAttribute(spec.rootDN, WELL_KNOWN_OBJECTS.key()) >> spec.wellKnownObjectValues
        1 * ldapClient.searchDeleted(spec.deletedObjectsContainer, spec.filterForDeletedObjectsSearch) >> spec.idOfDeletedObjects

        then: 'submit the id of each deleted entry to be processed'
        1 * entryProcessor.processDeleted(spec.idOfDeletedObjects[0])
        1 * entryProcessor.processDeleted(spec.idOfDeletedObjects[1])

        and: 'assert that the caller gets the new highest committed USN'
        spec.remoteHighestCommittedUSN == newLocalHighestCommittedUSN.toString()
    }

    /**
     * Instantiates the test subject (sync service) using the given {@link Spec spec} parameter that specifies various
     * conditions under which the test is to be run.
     */
    ActiveDirectorySyncServiceImpl buildService(Spec spec) {
        // interaction common to all feature methods
        _ * ldapClient.getAttributeResolver() >> attributeResolver

        // the following lines ensure that any interaction with the below mentioned mocks
        // other than those explicitly specified in feature methods will be reported as errors
        0 * ldapClient._(* _)
        0 * entryProcessor._(* _)

        new ActiveDirectorySyncServiceImpl(ldapClient, spec)
    }

    /**
     * Helper class providing DSL-like constructs to make it possible to easily set up different conditions in a declarative
     * manner at the beginning of feature methods. Ensures the consistency between interrelated properties and test data.
     */
    static class Spec implements DomainControllerAffiliation {
        final static UUID COMMON_INVOCATION_ID = new UUID(0x1234567890abcdef, 0xfedcba098765432)

        UUID localInvocationId = COMMON_INVOCATION_ID
        UUID remoteInvocationId = COMMON_INVOCATION_ID
        Long localHighestCommittedUSN = 1111
        String remoteHighestCommittedUSN = 2222
        def wellKnownObjectValues
        def numOfNewEntriesOnServer = 0
        def numOfUpdatedEntriesOnServer = 0
        def numOfDeletedEntriesOnServer = 0
        def isUSNCreatedAttributeIncluded = false
        List searchResults = []
        String fullSyncFilter
        String incrementalSyncFilter
        String filterForDeletedObjectsSearch
        String deletedObjectsContainer
        List<UUID> idOfDeletedObjects

        // region mocked DomainControllerAffiliation properties
        List attributesToSync = ['foo', 'bar']
        String searchFilter = 'search=filter'
        String searchDeletedObjectsFilter = 'searchDeletedObjects=filter'
        String rootDN = 'rootDN'
        String syncBaseDN = 'syncBaseDN'
        String protocol = 'protocol'
        String host = 'host'
        int port = -1
        String bindUser = 'bindUser'
        String bindPassword = 'bindPassword'

        UUID getInvocationId() { localInvocationId }

        Long getHighestCommittedUSN() { localHighestCommittedUSN }
        // endregion

        enum EntryType {
            NEW, UPDATED
        }

        def Spec() {
            filterForDeletedObjectsSearch = "(&(${searchDeletedObjectsFilter})(uSNChanged>=1111)(uSNChanged<=2222))".toString()
            incrementalSyncFilter = "(&(${searchFilter})(uSNChanged>=1111)(uSNChanged<=2222))".toString()
            fullSyncFilter = "(&(${searchFilter})(uSNChanged<=2222))".toString()

            def deletedObjectsContainerId = new UUID(0x1111222233334444, 0x5555666677778888)
            wellKnownObjectValues = "foo:bar:${deletedObjectsContainerId}:CN=Deleted Objects,DC=foo,DC=bar".toString()
            deletedObjectsContainer = "<WKGUID=${deletedObjectsContainerId},$rootDN>".toString()
            idOfDeletedObjects = [
                    new UUID(0x1111111111111111, 0x1111111111111111),
                    new UUID(0x2222222222222222, 0x2222222222222222)]
        }

        def uSNCreatedIsIncludedInSearchResults() {
            isUSNCreatedAttributeIncluded = true
        }

        def misMatchingLocalAndRemoteInvocationId() {
            localInvocationId = new UUID(0, 0)
            remoteInvocationId = new UUID(-1, -1)
        }

        List getSearchResults() {
            def id = 0
            def results = []

            // Emulate having entries created before/after the last sync operation by setting their usnCreated attribute
            // less/greater than the localHighestCommittedUSN.
            numOfNewEntriesOnServer.times { entryCounter ->
                results << entryOnDemand(id++, EntryType.NEW)
            }
            numOfUpdatedEntriesOnServer.times { entryCounter ->
                results << entryOnDemand(id++, EntryType.UPDATED)
            }

            searchResults = results
        }

        /**
         *
         * @return The search result list as generated by the previous invocation of {@link Spec#getSearchResults()},
         * unless {@code isUSNCreatedAttributeIncluded} indicates that the {@code uSNCreated} attribute is included in it.
         * In that case a modified search result list is returned that omits the first attribute of each entry
         * (effectively removing the {@code uSNCreated} attribute).
         */
        def getSearchResultsWithOutUSNCreated() {
            isUSNCreatedAttributeIncluded ?
                searchResults.collect { it[1..-1] as List } :
                searchResults
        }

        /**
         * Creates an entry like:
         * <ul>
         * <li>['account#1.foo', 'account#1.bar'] or
         * <li>[{@code uSNCreated}, 'account#1.foo', 'account#1.bar']
         * </ul>
         * Depending on the value of {@code isUSNCreatedAttributeIncluded}.
         */
        def entryOnDemand(def entryId, EntryType entryType) {
            def entry = []
            if (isUSNCreatedAttributeIncluded) {
                entry << localHighestCommittedUSN + (entryType == EntryType.NEW ? 1 : -1)
            }
            attributesToSync.size().times { attributeCounter ->
                def attributeName = attributesToSync[attributeCounter]
                entry << "${entryType} entry #${entryId}/${attributeName}".toString()
            }
            entry as String[]
        }
    }
}
