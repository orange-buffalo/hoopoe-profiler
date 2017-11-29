package hoopoe.extensions.webview

import hoopoe.api.HoopoeInvocationAttribute
import hoopoe.api.HoopoeProfiledInvocation
import hoopoe.core.instrumentation.ClassMetadataReader
import hoopoe.dev.tools.configureInvocation
import hoopoe.dev.tools.profiledResult
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import net.bytebuddy.description.type.TypeDescription
import java.util.*

fun createEmptyResult() = profiledResult { }

fun createSingleThreadResult() = profiledResult {
    root {
        threadName = "http-8080-1"
        invocation {
            methodSignature = "test"
            className = "hello1"
            attribute { HoopoeInvocationAttribute.noTimeContribution("test", "hello") }
            invocation {
                className = "hello"
                methodSignature = "test1"
            }

        }
    }
}

fun createRandomizedResult() = profiledResult {
    for (i in 1..random.nextInt(3) + 1) {
        root {
            threadName = "http-8080-$i"
            invocation = randomInvocation(random.nextInt(50) + 1, random.nextInt())
        }
    }
}

fun createRandomizedMinimalResult() = profiledResult {
    for (i in 1..random.nextInt(2) + 1) {
        root {
            threadName = "http-8080-$i"
            invocation = randomInvocation(5, random.nextInt())
        }
    }
}

private class ClassPathElement(
        val className: String,
        val methods: List<String>
)

private val classPathCache: List<ClassPathElement> by lazy {
    val metadataReader = ClassMetadataReader()
    FastClasspathScanner().scan().namesOfAllClasses.map {
        ClassPathElement(
                it,
                try {
                    TypeDescription.ForLoadedType(Class.forName(it)).declaredMethods
                            .map { metadataReader.getMethodSignature(it) }
                            .toList()

                } catch (t: Throwable) {
                    listOf<String>()
                }
        )
    }.filter {
        it.methods.isNotEmpty()
    }
}

private val random = Random(42)
private val attributes = listOf("SQL Query", "Custom", "JTA Transaction")
private val attributeValues = listOf(
        "INSERT INTO STATION VALUES (13, 'Phoenix', 'AZ', 33, 112);",
        "Transaction ID: jta46324602",
        """SELECT * FROM syscat.columns
          WHERE tablename='go_region_dim'
          AND schemaname='gosalesdw';""",
        """SELECT '%c%' as Chapter,
            SUM(CASE WHEN ticket.status IN ('new','assigned') THEN 1 ELSE 0 END) as `New`,
            ...
            SUM(CASE WHEN ticket.status='closed' THEN 1 ELSE 0 END) as 'Closed',
            count(id) AS Total,
            ticket.id AS _id
        FROM engine.ticket
        INNER JOIN engine.ticket_custom
            ON ticket.id = ticket_custom.ticket
        WHERE ticket_custom.name='chapter'
            AND ticket_custom.value LIKE '%c%'
            AND type='New material'
            AND milestone='1.1.12'
            AND component NOT LIKE 'internal_engine'
        GROUP BY ticket.id
            """
)

private fun randomInvocation(
        remainingDepth: Int,
        totalExecutionTimeInNs: Int): HoopoeProfiledInvocation = configureInvocation {
    val index = random.nextInt(classPathCache.size)
    val classPathElement = classPathCache[index]

    methodSignature = classPathElement.methods[random.nextInt(classPathElement.methods.size)]
    className = classPathElement.className
    invocationsCount = if (random.nextInt(100) > 49) 1 else random.nextInt(5) + 1

    val ownTimePercent = if (random.nextInt(100) > 30) random.nextInt(5) else random.nextInt(100)
    ownTimeInNs = (totalExecutionTimeInNs * ownTimePercent / 100).toLong()
    totalTimeInNs = totalExecutionTimeInNs.toLong()

    if (random.nextInt(100_000) > 99_900) {
        attribute {
            val attribute = attributes[random.nextInt(attributes.size)]
            val attributeValue = attributeValues[random.nextInt(attributeValues.size)]
            if (random.nextInt(100) > 50) {
                HoopoeInvocationAttribute.noTimeContribution(attribute, attributeValue)
            } else {
                HoopoeInvocationAttribute.withTimeContribution(attribute, attributeValue)
            }
        }

        if (random.nextInt(100_000) > 99_000) {
            attribute {
                val attribute = attributes[random.nextInt(attributes.size)]
                val attributeValue = attributeValues[random.nextInt(attributeValues.size)]
                if (random.nextInt(100) > 50) {
                    HoopoeInvocationAttribute.noTimeContribution(attribute, attributeValue)
                } else {
                    HoopoeInvocationAttribute.withTimeContribution(attribute, attributeValue)
                }
            }
        }
    }

    if (remainingDepth > 0) {
        val childrenCount = if (random.nextInt(100) >= 40) 1 else random.nextInt(4) + 1
        val mainChild = random.nextInt(childrenCount) + 1
        var timeLeftInNs = Math.max(totalExecutionTimeInNs - ownTimeInNs.toInt(), 0)
        for (i in 1..childrenCount) {
            val childInvocationTimeInNs = if (i == childrenCount) timeLeftInNs else random.nextInt(timeLeftInNs + 1)
            addChild(
                    randomInvocation(
                            if (i == mainChild) remainingDepth - 1 else random.nextInt(remainingDepth),
                            childInvocationTimeInNs
                    ))
            timeLeftInNs -= childInvocationTimeInNs
        }
    }
}