package yeoman.frontend

import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(YoController)
class YoControllerSpec extends Specification {

    def "flush resource"() {
        given:
        params.mapping = mapping
        params.id = id
        request.forwardURI = params.mapping + (params.id ? "/$params.id" : '')
        request.characterEncoding = 'utf-8'
        controller.yoResourceLocatorService = Mock(YoResourceLocatorService)
        1 * controller.yoResourceLocatorService.getResource(request.forwardURI) >> 'resource'

        when:
        controller.index()

        then:
        response.status == 200
        response.getHeader('Cache-Control') == 'no-cache, no-store, must-revalidate'
        response.getHeader('Pragma') == 'no-cache'
        response.getHeaderValue('Expires') == 0
        response.characterEncoding == 'utf-8'
        response.text == 'resource'

        where:
        mapping             | id
        'index.html'        | null
        'scripts'           | 'script.js '
    }

    def "set HTTP status to 404 if resource not found"() {
        given:
        controller.yoResourceLocatorService = Mock(YoResourceLocatorService)
        1 * controller.yoResourceLocatorService.getResource(_) >> null

        when:
        controller.index()

        then:
        response.status == 404
    }
}
