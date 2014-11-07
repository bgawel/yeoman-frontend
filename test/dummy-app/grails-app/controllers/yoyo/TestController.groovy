package yoyo

public class TestController {

    def awesomeThings() {
        render(contentType: 'text/json') {
            ['Grails Yeoman-Frontend Plugin!']
        }
    }
}
