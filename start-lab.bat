ECHO "Getting your environment setup"
cd c:/cmx-android/cmx-android-demo
git fetch origin
git reset --hard origin/master
git clean -d -f
git checkout .
git pull origin master