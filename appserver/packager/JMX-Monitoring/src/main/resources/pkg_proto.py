#!/usr/bin/env python
# -*- coding: utf-8 -*-
# Copyright (c) 2016 Payara Foundation. All rights reserved.
#
# The contents of this file are subject to the terms of the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License.  You can
# obtain a copy of the License at
# https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
# or packager/legal/LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at packager/legal/LICENSE.txt.

import imp

conf = imp.load_source("pkg_conf", "../pkg_conf.py")

pkg = {
    "name"          : "jmx-monitoring-package",
    "version"       : conf.healthcheck_version,
    "attributes"    : {
                        "pkg.summary" : "Monitoring Services Integration",
                        "pkg.description" : "Monitoring Core module",
                        "info.classification" : "OSGi Service Platform Release 4",
                      },
    "dirtrees"      : { "glassfish/modules" : {},
                      },
    "licenses"      : {
                        "../../../../ApacheLicense.txt" : {"license" : "ApacheV2"},
                      }
 }
