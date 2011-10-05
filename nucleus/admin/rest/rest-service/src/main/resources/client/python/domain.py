from restclientbase import *

class Domain(RestClientBase):
    def __init__(self, connection, parent):
        RestClientBase.__init__(self, connection, parent)
        self.parent = parent
        self.connection = connection
    def getSegment(self):
        return self.parent.getSegment() + '/domain'

    def changeAdminPassword(self ,  _AS_ADMIN_PASSWORD,  _AS_ADMIN_NEWPASSWORD,  _username, optional={}):
        optional['AS_ADMIN_PASSWORD'] = _AS_ADMIN_PASSWORD
        optional['AS_ADMIN_NEWPASSWORD'] = _AS_ADMIN_NEWPASSWORD
        optional['username'] = _username
        return self.execute(self.getSegment() + '/change-admin-password', 'POST', optional, False)

    def collectLogFiles(self , optional={}):
        return self.execute(self.getSegment() + '/collect-log-files', 'POST', optional, False)

    def configureLdapForAdmin(self ,  _basedn, optional={}):
        optional['basedn'] = _basedn
        return self.execute(self.getSegment() + '/configure-ldap-for-admin', 'POST', optional, False)

    def createInstance(self ,  _node,  _instance_name, optional={}):
        optional['nodeagent'] = _node
        optional['instance_name'] = _instance_name
        return self.execute(self.getSegment() + '/create-instance', 'POST', optional, False)

    def createPasswordAlias(self ,  _aliasname,  _AS_ADMIN_ALIASPASSWORD, optional={}):
        optional['aliasname'] = _aliasname
        optional['AS_ADMIN_ALIASPASSWORD'] = _AS_ADMIN_ALIASPASSWORD
        return self.execute(self.getSegment() + '/create-password-alias', 'POST', optional, False)

    def deletePasswordAlias(self ,  _aliasname, optional={}):
        optional['aliasname'] = _aliasname
        return self.execute(self.getSegment() + '/delete-password-alias', 'POST', optional, False)

    def listPasswordAliases(self , optional={}):
        return self.execute(self.getSegment() + '/list-password-aliases', 'GET', optional, False)

    def updatePasswordAlias(self ,  _aliasname,  _AS_ADMIN_ALIASPASSWORD, optional={}):
        optional['aliasname'] = _aliasname
        optional['AS_ADMIN_ALIASPASSWORD'] = _AS_ADMIN_ALIASPASSWORD
        return self.execute(self.getSegment() + '/update-password-alias', 'POST', optional, False)

    def disableMonitoring(self , optional={}):
        return self.execute(self.getSegment() + '/disable-monitoring', 'POST', optional, False)

    def disableSecureAdmin(self , optional={}):
        return self.execute(self.getSegment() + '/disable-secure-admin', 'POST', optional, False)

    def enableMonitoring(self , optional={}):
        return self.execute(self.getSegment() + '/enable-monitoring', 'POST', optional, False)

    def enableSecureAdmin(self , optional={}):
        return self.execute(self.getSegment() + '/enable-secure-admin', 'POST', optional, False)

    def exportSyncBundle(self ,  _target, optional={}):
        optional['target'] = _target
        return self.execute(self.getSegment() + '/export-sync-bundle', 'POST', optional, False)

    def generateDomainSchema(self , optional={}):
        return self.execute(self.getSegment() + '/generate-domain-schema', 'POST', optional, False)

    def get(self ,  _pattern, optional={}):
        optional['pattern'] = _pattern
        return self.execute(self.getSegment() + '/get', 'POST', optional, False)

    def listCommands(self , optional={}):
        return self.execute(self.getSegment() + '/list-commands', 'GET', optional, False)

    def listContainers(self , optional={}):
        return self.execute(self.getSegment() + '/list-containers', 'GET', optional, False)

    def listInstances(self , optional={}):
        return self.execute(self.getSegment() + '/list-instances', 'GET', optional, False)

    def listLogAttributes(self , optional={}):
        return self.execute(self.getSegment() + '/list-log-attributes', 'GET', optional, False)

    def listLogLevels(self , optional={}):
        return self.execute(self.getSegment() + '/list-log-levels', 'GET', optional, False)

    def deleteLogLevels(self ,  _logger_name, optional={}):
        optional['logger_name'] = _logger_name
        return self.execute(self.getSegment() + '/delete-log-levels', 'DELETE', optional, False)

    def listModules(self , optional={}):
        return self.execute(self.getSegment() + '/list-modules', 'GET', optional, False)

    def listNodes(self , optional={}):
        return self.execute(self.getSegment() + '/list-nodes', 'GET', optional, False)

    def listNodesConfig(self , optional={}):
        return self.execute(self.getSegment() + '/list-nodes-config', 'GET', optional, False)

    def listJvmOptions(self , optional={}):
        return self.execute(self.getSegment() + '/list-jvm-options', 'GET', optional, False)

    def listPersistenceTypes(self ,  _type, optional={}):
        optional['type'] = _type
        return self.execute(self.getSegment() + '/list-persistence-types', 'GET', optional, False)

    def listSystemProperties(self , optional={}):
        return self.execute(self.getSegment() + '/list-system-properties', 'GET', optional, False)

    def listTimers(self , optional={}):
        return self.execute(self.getSegment() + '/list-timers', 'GET', optional, False)

    def listTransports(self ,  _target, optional={}):
        optional['target'] = _target
        return self.execute(self.getSegment() + '/list-transports', 'GET', optional, False)

    def restartDomain(self , optional={}):
        return self.execute(self.getSegment() + '/restart', 'POST', optional, False)

    def rotateLog(self , optional={}):
        return self.execute(self.getSegment() + '/rotate-log', 'POST', optional, False)

    def setLogAttributes(self ,  _name_value, optional={}):
        optional['name_value'] = _name_value
        return self.execute(self.getSegment() + '/set-log-attributes', 'POST', optional, False)

    def setLogLevels(self ,  _name_value, optional={}):
        optional['name_value'] = _name_value
        return self.execute(self.getSegment() + '/set-log-levels', 'POST', optional, False)

    def stopDomain(self , optional={}):
        return self.execute(self.getSegment() + '/stop', 'POST', optional, False)

    def uptime(self , optional={}):
        return self.execute(self.getSegment() + '/uptime', 'GET', optional, False)

    def version(self , optional={}):
        return self.execute(self.getSegment() + '/version', 'GET', optional, False)

    def generateRestClient(self ,  _outputDir, optional={}):
        optional['outputDir'] = _outputDir
        return self.execute(self.getSegment() + '/client', 'GET', optional, False)

    def listServices(self , optional={}):
        return self.execute(self.getSegment() + '/list-services', 'GET', optional, False)

    def createSystemProperties(self ,  _name_value, optional={}):
        optional['name_value'] = _name_value
        return self.execute(self.getSegment() + '/system-property', 'POST', optional, False)

    def getVersion(self):
        return self.getValue('version')

    def setVersion(self, value):
        self.setValue('version', value)

    def getLocale(self):
        return self.getValue('locale')

    def setLocale(self, value):
        self.setValue('locale', value)

    def getApplicationRoot(self):
        return self.getValue('applicationRoot')

    def setApplicationRoot(self, value):
        self.setValue('applicationRoot', value)

    def getLogRoot(self):
        return self.getValue('logRoot')

    def setLogRoot(self, value):
        self.setValue('logRoot', value)
