package org.adsync4j.impl

import spock.lang.Specification

class UUIDUtilsTest extends Specification {

    def 'should decode fields as big endian'() {
        given:
        byte[] bytes = [
                0x33,0x22,0x11,0x00, // DWORD field in big-endian encoding
                0x55,0x44,           // WORD field in big-endian encoding
                0x77,0x66,           // WORD field in big-endian encoding
                0x88,0x99,0xaa,0xbb,0xcc,0xdd,0xee,0xff
        ]

        when:
        UUID uuid = UUIDUtils.bytesToUUID(bytes)

        then:
        uuid.toString() == '00112233-4455-6677-8899-aabbccddeeff'
    }
}
