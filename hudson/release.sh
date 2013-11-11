#!/bin/bash -e

source `dirname $0`/common.sh

######################
# PROCESSES CLEANUPS #
######################

kill_clean `jps | grep ASMain | awk '{print $1}'`
kill_clean `ps -auwwx | grep "depot.py" | grep -v grep | awk '{print $2}'`

####################
# MAVEN_OPTS SETUP #
####################

MAVEN_OPTS="-Xmx1024M -Xms256m -XX:MaxPermSize=512m -XX:-UseGCOverheadLimit"
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

######################
# REQUIRED VARIABLES #
######################

if [ -z $HUDSON_HOME ]
then
    printf "\n%s \n\n" "==== ERROR: HUDSON_HOME ENV VARIABLE MUST BE DEFINED ! ===="
    exit 1
else
	# TODO, should be function of the VERSION_QUALIFIER
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
    SVN_REVISION=$SYNCHTO
    printf "%s\n\n" "Synchronizing to $SYNCHTO"
elif [ "nightly" == "${1}" ]
then
    TRIGGER_JOB_URL="http://${HUDSON_MASTER_HOST}/hudson/view/GlassFish/view/Trunk%20Continuous/job/gf-trunk-build-dev"
    SVN_REVISION=$(wget -q -O - "${TRIGGER_JOB_URL}/dev-build/api/xml?xpath=//changeSet/revision[1]/revision/text()")
    if [[ ! "${SVN_REVISION}" = *[[:digit:]]* ]]
    then
	echo "failed to get the svn revision from ${TRIGGER_JOB_URL}"
	exit 1
    fi
    printf "%s\n\n" "Synchronizing to ${SVN_REVISION}"
else
    svn co --depth=files $GF_WORKSPACE_URL_SSH/trunk/main tmp
    SVN_REVISION=`svn propget svn:keyword main | grep 'clean_' | sed s@'clean_'@@g | awk '{print $2}'`
    rm -rf tmp
    printf "%s\n\n" "Synchronizing to the last 'good' revisions ($SVN_REVISION)"
fi

########################
# IPS REPOSITORY SETUP #
########################

# Increase timeout to 10 min to UC servers (default is 1 min).
PKG_CLIENT_CONNECT_TIMEOUT=600 ; export PKG_CLIENT_CONNECT_TIMEOUT
PKG_CLIENT_READ_TIMEOUT=600 ; export PKG_CLIENT_READ_TIMEOUT

# INSTALL IPS TOOLKIT
IPS_TOOLKIT_ZIP=pkg-toolkit-$UC2_BUILD-$IPS_REPO_TYPE.zip

printf "\n%s \n\n" "===== DOWNLOAD IPS TOOLKIT ====="
curl $UC_HOME_URL/$IPS_TOOLKIT_ZIP > $IPS_TOOLKIT_ZIP
printf "\n%s \n\n" "===== UNZIP IPS TOOLKIT ====="
unzip -o $IPS_TOOLKIT_ZIP
IPS_TOOLKIT=$WORKSPACE/pkg-toolkit-$IPS_REPO_TYPE ; export IPS_TOOLKIT

# enforce usage of bundled python
PYTHON_HOME=$IPS_TOOLKIT/pkg/python2.4-minimal; export PYTHON_HOME
LD_LIBRARY_PATH=$PYTHON_HOME/lib ; export LD_LIBRARY_PATH
PATH=$PYTHON_HOME/bin:$IPS_TOOLKIT/pkg/bin:$PATH; export PATH

printf "\n%s \n\n" "===== START IPS REPOSITORY ====="
# start the repository
mkdir -p $IPS_REPO_DIR
$IPS_TOOLKIT/pkg/bin/pkg.depotd \
    -d $IPS_REPO_DIR \
    -p $IPS_REPO_PORT \
    > $IPS_REPO_DIR/repo.log 2>&1 &

###################
# WORKSPACE SETUP #
###################

if [ "weekly" == "${1}" ]
then
    printf "\n%s \n\n" "===== DELETE TAG ====="
    set +e
    svn delete $GF_WORKSPACE_URL_SSH/tags/$RELEASE_VERSION -m "delete tag $RELEASE_VERSION"
    set -e
fi

printf "\n%s \n\n" "===== CHECKOUT ====="
svn checkout $GF_WORKSPACE_URL_SSH/trunk/main -r $SVN_REVISION

# create version-info.txt
# TODO, put env desc
# OS, arch, build node, build time, mvn version, jdk version
EFFECTIVE_REVISION=`svn info $WORKSPACE/tag/main | grep 'Revision:' | awk '{print $2}'`
echo "$GF_WORKSPACE_URL_HTTP/trunk/main $EFFECTIVE_REVISION" >> $WORKSPACE/version-info.txt
echo "Maven-Version: $RELEASE_VERSION" >> $WORKSPACE/version-info.txt
cat $WORKSPACE/version-info.txt

###############
# BUILD PHASE #
###############

MAVEN_REPO="-Dmaven.repo.local=$WORKSPACE/repository"
MAVEN_ARGS="$MAVEN_REPO -C -nsu -B"

if [ "weekly" == "${1}" ]
then

    printf "\n%s \n\n" "===== UPDATE POMS ====="
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
fi

printf "\n%s \n\n" "===== DO THE BUILD! ====="
unset BUILD_TARGETS MVN_PROFILES
if [ "weekly" == "${1}" ]
then
    BUILD_TARGETS="clean deploy"
    MVN_PROFILES="release-phase2,ips,embedded,javaee-api"
elif [ "nightly" == "${1}" ]
then
    BUILD_TARGETS="clean install"
    MVN_PROFILES="ips,javaee-api"
fi

mvn $MAVEN_ARGS -f main/pom.xml ${BUILD_TARGETS} \
    -P${MVN_PROFILES} \
    -Dbuild.id=$PKG_ID \
    -Dgpg.passphrase="$GPG_PASSPHRASE" \
    -Dgpg.executable=gpg2 \
    -Dmaven.test.failure.ignore=true \
    -Dtarget.repo.dir=$IPS_REPO_DIR \
    -Duc.toolkit.dir=$IPS_TOOLKIT \
    -Drepo.url=$IPS_REPO_URL:$IPS_REPO_PORT/ \
    -DjavadocExecutable=$HOME/jdk1.7.0_25/bin/javadoc \
    -Dpython=$PYTHON_HOME/bin/python

####################
# ARCHIVES BUNDLES #
####################

printf "\n%s \n\n" "===== ARCHIVE BUNDLES ====="
rm -rf $WORKSPACE/bundles ; mkdir $WORKSPACE/bundles

mv $WORKSPACE/version-info.txt $WORKSPACE/bundles
cp $WORKSPACE/main/appserver/distributions/glassfish/target/*.zip $WORKSPACE/bundles
cp $WORKSPACE/main/appserver/distributions/web/target/*.zip $WORKSPACE/bundles
cp $WORKSPACE/main/nucleus/distributions/nucleus/target/*.zip $WORKSPACE/bundles

# clean and zip the workspace
printf "\n%s \n\n" "===== ZIP THE WORKSPACE ====="
svn status main | grep ? | awk '{print $2}' | xargs rm -rf
zip $WORKSPACE/bundles/workspace.zip -r main
