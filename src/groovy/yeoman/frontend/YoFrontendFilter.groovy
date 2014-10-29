package yeoman.frontend

import grails.util.Holders
import groovy.transform.CompileStatic

import java.util.regex.Pattern

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletContext
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils

/**
 * Partially borrowed from https://github.com/bertramdev/asset-pipeline/blob/master/src/groovy/asset/pipeline/AssetPipelineFilter.groovy
 */
class YoFrontendFilter implements Filter {

    private WebApplicationContext applicationContext
    private ServletContext servletContext
    private Boolean checkGzip
    private Map mimeTypeMaxAgeMap
    private Closure getEtagStrategy
    private Pattern etagPattern

    @Override
    @CompileStatic
    void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
            ServletException {
        def request = (HttpServletRequest)req
        def response = (HttpServletResponse)res
        def resourceUri = request.requestURI
        def contextPath = request.contextPath
        if (resourceUri.startsWith(contextPath)) {
            resourceUri = resourceUri[contextPath.length()..-1]
        }
        def resource = applicationContext.getResource(resourceUri)
        if (resource.exists()) {
            if (isNewEtag(request, response, resource)) {
                if (checkGzip) {
                    def acceptsEncoding = request.getHeader('Accept-Encoding')
                    if (acceptsEncoding?.split(',').contains('gzip')) {
                        def gzipResource = applicationContext.getResource("${resourceUri}.gz")
                        if (gzipResource.exists()) {
                            resource = gzipResource
                            response.setHeader('Content-Encoding', 'gzip')
                        }
                    }
                }
                def mimeType = servletContext.getMimeType(request.requestURI)
                def encoding = request.characterEncoding
                response.with {
                    if (encoding) {
                        setCharacterEncoding(encoding)
                    }
                    setContentType(mimeType)
                    setHeader('Vary', 'Accept-Encoding')
                    setHeader('Cache-Control', "public, max-age=${mimeTypeMaxAgeMap[mimeType]}")
                    outputStream << resource.inputStream.bytes
                    flushBuffer()
                }
            }
        }
        if (!res.committed) {
            chain.doFilter(req, res)
        }
    }

    @CompileStatic
    private boolean isNewEtag(HttpServletRequest request, HttpServletResponse response, Resource resource) {
        String etagName = getEtagStrategy.call(resource)
        def ifNoneMatchHeader = request.getHeader('If-None-Match')
        if (ifNoneMatchHeader && ifNoneMatchHeader == etagName) {
            response.status = HttpStatus.NOT_MODIFIED.value()
            response.flushBuffer()
            return false
        }
        response.setHeader('ETag', etagName)
        true
    }

    @CompileStatic
    private String getEtagAsFilename(Resource resource) {
        resource.filename
    }

    @CompileStatic
    private String getEtagAsLastModifiedIfMissingInFilename(Resource resource) {
        def matcher = etagPattern.matcher(resource.filename)
        matcher.matches() ? matcher.group(1) : resource.file.lastModified()
    }

    @CompileStatic
    private String getEtagAsTimestamp(Resource resource) {
        new Date().time.toString()
    }

    @CompileStatic
    private String getEtagAsTimestampIfMissingInFilename(Resource resource) {
        def matcher = etagPattern.matcher(resource.filename)
        matcher.matches() ? matcher.group(1) : getEtagAsTimestamp(resource)
    }

    @Override
    void init(FilterConfig config) throws ServletException {
        servletContext = config.servletContext
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(config.servletContext)
        def yoFilterConfig = Holders.config.yo.filter
        checkGzip = yoFilterConfig.checkGzip instanceof Boolean ? yoFilterConfig.checkGzip : false
        mimeTypeMaxAgeMap = (yoFilterConfig.mimeTypeMaxAge ?: [:]).withDefault { yoFilterConfig.maxAge ?: 31536000 }
        initEtagStrategy(yoFilterConfig)
    }

    private initEtagStrategy(yoFilterConfig) {
        def etagStrategy = yoFilterConfig.etagStrategy
        if (etagStrategy) {
            if (etagStrategy instanceof Closure) {
                getEtagStrategy = etagStrategy
            } else if (etagStrategy instanceof String) {
                String defaultEtagPattern = /^([0-9a-f]+)\.[0-9A-Za-z_.]+\..+/
                switch (etagStrategy) {
                    case 'lastModifiedIfNotFilename':
                        getEtagStrategy = this.&getEtagAsLastModifiedIfMissingInFilename
                        etagPattern = ~(yoFilterConfig.etagPattern ?: defaultEtagPattern)
                        break
                    case 'timestampIfNotFilename':
                        getEtagStrategy = this.&getEtagAsTimestampIfMissingInFilename
                        etagPattern = ~(yoFilterConfig.etagPattern ?: defaultEtagPattern)
                        break
                    case 'timestamp':
                        getEtagStrategy = this.&getEtagAsTimestamp
                        break
                    case 'filename':
                        initDefaultEtagStrategy()
                        break
                    default:
                        throw new IllegalStateException("ETag strategy $etagStrategy is not supported")
                }
            } else {
                throw new IllegalStateException(
                    'ETag strategy (yo.etagStrategy) must be either a closure or a strategy name')
            }
        } else {
            initDefaultEtagStrategy()
        }
    }

    private initDefaultEtagStrategy() {
        getEtagStrategy = this.&getEtagAsFilename
    }

    @Override
    void destroy() {
    }
}
