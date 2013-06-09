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

import org.gradle.api.Project
import org.gradle.api.UnknownProjectException

class Projects {
    public static Project core
    public static Project systemTesting
    public static Project testUtils
    public static Project unboundidClient
    public static Project buildSrc

    public static def init(Project prj) {
        initProjectReferencesByFieldName(prj)
    }

    private static def initProjectReferencesByFieldName(Project prj) {
        Projects.declaredFields.each {
            if (it.type == Project) {
                try {
                    it.set(null, prj.project(it.name))
                } catch (UnknownProjectException ignored) {
                    throw new RuntimeException(
                            "Cannot initialize field Projects.$it.name, because there is no gradle project with that name.")
                }
            }
        }
    }
}
