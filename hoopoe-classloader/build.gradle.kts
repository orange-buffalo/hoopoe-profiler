import org.gradle.kotlin.dsl.*

apply {
    plugin("java")
}

dependencies {
    "compileOnly"(Libraries.lombok)

    "testCompile"(Libraries.junit)
    "testCompile"(Libraries.junitDataProvider)
    "testCompile"(Libraries.hamcrest)
    "testCompile"(Libraries.mockito)
    "testCompile"(Libraries.commonsIo)
}