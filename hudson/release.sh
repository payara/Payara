#!/bin/bash -e

source `dirname $0`/common.sh
init_release_version

######################
# PROCESSES CLEANUPS #
######################

kill_clean(){ if [ ${#1} -ne 0 ] ; then kill -9 $1 ; fi }
kill_clean `jps | grep ASMain | awk '{print $1}'`
for i in `ps -auwwx | grep "depot.py" | grep -v grep | awk '{print $2}'`
do
    kill_clean $i
done

####################
# MAVEN_OPTS SETUP #
####################

MAVEN_OPTS="\
    -Xmx3G \
    -Xms256m \
    -XX:MaxPermSize=512m \
    -XX:-UseGCOverheadLimit"

if [ ! -z $PROXY_HOST ] && [ ! -z $PROXY_PORT ]
then
    MAVEN_OPTS="$MAVEN_OPTS \
        -Dhttp.proxyHost=$PROXY_HOST \
        -Dhttp.proxyPort=$PROXY_PORT \
        -Dhttp.noProxyHosts='127.0.0.1|localhost|*.oracle.com' \
        -Dhttps.proxyHost=$PROXY_HOST \
        -Dhttps.proxyPort=$PROXY_PORT \
        -Dhttps.noProxyHosts='127.0.0.1|localhost|*.oracle.com'"
fi
export MAVEN_OPTS

##########################
# PRINT ENVIRONMENT INFO #
##########################

printf "\n%s \n\n" "==== ENVIRONMENT INFO ===="
pwd
uname -a
java -version
mvn --version
svn --version
printf "\n%s \n\n" "=========================="

######################
# REQUIRED VARIABLES #
######################

if [ -z $HUDSON_HOME ]
then
    printf "\n%s \n\n" "==== ERROR: HUDSON_HOME ENV VARIABLE MUST BE DEFINED ! ===="
    exit 1
else
    BUILD_ID=`cat $HUDSON_HOME/promote-trunk.version`
    PKG_ID=`cat $HUDSON_HOME/pkgid-trunk.version`
fi

if [ -z $GPG_PASSPHRASE ]
then
    printf "\n%s \n\n" "==== ERROR: GPG_PASSPHRASE ENV VARIABLE MUST BE DEFINED ! ===="
    exit 1
fi

####################
# GET SVN REVISION #
####################


printf "\n%s\n\n" "==== VERSION INFO ===="

# can be a build parameter
# value can be either "HEAD" or an SVN revision
if [ ! -z $SYNCHTO ] && [ ${#SYNCHTO} -gt 0 ]
then
    printf "\n%s\n\n" "Synchronizing to $SYNCHTO"
else
    svn co --depth=files $GF_WORKSPACE_URL_SSH/trunk/main tmp
    SVN_REVISION=`svn propget svn:keyword main | grep 'clean_' | sed s@'clean_'@@g | awk '{print $2}'`
    rm -rf tmp
    printf "\n%s\n\n" "Synchronizing to the last 'good' revisions ($SVN_REVISION)"
fi

#######################
# RECORD VERSION INFO #
#######################

# create version-info.txt
echo "$GF_WORKSPACE_URL_HTTP/trunk/main $SVN_REVISION" >> $WORKSPACE/version-info.txt
echo "Maven-Version: $RELEASE_VERSION" >> $WORKSPACE/version-info.txt
cat $WORKSPACE/version-info.txt

printf "\n%s\n\n" "======================"

########################
# IPS REPOSITORY SETUP #
########################

# Increase timeout to 10 min to UC servers (default is 1 min).
PKG_CLIENT_CONNECT_TIMEOUT=600 ; export PKG_CLIENT_CONNECT_TIMEOUT
PKG_CLIENT_READ_TIMEOUT=600 ; export PKG_CLIENT_READ_TIMEOUT

# INSTALL IPS TOOLKIT
IPS_TOOLKIT_ZIP=pkg-toolkit-$UC2_BUILD-$REPO_TYPE.zip
curl $UC_HOME_URL/$IPS_TOOLKIT_ZIP > $IPS_TOOLKIT_ZIP
unzip $IPS_TOOLKIT_ZIP
IPS_TOOLKIT=$WORKSPACE/pkg-toolkit-$REPO_TYPE ; export IPS_TOOLKIT

# enforce usage of bundled python
PYTHON_HOME=$IPS_TOOLKIT/pkg/python2.4-minimal; export PYTHON_HOME
LD_LIBRARY_PATH=$PYTHON_HOME/lib ; export LD_LIBRARY_PATH
PATH=$PYTHON_HOME/bin:$IPS_TOOLKIT/pkg/bin:$PATH; export PATH

# start the repository
$IPS_TOOLKIT/pkg/bin/pkg.depotd \
    -d $REPO_DIR \
    -p $REPO_PORT \
    > $REPO_DIR/repo.log 2>&1 &

###############
# BUILD PHASE #
###############

svn delete $GF_WORKSPACE_URL_SSH/tags/$RELEASE_VERSION -m "delete tag $RELEASE_VERSION"
svn checkout $GF_WORKSPACE_URL_SSH/trunk/main -r $SVN_REVISION

MAVEN_REPO="-Dmaven.repo.local=$WORKSPACE/repository"
MAVEN_ARGS="$MAVEN_REPO -C -nsu -B"

# update poms
mvn $MAVEN_ARGS -f main/pom.xml release:prepare \
                          -Dtag=$RELEASE_VERSION \
                          -Drelease-phase-all=true \
                          -Darguments="$MAVEN_REPO" \
                          -DreleaseVersion=$RELEASE_VERSION \
                          -DpreparationGoals="A_NON_EXISTENT_GOAL"  \
                          2>&1 | tee mvn_rel.output

egrep "Unknown lifecycle phase \"A_NON_EXISTENT_GOAL\"" mvn_rel.output
if [ $? -ne 0 ]; then 
   echo "Unable to perform release using maven release plugin.  Please see $WORKSPACE/mvn_rel.output."
   exit 1; 
fi

# do the build !
mvn $MAVEN_ARGS -f main/pom.xml clean deploy \
    -Prelease-phase2,ips,embedded,javaee-api \
    -Dbuild.id=$PKG_ID
    -Dgpg.passphrase="$GPG_PASSPHRASE" \
    -Dgpg.executable=gpg2 \
    -Dmaven.test.failure.ignore=true \
    -Dtarget.repo.dir=$REPO_DIR \
    -Duc.toolkit.dir=$IPS_TOOLKIT \
    -Drepo.url=$BUILD_REPO_URL:$REPO_PORT/ \
    -DjavadocExecutable=$HOME/jdk1.7.0_25/bin/javadoc \
    -Dpython=$PYTHON_HOME/bin/python

####################
# ARCHIVES BUNDLES #
####################

rm -rf $WORKSPACE/bundles ; mkdir $WORKSPACE/bundles

mv $WORKSPACE/svn-revisions.txt $WORKSPACE/bundles

# XXXJAVAEE cp $WORKSPACE/main/appserver/javaee-api/javax.javaee-api/target/javaee-api.jar $WORKSPACE/bundles
# XXXJAVAEE cp $WORKSPACE/main/appserver/javaee-api/javax.javaee-api/target/javaee-api-javadoc.jar $WORKSPACE/bundles
# XXXJAVAEE cp $WORKSPACE/main/appserver/javaee-api/javax.javaee-web-api/target/javaee-web-api.jar $WORKSPACE/bundles
# XXXJAVAEE cp $WORKSPACE/main/appserver/javaee-api/javax.javaee-web-api/target/javaee-web-api-javadoc.jar $WORKSPACE/bundles

cp $WORKSPACE/main/appserver/distributions/glassfish/target/*.zip $WORKSPACE/bundles
cp $WORKSPACE/main/appserver/distributions/web/target/*.zip $WORKSPACE/bundles
cp $WORKSPACE/main/nucleus/distributions/nucleus/target/*.zip $WORKSPACE/bundles
cp $WORKSPACE/main/appserver/installer/target/stage/*.sh $WORKSPACE/bundles
cp $WORKSPACE/main/appserver/installer/target/stage/*.exe $WORKSPACE/bundles

# clean and zip the workspace
mvn $MAVEN_ARGS -f main/pom.xml clean
zip $WORKSPACE/bundles/workspace.zip -r main