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
        URL testFixturesURL = getResourceURL(testFixturesName ?: DEFAULT_TEST_FIXTURES)
        testFixtures = new ConfigSlurper().parse(testFixturesURL)
    }
}
