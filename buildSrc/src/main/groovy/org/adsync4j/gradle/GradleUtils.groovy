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
import org.gradle.api.Task
import org.gradle.api.artifacts.maven.GroovyMavenDeployer
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publication.maven.internal.DefaultMavenRepositoryHandlerConvention
import org.gradle.api.tasks.Upload

/**
 * Miscellaneous helper methods, mostly required because we want to avoid using Gradle DSL. The methods here help to write
 * build scripts that are as concise as possible without using dynamic constructs that prevent the IDE from providing proper
 * support (e.g. code completion).
 */
class GradleUtils {

    /**
     *  Looks for a gradle script in the {@code gradle/userScripts} directory with the same name as the current (OS level) user,
     *  and applies it in the specified project if found.
     *  <p>
     *  If the {@code qualifier} argument is specified it gets appended to
     *  the script name. E.g. it looks for the script {@code gradle/userScripts/johnny_init.gradle} in case the user name
     * {@code johnny} and the qualifier is {@code init}.
     */
    static def runUserScript(Project prj, String qualifier = '') {
        // prepend an underscore to the qualifier
        qualifier = qualifier ? "_$qualifier" : ''
        def userScript = prj.file("gradle/userScripts/${System.getProperty("user.name")}${qualifier}.gradle")

        if (userScript.exists()) {
            prj.apply from: userScript
        }
    }

    /**
     * Quick way for adding dependencies to a project.
     *
     * @param prj The project to add the dependencies to.
     * @param dependencyMap A map that contains associations from a configuration name to a list of dependencies.
     */
    static def addDependencies(Project prj, Map<String, List<Object>> dependencyMap) {
        dependencyMap.each { String configuration, dependencies ->
            dependencies.each { dependencyNotation ->
                prj.dependencies.add(configuration, dependencyNotation)
            }
        }
    }

    /**
     * @param prj
     * @return The convention object that contains properties defined by the java plugin.
     */
    static JavaPluginConvention javaPlugin(Project prj) {
        prj.convention.getPlugin(JavaPluginConvention)
    }

    /**
     *
     * @param uploadTask
     * @return The convention object that the maven plugin attaches to the repositories container of the {@code uploadTask}.
     * Can be used to invoke {@link DefaultMavenRepositoryHandlerConvention#mavenDeployer} in order to add a remote
     * maven repository where the upload task will deploy artifacts.
     */
    static DefaultMavenRepositoryHandlerConvention mavenPlugin(Upload uploadTask) {
        new DslObject(uploadTask.repositories).convention.getPlugin(DefaultMavenRepositoryHandlerConvention)
    }

    /**
     * For every configuration (like "compile", "runtime", and most notably "archives") Gradle defines an upload task
     * which is supposed to upload the artifacts associated to the specific configuration (normally only "archives" has
     * associated artifacts). An upload task can have any number of repositories where it uploads to.
     * <p>
     * This method takes a configuration name, looks up its corresponding upload task, and adds a maven repository to that
     * upload task. The added repository is of type {@link GroovyMavenDeployer}.
     * <p>
     * The remote maven repository represented by the returned object can be configured in a follow-up call to
     * {@link #configureRemoteRepository configureRemoteRepository()}.
     *
     * @param prj
     * @param configuration A configuration that has associated artifacts to be deployed (it's usually the 'archives'
     *                      configuration).
     * @return A maven deployer that has been added to the repositories of the specified configuration's upload task.
     */
    static GroovyMavenDeployer getMavenDeployerForConfiguration(Project prj, String configuration) {
        def uploadTaskName = 'upload' + configuration.capitalize()
        Task task = prj.tasks[uploadTaskName]

        assert task, "$prj has no task with name $uploadTaskName"
        assert task instanceof Upload, "$uploadTaskName task in $prj is not an upload task"

        mavenPlugin(task).mavenDeployer()
    }

    /**
     * Sets the basic properties of the specified remote maven repository ({@link GroovyMavenDeployer}).
     *
     * @param mavenDeployer
     * @param repositoryUrl
     * @param userName
     * @param password
     * @return
     */
    static def configureRemoteRepository(GroovyMavenDeployer mavenDeployer, String repositoryUrl, String userName, String password) {
        // Unfortunately it is impossible to create a remote repository in a sane way.
        // We are forced to use the dynamic method DefaultGroovyMavenDeployer.repository() which delegates
        // to a Groovy builder (org.gradle.api.publication.maven.internal.ant.RepositoryBuilder) to create
        // the RemoteRepository. We cannot create an instance ourselves, because Gradle uses some foreign
        // class loader to define the RemoteRepository class. If we created one here, it wouldn't be the
        // same type at runtime (same class but loaded by two different class loaders), which results in
        // a ClassCastException...
        mavenDeployer.repository(url: repositoryUrl) {
            authentication(userName: userName, password: password)
        }
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