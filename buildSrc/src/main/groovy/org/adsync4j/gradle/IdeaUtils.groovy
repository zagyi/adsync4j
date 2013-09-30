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

import com.google.common.collect.Ordering
import org.gradle.api.Project
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.model.Module
import org.gradle.plugins.ide.idea.model.ModuleDependency
import org.gradle.plugins.ide.idea.model.ModuleLibrary
import org.gradle.plugins.ide.idea.model.SingleEntryModuleLibrary

/**
 * Utility class that helps sorting the dependencies of an IntelliJ IDEA module by type (type means here project source folder
 * vs. external jar dependency), scope and name. This is critical if you must resort to hacks where some project sources
 * override/patch classes from an external jar dependency, in which case you have to ensure that project sources are positioned
 * on the classpath before the jar that is being patched).
 */
class IdeaUtils {

    private static final Ordering scopeOrdering = Ordering.explicit("COMPILE", "RUNTIME", "TEST", "PROVIDED");

    private static def typeComparator = { d1, d2 ->
        dependencyTypeRankMap[d1.getClass()] <=> dependencyTypeRankMap[d2.getClass()]
    } as Comparator

    private static def dependencyTypeRankMap = [
            (ModuleDependency): 1, // type representing an internal module dependency
            (ModuleLibrary): 1000, // type representing an external jar dependency
            (SingleEntryModuleLibrary): 1000, // type representing an external jar dependency
    ]

    private static def scopeComparator = { d1, d2 ->
        scopeOrdering.compare(d1.scope, d2.scope) } as Comparator

    private static def nameComparator = { d1, d2 -> extractName(d1) <=> extractName(d2) } as Comparator

    private static def dependenciesOrdering = Ordering.compound([typeComparator, scopeComparator, nameComparator])

    private static def extractName(ModuleDependency md) {
        md.name
    }

    private static def extractName(ModuleLibrary ml) {
        if (!ml.classes.isEmpty()) {
            def firstUrl = ml.classes.iterator().next().url
            def match = (firstUrl =~ /([^\/]*)\.jar/)
            if (match) {
                match[0][1]
            }
        }
    }

    /**
     * Sorts the dependencies of the IntelliJ IDEA module generated for the specified project. The sort order is: type, scope,
     * name. Type means here project source folder vs. external jar dependency.
     * @param prj
     */
    static def sortDependenciesOfIntelliJModules(Project prj) {
        IdeaModel idea = prj.idea
        idea.module.iml.whenMerged { Module module ->
            def sortedDependencies = new TreeSet(IdeaUtils.dependenciesOrdering)
            sortedDependencies.addAll(module.dependencies)
            module.dependencies = sortedDependencies
        }
    }

    static def checkIfJavaPluginHasAlreadyBeenAppliedAtThisPoint(project) {
        assert project.plugins.hasPlugin('java'), """
    The idea plugin has some implicit dependencies on the java plugin. It seems that in $project the java plugin has not been
    applied, or it was applied after the idea plugin. Please fix this problem by applying the java plugin before the idea plugin.
    """
    }
}

