package yeoman.frontend

import grails.util.Environment

import javax.annotation.PostConstruct

import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.web.context.ServletContextHolder

class YoResourceLocatorService {

    static transactional = false

    def grailsApplication

    private frontendPath
    private indexPath
    private appPath
    private tmpPath

    def getIndex() {
        getResource(indexPath)
    }

    def getResource(path) {
        if (Environment.developmentMode) {
            def fileForPath = new File("$frontendPath/$appPath/$path")
            if (fileForPath.exists()) {
                new FileInputStream(fileForPath)
            } else {
                fileForPath = new File("$frontendPath/$tmpPath/$path")
                if (fileForPath.exists()) {
                    new FileInputStream(fileForPath)
                } else {
                    log.debug "Cannot map resource for $path"
                    null
                }
            }
        } else {
            ServletContextHolder.servletContext.getResourceAsStream(path)
        }
    }

    def listOfResources() {
        if (Environment.developmentMode) {
            new File("$frontendPath/$appPath").list()
        } else {
            []
        }
    }

    @PostConstruct
    protected init() {
        def config = grailsApplication.config.yo
        frontendPath = config.frontend.dir ?: "${GrailsResourceUtils.GRAILS_APP_DIR}/frontend"
        appPath = config.frontend.app.dir ?: 'app'
        indexPath = config.frontend.app.index ?: 'index.html'
        tmpPath = config.frontend.tmp.dir ?: '.tmp'
    }
}
