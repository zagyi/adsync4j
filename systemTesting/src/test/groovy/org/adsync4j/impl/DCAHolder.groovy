package org.adsync4j.impl

import org.adsync4j.spi.DomainControllerAffiliation
import org.adsync4j.spi.DCARepository

class DCAHolder implements DCARepository {

    def dca

    DCAHolder(dca) {
        this.dca = dca
    }

    @Override
    DomainControllerAffiliation load(key) {
        dca
    }

    @Override
    DomainControllerAffiliation save(DomainControllerAffiliation dca) {
        this.dca = dca
    }
}
