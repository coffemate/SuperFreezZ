#!/bin/bash

set -e
echo "Welcome to F-Droid version publisher!"
echo 
echo "Press Ctrl+C at any time to abort."
echo

echo "+git pull weblate master"
git remote add weblate https://hosted.weblate.org/git/superfreezz/superfreezz/ 2>/dev/null
git pull weblate master

echo "Getting the list of apps on F-Droid"
wget https://f-droid.org/repo/index-v1.jar
unzip index-v1.jar -x "M*"
python3 <<EOF >src/superfreeze/tool/android/database/FDroidPackages.kt
import re
index = open("index-v1.json").read()
list = re.findall(r'"packageName": "([\w\.]+)"', index)
packages = set(list)
print("package superfreeze.tool.android.database")
print()
print('val fDroidPackages = hashSetOf(')
for p in packages:
    print('"' + p + '",')
print('"superfreeze.tool.android.debug"')
print(')')
EOF
rm index-v1.jar index-v1.json
git add src/superfreeze/tool/android/database/FDroidPackages.kt

echo
echo "+git status"
git status
echo

echo -n 'new versionCode="..." (integer) '
read vCode
echo -n 'new versionName="..." (string) '
read vName

echo "Change versionCode and versionName in AndroidManifest.xml, then press enter"
read
echo "+git add AndroidManifest.xml"
git add AndroidManifest.xml || git add app/AndroidManifest.xml

# Generate release notes, let the user edit them and move them to the fastlane changelogs directory
git log $(git describe --tags --abbrev=0)..HEAD --oneline --no-decorate --no-color | cut -d' ' -f2- > F-Droid-new-version-RELEASE-NOTES.txt
nano F-Droid-new-version-RELEASE-NOTES.txt 
echo "cp F-Droid-new-version-RELEASE-NOTES.txt ./fastlane/metadata/android/en-US/changelogs/${vCode}.txt"
cp F-Droid-new-version-RELEASE-NOTES.txt "./fastlane/metadata/android/en-US/changelogs/${vCode}.txt"

echo "+git add fastlane/metadata/android/en-US/changelogs"
git add fastlane/metadata/android/en-US/changelogs/

echo "+git commit -m 'Bump version'"
git commit -m "Bump version to $vName"

echo "+git tag -a v${vName} -F F-Droid-new-version-RELEASE-NOTES.txt"
git tag -a "v${vName}" -F F-Droid-new-version-RELEASE-NOTES.txt

echo "Press enter to publish the new version."
read
echo "+git pull && git push && git push --tags"
git pull && git push && git push --tags

echo "+rm F-Droid-new-version-RELEASE-NOTES.txt"
rm F-Droid-new-version-RELEASE-NOTES.txt

echo
echo "Update to the GitHub mirror"
cd ~/.update-github-mirror-dir
git pull github master --no-edit
git pull gitlab master --no-edit
git push github
