import json

class RestResponse:
    def __init__(self, response, content):
        responseMap = json.loads(content)
        self.extraProperties = responseMap['extraProperties'] if responseMap.has_key('extraProperties') else {}
        self.children = self.extraProperties['childResources'] if self.extraProperties.has_key('childResources') else []
        self.status = int(response['status'])
        self.message = responseMap["message"]
        self.properties = responseMap['properties'] if responseMap.has_key('properties') else {}

    def getStatus(self):
        return self.status

    def getMessage(self):
        return self.message

    def getExtraProperies(self):
        return self.extraProperties

    def getChildren(self):
        return self.children

    def getProperties(self):
        return self.properties

    def isSuccess(self):
        return 200 <= self.status <= 299