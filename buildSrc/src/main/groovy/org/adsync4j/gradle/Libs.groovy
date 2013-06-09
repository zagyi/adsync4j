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

import com.google.common.base.Preconditions
import org.gradle.api.Project

class Libs {
    static class Versions {
        static def groovy = '2.1.3'
        static def guava = '14.0.1'
        static def hamcrest = '1.3'
        static def jsr305 = '2.0.1'
        static def junit = '4.11'
        static def logback = '1.0.13'
        static def slf4j = '1.7.5'
        static def spock = '0.7-groovy-2.0'
        static def spring = '3.2.2.RELEASE'
        static def unboundid = '2.3.3'
    }

    static def gradle
    static def groovy = "org.codehaus.groovy:groovy-all:${Versions.groovy}"
    static def guava = "com.google.guava:guava:${Versions.guava}"
    static def hamcrest = "org.hamcrest:hamcrest-library:${Versions.hamcrest}"
    static def jsr305_annotations = "com.google.code.findbugs:jsr305:${Versions.jsr305}"
    static def junit = "junit:junit:${Versions.junit}"
    static def logback = "ch.qos.logback:logback-classic:${Versions.logback}"
    static def slf4j_api = slf4j('slf4j-api')
    static def slf4j_ext = slf4j('slf4j-ext')
    static def slf4j_bridge_jcl = slf4j('jcl-over-slf4j')
    static def slf4j_bridge_jul = slf4j('jul-to-slf4j')
    static def spock = "org.spockframework:spock-core:${Versions.spock}"
    static def spock_spring = "org.spockframework:spock-spring:${Versions.spock}"
    static def spring_beans = spring('beans')
    static def spring_context = spring('context')
    static def spring_test = spring('test')
    static def unboundid = "com.unboundid:unboundid-ldapsdk:${Versions.unboundid}"

    static def slf4j(lib) { "org.slf4j:${lib}:${Versions.slf4j}" }

    static def spring(lib) { "org.springframework:spring-${lib}:${Versions.spring}" }

    @SuppressWarnings("GroovyAssignabilityCheck")
    static def getGradle() {
        Preconditions.checkState(
                gradle != null,
                "${Libs.class.name}.gradle_api can only be accessed after invoking ${Libs.class.name}.init(Project)".toString())
        gradle
    }

    static def init(Project prj) {
        gradle = prj.fileTree("$prj.rootDir/gradle/jars/").include('gradle-*-1.6.jar')
    }
}
