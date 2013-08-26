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

class IdeaUtils {
    static final Ordering scopeOrdering = Ordering.explicit("COMPILE", "RUNTIME", "TEST", "PROVIDED");

    static def typeComparator = { d1, d2 ->
        dependencyTypeRankMap[d1.getClass()] <=> dependencyTypeRankMap[d2.getClass()]
    } as Comparator

    static def dependencyTypeRankMap = [
            (ModuleDependency): 1, // type representing an internal module dependency
            (ModuleLibrary): 1000, // type representing an external jar dependency
            (SingleEntryModuleLibrary): 1000, // type representing an external jar dependency
    ]

    static def scopeComparator = { d1, d2 ->
        scopeOrdering.compare(d1.scope, d2.scope) } as Comparator

    static def nameComparator = { d1, d2 -> extractName(d1) <=> extractName(d2) } as Comparator

    static def dependenciesOrdering = Ordering.compound([typeComparator, scopeComparator, nameComparator])

    static def extractName(ModuleDependency md) {
        md.name
    }

    static def extractName(ModuleLibrary ml) {
        if (!ml.classes.isEmpty()) {
            def firstUrl = ml.classes.iterator().next().url
            def match = (firstUrl =~ /([^\/]*)\.jar/)
            if (match) {
                match[0][1]
            }
        }
    }

    static def sortIdeaDependencies(Project prj) {
        IdeaModel idea = prj.idea
        idea.module.iml.whenMerged { Module module ->
            def sortedDependencies = new TreeSet(IdeaUtils.dependenciesOrdering)
            sortedDependencies.addAll(module.dependencies)
            module.dependencies = sortedDependencies
        }
    }

    static def checkIfJavaPluginHasAlreadyBeenAppliedAtThisPoint(project) {
        assert project.plugins.hasPlugin('java'), """
    The 'idea' plugin is being applied without the 'java' plugin being applied first. As the former basically depends on the
    latter, you will have to make sure that the 'java' plugin is applied first in project '${project.name}'.
    """
    }
}

