package org.adsync4j.unboundid

import com.unboundid.ldap.sdk.Attribute
import org.adsync4j.UUIDUtils
import org.adsync4j.impl.ActiveDirectorySyncServiceImpl
import org.adsync4j.impl.DomainControllerAffiliationBean
import org.adsync4j.test.AbstractSystemSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(locations = '/org/adsync4j/impl/system-test-context.xml')
class UnboundIDLdapClientSystemSpec extends AbstractSystemSpec {

    @Autowired
    UnboundIDLdapClient ldapClient

    @Autowired
    DomainControllerAffiliationBean dca

    @Autowired
    ActiveDirectorySyncServiceImpl adService

    def setup() {
        assert testFixtures.search.filter, 'Missing property from test fixtures!'
        assert testFixtures.search.expectedDNs, 'Missing property from test fixtures!'
        assert testFixtures.dsServiceName, 'Missing property from test fixtures!'
        assert testFixtures.deleted, 'Missing property from test fixtures!'
    }

    def 'test_getRootDSEAttribute'() {
        expect:
        testFixtures.dsServiceName == ldapClient.getRootDSEAttribute('dsServiceName').value
    }

    def 'test_getEntryAttribute'() {
        def actualInvocationIDBytes = ldapClient.getEntryAttribute(testFixtures.dsServiceName, 'invocationID').valueByteArray
        def actualInvocationID = UUIDUtils.bytesToUUID(actualInvocationIDBytes)

        expect:
        actualInvocationID == dca.invocationId
    }

    def 'test_search'() {
        when:
        def result = ldapClient.search(dca.syncBaseDN, testFixtures.search.filter, ['distinguishedName']).collect()

        then:
        def actualDNs = result.collect { Attribute[] attributes -> attributes[0].value }
        actualDNs == testFixtures.search.expectedDNs
    }

    def 'test_searchDeleted'() {
        when:
        def result = ldapClient.searchDeleted(dca.rootDN, "lastKnownParent=$dca.syncBaseDN")

        then:
        def actualDeleted = result.collect { it.toString() }
        actualDeleted == testFixtures.deleted
    }
}
