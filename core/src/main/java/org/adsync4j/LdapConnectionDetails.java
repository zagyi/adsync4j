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
 ******************************************************************************/
package org.adsync4j;

/**
 * Interface for objects that store information required to open a connection to an LDAP server.
 */
public interface LdapConnectionDetails {

    String getProtocol();

    String getHost();

    int getPort();

    String getBindUser();

    String getBindPassword();

    /**
     * @return DN of the root entry of an Active Directory tree (e.g. {@code DC=example,DC=com}).
     */
    String getRootDN();
}
