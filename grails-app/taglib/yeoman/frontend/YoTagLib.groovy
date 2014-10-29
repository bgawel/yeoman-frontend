package yeoman.frontend

class YoTagLib {

    static namespace = 'yo'

    def yoResourceLocatorService

    def index = { attrs ->
        out << yoResourceLocatorService.index.text
    }
}
