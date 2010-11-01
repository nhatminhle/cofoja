#!/bin/sh
# Copyright 2010 Google Inc.
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA

usage()
{
  cat <<EOF
Usage: ${0##*/} [<options>] <file>[.class] ...
Options:
  -d <dir>               output class directory; if not specified, output
                         class files are suffixed with .contracted
  -java <java>           calls this instead of java
  -jar <file>            specifies the JAR file that contains Contracts for Java
  -configurator <class>  instantiates and calls this configurator
  <file>[.class]         the class files to instrument; helper class
                         files need not be specified explicitly
EOF
  exit
}

shellquote()
{
  echo "$1" | sed "s/'/'\\\\''/g"
}

JAVA=${JAVA:-java}

configurator=
jarfile=
classoutput=
files=

if [ $# -eq 0 ]; then
  usage
fi

while [ $# -gt 0 ]; do
  case "$1" in
    -configurator)
      shift
      [ $# -eq 0 ] && usage
      configurator="$1"
      ;;
    -d)
      shift
      [ $# -eq 0 ] && usage
      classoutput="$1"
      ;;
    -jar)
      shift
      [ $# -eq 0 ] && usage
      jarfile="$1"
      ;;
    -java)
      shift
      [ $# -eq 0 ] && usage
      JAVA="$1"
      ;;
    *)
      files="$files '$(shellquote "$1")'"
  esac
  shift
done

cmd=$JAVA
if [ "$configurator" ]; then
  cmd="$cmd -Dcom.google.java.contract.configurator=\"$configurator\""
fi
if [ "$jarfile" ]; then
  if [ "$CLASSPATH" ]; then
    export CLASSPATH=$jarfile:$CLASSPATH
  else
    export CLASSPATH=$jarfile:.
  fi
fi
if [ "$classoutput" ]; then
  cmd="$cmd -Dcom.google.java.contract.classoutput=\"$classoutput\""
fi
cmd="$cmd com.google.java.contract.core.agent.PreMain $files"

eval "$cmd" || exit
