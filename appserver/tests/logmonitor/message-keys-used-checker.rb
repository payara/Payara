#!/usr/bin/ruby
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

# Author: Dies Koper (dkoper@dev.java.net)
# This script reads a list of message keys from an input file and
# checks all Java source files for the usage of these keys.
# Message logged for debugging (level FINE, etc.) are omitted from the results.
require 'find'

keys_found = []
# load message keys (or "msg.key=msg text" pairs)
keys = IO.readlines("msgs-with-no-ids.txt")
keys.collect! { |item|
  # only interested in message keys (so remove msg text)
  if item =~ /^([\w._\-]+\s*[^\\])[=:].*/
    $1
  else
    item
  end
}

# return regex pattern from given array in .*"(a|b|c)".* format, i.e.
# a, b or c is enclosed in double quotes
def build_regex(keys)
  #puts keys
  regex=".*\"("
  keys.each { |key|
    regex += key.to_s + '|'
  }
  regex = regex.chomp!('|') + ')".*'
  #puts regex
end

#puts keys
# build regex pattern from array elements
regex = build_regex(keys)
#puts regex

keys_total = keys.size
matches = 0
no_id = 0
prev_line = ""

puts "GlassFish Home Is: "+ARGV[0] 

# traverse all files under the following root dir
Find.find(ARGV[0] ) do |f|

  case File.dirname(f)
    # skip directories that store copies of property files
  when /.*\/.svn/ then Find.prune # skip .svn dirs
  when /.*\/target/ then Find.prune # skip build dirs
  when /.*\/tests/ then Find.prune # skip test dirs
  end

  case File.extname(f)
  when '.java' then
    IO.foreach(f) {|line|
      # if line contains a message key
      unless (line.match(regex).nil?)
        key = $1
        keys_found << key

        case line
          # if a line starts with a double quote or opening bracket '(' it is
          # probably a continuance of the previous line
        when /^\s*["\(].*/
          line = prev_line.chomp + line.lstrip
          #puts "double line: #{line}"
        end

        case line
          # do not report if message logged as debug message
        when /.*log\(Level\.(FINE|FINER|FINEST|CONFIG).*/ then
          #          puts "debug msg found: #{key} in #{f.gsub(File::SEPARATOR,
          #          File::ALT_SEPARATOR || File::SEPARATOR)}\n#{line}"
        when /.*\.(fine|finer|finest|config)\(.*/ then
          #          puts "debug msg found: #{key} in #{f.gsub(File::SEPARATOR,
          #          File::ALT_SEPARATOR || File::SEPARATOR)}\n#{line}"
        else
          # Message key found. (Could be in code that is commented out,
          # not used, logged as info/warn/severe message, etc.)
          puts "- Found key '#{key}' used in #{f.gsub(File::SEPARATOR,
          File::ALT_SEPARATOR || File::SEPARATOR)}\n#{line}"
          no_id += 1
        end
      end
      prev_line = line
    }
  end

end

keys_not_found = keys - keys_found
puts "#{no_id} messages may need an Id."
puts "#{keys_not_found.size} of provided #{keys_total} message keys were not found:\n" +
  keys_not_found.inspect
