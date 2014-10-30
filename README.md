Grails Yeoman-Frontend Plugin
===============
[![Build Status](https://travis-ci.org/bgawel/yeoman-frontend.svg?branch=master)](https://travis-ci.org/bgawel/yeoman-frontend)

Overview
--------
The Yeoman-Frontend is a Grails plugin used for managing and processing a frontend developed with Yeoman. The plugin integrates the frontend (preserving the Yeoman's directory structure) with a Grails web container during development of a Grails backend application; it assembles the frontend into a Web Application Archive (WAR) file during deployment of the Grails application.

![Overview](https://github.com/bgawel/bgawel.github.io/blob/master/yeoman-frontend/img/overview.png)

Installation
------------
##### Dependency:
```groovy
	runtime ":yeoman-frontend:0.1-RC1"
```

Issues
------
Be aware of this Grails bug https://jira.grails.org/browse/GRAILS-11229; the event eventConfigureTomcat must be invoked to run the plugin in the Grails development mode.

Documentation
-------------
http://bgawel.github.io/yeoman-frontend/guide/introduction.html

Last release: Oct 29, 2014
--------------------------
0.1-RC1 - First version (tested on Linux, Windows with Grails versions: 2.3.5, 2.4.3)

