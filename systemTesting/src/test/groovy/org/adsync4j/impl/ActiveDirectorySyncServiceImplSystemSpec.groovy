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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(locations = 'system-test-context.xml')
class ActiveDirectorySyncServiceImplSystemSpec extends Specification {

    @Autowired
    ActiveDirectorySyncServiceImpl adService

    @Value('${expectedEntriesFile:data/expectedEntries.groovycfg}')
    String expectedEntriesFile

    static Set EXISTING_USERS
    static Set CHANGED_USERS
    static Set INSERTED_USERS
    static Set DELETED_USER_IDS
    static Set ALL_USERS

    static final long LAST_KNOWN_HCUSN = 21303

    EntryProcessor entryProcessor = [
            processNew: { actualNewUsers << attributesToString(it) },
            processChanged: { actualChangedUsers << attributesToString(it) },
            processDeleted: { actualDeletedUsers << it },
    ] as EntryProcessor

    Set actualNewUsers = [] as Set
    Set actualChangedUsers = [] as Set
    Set actualDeletedUsers = [] as Set

    def setup() {
        URL resource = getResource()

        assert resource, "could not resolve $expectedEntriesFile as classpath resource or URL"

        def script = new ConfigSlurper().parse(resource)
        EXISTING_USERS = script.existing.collect { attributesToString(it) } as Set
        CHANGED_USERS = script.changed.collect { attributesToString(it) } as Set
        INSERTED_USERS = script.inserted.collect { attributesToString(it) } as Set
        DELETED_USER_IDS = script.deleted.collect { UUID.fromString(it) } as Set
        ALL_USERS = EXISTING_USERS + CHANGED_USERS + INSERTED_USERS
    }

    private URL getResource() {
        def resource = ActiveDirectorySyncServiceImplSystemSpec.getResource(expectedEntriesFile)
        if (!resource) {
            try {
                resource = new URL(expectedEntriesFile)
            } catch (MalformedURLException ignored) {
            }
        }
        resource
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

    static def attributesToString(List<Attribute> entry) {
        entry.collect { attribute -> attribute?.value }.join('|')
    }
}
