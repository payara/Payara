#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# Copyright (c) 2016-2019 Payara Foundation and/or its affiliates.
# All rights reserved.
#
# The contents of this file are subject to the terms of the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License.  You can
# obtain a copy of the License at
# https://github.com/payara/Payara/blob/main/LICENSE.txt
# See the License for the specific
# language governing permissions and limitations under the License.
# 
# When distributing the software, include this License Header Notice in each
# file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.

import imp

conf = imp.load_source("pkg_conf", "../pkg_conf.py")

pkg = {
    "name"          : "opentracing-jaxws-package",
    "version"       : conf.glassfish_version,
    "attributes"    : {
                        "pkg.summary" : "JAX-WS Tracing",
                        "pkg.description" : "Modules to support tracing of JAX-WS methods",
                        "info.classification" : "OSGi Service Platform Release 4",
                      },
    "dirtrees"      : { "glassfish/modules" : {},
                      },
    "licenses"      : {
                        "../../../../ApacheLicense.txt" : {"license" : "ApacheV2"},
                      }
 }
 
