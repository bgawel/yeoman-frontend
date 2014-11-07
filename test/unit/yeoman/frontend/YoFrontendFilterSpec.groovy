package yeoman.frontend

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.util.Holders

import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.springframework.core.io.Resource
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.context.WebApplicationContext

import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class YoFrontendFilterSpec extends Specification {

    private filter

    def "check default configuration"() {
        given:
        def filterConfig = Mock(FilterConfig)
        2 * filterConfig.servletContext >> Mock(ServletContext)

        when:
        filter.init(filterConfig)

        then:
        filter.servletContext
        !filter.checkGzip
        filter.mimeTypeMaxAgeMap['text/html'] == 31536000
        filter.getEtagStrategy.method == 'getEtagAsFilename'
    }

    def "check custom configuration"() {
        given:
        def filterConfig = Mock(FilterConfig)
        2 * filterConfig.servletContext >> Mock(ServletContext)
        def config = new ConfigObject()
        config.yo.filter.checkGzip = true
        config.yo.filter.mimeTypeMaxAge = ['image/png' : 50]
        config.yo.filter.maxAge = 100
        config.yo.filter.etagStrategy = 'timestamp'
        Holders.config = config

        when:
        filter.init(filterConfig)

        then:
        filter.checkGzip
        filter.mimeTypeMaxAgeMap['image/png'] == 50
        filter.mimeTypeMaxAgeMap['text/html'] == 100
        filter.getEtagStrategy.method == 'getEtagAsTimestamp'
    }

    def "check Etag strategy lastModifiedIfNotFilename for a fingerprinted asset"() {
        given:
        def yoFilterConfig = new ConfigObject()
        yoFilterConfig.etagStrategy = 'lastModifiedIfNotFilename'
        def resource = Mock(Resource)
        1 * resource.filename >> '123a45.scripts.js'

        when:
        filter.initEtagStrategy(yoFilterConfig)

        then:
        filter.getEtagStrategy.method == 'getEtagAsLastModifiedIfMissingInFilename'
        filter.getEtagStrategy(resource) == '123a45'
    }

    def "check Etag strategy lastModifiedIfNotFilename for a non-fingerprinted asset"() {
        given:
        def yoFilterConfig = new ConfigObject()
        yoFilterConfig.etagStrategy = 'lastModifiedIfNotFilename'
        def resource = Mock(Resource)
        1 * resource.filename >> 'scripts.js'
        def file = Mock(File)
        1 * file.lastModified() >> 12345
        1 * resource.file >> file

        when:
        filter.initEtagStrategy(yoFilterConfig)

        then:
        filter.getEtagStrategy.method == 'getEtagAsLastModifiedIfMissingInFilename'
        filter.getEtagStrategy(resource) == '12345'
    }

    def "check Etag strategy timestampIfNotFilename for a non-fingerprinted asset"() {
        given:
        def yoFilterConfig = new ConfigObject()
        yoFilterConfig.etagStrategy = 'timestampIfNotFilename'
        def resource = Mock(Resource)
        1 * resource.filename >> 'scripts.js'

        when:
        filter.initEtagStrategy(yoFilterConfig)

        then:
        filter.getEtagStrategy.method == 'getEtagAsTimestampIfMissingInFilename'
        filter.getEtagStrategy(resource)
    }

    def "check custom Etag strategy implemented as closure"() {
        given:
        def yoFilterConfig = new ConfigObject()
        yoFilterConfig.etagStrategy = {
            assert resource
            assert request
            assert response
            assert applicationContext
            assert servletContext
            'custom etag'
        }

        when:
        filter.initEtagStrategy(yoFilterConfig)

        then:
        filter.getEtagStrategy(null) >> 'custom etag'
    }

    def "doFilter -> 304"() {
        given:
        filter.getEtagStrategy = filter.&getEtagAsFilename
        def request = Mock(HttpServletRequest)
        def response = Mock(HttpServletResponse)
        1 * request.requestURI >> '/scripts/12345.script.js'
        1 * request.contextPath >> '/'
        1 * request.getHeader('If-None-Match') >> '12345.script.js'
        filter.applicationContext = Mock(WebApplicationContext)
        def resource = Mock(Resource)
        1 * resource.exists() >> true
        1 * resource.filename >> '12345.script.js'
        1 * filter.applicationContext.getResource('scripts/12345.script.js') >> resource

        when:
        filter.doFilter(request, response, Mock(FilterChain))

        then:
        1 * response.setStatus(304)
        0 * response.setHeader('ETag', _)
    }

    def "doFilter -> 200"() {
        given:
        filter.getEtagStrategy = filter.&getEtagAsFilename
        filter.checkGzip = true
        filter.mimeTypeMaxAgeMap = [:].withDefault { 100 }
        filter.servletContext = Mock(ServletContext)
        def request = Mock(HttpServletRequest)
        def response = Spy(MockHttpServletResponse)
        1 * request.requestURI >> '/scripts/12345.script.js'
        1 * filter.servletContext.getMimeType('/scripts/12345.script.js') >> 'text/javascript'
        1 * request.contextPath >> '/'
        1 * request.getHeader('If-None-Match') >> '12a45.script.js'
        1 * request.getHeader('Accept-Encoding') >> 'gzip, deflate'
        1 * request.characterEncoding >> 'utf-8'
        filter.applicationContext = Mock(WebApplicationContext)
        def resource = Mock(Resource)
        1 * resource.exists() >> true
        1 * resource.filename >> '12345.script.js'
        1 * filter.applicationContext.getResource('scripts/12345.script.js') >> resource
        def resourceGzipped = Mock(Resource)
        1 * resourceGzipped.exists() >> true
        1 * resourceGzipped.inputStream >> new ByteArrayInputStream('resource as stream'.bytes)
        1 * filter.applicationContext.getResource('scripts/12345.script.js.gz') >> resourceGzipped

        when:
        filter.doFilter(request, response, Mock(FilterChain))

        then:
        1 * response.setHeader('ETag', '12345.script.js')
        1 * response.setHeader('Content-Encoding', 'gzip')
        1 * response.setCharacterEncoding('utf-8')
        1 * response.setContentType('text/javascript')
        1 * response.setHeader('Vary', 'Accept-Encoding')
        1 * response.setHeader('Cache-Control', 'public, max-age=100')
        new String(response.contentAsByteArray) == 'resource as stream'
    }

    def setup() {
        filter = new YoFrontendFilter()
    }
}
