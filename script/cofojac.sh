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
Usage: ${0##*/} [<options>] [<javac-option>] ... <source> ...
Options:
  -java <javac>   calls this instead of java
  -javac <javac>  calls this instead of javac
  -g:contracts    compiles debug contract code
  -deps:none      skip source dependency preprocessing
  -deps:only      stop after source dependency preprocessing
  <javac-option>  passes argument to javac
  <source>        source file
EOF
  exit
}

shellquote()
{
  echo "$1" | sed "s/'/'\\\\''/g"
}

JAVA=${JAVA:-java}
JAVAC=${JAVAC:-javac}

classpath=
classoutput=
debug=no
passthrough=

procapt=yes
procdeps=yes

if [ $# -eq 0 ]; then
  usage
fi

while [ $# -gt 0 ]; do
  case "$1" in
    -cp|-classpath)
      shift
      [ $# -eq 0 ] && usage
      classpath="$1"
      ;;
    -d)
      shift
      [ $# -eq 0 ] && usage
      classoutput="$1"
      ;;
    -deps:only)
      procdeps=yes
      procapt=no
      ;;
    -deps:none)
      procdeps=no
      ;;
    -g:contracts)
      debug=yes
      ;;
    -java)
      shift
      [ $# -eq 0 ] && usage
      JAVA="$1"
      ;;
    -javac)
      shift
      [ $# -eq 0 ] && usage
      JAVAC="$1"
      ;;
    *)
      passthrough="$passthrough '$(shellquote "$1")'"
  esac
  shift
done

depsdir=
if [ $procdeps = yes ]; then
  cmd=$JAVA
  if [ $procapt = yes ]; then
    depsdir=$$.com.google.java.contract.d
    mkdir "$depsdir"
    cmd="$cmd -Dcom.google.java.contract.depsoutput=\"\$depsdir\""
  elif [ "$classoutput" ]; then
    cmd="$cmd -Dcom.google.java.contract.depsoutput=\"\$classoutput\""
  fi
  cmd="$cmd com.google.java.contract.core.apt.SourcePreprocessor $passthrough"

  eval "$cmd" || exit
fi

if [ $procapt = yes ]; then
  cmd=$JAVAC
  if [ "$classpath" ]; then
    cmd="$cmd -cp \"\$classpath\" -Acom.google.java.contract.classpath=\"\$classpath\""
  fi
  if [ "$classoutput" ]; then
    cmd="$cmd -d \"\$classoutput\" -Acom.google.java.contract.classoutput=\"\$classoutput\""
  fi
  if [ $debug = yes ]; then
    cmd="$cmd -Acom.google.java.contract.debug"
  fi
  if [ "$depsdir" ]; then
    cmd="$cmd -Acom.google.java.contract.depspath=\"\$depsdir\""
  fi
  cmd="$cmd $passthrough"

  eval "$cmd"
  excode=$?

  if [ "$depsdir" ]; then
    rm -rf "$depsdir"
  fi

  if [ $excode -ne 0 ]; then
    exit $excode
  fi
fi
