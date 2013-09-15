package org.adsync4j.impl

import org.adsync4j.DomainControllerAffiliation
import org.adsync4j.GenericRepository

class DomainControllerAffiliationHolder implements GenericRepository<Object, DomainControllerAffiliation> {

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
