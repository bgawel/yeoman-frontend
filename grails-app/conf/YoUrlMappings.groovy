import org.springframework.context.ApplicationContext

class YoUrlMappings {

	static mappings = { ApplicationContext context ->

        context.yoResourceLocatorService?.listOfResources().each {
            def mappingFor = it
            "/$mappingFor/$id**" (
                controller: 'yo'
            ) {
                mapping = mappingFor
            }
        }
	}
}
