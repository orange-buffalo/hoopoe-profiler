import hoopoe.gradle.buildscript.libraries

apply {
    plugin("java")
}

dependencies {
    "compileOnly"(libraries.lombok)
}