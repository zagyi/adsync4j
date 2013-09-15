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

/**
 * Class defining static references for each subproject. Must be initialized from outside, see {@link Projects#init(Project)
 * init(Project)}.
 */
class Projects {
    public static Project core
    public static Project systemTesting
    public static Project testUtils
    public static Project unboundidClient
    public static Project buildSrc

    /**
     * The root project instance is required in order to initialize the references to the subprojects. This method should be
     * invoked early on from the main build script.
     *
     * @param rootProject The {@link Project} instance representing the root project.
     */
    public static def init(Project rootProject) {
        initProjectReferencesByFieldName(rootProject)
    }

    private static def initProjectReferencesByFieldName(Project rootProject) {
        Projects.declaredFields.each {
            if (it.type == Project) {
                try {
                    it.set(null, rootProject.project(it.name))
                } catch (UnknownProjectException ignored) {
                    throw new RuntimeException(
                            "Cannot initialize field Projects.$it.name, because there is no Gradle subproject with that name.")
                }
            }
        }
    }
}
