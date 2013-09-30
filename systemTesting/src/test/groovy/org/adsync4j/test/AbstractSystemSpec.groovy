package org.adsync4j.test

import org.springframework.beans.factory.annotation.Value
import spock.lang.Specification

import static org.adsync4j.testutils.TestUtils.getResourceURL

abstract class AbstractSystemSpec extends Specification {

    static def DEFAULT_TEST_FIXTURES = '/org/adsync4j/impl/data/testFixtures.groovycfg'

    @Value('${testFixtures:}')
    String testFixturesName

    def testFixtures

    def setup() {
        waitIfNecessary()

        URL testFixturesURL = getResourceURL(testFixturesName ?: DEFAULT_TEST_FIXTURES)
        testFixtures = new ConfigSlurper().parse(testFixturesURL)

//        Uncomment if you need debug log messages related to LDAP communication.
//        Debug.setIncludeStackTrace(true);
//        Debug.setEnabled(true);
    }

    /**
     * Scandal!!! See comment on {@link org.adsync4j.unboundid.PagingLdapConnectionTestImpl} for details.
     */
    def waitIfNecessary() {
        if (System.properties.getProperty('delayTests') != null) {
            Thread.sleep(1000)
        }
    }
}
