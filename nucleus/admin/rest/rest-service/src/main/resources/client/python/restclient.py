from domain import *
from connection import *

class RestClient:
    def __init__(self, host='localhost', port=4848, useSsl=False, userName = None, password = None):
        self.host = host
        self.port = port
        self.useSsl = useSsl

        self.connection = Connection(host, port, useSsl, userName, password)

    def getRestUrl(self):
        return ("https" if self.useSsl else "http") + "://" + self.host + ":" + str(self.port) + "/management"

    def getDomain(self):
        return Domain(self.connection, self)
