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
package org.adsync4j.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Interface for classes that interpret an LDAP attribute type (specific to a certain LDAP SDK) to well known types like
 * String, Long, etc.
 * <p/>
 * {@link LdapClient} implementations are free to use any LDAP SDK available for Java to implement the {@link LdapClient}
 * interface. Each of these SDKs define a specific type to represent an LDAP attribute E.g. it's
 * {@link javax.naming.directory.Attribute} in case of JNDI, or {@link com.unboundid.ldap.sdk.Attribute} in case of the
 * UnboundID LDAP SDK, etc. Since callers of the {@link LdapClient} interface can not possibly be prepared to deal with all the
 * different attribute types, an auxiliary class is needed that helps to interpret the SDK specific attribute type. This is
 * exactly what this type is defined for.
 *
 * @param <LDAP_ATTRIBUTE> The SDK specific LDAP attribute type that this class is able to interpret.
 */
public interface LdapAttributeResolver<LDAP_ATTRIBUTE> {

    /**
     * Resolves the given LDAP attribute to a string.
     *
     * @param attribute An LDAP attribute.
     * @return The string value of the given attribute.
     */
    @Nullable
    String getAsString(LDAP_ATTRIBUTE attribute);

    /**
     * Resolves the given LDAP attribute to a long.
     *
     * @param attribute An LDAP attribute.
     * @return The long value of the given attribute.
     */
    @Nullable
    Long getAsLong(LDAP_ATTRIBUTE attribute);

    /**
     * Resolves the given LDAP attribute to a byte array.
     *
     * @param attribute An LDAP attribute.
     * @return The value of the given attribute as a byte array.
     */
    @Nullable
    byte[] getAsByteArray(LDAP_ATTRIBUTE attribute);

    /**
     * Resolves the given LDAP attribute to a list of strings. Used in conjunction with multi-valued LDAP attributes.
     *
     * @param attribute An LDAP attribute.
     * @return All string values of the given multi-valued LDAP attributes.
     */
    @Nonnull
    List<String> getAsStringList(LDAP_ATTRIBUTE attribute);
}
