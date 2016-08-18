# hoopoe-profiler [![Build Status](https://travis-ci.org/orange-buffalo/hoopoe-profiler.svg)](https://travis-ci.org/orange-buffalo/hoopoe-profiler)  [![Code Coverage](https://img.shields.io/codecov/c/github/orange-buffalo/hoopoe-profiler.svg)](https://codecov.io/gh/orange-buffalo/hoopoe-profiler) [![Download](https://img.shields.io/bintray/v/orange-buffalo/hoopoe-profiler/hoopoe-profiler.svg) ](https://bintray.com/orange-buffalo/hoopoe-profiler/hoopoe-profiler/_latestVersion) [![Issues to be done in next release](https://badge.waffle.io/orange-buffalo/hoopoe-profiler.svg?label=todo%20next%20release&title=Todo%20Next%20Release)](https://waffle.io/orange-buffalo/hoopoe-profiler) [![Issues in progress](https://badge.waffle.io/orange-buffalo/hoopoe-profiler.svg?label=in%20progress&title=In%20Progress)](https://waffle.io/orange-buffalo/hoopoe-profiler)  

![hoopoe in action](https://github.com/orange-buffalo/hoopoe-profiler/wiki/img/movie.gif)

## Why?
Quick answer is that we did not find any java profiler with reasonable price, active development / support and human-friendly profiling results delivery. Check our [wiki page](https://github.com/orange-buffalo/hoopoe-profiler/wiki/Why-the-heck-another-profiler%3F) for more explanations.

## What exactly is hoopoe?
Hoopoe is a java agent deployed to JVM of profiled application. It instruments the code to trace its execution. It supports plugins and extensions. Out-of-the-box we have a plugin to track SQL queries and extension to provide Web-UI. Hoopoe is not a standalone application. Every profiled java process will bring new hoopoe instance up.   

## Where are we now?
Currently we are in alpha-version. This means two things: firstly, the absolute minimum set of fetures is developed and is under active real-life testing; secondly, early adopters are welcome as well as any constructive critics and contributions.
[Our roadmap](https://github.com/orange-buffalo/hoopoe-profiler/wiki/Roadmap) explains the plans of future development.  

## Requirements

The only requirement is Java 8.

## Installation
1. Download [the latest release](https://bintray.com/orange-buffalo/hoopoe-profiler/hoopoe-profiler/_latestVersion).
2. Register the agent for your application:
  ```
  -javaagent:<path-to-hoopoe-agent.jar>
  ```

  If you are using JBoss servers (AS, WildFly, EAP), please check [additional instructions](https://github.com/orange-buffalo/hoopoe-profiler/wiki/Installation-Guide#jboss).

3. Start the application.
Please note: we instrument a lot of code to give the best profiling results we can. It means the time to load any class will increase, as well as the total startup time of the application.
4. The web-ui for hoopoe-profiler is deployed at [http://localhost:9786/](http://localhost:9786/).

## Full list of features
* Track code execution
* Track SQL queries
* Web-UI to display profiling results
* More to come soon!

## Credits
We want to thank the community for all the pieces of code we use. Special thanks to Rafael Winterhalter and his amazing [Byte Buddy](http://bytebuddy.net), which makes java code instrumentation a lot much easier and pleasurable.  
