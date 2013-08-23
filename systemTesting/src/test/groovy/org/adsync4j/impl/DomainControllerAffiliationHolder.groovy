package org.adsync4j.impl

import org.adsync4j.DomainControllerAffiliation
import org.adsync4j.SimpleRepository

class DomainControllerAffiliationHolder implements SimpleRepository<Object, DomainControllerAffiliation> {

    def dca

    DomainControllerAffiliationHolder(dca) {
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
