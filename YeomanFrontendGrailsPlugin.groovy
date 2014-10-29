import grails.plugin.webxml.FilterManager;

class YeomanFrontendGrailsPlugin {
    // the plugin version
    def version = "0.1-RC1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Yeoman Frontend Plugin" // Headline display name of the plugin
    def author = "Bartek Gawel"
    def authorEmail = "bartek.gawel@gmail.com"
    def description = 'The Yeoman-Frontend is a plugin used for managing and processing frontend developed with Yeoman. The plugin integrates the frontend (preserving the Yeoman\'s directory structure) with a Grails web container during development of Grails backend application; it assembles the frontend into a Web Application Archive (WAR) file during deployment of Grails application.'

    // URL to the plugin's documentation
    def documentation = "http://bgawel.github.io/yeoman-frontend/guide/introduction.html"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "Dada Soft Lab" ]

    // Any additional developers beyond the author specified above.
    def developers = [ [ name: "Bartek Gawel", email: "bartek.gawel@gmail.com" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "GITHUB", url: "https://github.com/bgawel/yeoman-frontend/issues" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "https://github.com/bgawel/yeoman-frontend/" ]

    def getWebXmlFilterOrder() {
        ['YeomanFrontendFilter': FilterManager.GRAILS_WEB_REQUEST_POSITION - 150]
    }
    
    def doWithWebDescriptor = { xml ->
        def filterConfig = application.config.yo.filter
        def filterOff = filterConfig.off instanceof Boolean ? filterConfig.off : false
        if (!filterOff) {
            def urlPatterns = filterConfig.urlPatterns ?: ['/scripts/*', '/styles/*', '/images/*']
            xml.filter[0] + {
                'filter' {
                    'filter-name'('YoFrontendFilter')
                    'filter-class'('yeoman.frontend.YoFrontendFilter')
                }
            }
            def pattern
            xml.'filter-mapping'[0] + {
                'filter-mapping' {
                    'filter-name'('YoFrontendFilter')
                    urlPatterns.each {
                        'url-pattern'("$it")
                    }
                    dispatcher('REQUEST')
                }
            }
            println "YoFrontendFilter configured for $urlPatterns"
        }
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { ctx ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
