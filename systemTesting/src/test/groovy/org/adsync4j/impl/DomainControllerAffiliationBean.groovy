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

import org.adsync4j.spi.DomainControllerAffiliation

class DomainControllerAffiliationBean implements DomainControllerAffiliation {
    String url
    String bindUser
    String bindPassword

    UUID invocationId
    Long highestCommittedUSN

    String rootDN
    String syncBaseDN
    List<String> attributesToSync
    String searchFilter
    String searchDeletedObjectsFilter

    @Override
    DomainControllerAffiliation setInvocationId(UUID id) {
        assert id
        invocationId = id
        this
    }

    @Override
    DomainControllerAffiliation setHighestCommittedUSN(Long hcusn) {
        assert hcusn
        highestCommittedUSN = hcusn
        this
    }
}
