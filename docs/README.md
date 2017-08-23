# hoopoe-profiler [![Build Status](https://travis-ci.org/orange-buffalo/hoopoe-profiler.svg)](https://travis-ci.org/orange-buffalo/hoopoe-profiler)  [![Code Coverage](https://img.shields.io/codecov/c/github/orange-buffalo/hoopoe-profiler.svg)](https://codecov.io/gh/orange-buffalo/hoopoe-profiler) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/8fe89e83a40d41a38677a997f1167261)](https://www.codacy.com/app/orange-buffalo/hoopoe-profiler?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=orange-buffalo/hoopoe-profiler&amp;utm_campaign=Badge_Grade) [![Download](https://img.shields.io/bintray/v/orange-buffalo/hoopoe-profiler/hoopoe-profiler.svg) ](https://bintray.com/orange-buffalo/hoopoe-profiler/hoopoe-profiler/_latestVersion) [![Issues to be done in next release](https://badge.waffle.io/orange-buffalo/hoopoe-profiler.svg?label=todo%20next%20release&title=Todo%20Next%20Release)](https://waffle.io/orange-buffalo/hoopoe-profiler) [![Issues in progress](https://badge.waffle.io/orange-buffalo/hoopoe-profiler.svg?label=in%20progress&title=In%20Progress)](https://waffle.io/orange-buffalo/hoopoe-profiler)  

![hoopoe in action](https://orange-buffalo.github.io/hoopoe-profiler/assets/img/hoopoe-in-action.gif)

## Why?
Quick answer is that we did not find any java profiler with reasonable price, active development / support and human-friendly 
profiling results delivery. Check our [User Guide](https://orange-buffalo.github.io/hoopoe-profiler/user-guide/why-another-profiler/) for more explanations.

## What exactly is hoopoe?
Hoopoe is a java agent. It instruments the code to trace the execution. 
It supports plugins and extensions. Out-of-the-box we have a plugin to track SQL queries and extension to provide Web-UI.
 Hoopoe is not a standalone application. Every profiled java process will bring new hoopoe instance up. 
 
See more [at hoopoe](https://orange-buffalo.github.io/hoopoe-profiler/).   

## Credits
We want to thank the community for all the pieces of code we use. Special thanks to Rafael Winterhalter and his amazing
 [Byte Buddy](http://bytebuddy.net), which makes java code instrumentation a lot much easier and pleasurable.  
