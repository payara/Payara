#!/bin/bash -e
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License.  You can
# obtain a copy of the License at
# https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
# or packager/legal/LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at packager/legal/LICENSE.txt.
#
# GPL Classpath Exception:
# Oracle designates this particular file as subject to the "Classpath"
# exception as provided by Oracle in the GPL Version 2 section of the License
# file that accompanied this code.
#
# Modifications:
# If applicable, add the following below the License Header, with the fields
# enclosed by brackets [] replaced by your own identifying information:
# "Portions Copyright [year] [name of copyright owner]"
#
# Contributor(s):
# If you wish your version of this file to be governed by only the CDDL or
# only the GPL Version 2, indicate your decision by adding "[Contributor]
# elects to include this software in this distribution under the [CDDL or GPL
# Version 2] license."  If you don't indicate a single choice of license, a
# recipient has the option to distribute your version of this file under
# either the CDDL, the GPL Version 2 or to extend the choice of license to
# its licensees as provided above.  However, if you add GPL Version 2 code
# and therefore, elected the GPL Version 2 license, then the option applies
# only if the new code is made subject to such option by the copyright
# holder.
#

# REQUIRED VARIABLES

#RE_USER=
#HUDSON_MASTER_HOST=
#STORAGE_HOST=
#JNET_USER=
#JNET_STORAGE_HOST=
#STORAGE_HOST_HTTP=
#WORKSPACE=
#NOTIFICATION_SENDTO
#NOTIFICATION_FROM
#GPG_PASSPHRASE
#HUDSON_HOME

build_init(){
    init_common
    kill_glassfish
    print_env_info
    get_svn_rev
    create_version_info
}

build_re_init(){
    build_init
    kill_ips_repo
    install_uc_toolkit
    start_ips_repository
}

build_re_finalize(){
    archive_bundles
    zip_tests_workspace
    zip_tests_maven_repo
}

build_re_dev(){
    build_init
    dev_build
    build_re_finalize
}

build_re_nightly(){
    export BUILD_KIND="nightly"
    build_re_init
    init_nightly
    svn_checkout ${SVN_REVISION}
    run_findbugs
    release_build "clean install" "ips,javaee-api"
    build_re_finalize
}

build_re_weekly(){
    export BUILD_KIND="weekly"
    build_re_init
    init_weekly
    delete_svn_tag ${RELEASE_VERSION}
    svn_checkout ${SVN_REVISION}
    release_prepare
    release_build "clean deploy" "release-phase2,ips,embedded,javaee-api"
    build_re_finalize
    clean_and_zip_workspace
}

promote_init(){
    init_common
    if [ "nightly" == "${1}" ]
    then
		init_nightly
    elif [ "weekly" == "${1}" ]
    then
		init_weekly
    fi

    export PROMOTION_SUMMARY=${WORKSPACE_BUNDLES}/${BUILD_KIND}-promotion-summary.txt
    rm -f $PROMOTION_SUMMARY
    export JNET_DIR=${JNET_USER}@${JNET_STORAGE_HOST}:/dlc/${ARCHIVE_PATH}
    export JNET_DIR_HTTP=http://download.java.net/${ARCHIVE_PATH}
    export ARCHIVE_STORAGE_BUNDLES=/java/re/${ARCHIVE_MASTER_BUNDLES}
    export SSH_MASTER=${RE_USER}@${HUDSON_MASTER_HOST}
    export SSH_STORAGE=${RE_USER}@${STORAGE_HOST}
    export SCP=${SSH_STORAGE}:${ARCHIVE_STORAGE_BUNDLES}
    export ARCHIVE_URL=http://${STORAGE_HOST_HTTP}/java/re/${ARCHIVE_MASTER_BUNDLES}

    init_storage_area
}

promote_finalize(){
    create_symlinks
    send_notification
}

purge_old_nightlies(){
#########################
# PURGE OLDER NIGHTLIES #
#########################

    rm -rf /tmp/purgeNightlies.sh
    cat <<EOF > /tmp/purgeNightlies.sh
#!/bin/bash
# Max builds to keep around
    MAX_BUILDS=21
    cd \$1
    LISTING=\`ls -trd b*\`
    nbuilds=0
    for i in \$LISTING; do
	nbuilds=\`expr \$nbuilds + 1\`
    done
    echo "Total number of builds is \$nbuilds"
    
    while [ \$nbuilds -gt \$MAX_BUILDS ]; do
	oldest_dir=\`ls -trd b* | head -n1\`
	echo "rm -rf \$oldest_dir"
	rm -rf \$oldest_dir
	nbuilds=\`expr \$nbuilds - 1\`
	echo "Number of builds is now \$nbuilds"
    done    
EOF
    ssh ${SSH_MASTER} "rm -rf /tmp/purgeNightlies.sh"
    scp /tmp/purgeNightlies.sh ${SSH_MASTER}:/tmp
    ssh ${SSH_MASTER} "chmod +x /tmp/purgeNightlies.sh ; bash -e /tmp/purgeNightlies.sh /java/re/${ARCHIVE_PATH}"
}

promote_nightly(){
    promote_init "nightly"
    promote_bundle ${PROMOTED_BUNDLES}/web-ips.zip ${PRODUCT_GF}-${PRODUCT_VERSION_GF}-web-${BUILD_ID}-${MDATE}.zip
    promote_bundle ${PROMOTED_BUNDLES}/glassfish-ips.zip ${PRODUCT_GF}-${PRODUCT_VERSION_GF}-${BUILD_ID}-${MDATE}.zip
    promote_bundle ${PROMOTED_BUNDLES}/nucleus-new.zip nucleus-${PRODUCT_VERSION_GF}-${BUILD_ID}-${MDATE}.zip
    promote_bundle ${PROMOTED_BUNDLES}/version-info.txt version-info-${PRODUCT_VERSION_GF}-${BUILD_ID}-${MDATE}.txt
    promote_bundle ${PROMOTED_BUNDLES}/changes.txt changes-${PRODUCT_VERSION_GF}-${BUILD_ID}-${MDATE}.txt
    VERSION_INFO="${WORKSPACE_BUNDLES}/version-info-${PRODUCT_VERSION_GF}-${BUILD_ID}-${MDATE}.txt"
    SVN_REVISION=`head -1 ${VERSION_INFO} | awk '{print $2}'`
    #record_svn_rev ${SVN_REVISION}
    purge_old_nightlies
    # hook for the docker image of the nightly
    curl -H "Content-Type: application/json" \
        --data '{"build": true}' \
        -X POST \
        -k \
        https://registry.hub.docker.com/u/glassfish/nightly/trigger/945d55fc-1d4c-4043-8221-74185d9a4d53/
    promote_finalize
}

promote_weekly(){
    promote_init "weekly"
    promote_bundle ${PROMOTED_BUNDLES}/web-ips.zip ${PRODUCT_GF}-${PRODUCT_VERSION_GF}-web-${BUILD_ID}.zip
    promote_bundle ${PROMOTED_BUNDLES}/glassfish-ips.zip ${PRODUCT_GF}-${PRODUCT_VERSION_GF}-${BUILD_ID}.zip
    promote_bundle ${PROMOTED_BUNDLES}/nucleus-new.zip nucleus-${PRODUCT_VERSION_GF}-${BUILD_ID}.zip
    promote_bundle ${PROMOTED_BUNDLES}/version-info.txt version-info-${PRODUCT_VERSION_GF}-${BUILD_ID}.txt
    #promote_bundle ${PROMOTED_BUNDLES}/changes.txt changes-${PRODUCT_VERSION_GF}-${BUILD_ID}.txt
    VERSION_INFO="${WORKSPACE_BUNDLES}/version-info-${PRODUCT_VERSION_GF}-${BUILD_ID}.txt"
    create_svn_tag

    # increment build value in both promote-trunk.version and pkgid-trunk.version
    # only when build parameter RELEASE_VERSION has been resolved (i.e not provided explicitly).
    if [ ! -z $INCREMENT_BUILD_ID ]
    then    
        BUILD_ID=`cat ${HUDSON_HOME}/promote-trunk.version`
        PKG_ID=`cat ${HUDSON_HOME}/pkgid-trunk.version`    
        NEXT_ID=$((PKG_ID+1))

        # prepend a 0 if less than 10
        if [ $NEXT_ID -lt 10 ]
        then
            NEXT_BUILD_ID="b0$NEXT_ID"
        else
            NEXT_BUILD_ID="b$NEXT_ID"
        fi
        ssh $SSH_MASTER `echo "echo $NEXT_BUILD_ID > /scratch/java_re/hudson/hudson_install/promote-trunk.version"`
        ssh $SSH_MASTER `echo "echo $NEXT_ID > /scratch/java_re/hudson/hudson_install/pkgid-trunk.version"`
    fi

    promote_finalize
}

promote_dev(){
    BUILD_KIND="dev"
    init_common
    mkdir -p ${WORKSPACE}/dev-bundles
    curl ${PROMOTED_BUNDLES}/version-info.txt > ${WORKSPACE}/dev-bundles/version-info.txt
    SVN_REVISION=`cat ${WORKSPACE}/dev-bundles/version-info.txt | head -1 | awk '{print $2}'`
    record_svn_rev ${SVN_REVISION}
}

init_weekly(){
    BUILD_KIND="weekly"
    require_env_var "GPG_PASSPHRASE"

    ARCHIVE_PATH=${PRODUCT_GF}/${PRODUCT_VERSION_GF}
    if [ ${#BUILD_ID} -gt 0 ]
    then
        ARCHIVE_PATH=${ARCHIVE_PATH}/promoted
    else
        ARCHIVE_PATH=${ARCHIVE_PATH}/release
    fi
    ARCHIVE_MASTER_BUNDLES=${ARCHIVE_PATH}/${BUILD_ID}/archive/bundles
    export BUILD_ID BUILD_KIND ARCHIVE_PATH ARCHIVE_MASTER_BUNDLES
    init_bundles_dir
    init_version 
}

init_nightly(){
    BUILD_KIND="nightly"
    ARCHIVE_PATH=${PRODUCT_GF}/${PRODUCT_VERSION_GF}/nightly
    ARCHIVE_MASTER_BUNDLES=${ARCHIVE_PATH}/${BUILD_ID}-${MDATE}
    export BUILD_KIND ARCHIVE_PATH ARCHIVE_MASTER_BUNDLES
    init_bundles_dir
    init_version
}

init_common(){
    require_env_var "HUDSON_HOME"
    BUILD_ID=`cat ${HUDSON_HOME}/promote-trunk.version`
    PKG_ID=`cat ${HUDSON_HOME}/pkgid-trunk.version`

    PRODUCT_GF="glassfish"
    JAVAEE_VERSION=7.0
    MAJOR_VERSION=4
    MINOR_VERSION=1
    PRODUCT_VERSION_GF=${MAJOR_VERSION}.${MINOR_VERSION}
    #MICRO_VERSION=
    if [ ! -z $MICRO_VERSION ] && [ ${#MICRO_VERSION} -gt 0 ]; then
        PRODUCT_VERSION_GF=$PRODUCT_VERSION_GF.${MICRO_VERSION} 
    fi

    PROMOTED_JOB_URL=${HUDSON_URL}/job/${PROMOTED_JOB_NAME}/${PROMOTED_NUMBER}
    PROMOTED_BUNDLES=${PROMOTED_JOB_URL}/artifact/bundles/
    GF_WORKSPACE_URL_SSH=svn+ssh://${RE_USER}@svn.java.net/glassfish~svn
    GF_WORKSPACE_URL_HTTP=https://svn.java.net/svn/glassfish~svn

    IPS_REPO_URL=http://localhost
    IPS_REPO_DIR=${WORKSPACE}/promorepo
    IPS_REPO_PORT=16500
    IPS_REPO_TYPE=sunos-sparc
    UC2_VERSION=2.3
    UC2_BUILD=57
    UC_HOME_URL=http://${STORAGE_HOST_HTTP}/java/re/updatecenter/${UC2_VERSION}
    UC_HOME_URL="${UC_HOME_URL}/promoted/B${UC2_BUILD}/archive/uc2/build"
    MDATE=$(date +%m_%d_%Y)
    DATE=$(date)

    MAVEN_REPO="-Dmaven.repo.local=${WORKSPACE}/repository"
    MAVEN_ARGS="${MAVEN_REPO} -C -nsu -B"
    MAVEN_OPTS="-Xmx1024M -Xms256m -XX:MaxPermSize=512m -XX:-UseGCOverheadLimit"
    if [ ! -z ${PROXY_HOST} ] && [ ! -z ${PROXY_PORT} ]
    then
        MAVEN_OPTS="${MAVEN_OPTS} \
    -Dhttp.proxyHost=$PROXY_HOST \
    -Dhttp.proxyPort=$PROXY_PORT \
    -Dhttp.noProxyHosts='127.0.0.1|localhost|*.oracle.com' \
    -Dhttps.proxyHost=${PROXY_HOST} \
    -Dhttps.proxyPort=${PROXY_PORT} \
    -Dhttps.noProxyHosts='127.0.0.1|localhost|*.oracle.com'"
    fi

    if [ -z $WORKSPACE ]
    then
        WORKSPACE=$PWD
    fi

    export JAVAEE_VERSION \
            MAJOR_VERSION \
            MINOR_VERSION \
            MICRO_VERSION \
            VERSION \
            BUILD_ID \
            PKG_ID \
            RELEASE_VERSION \
            PRODUCT_GF \
            PRODUCT_VERSION_GF \
            MDATE \
            DATE \
            IPS_REPO_URL \
            IPS_REPO_DIR \
            IPS_REPO_PORT \
            IPS_REPO_TYPE \
            PROMOTED_BUNDLES \
            GF_WORKSPACE_URL_SSH \
            GF_WORKSPACE_URL_HTTP \
            MAVEN_OPTS \
            MAVEN_REPO \
            MAVEN_ARGS \
            WORKSPACE
}

init_version(){
    # PRODUCT_GF_VERSION - next version to be released (e.g 4.1)
    # RELEASE_VERSION - used by the Maven release builds
    # VERSION - main version information (used for notification)

	if [ "${BUILD_KIND}" = "weekly" ] ; then
	    # retrieving version-info.txt if promoting a weekly
	    # to resolve value of RELEASE_VERSION
	    if [ "${BUILD_KIND}" = "weekly" ] &&  [ -z ${RELEASE_VERSION} ]
	    then
    		curl ${PROMOTED_BUNDLES}/version-info.txt > ${WORKSPACE_BUNDLES}/version-info.txt	
    		RELEASE_VERSION=`grep 'Maven-Version' ${WORKSPACE_BUNDLES}/version-info.txt | awk '{print $2}'`
    		INCREMENT_BUILD_ID=true
    		rm -f ${WORKSPACE_BUNDLES}/version-info.txt
	    fi
		
	    # deduce BUILD_ID and PRODUCT_VERSION_GF
	    # from the value of RELEASE_VERSION
	    if [ ! -z ${RELEASE_VERSION} ] && [ ${#RELEASE_VERSION} -gt 0 ]
	    then
            IS_NON_FINAL_VERSION=`grep '-'  <<< ${RELEASE_VERSION} | wc -l | awk '{print $1}'`
            if [ ${IS_NON_FINAL_VERSION} -gt 0 ]; then
                # PROMOTED BUILD
                BUILD_ID=`cut -d '-' -f2- <<< ${RELEASE_VERSION}`
                PKG_ID=`sed -e s@"b0"@@g -e s@"b"@@g <<< ${BUILD_ID}`
                PRODUCT_VERSION_GF=`sed s@"-${BUILD_ID}"@@g <<< ${RELEASE_VERSION}`
            else
                # RELEASE BUILD
                PRODUCT_VERSION_GF="${RELEASE_VERSION}"
            fi
            VERSION=${RELEASE_VERSION}
	    else
	        printf "\n==== ERROR: %s RELEASE_VERSION must be defined with a non empty value ! ==== \n\n" "${1}"
	        exit 1
	    fi
    else
        if [ "${BUILD_KIND}" = "nightly" ] ; then
            VERSION="${PRODUCT_VERSION_GF}-${BUILD_ID}-${MDATE}"
        else
            VERSION="${PRODUCT_VERSION_GF}-${BUILD_ID}-${USER}"
        fi
	fi
    export VERSION

    printf "\n%s \n\n" "===== VERSION VALUES ====="
    printf "VERSION=%s \nBUILD_ID=%s \nPKG_ID=%s\n\n" \
        "${VERSION}" \
        "${BUILD_ID}" \
        "${PKG_ID}"
}

init_bundles_dir(){
    WORKSPACE_BUNDLES=${WORKSPACE}/${BUILD_KIND}_bundles
    if [ ! -d "${WORKSPACE_BUNDLES}" ]
    then
        mkdir -p "${WORKSPACE_BUNDLES}"
    fi
    export WORKSPACE_BUNDLES
}

require_env_var(){
    var=`eval echo '\$'"$1"`
    if [ ${#var} -eq 0 ]
    then
        printf "\n==== ERROR: %s VARIABLE MUST BE DEFINED ! ==== \n\n" "${1}"
        exit 1
    fi
}

kill_clean(){
    if [ ${#1} -ne 0 ]
    then
        kill -9 ${1} || true
    fi
}

kill_glassfish(){
    kill_clean `jps | grep ASMain | awk '{print $1}'`
}

kill_ips_repo(){
    kill_clean `ps -auwwx | grep "depot.py" | grep -v grep | awk '{print $2}'`
}

print_env_info(){
    printf "\n%s \n\n" "==== ENVIRONMENT INFO ===="
    pwd
    uname -a
    java -version
    mvn --version
    svn --version
}

dev_build(){
    printf "\n%s \n\n" "===== DO THE BUILD! ====="
    mvn ${MAVEN_ARGS} -f main/pom.xml clean install \
        -Dmaven.test.failure.ignore=true
}

release_build(){
    printf "\n%s \n\n" "===== DO THE BUILD! ====="
    mvn ${MAVEN_ARGS} -f main/pom.xml ${1} \
        -P${2} \
        -Dbuild.id=${PKG_ID} \
        -Dgpg.passphrase="${GPG_PASSPHRASE}" \
        -Dgpg.executable=gpg2 \
        -Dmaven.test.failure.ignore=true \
        -Dips.compress=false \
        -Dips.build.installer=false \
        -Dtarget.repo.dir=${IPS_REPO_DIR} \
        -Duc.toolkit.dir=${IPS_TOOLKIT} \
        -Drepo.url=${IPS_REPO_URL}:${IPS_REPO_PORT}/ \
        -DjavadocExecutable=${HOME}/jdk1.7.0_25/bin/javadoc \
        -Dpython=${PYTHON_HOME}/bin/python
}

release_prepare(){
    printf "\n%s \n\n" "===== UPDATE POMS ====="
    mvn ${MAVEN_ARGS} -f main/pom.xml release:prepare \
        -Dtag=${RELEASE_VERSION} \
        -Drelease-phase-all=true \
        -Darguments="${MAVEN_REPO}" \
        -DreleaseVersion="${RELEASE_VERSION}" \
        -DpreparationGoals="A_NON_EXISTENT_GOAL"  \
        2>&1 | tee mvn_rel.output

    egrep "Unknown lifecycle phase \"A_NON_EXISTENT_GOAL\"" mvn_rel.output
    if [ $? -ne 0 ]; then
	echo "Unable to perform release using maven release plugin."
        echo "Please see ${WORKSPACE}/mvn_rel.output."
	exit 1;
    fi
}

run_findbugs(){
    printf "\n%s \n\n" "===== RUN FINDBUGS ====="
    mvn ${MAVEN_ARGS} -f main/pom.xml install findbugs:findbugs

    printf "\n%s \n\n" "===== PROCESS FINDBUGS RESULTS ====="
    FINDBUGS_RESULTS=${WORKSPACE}/findbugs_results
    mkdir ${FINDBUGS_RESULTS} | true

    # run findbugs-tool
    OLD_PWD=`pwd`
    cd ${HUDSON_HOME}/tools/findbugs-tool-latest
    ./findbugscheck ${WORKSPACE}/main
    if [ $? -ne 0 ]
    then
       echo "FAILED" > ${FINDBUGS_RESULTS}/findbugscheck.log
    else
       echo "SUCESS" > ${FINDBUGS_RESULTS}/findbugscheck.log
    fi
    cd ${OLD_PWD}

    # copy all results
    for i in `find ${WORKSPACE}/main -name findbugsXml.xml`
    do
       target=`sed s@"${WORKSPACE}"@@g | sed s@"/"@"_"@g <<< ${i}`
       cp ${i} ${FINDBUGS_RESULTS}/${target}
    done
}

get_svn_rev(){
    # can be a build parameter
    # value can be either "HEAD" or an SVN revision
    if [ ! -z ${SYNCHTO} ] && [ ${#SYNCHTO} -gt 0 ]
    then
        SVN_REVISION=${SYNCHTO}
    elif [ "${BUILD_KIND}" == "nightly" ]
    then
        export triggering_build_url=$(wget -q -O - "${TRIGGER_JOB_URL}/dev-build/api/xml?xpath=//url/text()")
    fi

    if [ -z ${SVN_REVISION} ] \
        || [ `grep -i 'head' <<< "${SVN_REVISION}" | wc -l | awk '{print $1}'` -eq 1 ]
    then
        svn co --depth=files ${GF_WORKSPACE_URL_SSH}/trunk/main tmp

        # if not defined, we are looking for last known good revisions
        if [ -z ${SVN_REVISION} ]
        then
            SVN_REVISION=`get_clean_svn_rev tmp`
        fi

        # if still not defined or empty or equal to 'head' or 'HEAD'
        # we retrieve the current HEAD's value
        if [ -z ${SVN_REVISION} ] || \
           [ ${#SVN_REVISION} -eq 0 ] || \
           [ `grep -i 'head' <<< "${SVN_REVISION}" | wc -l | awk '{print $1}'` -eq 1 ]
        then
            SVN_REVISION=`get_current_svn_rev tmp`
        fi
        rm -rf tmp
    fi
    export SVN_REVISION
}

get_svn_rev_from_version_info(){
    export SVN_REVISION=`cat ${VERSION_INFO} | head -1 | awk '{print $2}'`
}

record_svn_rev(){
    printf "\n%s \n\n" "===== RECORD CLEAN REVISION ====="

    if [ -z ${1} ] || [ ${#1} -eq 0 ]
    then
        printf "\n%s \n\n" "===== ERROR: revision supplied to record_svn_rev function is not set or empty ====="
        exit 1
    fi  

    svn co --depth=empty ${GF_WORKSPACE_URL_SSH}/trunk/main tmp-co

    COMMIT_MSG="setting clean revision"
    LOG=`svn propget svn:log --revprop -r ${1} tmp-co`

    # record one clean_revision only once !
    if [ "${LOG}" != "${COMMIT_MSG}" ] && [ "${1}" != "`get_clean_svn_rev tmp-co`" ]
    then
        echo ${1} > svn-keywords
        svn propset -F svn-keywords svn:keyword tmp-co
        svn commit ${WORKSPACE}/tmp-co -m "${COMMIT_MSG}"
        rm -rf tmp-co svn-keywords
    else
        echo "Nothing to do. Current clean_revision is already recorded"
        exit 0
    fi
}

get_clean_svn_rev(){
    svn propget svn:keyword ${1} | head -1 | awk '{print $1}'
}

get_current_svn_rev(){
    svn info ${1} | grep 'Revision:' | awk '{print $2}'
}

delete_svn_tag(){
    printf "\n%s \n\n" "===== DELETE TAG ====="
    svn delete ${GF_WORKSPACE_URL_SSH}/tags/${1} -m "delete tag ${1}" | true
}

svn_checkout(){
    printf "\n%s \n\n" "===== CHECKOUT ====="
    svn checkout ${GF_WORKSPACE_URL_SSH}/trunk/main -r ${1}
}

create_version_info(){
    # create version-info.txt
    # TODO, put env desc
    # OS, arch, build node, mvn version, jdk version
    echo "${GF_WORKSPACE_URL_HTTP}/trunk/main ${SVN_REVISION}" > ${WORKSPACE}/version-info.txt
    
    # RELEASE_VERSION has not be provided
    # releasing next promoted build
    if [ "${BUILD_KIND}" = "weekly" ] && [ ${#RELEASE_VERSION} -eq 0 ]
    then
    	RELEASE_VERSION="${PRODUCT_VERSION_GF}-${BUILD_ID}"
    fi

    if [ ! -z ${RELEASE_VERSION} ]
    then
	   echo "Maven-Version: ${RELEASE_VERSION}" >> ${WORKSPACE}/version-info.txt
    fi

    if [ ! -z ${triggering_build_url} ]
    then
        echo "triggering_url ${triggering_build_url}" >> ${WORKSPACE}/version-info.txt
    fi

    printf "\n%s\n\n" "==== VERSION INFO ===="
    echo "Date: `date`" >> ${WORKSPACE}/version-info.txt
    cat ${WORKSPACE}/version-info.txt

    create_changes_info
}

create_changes_info(){
    curl $JOB_URL/lastSuccessfulBuild/artifact/bundles/version-info.txt > ${WORKSPACE}/previous-version-info.txt
    PREVIOUS_SVN_REV=`cat ${WORKSPACE}/previous-version-info.txt | head -1 | awk '{print $2}'`
    touch ${WORKSPACE}/changes.txt
    if [ "${PREVIOUS_SVN_REV}" != "${SVN_REVISION}" ] ; then
        PREVIOUS_SVN_REV=$((PREVIOUS_SVN_REV+1))
        svn log -r ${PREVIOUS_SVN_REV}:${SVN_REVISION} ${GF_WORKSPACE_URL_SSH}/trunk/main > ${WORKSPACE}/changes.txt
        printf "\n%s\n\n" "==== CHANGELOG ===="
        cat ${WORKSPACE}/changes.txt
    fi
}

create_svn_tag(){
    printf "\n%s \n\n" "===== CREATE SVN TAG ====="

    # download and unzip the workspace
    curl $PROMOTED_BUNDLES/workspace.zip > ${WORKSPACE_BUNDLES}/workspace.zip
    rm -rf ${WORKSPACE}/tag ; unzip -d ${WORKSPACE}/tag ${WORKSPACE_BUNDLES}/workspace.zip

    # delete tag (for promotion forcing)
    svn del ${GF_WORKSPACE_URL_SSH}/tags/${RELEASE_VERSION} -m "del tag ${RELEASE_VERSION}" | true

    # copy the exact trunk used to run the release
    SVN_REVISION=`svn info ${WORKSPACE}/tag/main | grep 'Revision:' | awk '{print $2}'`
    svn cp ${GF_WORKSPACE_URL_SSH}/trunk/main@${SVN_REVISION} ${GF_WORKSPACE_URL_SSH}/tags/${RELEASE_VERSION} -m "create tag ${RELEASE_VERSION} based on r${SVN_REVISION}"

    # switch the workspace
    svn switch ${GF_WORKSPACE_URL_SSH}/tags/${RELEASE_VERSION} ${WORKSPACE}/tag/main

    # commit the local changes
    svn commit ${WORKSPACE}/tag/main -m "commit tag ${RELEASE_VERSION}"
}

install_uc_toolkit(){
    IPS_TOOLKIT_ZIP=pkg-toolkit-${UC2_VERSION}-b${UC2_BUILD}-${IPS_REPO_TYPE}.zip

    printf "\n%s \n\n" "===== DOWNLOAD IPS TOOLKIT ====="
    curl ${UC_HOME_URL}/${IPS_TOOLKIT_ZIP} > ${IPS_TOOLKIT_ZIP}
    printf "\n%s \n\n" "===== UNZIP IPS TOOLKIT ====="
    unzip -o ${IPS_TOOLKIT_ZIP}
    IPS_TOOLKIT=${WORKSPACE}/pkg-toolkit-$IPS_REPO_TYPE ; export IPS_TOOLKIT
}

start_ips_repository(){
    # Increase timeout to 10 min to UC servers (default is 1 min).
    PKG_CLIENT_CONNECT_TIMEOUT=600 ; export PKG_CLIENT_CONNECT_TIMEOUT
    PKG_CLIENT_READ_TIMEOUT=600 ; export PKG_CLIENT_READ_TIMEOUT

    # enforce usage of bundled python
    PYTHON_HOME=${IPS_TOOLKIT}/pkg/python2.4-minimal; export PYTHON_HOME
    LD_LIBRARY_PATH=${PYTHON_HOME}/lib ; export LD_LIBRARY_PATH
    PATH=${PYTHON_HOME}/bin:${IPS_TOOLKIT}/pkg/bin:$PATH; export PATH

    printf "\n%s \n\n" "===== START IPS REPOSITORY ====="
    # start the repository
    mkdir -p ${IPS_REPO_DIR}
    ${IPS_TOOLKIT}/pkg/bin/pkg.depotd \
        -d ${IPS_REPO_DIR} \
        -p ${IPS_REPO_PORT} \
        > ${IPS_REPO_DIR}/repo.log 2>&1 &
}

archive_bundles(){
    printf "\n%s \n\n" "===== ARCHIVE BUNDLES ====="
    rm -rf ${WORKSPACE}/bundles ; mkdir ${WORKSPACE}/bundles

    mv ${WORKSPACE}/version-info.txt $WORKSPACE/bundles
    mv ${WORKSPACE}/changes.txt ${WORKSPACE}/bundles
    cp ${WORKSPACE}/main/appserver/distributions/glassfish/target/*.zip ${WORKSPACE}/bundles
    cp ${WORKSPACE}/main/appserver/distributions/web/target/*.zip ${WORKSPACE}/bundles
    cp ${WORKSPACE}/main/nucleus/distributions/nucleus/target/*.zip ${WORKSPACE}/bundles
}

clean_and_zip_workspace(){
    printf "\n%s \n\n" "===== CLEAN AND ZIP THE WORKSPACE ====="
    svn status main | grep ? | awk '{print $2}' | xargs rm -rf
    zip ${WORKSPACE}/bundles/workspace.zip -r main
}

zip_tests_workspace(){
    printf "\n%s \n\n" "===== ZIP THE TESTS WORKSPACE ====="
    svn status main | grep ? | awk '{print $2}' | xargs rm -rf
    zip -r ${WORKSPACE}/bundles/tests-workspace.zip \
        main/nucleus/pom.xml \
        main/nucleus/tests/ \
        main/appserver/pom.xml \
        main/appserver/tests/ \
        -x *.svn/*
}

zip_tests_maven_repo(){
    printf "\n%s \n\n" "===== ZIP PART OF THE MAVEN REPO REQUIRED FOR TESTING ====="
    pushd ${WORKSPACE}/repository

    # ideally this should be done
    # from a maven plugin...
    zip -r ${WORKSPACE}/bundles/tests-maven-repo.zip \
        org/glassfish/main/common/* \
        org/glassfish/main/grizzly/* \
        org/glassfish/main/glassfish-nucleus-parent/* \
        org/glassfish/main/test-utils/* \
        org/glassfish/main/tests/* \
        org/glassfish/main/admin/* \
        org/glassfish/main/core/* \
        org/glassfish/main/deployment/deployment-common/* \
        org/glassfish/main/deployment/nucleus-deployment/* \
        org/glassfish/main/external/ldapbp-repackaged/* \
        org/glassfish/main/external/nucleus-external/* \
        org/glassfish/main/flashlight/flashlight-framework/* \
        org/glassfish/main/grizzly/grizzly-config/* \
        org/glassfish/main/grizzly/nucleus-grizzly-all/* \
        org/glassfish/main/security/security/* \
        org/glassfish/main/security/security-services/* \
        org/glassfish/main/security/ssl-impl/* \
        org/glassfish/main/security/nucleus-security/*
    popd
}

align_column(){
    max=${1}
    char=${2}
    string=${3}
    stringlength=${#string}
    y=$((max-stringlength))
    while [ $y -gt 0 ] ; do string="${string}${char}" ; y=$((y-1)) ; done
    echo "${string}"
}

aggregated_tests_summary(){
    # Hudson rest API does not give the report
    # parsing html manually...

    curl ${1} 1> tests.html 2> /dev/null

    # need to use gawk on solaris
    if [ `uname | grep -i sunos | wc -l` -eq 1 ]
    then
        AWK="gawk"
    else
        AWK="awk"
    fi

    for i in `${AWK} 'BEGIN{RS="<tr><td class=\"pane"} \
         { if (NR > 2) print $2" "$8" "$11 }' \
         tests.html | sed \
          -e s@'"'@@g \
          -e s@'href=/hudson/job/'@@g \
          -e s@'/testReport/><img style=text-align:right>'@'#'@g \
          -e s@'/aggregatedTestReport/><img style=text-align:right>'@'#'@g \
          -e s@' style=text-align:right>'@'#'@g \
          -e s@'/'@'#'@g`
    do
        jobname=`cut -d '#' -f1 <<< $i`
        buildnumber=`cut -d '#' -f2 <<< $i`
        failednumber=`cut -d '#' -f3 <<< $i`
        totalnumber=`cut -d '#' -f4 <<< $i`
        passednumber=$((totalnumber-failed))
    printf "%s%s%s%s\n" \
            `align_column 55 "." "$jobname (#${buildnumber})"` \
            `align_column 15 "." "PASSED(${passednumber})"` \
            "FAILED(${failednumber})"
    done
    rm tests.html
}

create_symlinks(){
    PROMOTE_SCRIPT=/tmp/promotebuild.sh
    cat <<EOF > ${PROMOTE_SCRIPT}
#!/bin/bash -e
# arg1 BUILD_ID
# arg2 PRODUCT_VERSION_GF
# arg3 ARCHIVE_MASTER_BUNDLES
# arg4 JAVAEE_VERSION

cd \$3
rm -rf latest-*

for i in \`ls\`
do
    simple_name=\`echo \${i} | \
        sed -e s@"-\${1}"@@g \
        -e s@"-\${2}"@@g \
        -e s@"--"@"-"@g\` 
    
    ln -fs \${i} "latest-\${simple_name}"
    if [ "\${simple_name}" == "glassfish-ml.zip" ]
    then
        ln -fs \${i} "latest-glassfish.zip"
    fi
    if [ "\${simple_name}" == "web-ml.zip" ]
    then
        ln -fs \${i} "latest-web.zip"
    fi
done

cd /java/re/\${5}
rm -rf latest
ln -s \${1} latest
EOF
    echo "trying to create symlink"
    scp ${PROMOTE_SCRIPT} ${SSH_MASTER}:/tmp
    ssh ${SSH_MASTER} "chmod +x ${PROMOTE_SCRIPT}"
    if [ "weekly" == "${BUILD_KIND}" ]
    then
	    ssh ${SSH_MASTER} \
            "${PROMOTE_SCRIPT} ${BUILD_ID} ${PRODUCT_VERSION_GF} /java/re/${ARCHIVE_MASTER_BUNDLES} ${JAVAEE_VERSION} ${ARCHIVE_PATH}"
    elif [ "nightly" == "${BUILD_KIND}" ]
    then
	    echo "ssh ${SSH_MASTER}  ${PROMOTE_SCRIPT} ${BUILD_ID}-${MDATE} ${PRODUCT_VERSION_GF} /java/re/${ARCHIVE_MASTER_BUNDLES} ${JAVAEE_VERSION} ${ARCHIVE_PATH}"
	    ssh ${SSH_MASTER} \
            "${PROMOTE_SCRIPT} ${BUILD_ID}-${MDATE} ${PRODUCT_VERSION_GF} /java/re/${ARCHIVE_MASTER_BUNDLES} ${JAVAEE_VERSION} ${ARCHIVE_PATH}"
    fi
}

init_storage_area(){
    ssh ${SSH_STORAGE} \
        "rm -rf ${ARCHIVE_STORAGE_BUNDLES} ; mkdir -p ${ARCHIVE_STORAGE_BUNDLES}"
}

scp_jnet(){
    file=`basename ${1}`
    simple_name=`echo ${file} | \
        sed -e s@"${PRODUCT_GF}-${PRODUCT_VERSION_GF}-web"@web@g \
        -e s@"${JAVAEE_VERSION}-${BUILD_ID}-"@@g \
        -e s@"-${JAVAEE_VERSION}-${BUILD_ID}"@@g \
        -e s@"${BUILD_ID}-"@@g \
        -e s@"-${BUILD_ID}"@@g \
        -e s@"-${PRODUCT_VERSION_GF}"@@g \
        -e s@"--"@"-"@g `
    if [ "nightly" == "${BUILD_KIND}" ]
    then
	   simple_name=`echo ${simple_name} | \
	           sed -e s@"-${MDATE}"@@g \
	               -e s@"${MDATE}-"@@g`
    fi

    ssh ${SSH_MASTER} \
        "scp /java/re/${ARCHIVE_MASTER_BUNDLES}/${file} ${JNET_DIR}"
    ssh ${SSH_MASTER} \
        "scp /java/re/${ARCHIVE_MASTER_BUNDLES}/${file} ${JNET_DIR}/latest-${simple_name}"
}

promote_bundle(){
    printf "\n==== PROMOTE_BUNDLE (%s) ====\n\n" ${2}
    curl ${1} > ${WORKSPACE_BUNDLES}/${2}
    scp ${WORKSPACE_BUNDLES}/${2} ${SCP}
    scp_jnet ${WORKSPACE_BUNDLES}/${2}
    if [ "nightly" == "${BUILD_KIND}" ]
    then
	   simple_name=`echo ${2}| tr -d " " | sed \
            -e s@"${PRODUCT_VERSION_GF}-"@@g \
            -e s@"${BUILD_ID}-${MDATE}-"@@g \
            -e s@"-${BUILD_ID}-${MDATE}"@@g \
            -e s@"--"@"-"@g`
    elif [ "weekly" == "${BUILD_KIND}" ]
    then
	   simple_name=`echo ${2}| tr -d " " | sed \
            -e s@"${PRODUCT_VERSION_GF}-"@@g \
            -e s@"${BUILD_ID}-"@@g \
            -e s@"-${BUILD_ID}"@@g \
            -e s@"--"@"-"@g`
    fi
    echo "${simple_name} -> ${ARCHIVE_URL}/${2}" >> ${PROMOTION_SUMMARY}
}

send_notification(){
    get_svn_rev_from_version_info
    /usr/lib/sendmail -t << MESSAGE
From: ${NOTIFICATION_FROM}
To: ${NOTIFICATION_SENDTO}
Subject: [ ${PRODUCT_GF}-${PRODUCT_VERSION_GF} ] Trunk ${BUILD_KIND} Build (${VERSION})

Product : ${PRODUCT_GF} ${PRODUCT_VERSION_GF}
Date    : ${DATE}
Version : ${VERSION}

External: ${JNET_DIR_HTTP}
Internal: ${ARCHIVE_URL}
Hudson job: ${PROMOTED_JOB_URL}
SVN revision: ${SVN_REVISION}

Aggregated tests summary:

`aggregated_tests_summary ${PROMOTED_JOB_URL}/aggregatedTestReport/`

Promotion summary:

`cat $PROMOTION_SUMMARY`

Thanks,
RE
MESSAGE
}
