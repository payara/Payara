# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License.  You can
# obtain a copy of the License at
# https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
# or packager/legal/LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at packager/legal/LICENSE.txt.
#
# GPL Classpath Exception:
# Oracle designates this particular file as subject to the "Classpath"
# exception as provided by Oracle in the GPL Version 2 section of the License
# file that accompanied this code.
#
# Modifications:
# If applicable, add the following below the License Header, with the fields
# enclosed by brackets [] replaced by your own identifying information:
# "Portions Copyright [year] [name of copyright owner]"
#
# Contributor(s):
# If you wish your version of this file to be governed by only the CDDL or
# only the GPL Version 2, indicate your decision by adding "[Contributor]
# elects to include this software in this distribution under the [CDDL or GPL
# Version 2] license."  If you don't indicate a single choice of license, a
# recipient has the option to distribute your version of this file under
# either the CDDL, the GPL Version 2 or to extend the choice of license to
# its licensees as provided above.  However, if you add GPL Version 2 code
# and therefore, elected the GPL Version 2 license, then the option applies
# only if the new code is made subject to such option by the copyright
# holder.

import traceback
import urllib
import random
import string
import mimetypes
import sys
from restresponse import *
from connection import *

class RestClientBase:
    def __init__(self, connection, parent, name = None):
        self.connection = connection
        self.parent = parent
        self.entityValues = { }
        self.children = {}

        if name:
            self.setName(name)
            self.name = name

        try:
            restResponse = self.connection.get(self.getRestUrl())

            self.status = restResponse.getStatus()
            self.entityValues = restResponse.getEntityValues()
            self.children = restResponse.getChildren()
        except Exception as e:
            print e
            traceback.print_exc(file=sys.stdout)

    def getParent(self):
        return self.parent

    def getRestUrl(self):
        return self.getParent().getRestUrl() + self.getSegment()

    def getSegment(self):
        return ""

    def getStatus(self):
        return self.status

    def getMessage(self):
        return self.message

    def save(self):
        response = self.connection.post(self.getRestUrl(), urllib.urlencode(self.entityValues), headers={'Content-type': 'application/x-www-form-urlencoded'})
        self.status = response.getStatus()

    def delete(self):
        response = self.connection.delete(self.getRestUrl())
        self.status = response.getStatus()

    def execute(self, endPoint, method = "GET", payload = {}, needsMultiPart = False):
        if method == "POST":
            if needsMultiPart:
                content_type, body = self.encode_multipart_formdata(payload)
                restResponse = self.connection.post(self.getRestUrl() + endPoint, body, headers={'Content-type': content_type})
            else:
                restResponse = self.connection.post(self.getRestUrl() + endPoint, urllib.urlencode(payload), headers={'Content-type': 'application/x-www-form-urlencoded'})
        else:
            restResponse = self.connection.request(self.getRestUrl() + endPoint, method, urllib.urlencode(payload))

        if restResponse:
            self.status = restResponse.getStatus()
            return restResponse
        else:
            self.status = -1
            # raise an exception

    def setValue(self, key, value):
        self.entityValues[key] = value

    def getValue(self, key):
        return self.entityValues[key]

    def encode_multipart_formdata(self, args):
        BOUNDARY = '----------' + self._random_string (30)
        EOL = '\r\n'
        body = []
        for (key, value) in args.items():
            if type(value) is file:
                filename = value.name
                body.append('--' + BOUNDARY)
                body.append('Content-Disposition: form-data; name="%s"; filename="%s"' % (key, filename))
                body.append('Content-Type: %s' % self.get_content_type(filename))
                body.append('')
                body.append(value.read())
            else:
                body.append('--' + BOUNDARY)
                body.append('Content-Disposition: form-data; name="%s"' % key)
                body.append('')
                body.append(str(value))

        body.append('--' + BOUNDARY + '--')
        body.append('')
        content_type = 'multipart/form-data; boundary=%s' % BOUNDARY
        return content_type, EOL.join(body)

    def get_content_type(self, filename):
        return mimetypes.guess_type(filename)[0] or 'application/octet-stream'

    def _random_string (self, length):
        return ''.join (random.choice (string.letters) for ii in range (length + 1))
