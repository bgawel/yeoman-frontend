package yeoman.frontend

import grails.test.mixin.TestFor
import grails.util.Environment

import javax.servlet.ServletContext

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.web.context.ServletContextHolder

import spock.lang.Specification

@TestFor(YoResourceLocatorService)
class YoResourceLocatorServiceSpec extends Specification {

    def "check default configuration"() {
        when:
        service.init()

        then:
        service.indexPath == 'index.html'
        service.frontendPath == 'grails-app/frontend'
        service.appPath == 'app'
        service.tmpPath == '.tmp'
    }

    def "check custom configuration"() {
        given:
        def grailsApplication = new DefaultGrailsApplication()
        grailsApplication.config = new ConfigObject()
        grailsApplication.config.yo.frontend.dir = 'c_frontend'
        grailsApplication.config.yo.frontend.app.dir = 'c_app'
        grailsApplication.config.yo.frontend.app.index = 'index.php'
        grailsApplication.config.yo.frontend.tmp.dir = '.c_tmp'
        service.grailsApplication = grailsApplication

        when:
        service.init()

        then:
        service.indexPath == 'index.php'
        service.frontendPath == 'c_frontend'
        service.appPath == 'c_app'
        service.tmpPath == '.c_tmp'
    }

    def "get resource (index) from app"() {
        given:
        service.init()
        service.frontendPath = 'test/resources'
        Environment.metaClass.static.isDevelopmentMode = { true }

        when:
        def resource = service.getIndex()

        then:
        resource.text.startsWith('index.html')
    }

    def "get resource from .tmp"() {
        given:
        service.init()
        service.frontendPath = 'test/resources'
        Environment.metaClass.static.isDevelopmentMode = { true }

        when:
        def resource = service.getResource('main.css')

        then:
        resource.text.startsWith('main.css')
    }

    def "get null if resource not found"() {
        given:
        service.init()
        service.frontendPath = 'test/resources'
        Environment.metaClass.static.isDevelopmentMode = { true }

        when:
        def resource = service.getResource('script.js')

        then:
        !resource
    }

    def "get resource via servlet context for production"() {
        given:
        Environment.metaClass.static.isDevelopmentMode = { false }
        def servletContext = Mock(ServletContext)
        def resourceAsStream = Mock(InputStream)
        1 * servletContext.getResourceAsStream('script.js') >> resourceAsStream
        ServletContextHolder.setServletContext(servletContext)

        when:
        def resource = service.getResource('script.js')

        then:
        resource == resourceAsStream
    }

    def "get list of resources"() {
        given:
        service.init()
        service.frontendPath = 'test/resources'
        Environment.metaClass.static.isDevelopmentMode = { true }

        when:
        def resources = service.listOfResources()

        then:
        resources.size() == 1
        resources[0] == 'index.html'
    }

    def "get list of resources for production"() {
        given:
        service.init()
        service.frontendPath = 'test/resources'
        Environment.metaClass.static.isDevelopmentMode = { false }

        when:
        def resources = service.listOfResources()

        then:
        !resources
    }
}
