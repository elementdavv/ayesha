# This is a very simple Makefile that calls 'gradlew' to do the heavy lifting.

default: debug

debug:
	./gradlew --warning-mode=all assembleDebug
release:
	./gradlew --warning-mode=all assembleRelease
bundle:
	./gradlew --warning-mode=all bundle
install:
	./gradlew --warning-mode=all installDebug
installRelease:
	./gradlew --warning-mode=all installRelease
run: install
	adb shell am start -n net.timelegend.ayesha/.MainActivity
runRelease: installRelease
	adb shell am start -n net.timelegend.ayesha/.MainActivity
uninstall:
	./gradlew --warning-mode=all uninstallDebug
uninstallRelease:
	./gradlew --warning-mode=all uninstallRelease
lint:
	./gradlew --warning-mode=all lint
archive:
	./gradlew --warning-mode=all publishReleasePublicationToLocalRepository
sync: archive
	rsync -av --chmod=g+w --chown=:gs-priv $(HOME)/MAVEN/com/ ghostscript.com:/var/www/maven.ghostscript.com/com/
tarball: release
	cp app/build/outputs/apk/release/app-universal-release.apk \
		ayesha-$(shell git describe --tags).apk
tasks:
	./gradlew tasks
clean:
	./gradlew clean
