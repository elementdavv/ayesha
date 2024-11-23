#! /bin/bash

set -x

find ../ -name build.gradle -exec sed -i -E '/^\s{12}maven\s*\{\s*$/,/^\s{12}\}\s*$/d' {} \;
find ../ -name build.gradle -exec sed -i -E '/maven\s*\{/d' {} \;
