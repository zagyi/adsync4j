package org.adsync4j.unboundid

import org.adsync4j.api.LdapClientException
import org.adsync4j.spi.DCARepository
import org.adsync4j.spi.DomainControllerAffiliation
import spock.lang.Specification

class DefaultUnboundIDConnectionFactoryTest extends Specification {

    DCARepository dcaRepo = Mock(DCARepository)
    def dca = [getBindUser: { 'foo' }, getBindPassword: { 'bar' }]

    def 'should parse as LDAP URL'() {
        given:
        dca << [getUrl: { 'ldap://foo.bar:389' }]
        1 * dcaRepo.load('dcaKey') >> (dca as DomainControllerAffiliation)

        when:
        DefaultUnboundIDConnectionFactory f = new DefaultUnboundIDConnectionFactory('dcaKey', dcaRepo)
        f.loadDCA()

        then:
        f._host == 'foo.bar'
        f._port == 389
    }

    def 'should not parse ldaps url'() {
        given:
        dca << [getUrl: { 'ldaps://foo.bar:389' }]
        1 * dcaRepo.load('dcaKey') >> (dca as DomainControllerAffiliation)

        when:
        DefaultUnboundIDConnectionFactory f = new DefaultUnboundIDConnectionFactory('dcaKey', dcaRepo)
        f.loadDCA()

        then:
        thrown LdapClientException
    }

    def 'should not parse url with invalid port'() {
        given:
        dca << [getUrl: { 'ldap://foo.bar:65536' }]
        1 * dcaRepo.load('dcaKey') >> (dca as DomainControllerAffiliation)

        when:
        DefaultUnboundIDConnectionFactory f = new DefaultUnboundIDConnectionFactory('dcaKey', dcaRepo)
        f.loadDCA()

        then:
        thrown LdapClientException
    }

    def 'should use credentials from DCA'() {
        given:
        def dca = [
                getUrl: { 'ldap://foo.bar:1' },
                getBindUser: { 'foo' },
                getBindPassword: { 'bar' }
        ] as DomainControllerAffiliation
        1 * dcaRepo.load('dcaKey') >> (dca as DomainControllerAffiliation)

        when:
        DefaultUnboundIDConnectionFactory f = new DefaultUnboundIDConnectionFactory('dcaKey', dcaRepo)
        f.loadDCA()

        then:
        f._bindUser == 'foo'
        f._bindPassword == 'bar'
    }

    def 'should use credentials from ctor args'() {
        given:
        def dca = [
                getUrl: { 'ldap://foo.bar:1' },
                getBindUser: { 'foo' },
                getBindPassword: { 'bar' }
        ] as DomainControllerAffiliation
        1 * dcaRepo.load('dcaKey') >> (dca as DomainControllerAffiliation)

        when:
        DefaultUnboundIDConnectionFactory f = new DefaultUnboundIDConnectionFactory('dcaKey', dcaRepo, 'usr', 'pwd')
        f.loadDCA()

        then:
        f._bindUser == 'usr'
        f._bindPassword == 'pwd'
    }
}
