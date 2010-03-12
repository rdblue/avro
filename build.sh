#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e						  # exit on error

cd `dirname "$0"`				  # connect to root

VERSION=`cat share/VERSION.txt`

function usage {
  echo "Usage: $0 {test|dist|clean}"
  exit 1
}

if [ $# -eq 0 ]
then
  usage
fi

set -x						  # echo commands

for target in "$@"
do

case "$target" in

    test)
	# run lang-specific tests
	(cd lang/java; ant test)
	(cd lang/py; ant test)
	(cd lang/c; ./build.sh test)
	(cd lang/c++; ./build.sh test)
	(cd lang/ruby; rake test)

	# create interop test data
	(cd lang/java; ant interop-data-generate)
	(cd lang/py; ant interop-data-generate)
	(cd lang/c; ./build.sh interop-data-generate)
	#(cd lang/c++; make interop-data-generate)
	(cd lang/ruby; rake generate_interop)

	# run interop data tests
	(cd lang/java; ant interop-data-test)
	(cd lang/py; ant interop-data-test)
	(cd lang/c; ./build.sh interop-data-test)
	#(cd lang/c++; make interop-data-test)
	(cd lang/ruby; rake interop)

	# run interop rpc tests
	/bin/bash share/test/interop/bin/test_rpc_interop.sh

	;;

    dist)
	# build source tarball
	mkdir -p build
	rm -rf build/avro-src-$VERSION
	svn export --force . build/avro-src-$VERSION
	(cd lang/java; ant rat)

	mkdir -p dist
        (cd build; tar czf ../dist/avro-src-$VERSION.tar.gz avro-src-$VERSION)

	# build lang-specific artifacts
	(cd lang/java; ant dist)

	(cd lang/py; ant dist)

	(cd lang/c; ./build.sh dist)

	(cd lang/c++; ./build.sh dist)

	(cd lang/ruby; rake dist)

	# build docs
	(cd doc; ant)
	(cd build; tar czf ../dist/avro-doc-$VERSION.tar.gz avro-doc-$VERSION)

	cp DIST_README.txt dist/README.txt
	;;

    sign)

	set +x

	echo -n "Enter password: "
	stty -echo
	read password
	stty echo

	for f in $(find dist -type f \
	    \! -name '*.sha1' \! -name '*.asc' \! -name '*.txt' );
	do
	    sha1sum $f > $f.sha1
	    gpg --passphrase $password --armor --output $f.asc --detach-sig $f
	done

	set -x
	;;

    clean)
	rm -rf build dist
	(cd doc; ant clean)

	(cd lang/java; ant clean)

	(cd lang/py; ant clean)

	(cd lang/c; ./build.sh clean)

	(cd lang/c++; ./build.sh clean)

	(cd lang/ruby; rake clean)

	;;

    *)
        usage
        ;;
esac

done

exit 0
