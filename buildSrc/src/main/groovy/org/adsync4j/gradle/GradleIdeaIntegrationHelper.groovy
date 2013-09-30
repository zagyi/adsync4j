package org.adsync4j.gradle

import org.gradle.api.Project
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.model.Module
import org.gradle.plugins.ide.idea.model.ModuleLibrary
import org.gradle.plugins.ide.idea.model.Path

/**
 * Helper class that is used to add Gradle jars (and sources) as dependencies in your IntelliJ IDEA project. Depending on these
 * jars makes it possible to get the same level of support from IntelliJ IDEA when editing Gradle build files as the usual
 * features it offers when editing Java/Groovy sources (such as code completion, quick access to javadocs, etc).
 * <p>
 * Of course you will have to forget about Gradle DSL if you want to benefit from the IDE support. Instead of using
 * Gradle DSL's magic incantations, realize that Gradle is just an ordinary library that contains classes and methods
 * which you should use exactly the same way as you use any other normal library. DSL scripts might be easy to read,
 * but it's a real pain to write them, and it's just doesn't worth the time.
 * <p>
 * Gradle jars are only added to the 'provided' scope/configuration, so that they won't pollute the runtime classpath
 * and the final artifacts.
 */
class GradleIdeaIntegrationHelper {

    static final def GRADLE_SRC_DIR = "/src/subprojects"
    static final def GRADLE_JAR_PATTERN = ~'.*/gradle-(.*)-.*.jar'

    /**
     * Find the detailed description on the {@link GradleIdeaIntegrationHelper class level}.
     */
    static def addGradleAsDependency(Project prj) {
        def gradleHome = System.getenv('GRADLE_HOME')

        if (isGradleHomeSet(prj, gradleHome)) {
            def addedGradleJars = [] as Set

            Libs.gradleJars.each { shortNameOfGradleJar ->
                def fullPathOfGradleJar = getFullPathOfGradleJar(prj, gradleHome, shortNameOfGradleJar)
                if (fullPathOfGradleJar) {
                    prj.dependencies.add('provided', prj.files(fullPathOfGradleJar))
                    addedGradleJars << fullPathOfGradleJar
                }
            }

            attachGradleSourcesToIdeaModule(prj, gradleHome, addedGradleJars)
        }
    }

    /**
     * Takes the short name of a Gradle jar, and finds the corresponding physical jar file either in {@code $GRADLE_HOME/lib} or
     * in {@code $GRADLE_HOME/lib/plugins}. It returns the jar's absolute path if found or null otherwise.
     * <p>
     * E.g. this method returns {@code $GRADLE_HOME/lib/gradle-core-1.7.jar} in case the input {@code gradleJarName} was
     * {@code core}, and the version of Gradle in use is 1.7.
     */
    private static def getFullPathOfGradleJar(Project prj, gradleHome, gradleJarName) {
        def gradleLib = "${gradleHome}/lib"
        def jarFileName = "gradle-$gradleJarName-${prj.gradle.gradleVersion}.jar"

        // Gradle jars are either under $GRADLE_HOME/lib or under $GRADLE_HOME/lib/plugins,
        // let's see which of the two contains this jar
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

    private static def isGradleHomeSet(Project prj, gradleHome) {
        if (gradleHome == null) {
            prj.logger.warn("Could not add Gradle jars as dependencies to project $prj.name, because GRADLE_HOME environment " +
                    "variable is not set. Adding Gradle jars as dependencies (in provided scope) would enable better IDE support " +
                    "when editing build scripts.")
        }
        gradleHome
    }

    /**
     * Manipulates the generated IntelliJ IDEA module descriptor xml file, so that the Gradle jars have sources attached to
     * them.
     */
    private static def attachGradleSourcesToIdeaModule(Project prj, gradleHome, Set gradleJarPaths) {
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

    private static def areGradleSourcesAvailable(Project prj, gradleHome) {
        def result = new File("${gradleHome}/src/subprojects/").exists()
        if (!result) {
            prj.logger.warn("Cannot attach Gradle sources to prject $prj.name. Sources not found under \$GRADLE_HOME/src")
            prj.logger.warn("Please download Gradle source distribution and unzip it in \$GRADLE_HOME, " +
                    "then rerun './gradlew idea'")
        }
        result
    }

    /**
     * Extracts the absolute path of every jar referenced in the specified module library (which represents an entry in the
     * list of dependencies of an IDEA module).
     */
    private static Set extractPaths(ModuleLibrary ml) {
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

    /**
     * Performs the reverse operation of {@link GradleIdeaIntegrationHelper#getFullPathOfGradleJar},
     * in other words it returns the short name of a Gradle jar given its absolute path.
     * <p>
     * E.g. this method returns {@code core} in case the input was {@code $GRADLE_HOME/lib/gradle-core-1.7.jar}.
     */
    private static def extractGradleSubprojectName(String gradleJarFileAbsolutePath) {
        def match = GRADLE_JAR_PATTERN.matcher(gradleJarFileAbsolutePath)
        match ? match[0][1] : ''
    }
}