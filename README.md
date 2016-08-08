# hoopoe-profiler [![Build Status](https://travis-ci.org/orange-buffalo/hoopoe-profiler.svg)](https://travis-ci.org/orange-buffalo/hoopoe-profiler)  [![Code Coverage](https://img.shields.io/codecov/c/github/orange-buffalo/hoopoe-profiler.svg)](https://codecov.io/gh/orange-buffalo/hoopoe-profiler) [![Download](https://img.shields.io/bintray/v/orange-buffalo/hoopoe-profiler/hoopoe-profiler.svg) ](https://bintray.com/orange-buffalo/hoopoe-profiler/hoopoe-profiler/_latestVersion)  

[hoopoe](https://raw.githubusercontent.com/wiki/orange-buffalo/hoopoe-profiler/img/banner.gif)

## Why?
Quick answer is that we did not find any java profiler with reasonable price, active development / support and human-friendly profiling results delivery. Check our [wiki page](https://github.com/orange-buffalo/hoopoe-profiler/wiki/Why-the-heck-another-profiler%3F) for more explanations. 

## Where are we now?
Currently we are in alpha-version. This means two things: firstly, the absolute minimum set of fetures is developed and is under active real-life testing; secondly, early adopters are welcome as well as any constructive critics and contributions.
[Our roadmap](https://github.com/orange-buffalo/hoopoe-profiler/wiki/Roadmap) explains the plans of future development.  

## Installation
1. Download [the latest release](https://bintray.com/orange-buffalo/hoopoe-profiler/hoopoe-profiler/_latestVersion).
2. Register the agent for your application:
  ```
  -javaagent:<path-to-hoopoe-agent.jar>
  ```
  
  If you are using JBoss servers (AS, WildFly, EAP), please check [additional instructions](https://github.com/orange-buffalo/hoopoe-profiler/wiki/Installation-Guide). 
    
3. Start the application.
Please note: we instrument a lot of code to give the best profiling results we can. It means the time to load any class will increase, as well as the total startup time of the application.
4. The web-ui for hoopoe-profiler is deployed at [http://localhost:9786/](http://localhost:9786/).

## Full list of fetures
Coming soon, stay tuned.
