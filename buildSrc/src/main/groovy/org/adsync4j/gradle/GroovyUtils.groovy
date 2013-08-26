package org.adsync4j.gradle

class GroovyUtils {
    private GroovyUtils() {}

    static def <T> T returnAfterClosure(T expression, Closure<?> c) {
        c.call(expression)
        expression
    }

}
