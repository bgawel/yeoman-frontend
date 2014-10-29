import org.codehaus.groovy.grails.io.support.GrailsResourceUtils

import grails.util.Environment
import grails.util.Holders

def config = new ConfigSlurper(grailsSettings.grailsEnv).parse(
    new File("${GrailsResourceUtils.GRAILS_APP_DIR}/conf/Config.groovy").toURI().toURL()).yo
def dir = config.frontend.dir ?: "${GrailsResourceUtils.GRAILS_APP_DIR}/frontend"

def getCommandArgsInLine = { cmdName ->
    System.properties["yo.$cmdName"] ?: config."$cmdName" ?: ''
}

def toCommandAndArgs(commandArgsInLine, defaultCommand, defaultArgs) {
    def command, args
    def os = System.properties['os.name']?.toLowerCase()
    if (os.indexOf('win') >= 0) {
        command = 'cmd.exe'
        if (commandArgsInLine) {
            args = "/c $commandArgsInLine"
        } else {
            args = "/c $defaultCommand $defaultArgs"
        }
    } else {
        if (commandArgsInLine) {
            def commandArgs = commandArgsInLine.split(' ')
            command = commandArgs[0]
            args = commandArgs.size() > 1 ? commandArgs[1..-1].join(' ') : ''
        } else {
            command = defaultCommand
            args = defaultArgs
        }
    }
    [command, args]
}

def runCommand = { command, args ->
    println "Starting command '$command $args' in $dir"
    def ant = new AntBuilder()
    ant.exec(failonerror: 'true',
             dir: dir,
             executable: command) {
                 arg(line: args)
             }
}

eventConfigureTomcat = { tomcat ->
    println 'Yeoman-frontend: \'eventConfigureTomcat\' received'   // added to monitor https://jira.grails.org/browse/GRAILS-11229
    def runappcmdOff = System.properties['yo.runappcmd.off'] || config.runappcmd.off
    if (Environment.developmentMode && !runappcmdOff) {
        def (command, args) = toCommandAndArgs(getCommandArgsInLine('runappcmd.exe'), 'grunt', 'serve')
        Thread.start {
            runCommand(command, args)
        }
    }
}

eventCreateWarStart = { warName, stagingDir ->
    println 'Yeoman-frontend: \'eventCreateWarStart\' received'
    def warcmdOff = System.properties['yo.warcmd.off'] || config.warcmd.off
    if (!warcmdOff) {
        def (command, args) = toCommandAndArgs(getCommandArgsInLine('warcmd.exe'), 'grunt', 'build')
        runCommand(command, args)
        def distDir = new File(dir, config.frontend.dist.dir ?: 'dist')
        def excludes = config.frontend.dist.excludes ?: ['.htaccess', '404.html', 'robots.txt']
        println "Copying $distDir into $stagingDir, exclude: $excludes"
        ant.copy(todir: stagingDir.path) {
            fileset(dir: distDir.path, excludes: excludes.join(','))
        }
    } else {
        println 'Yeoman\'s frontend not assembled into WAR'
    }
}