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
import org.gradle.api.plugins.JavaPluginConvention

class GradleUtils {
    static def runUserScript(Project prj, String qualifier = '') {
        // prepend an underscore to the qualifier
        qualifier = qualifier ? "_$qualifier" : ''
        def userScript = prj.file("gradle/userScripts/${System.getProperty("user.name")}${qualifier}.gradle")

        if (userScript.exists()) {
            prj.apply from: userScript
        }
    }

    static def addDependencies(Project prj, Map dependencyMap) {
        dependencyMap.each { String configuration, dependencies ->
            dependencies.each { dependencyNotation ->
                prj.dependencies.add(configuration, dependencyNotation)
            }
        }
    }

    static JavaPluginConvention javaPlugin(Project prj) {
        prj.convention.getPlugin(JavaPluginConvention)
    }

    /**
     * Same as reading an extension property from the project's ext "namespace" in Gradle DSL - without the black magic.
     */
    static def ext(Project prj, String name) {
        prj.extensions.getExtraProperties().get(name)
    }

    /**
     * Same as defining an extension property in the project's ext "namespace" in Gradle DSL - without the black magic.
     */
    static def ext(Project prj, String name, def value) {
        prj.extensions.getExtraProperties().set(name, value)
    }

//    static def reorderProjectResourcesBeforeExternalDependencies(FileCollection classPath, Project prj) {
//        def splitClassPathFiles = classPath.files.split { isProjectResource(prj, it) }
//        def projectResources = splitClassPathFiles[0]
//        def externalDependencies = splitClassPathFiles[1]
//        prj.files(projectResources) + prj.files(externalDependencies)
//    }
//
//    static def isProjectResource(Project project, File element) {
//        project.rootProject.allprojects.any { prj ->
//            element.absolutePath.startsWith(prj.buildDir.absolutePath)
//        }
//    }
}