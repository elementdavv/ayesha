#! /usr/bin/env python3
# -*- coding: utf-8 -*-
# vim:fenc=utf-8
#
# Copyright © 2024 Element Davv <elementdavv@hotmail.com>
#
# Distributed under terms of the BSD license.
# reference:
# https://github.com/eclipse-jdtls/eclipse.jdt.ls/issues/3284#issuecomment-2452001716

"""
Generate classpath for gradle android project
usage:
1. add folowing task to app/build.gradle
tasks.register("printCompileClasspath") {
    doLast {
        println("---START---")
        configurations.getByName("debugCompileClasspath").files.forEach { file ->
            println(file.absolutePath)
        }
        println("---END---")
    }
}
2. run this file after app/.classpath file had been created
"""

import subprocess
import os

result = subprocess.run(['./gradlew', 'app:printCompileClasspath'], stdout=subprocess.PIPE).stdout.decode("utf-8")
# with open("raw.txt", "w") as f:
#     f.write(result)

# Split by lines
lines = result.split('\n')
# Get the lines between ---START--- and ---END--- lines
start = lines.index("---START---") + 1
end = lines.index("---END---")
lines = lines[start:end]
# print(lines)

final_lines = []

for line in lines:
    if line.endswith('.aar'):
        # Get the folder path
        folder = os.path.dirname(line)
        # Extract the aar in that folder
        subprocess.run(['unzip', '-o', '-q', line, '-d', folder])
        # Add the classes.jar
        final_lines.append(os.path.join(folder, 'classes.jar'))
    else:
        final_lines.append(line)

# get cp.py directory
curdir = os.path.dirname(os.path.realpath(__file__))

with open('app/.classpath', 'w') as f:
    f.write("""<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="output" path="bin/default"/>
    <classpathentry kind="src" path="src/main/java"/>
    <classpathentry kind="lib" path="{0}/app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/R.jar"/>
    <classpathentry kind="lib" path="{1}/platforms/android-34/android.jar"/>
    <classpathentry kind="lib" path="{1}/platforms/android-34/core-for-system-modules.jar"/>
""".format(curdir, os.environ['ANDROID_HOME']))
    for line in final_lines:
        f.write(f'    <classpathentry kind="lib" path="{line}"/>\n')
    f.write("""</classpath>""")

# disable override everytime eclipse.jdt start
sedline = "sed -i 's/^override.*/{0}/' {1}/app/.settings/org.eclipse.buildship.core.prefs".format("override\\.workspace\\.settings=false", curdir)
subprocess.call([sedline], shell=True)
