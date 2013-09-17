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
 ******************************************************************************/
package org.adsync4j;

/**
 * Interface of a simple generic repository that is able to load and save a certain entity type.
 * <p/>
 * Side note:
 * The name of the class includes "Generic" only for the reason not to cause name collision with widely used persistency
 * frameworks like Spring Data. It can get very annoying when your IDE offers many different "Repository" classes on code
 * completion, which happens when several libraries on the classpath all declare a class with that name. This often happens
 * with generic terms like "Action", "Task", but there are some really inconsiderate libraries defining classes like "List",
 * "Map" (which are not meant for public use though). These libraries usually end up on the IDE's import exclude list...
 *
 * @param <VALUE> The type of the entities help in the repository.
 * @param <KEY>   The type of the entities' key.
 */
public interface GenericRepository<KEY, VALUE> {
    VALUE load(KEY key);
    VALUE save(VALUE dca);
}
