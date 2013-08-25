package org.adsync4j.gradle

import org.gradle.api.Project
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.model.Module
import org.gradle.plugins.ide.idea.model.ModuleLibrary
import org.gradle.plugins.ide.idea.model.Path

class GradleIdeaIntegrationHelper {

    static final def GRADLE_SRC_DIR = "/src/subprojects"
    static final def GRADLE_JAR_PATTERN = ~'.*/gradle-(.*)-.*.jar'

    static def addGradleAsDependency(Project prj) {
        def gradleHome = System.getenv('GRADLE_HOME')

        if (isGradleHomeSet(prj, gradleHome)) {
            def addedGradleJars = [] as Set

            Libs.gradleJars.each { gradleJarName ->
                def gradleJarPath = getGradleJarPath(prj, gradleHome, gradleJarName)
                if (gradleJarPath) {
                    prj.dependencies.add('provided', prj.files(gradleJarPath))
                    addedGradleJars << gradleJarPath
                }
            }

            attachGradleSourcesInIdea(prj, gradleHome, addedGradleJars)
        }
    }

    static def getGradleJarPath(Project prj, gradleHome, gradleJarName) {
        def gradleLib = "${gradleHome}/lib"
        def jarFileName = "gradle-$gradleJarName-${prj.gradle.gradleVersion}.jar"

        // Gradle jars are either under $GRADLE_HOME/lib or under $GRADLE_HOME/lib/plugins, let's see which wins for this jar
        def gradleLibJar = new File("$gradleLib/$jarFileName")
        def gradlePluginJar = new File("$gradleLib/plugins/$jarFileName")
        def gradleJar = gradleLibJar.exists() ? gradleLibJar : gradlePluginJar

        if (gradleJar.exists()) {
            return gradleJar.absolutePath
        } else {
            prj.logger.warn("$jarFileName is neither in \$GRADLE_HOME/lib nor in \$GRADLE_HOME/lib/plugins")
            return null
        }
    }

    static def isGradleHomeSet(Project prj, gradleHome) {
        if (gradleHome == null) {
            prj.logger.warn("Could not add Gradle jars as dependencies to project $prj.name, because GRADLE_HOME environment " +
                    "variable is not set. Adding Gradle jars as dependencies (in provided scope) would enable better IDE support " +
                    "when editing build scripts.")
        }
        gradleHome
    }

    static def attachGradleSourcesInIdea(Project prj, gradleHome, Set gradleJarPaths) {
        IdeaModel ideaModel = prj.idea

        if (areGradleSourcesAvailable(prj, gradleHome)) {
            ideaModel.module.iml.whenMerged { Module module ->
                module.dependencies.each { dependency ->
                    def isExternalDependency = dependency instanceof ModuleLibrary
                    if (isExternalDependency) {
                        ModuleLibrary externalDependency = dependency as ModuleLibrary

                        def pathsOfDependentLib = extractPaths(externalDependency)

                        gradleJarPaths.intersect(pathsOfDependentLib).each { String path ->
                            def gradleSubprojectName = extractGradleSubprojectName(path)
                            new File("$gradleHome/$GRADLE_SRC_DIR/${gradleSubprojectName}/src/main")
                                    .eachDir { sourceRootDir ->
                                externalDependency.sources.add(new Path("file://$sourceRootDir.absolutePath"))
                            }
                        }
                    }
                }
            }
        }
    }

    static def extractGradleSubprojectName(String gradleJarFileAbsolutePath) {
        def match = GRADLE_JAR_PATTERN.matcher(gradleJarFileAbsolutePath)
        match ? match[0][1] : ''
    }

    static def areGradleSourcesAvailable(Project prj, gradleHome) {
        def result = new File("${gradleHome}/src/subprojects/").exists()
        if (!result) {
            prj.logger.warn("Cannot attach Gradle sources to prject $prj.name. Sources not found under \$GRADLE_HOME/src")
            prj.logger.warn("Please download Gradle source distribution and unzip it in \$GRADLE_HOME, " +
                    "then rerun the 'gradle idea'")
        }
        result
    }

    static Set extractPaths(ModuleLibrary ml) {
        if (!ml.classes.isEmpty()) {
            ml.classes.collect { Path path ->
                def match = (path.url =~ /.+:\/\/(.*\.jar).*/)
                if (match) {
                    match[0][1]
                }
            } as Set
        } else {
            [] as Set
        }
    }
}