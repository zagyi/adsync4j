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

import org.adsync4j.DomainControllerAffiliation

class DomainControllerAffiliationBean implements DomainControllerAffiliation {
    UUID invocationId
    Long highestCommittedUSN

    String protocol
    String host
    int port
    String bindUser
    String bindPassword

    String rootDN
    String syncBaseDN
    List<String> attributesToSync
    String searchFilter
    String searchDeletedObjectsFilter

    @Override
    void setHighestCommittedUSN(Long hcusn) {
        assert hcusn
        highestCommittedUSN = hcusn
    }
}
