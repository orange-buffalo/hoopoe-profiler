package hoopoe.gradle.buildscript

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult
import org.gradle.kotlin.dsl.KotlinClosure2
import org.gradle.kotlin.dsl.the

val Project.sourceSets: SourceSetContainer
    get() = the<JavaPluginConvention>().sourceSets

val Project.printTestResult: KotlinClosure2<TestDescriptor, TestResult, Void>
    get() = KotlinClosure2({ desc, result ->
        if (desc.parent == null) { // will match the outermost suite
            println("------")
            println("Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} " +
                    "successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)")
            println("------")
        }
        null
    })