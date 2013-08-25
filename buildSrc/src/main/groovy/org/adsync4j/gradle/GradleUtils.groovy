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
    static def addDependencies(Project prj, Map dependencyMap) {
        dependencyMap.each { String configuration, dependencies ->
            dependencies.each { dependencyNotation ->
                prj.dependencies.add(configuration, dependencyNotation)
            }
        }
    }

    static JavaPluginConvention javaProperties(Project prj) {
        JavaPluginConvention result = prj.convention.getPlugin(JavaPluginConvention)
        assert result, "java plugin does not seem to be applied in project $prj"
        result
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