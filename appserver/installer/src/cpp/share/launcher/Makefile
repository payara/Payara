#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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
#

ifndef COMPONENT_NAME
COMPONENT_NAME=installer
endif
BUILD_ROOT=../../../..
NO_LINK_CPLUSPLUS=TRUE
include $(BUILD_ROOT)/make/defines.mk

ZLIBDIR = zlib-1.1.4
ZLIB_INCLUDES = -I$(ZLIBDIR)
RES_BUNDLE_NAME = messages

# Get the name of the archive
include $(ZLIBDIR)/defines.mk

# Compile zlib first
DIRS = $(ZLIBDIR)

COMMON_EXE_OBJS = $(RES_BUNDLE_NAME) java zip_util
ifeq ($(OS_ARCH), WINNT)
COMMON_EXE_OBJS += java_md.winnt
else
COMMON_EXE_OBJS += java_md.unix
endif

EXE_TARGET = setup
EXE1_TARGET = uninstall
EXE_OBJS = $(COMMON_EXE_OBJS) setup
EXE1_OBJS = $(COMMON_EXE_OBJS) uninstall
EXE_LIBDIRS += $(ZLIBDIR)/$(OBJDIR)
EXE1_LIBDIRS += $(ZLIBDIR)/$(OBJDIR)
ifeq ($(OS_ARCH), WINNT)
EXE_RES=java
EXE1_RES=java
EXE2_TARGET = setup.dos
EXE3_TARGET = uninstall.dos
EXE2_OBJS = $(COMMON_EXE_OBJS) setup.dos
EXE3_OBJS = $(COMMON_EXE_OBJS) uninstall.dos
EXE2_LIBDIRS += $(ZLIBDIR)/$(OBJDIR)
EXE3_LIBDIRS += $(ZLIBDIR)/$(OBJDIR)
endif

LOCAL_INC += $(JNI_INCLUDES) $(ZLIB_INCLUDES)
ifeq ($(OS_ARCH), Linux)
# Add X11 include path explicitly since some gcc versions may not automatically
# include them
LOCAL_INC += -I/usr/X11R6/include
endif
ifeq ($(OS_ARCH), SunOS)
# To be absolutely safe, explicitly add the X11 include paths even though
# Forte automatically includes them
LOCAL_INC += -I/usr/dt/include -I/usr/openwin/include
endif
LOCAL_BINARIES = $(EXE_TARGET) $(EXE1_TARGET)
ifeq ($(OS_ARCH), WINNT)
LOCAL_BINARIES += $(EXE2_TARGET) $(EXE3_TARGET)
LOCAL_LIB += user32 gdi32
endif
ifeq ($(OS_ARCH), Linux)
# Needed to make the executable's resource symbols available via dlsym()
LD_FLAGS += -Wl,-export-dynamic
endif

# Link in zlib archive
LOCAL_LIB += $(ZLIB)

PCH_DEP += $(OBJDIR)/$(RES_BUNDLE_NAME).c $(wildcard *.h) $(wildcard $(ZLIBDIR)/*.h)

# Needed for finding the symbol names in the generated
# $(OBJDIR)/$(RES_BUNDLE_NAME).c file
DEFINES += -DRES_BUNDLE_NAME=\"$(RES_BUNDLE_NAME)\"
ifeq ($(OS_ARCH), Linux)
# Needed when including dlfcn.h
DEFINES += -D_GNU_SOURCE
endif

include $(BUILD_ROOT)/make/rules.mk

# User customizable rules

JAVA_PROPS_TO_C_CLASS = JavaPropertiesToC

$(OBJDIR)/%.c: $(OBJDIR)/$(JAVA_PROPS_TO_C_CLASS).class $(wildcard $(RES_BUNDLE_NAME)*.properties)
	@$(RM) -f $@ $@.out
	$(EXTERNAL_JDK_DIR)/bin/java -classpath $(OBJDIR) $(JAVA_PROPS_TO_C_CLASS) -srcdir . -bundle $(RES_BUNDLE_NAME) > $@.out
	$(CP) $@.out $@

$(OBJDIR)/%.class: %.java
	$(EXTERNAL_JDK_DIR)/bin/javac -d $(OBJDIR) -classpath . $?

# Don't let make delete generated source files
.PHONY: $(OBJDIR)/%.c
