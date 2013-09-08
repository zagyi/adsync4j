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
package org.adsync4j.impl
import com.unboundid.ldap.sdk.Attribute
import org.adsync4j.EntryProcessor
import org.adsync4j.test.AbstractSystemSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(locations = '/org/adsync4j/impl/system-test-context.xml')
class ActiveDirectorySyncServiceImplSystemSpec extends AbstractSystemSpec {

    @Autowired
    ActiveDirectorySyncServiceImpl adService

    static Set EXISTING_USERS
    static Set CHANGED_USERS
    static Set INSERTED_USERS
    static Set DELETED_USER_IDS
    static Set ALL_USERS

    static final long LAST_KNOWN_HCUSN = 21303
    Set actualNewUsers = [] as Set
    Set actualChangedUsers = [] as Set
    Set actualDeletedUsers = [] as Set

    @Autowired
    ActiveDirectorySyncServiceImpl adService


    EntryProcessor entryProcessor = [
            processNew: { actualNewUsers << attributesToString(it) },
            processChanged: { actualChangedUsers << attributesToString(it) },
            processDeleted: { actualDeletedUsers << it },
    ] as EntryProcessor

    static def attributesToString(List<Attribute> entry) {
        entry.collect { attribute -> attribute?.value }.join('|')
    }

    public def setup() {
        EXISTING_USERS = testFixtures.existing.collect { it.join('|') } as Set
        CHANGED_USERS = testFixtures.changed.collect { it.join('|') } as Set
        INSERTED_USERS = testFixtures.inserted.collect { it.join('|') } as Set
        DELETED_USER_IDS = testFixtures.deleted.collect { UUID.fromString(it) } as Set
        ALL_USERS = EXISTING_USERS + CHANGED_USERS + INSERTED_USERS
    }

    def 'should allow to do incremental sync'() {
        given:
        adService.reloadAffiliation()

        expect:
        adService.assertIncrementalSyncIsPossible()
    }

    def 'testing incremental sync'() {
        when:
        def newHCUSN = adService.incrementalSync(entryProcessor)

        then:
        newHCUSN >= LAST_KNOWN_HCUSN
        actualNewUsers == INSERTED_USERS
        actualChangedUsers == CHANGED_USERS
        actualDeletedUsers == DELETED_USER_IDS
    }

    def 'testing full sync'() {
        when:
        def newHCUSN = adService.fullSync(entryProcessor)

        then:
        newHCUSN >= LAST_KNOWN_HCUSN
        actualNewUsers == ALL_USERS
        actualChangedUsers.isEmpty()
        actualDeletedUsers.isEmpty()
    }
}
