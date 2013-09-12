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
package org.adsync4j.gradle

class Libs {
    static class Versions {
        final static def groovy = '2.1.3'
        final static def guava = '14.0.1'
        final static def hamcrest = '1.3'
        final static def jsr305 = '2.0.1'
        final static def junit = '4.11'
        final static def logback = '1.0.13'
        final static def slf4j = '1.7.5'
        final static def spock = '0.7-groovy-2.0'
        final static def spring = '3.2.2.RELEASE'
        final static def unboundid = '2.3.4'
    }

    final static def gradleJars = ['base-services', 'core', 'ide', 'build-setup', 'plugins', 'maven', 'signing']
    final static def groovy = "org.codehaus.groovy:groovy-all:${Versions.groovy}"
    final static def guava = "com.google.guava:guava:${Versions.guava}"
    final static def hamcrest = "org.hamcrest:hamcrest-library:${Versions.hamcrest}"
    final static def jsr305Annotations = "com.google.code.findbugs:jsr305:${Versions.jsr305}"
    final static def junit = "junit:junit:${Versions.junit}"
    final static def logback = "ch.qos.logback:logback-classic:${Versions.logback}"
    final static def slf4jApi = slf4j('slf4j-api')
    final static def slf4jExt = slf4j('slf4j-ext')
    final static def slf4jBridgeJCL = slf4j('jcl-over-slf4j')
    final static def slf4jBridgeJUL = slf4j('jul-to-slf4j')
    final static def spock = "org.spockframework:spock-core:${Versions.spock}"
    final static def spockSpring = "org.spockframework:spock-spring:${Versions.spock}"
    final static def springBeans = spring('beans')
    final static def springContext = spring('context')
    final static def springTest = spring('test')
    final static def unboundid = "com.unboundid:unboundid-ldapsdk:${Versions.unboundid}"

    final static def slf4j(lib) { "org.slf4j:${lib}:${Versions.slf4j}" }

    final static def spring(lib) { "org.springframework:spring-${lib}:${Versions.spring}" }
}
