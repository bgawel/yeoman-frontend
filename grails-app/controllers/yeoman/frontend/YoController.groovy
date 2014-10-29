package yeoman.frontend

import org.springframework.http.HttpStatus


/**
 * Partially borrowed from https://github.com/bertramdev/asset-pipeline/blob/master/grails-app/controllers/asset/pipeline/AssetsController.groovy
 */
class YoController {

	def yoResourceLocatorService

    def index() {
        def mappedResource = yoResourceLocatorService.getResource(params.mapping + (params.id ? "/$params.id" : ''))
        if (mappedResource) {
            def encoding = params.encoding ?: request.characterEncoding
            def mimeType = servletContext.getMimeType(request.forwardURI)
            response.with {
                setHeader('Cache-Control', 'no-cache, no-store, must-revalidate')
                setHeader('Pragma', 'no-cache')
                setDateHeader('Expires', 0)
                if (encoding) {
                    setCharacterEncoding(encoding)
                }
                setContentType(mimeType)
                if (mimeType == 'text/html') {  // this is needed for Grails 2.3.x (and probably earlier); not needed for 2.4.3
                    render contentType: mimeType, text: mappedResource.text
                } else {
                    outputStream << mappedResource
                    flushBuffer()
                }
            }
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }
}
