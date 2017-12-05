package hoopoe.core.tracer

import hoopoe.api.HoopoeInvocationAttribute
import hoopoe.api.HoopoeProfiledInvocation
import hoopoe.api.HoopoeProfiledInvocationRoot
import hoopoe.api.HoopoeProfiledResult
import hoopoe.dev.tools.profiledInvocationRoot
import hoopoe.dev.tools.profiledResult
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.Test

class HotSpotCalculatorTest {

    @Test
    fun `should calculate hot spots`() {
        val calculatedHotSpots = HotSpotCalculator().calculateHotSpots(buildDefaultProfilesResult(), 3)

        assertThat("Hot spots calculation should never return null",
                calculatedHotSpots, notNullValue())

        assertThat("User-defined limit of hot spots count should be honoured",
                calculatedHotSpots.invocations, hasSize(5))

        val actualRoots = ArrayList(calculatedHotSpots.invocations)

        assertInvocationRoot(actualRoots[0], buildHotSpot1Tread1())
        assertInvocationRoot(actualRoots[1], buildHotSpot2Tread1())
        assertInvocationRoot(actualRoots[2], buildHotSpot3Tread1())
        assertInvocationRoot(actualRoots[3], buildHotSpot1Tread2())
        assertInvocationRoot(actualRoots[4], buildHotSpot2Tread2())
    }

    private fun assertInvocationRoot(
            actual: HoopoeProfiledInvocationRoot,
            expected: HoopoeProfiledInvocationRoot) {

        assertThat("Thread name should be equal",
                actual.threadName, equalTo(expected.threadName))

        assertInvocation(actual.invocation, expected.invocation)
    }

    private fun assertInvocation(
            actual: HoopoeProfiledInvocation,
            expected: HoopoeProfiledInvocation,
            path: String = "/") {

        assertThat("Class name is different from the expected one in $path",
                actual.className, equalTo(expected.className))

        assertThat("Method signature is different from the expected one in $path",
                actual.methodSignature, equalTo(expected.methodSignature))

        assertThat("Time of own execution is different from the expected one in $path",
                actual.ownTimeInNs, equalTo(expected.ownTimeInNs))

        assertThat("Total time of execution is different from the expected one in $path",
                actual.totalTimeInNs, equalTo(expected.totalTimeInNs))

        assertThat("Invocations count is different from the expected one in $path",
                actual.invocationsCount, equalTo(expected.invocationsCount))

        assertThat("Attributes count is different from the expected one in $path",
                actual.attributes.size, equalTo(expected.attributes.size))

        assertThat("Children count is different from the expected one in $path",
                actual.children.size, equalTo(expected.children.size))

        for (i in 0 until actual.children.size) {
            assertInvocation(
                    actual.children[i],
                    expected.children[i],
                    "$path${actual.className}.${actual.methodSignature}/")
        }
    }

    private fun buildDefaultProfilesResult(): HoopoeProfiledResult {
        return profiledResult {
            root {
                threadName = "t1"
                invocation {
                    className = "c1"
                    methodSignature = "m1"
                    ownTimeInNs = 13
                    totalTimeInNs = 479

                    invocation {
                        className = "c2"
                        methodSignature = "m2"
                        ownTimeInNs = 42
                        totalTimeInNs = 342

                        attribute { HoopoeInvocationAttribute.noTimeContribution("a1", null) }

                        invocation {
                            className = "c3"
                            methodSignature = "m3"
                            ownTimeInNs = 200
                            totalTimeInNs = 300

                            invocation {
                                className = "c4"
                                methodSignature = "m4"
                                ownTimeInNs = 100
                                totalTimeInNs = 100
                                invocationsCount = 7
                            }
                        }
                    }

                    invocation {
                        className = "c3"
                        methodSignature = "m3"
                        ownTimeInNs = 100
                        totalTimeInNs = 120
                    }

                    invocation {
                        className = "c4"
                        methodSignature = "m4"
                        ownTimeInNs = 10
                        totalTimeInNs = 17

                        invocation {
                            className = "c3"
                            methodSignature = "m3"
                            ownTimeInNs = 5
                            totalTimeInNs = 5
                        }
                    }
                }
            }

            root {
                threadName = "t2"
                invocation {
                    className = "c4"
                    methodSignature = "m4"
                    ownTimeInNs = 1
                    totalTimeInNs = 5

                    invocation {
                        className = "c3"
                        methodSignature = "m3"
                        ownTimeInNs = 1
                        totalTimeInNs = 3
                    }
                }

            }
        }
    }

    private fun buildHotSpot2Tread2(): HoopoeProfiledInvocationRoot {
        return profiledInvocationRoot {
            threadName = "t2"
            invocation {
                className = "c3"
                methodSignature = "m3"
                ownTimeInNs = 1
                totalTimeInNs = 3

                invocation {
                    className = "c4"
                    methodSignature = "m4"
                    ownTimeInNs = 1
                    totalTimeInNs = 5
                }
            }
        }
    }

    private fun buildHotSpot1Tread2(): HoopoeProfiledInvocationRoot {
        return profiledInvocationRoot {
            threadName = "t2"
            invocation {
                className = "c4"
                methodSignature = "m4"
                ownTimeInNs = 1
                totalTimeInNs = 5
            }
        }
    }

    private fun buildHotSpot3Tread1(): HoopoeProfiledInvocationRoot {
        return profiledInvocationRoot {
            threadName = "t1"
            invocation {
                className = "c2"
                methodSignature = "m2"
                ownTimeInNs = 42
                totalTimeInNs = 342

                attribute { HoopoeInvocationAttribute.noTimeContribution("a1", null) }

                invocation {
                    className = "c1"
                    methodSignature = "m1"
                    ownTimeInNs = 13
                    totalTimeInNs = 479
                }
            }
        }
    }

    private fun buildHotSpot2Tread1(): HoopoeProfiledInvocationRoot {
        return profiledInvocationRoot {
            threadName = "t1"
            invocation {
                className = "c4"
                methodSignature = "m4"
                ownTimeInNs = 110
                totalTimeInNs = 117
                invocationsCount = 8

                invocation {
                    className = "c3"
                    methodSignature = "m3"
                    ownTimeInNs = 200
                    totalTimeInNs = 300

                    invocation {
                        className = "c2"
                        methodSignature = "m2"
                        ownTimeInNs = 42
                        totalTimeInNs = 342

                        attribute { HoopoeInvocationAttribute.noTimeContribution("a1", null) }

                        invocation {
                            className = "c1"
                            methodSignature = "m1"
                            ownTimeInNs = 13
                            totalTimeInNs = 479
                        }
                    }
                }

                invocation {
                    className = "c1"
                    methodSignature = "m1"
                    ownTimeInNs = 13
                    totalTimeInNs = 479
                }
            }
        }
    }

    private fun buildHotSpot1Tread1(): HoopoeProfiledInvocationRoot {
        return profiledInvocationRoot {
            threadName = "t1"
            invocation {
                className = "c3"
                methodSignature = "m3"
                ownTimeInNs = 305
                totalTimeInNs = 425
                invocationsCount = 3

                invocation {
                    className = "c2"
                    methodSignature = "m2"
                    ownTimeInNs = 42
                    totalTimeInNs = 342
                    attribute { HoopoeInvocationAttribute.noTimeContribution("a1", null) }

                    invocation {
                        className = "c1"
                        methodSignature = "m1"
                        ownTimeInNs = 13
                        totalTimeInNs = 479
                    }
                }

                invocation {
                    className = "c1"
                    methodSignature = "m1"
                    ownTimeInNs = 13
                    totalTimeInNs = 479
                }

                invocation {
                    className = "c4"
                    methodSignature = "m4"
                    ownTimeInNs = 10
                    totalTimeInNs = 17

                    invocation {
                        className = "c1"
                        methodSignature = "m1"
                        ownTimeInNs = 13
                        totalTimeInNs = 479
                    }
                }
            }
        }
    }
}