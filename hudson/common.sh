#!/bin/bash -e

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

init_common(){
    require_env_var "HUDSON_HOME"
    BUILD_ID=`cat ${HUDSON_HOME}/promote-trunk.version`
    PKG_ID=`cat ${HUDSON_HOME}/pkgid-trunk.version`

    JAVAEE_VERSION=7.0
    MAJOR_VERSION=4
    MINOR_VERSION=0
    MICRO_VERSION=1
    PRODUCT_GF="glassfish"
    PRODUCT_VERSION_GF=${MAJOR_VERSION}.${MINOR_VERSION}.${MICRO_VERSION}

    SSH_MASTER=${RE_USER}@${HUDSON_MASTER_HOST}
    SSH_STORAGE=${RE_USER}@${STORAGE_HOST}
    PROMOTED_BUNDLES=${PROMOTED_URL}/artifact/bundles/
    GF_WORKSPACE_URL_SSH=svn+ssh://${RE_USER}@svn.java.net/glassfish~svn
    GF_WORKSPACE_URL_HTTP=https://svn.java.net/svn/glassfish~svn

    IPS_REPO_URL=http://localhost
    IPS_REPO_DIR=${WORKSPACE}/promorepo
    IPS_REPO_PORT=16500
    IPS_REPO_TYPE=sunos-sparc
    UC2_VERSION=2.3
    UC2_BUILD=56
    UC_HOME_URL=http://${STORAGE_HOST_HTTP}/java/re/updatecenter/${UC2_VERSION}
    UC_HOME_URL="${UC_HOME_URL}/promoted/B${UC2_BUILD}/archive/uc2/build"

    BUILD_KIND=${1}
    if [ "${BUILD_KIND}" == "weekly" ]
    then
        require_env_var "GPG_PASSPHRASE"
        if [ ! -z ${RELEASE_VERSION} ] && [ ${#RELEASE_VERSION} -gt 0 ]
        then
            BUILD_ID=`cut -d '-' -f2- <<< ${RELEASE_VERSION}`
            PRODUCT_VERSION_GF=`sed s@"-${BUILD_ID}"@@g <<< ${RELEASE_VERSION}`
        else
            RELEASE_VERSION="${PRODUCT_VERSION_GF-$BUILD_ID}"
        fi

        printf "\n%s \n\n" "===== RELEASE_VERSION ====="
        printf "VERSION %s - QUALIFIER: %s\n\n" \
            "${RELEASE_VERSION}" \
            "${BUILD_ID}"

        ARCHIVE_PATH=${PRODUCT_GF}/${PRODUCT_VERSION_GF}
        if [ ${#BUILD_ID} -gt 0 ]
        then
            ARCHIVE_PATH=${ARCHIVE_PATH}/promoted
        else
            ARCHIVE_PATH=${ARCHIVE_PATH}/release
        fi
        ARCHIVE_MASTER_BUNDLES=${ARCHIVE_PATH}/${BUILD_ID}/archive/bundles

    elif [ "${BUILD_KIND}"  == "nightly" ]
    then
        ARCHIVE_PATH=${PRODUCT_GF}/${PRODUCT_VERSION_GF}/nightly
        MDATE=$(date +%m_%d_%Y)
        BUILD_ID=`cat ${HUDSON_HOME}/promote-trunk.version`
        ARCHIVE_MASTER_BUNDLES=${ARCHIVE_PATH}/${BUILD_ID}-${MDATE}
    else
        printf "\n%s \n\n" "Error: wrong argument passed, please pass either weekly or nightly as parameter to the script"
        exit 1
    fi

    if [ -z ${VERSION} ]
    then
        VERSION=${RELEASE_VERSION}
    else
        VERSION=${PRODUCT_VERSION_GF}
        if [ ${#BUILD_ID} -gt 0 ]
        then
            VERSION="${VERSION}-${BUILD_ID}"
        fi
    fi

    WORKSPACE_BUNDLES=${WORKSPACE}/${BUILD_KIND}_bundles
    if [ ! -d "${WORKSPACE_BUNDLES}" ]
    then
        mkdir -p "${WORKSPACE_BUNDLES}"
    fi

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
    export MAVEN_OPTS

    PROMOTION_SUMMARY=${WORKSPACE_BUNDLES}/${BUILD_KIND}-promotion-summary.txt
    JNET_DIR=${JNET_USER}@${JNET_STORAGE_HOST}:/export/nfs/dlc/${ARCHIVE_PATH}
    JNET_DIR_HTTP=http://download.java.net/${ARCHIVE_PATH}
    ARCHIVE_STORAGE_BUNDLES=/onestop/${ARCHIVE_MASTER_BUNDLES}
    SCP=${SSH_STORAGE}:${ARCHIVE_STORAGE_BUNDLES}
    ARCHIVE_URL=http://${STORAGE_HOST_HTTP}/java/re/${ARCHIVE_MASTER_BUNDLES}

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
            SSH_MASTER \
            SSH_STORAGE \
            IPS_REPO_URL \
            IPS_REPO_DIR \
            IPS_REPO_PORT \
            IPS_REPO_TYPE \
            ARCHIVE_PATH \
            JNET_DIR \
            JNET_DIR_HTTP \
            PROMOTED_BUNDLES \
            GF_WORKSPACE_URL_SSH \
            GF_WORKSPACE_URL_HTTP \
            ARCHIVE_MASTER \
            ARCHIVE_MASTER_BUNDLES \
            ARCHIVE_URL
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
        kill -9 ${1}
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
        TRIGGER_JOB_URL="http://${HUDSON_MASTER_HOST}/hudson/job/gf-trunk-build-dev"
        SVN_REVISION=$(wget -q -O - "${TRIGGER_JOB_URL}/dev-build/api/xml?xpath=//changeSet/revision[1]/revision/text()")
        if [[ ! "${SVN_REVISION}" = *[[:digit:]]* ]]
        then
            echo "failed to get the svn revision from ${TRIGGER_JOB_URL}"
            exit 1
        fi
    fi

    if [ -z ${SVN_REVISION} ] \
        || [ `grep -i 'head' <<< "${SVN_REVISION}" | wc -l | awk '{print $1}'` -eq 1 ]
    then
        svn co --depth=files ${GF_WORKSPACE_URL_SSH}/trunk/main tmp
        if [ -z ${SVN_REVISION} ]
        then
            # retrieving last known good revision
            SVN_REVISION=`get_clean_svn_rev tmp`
        else
            # retrieving HEAD's value
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

    svn co --depth=empty ${GF_WORKSPACE_URL_SSH}/trunk/main tmp-co

    COMMIT_MSG="setting clean revision"
    CURRENT_KEYWORD=`svn propget svn:keyword tmp-co`
    CURRENT_KEYWORD=`sed '/^$/d' <<< "${CURRENT_KEYWORD}"`
    LAST_LOG=`svn propget svn:log --revprop -r HEAD tmp-co`

    if [ "${LAST_LOG}" = "${COMMIT_MSG}" ]
    then
        echo "Nothing to do. Current clean_revision is already recorded"
        exit 0
    fi

    echo ${1} > svn-keywords
    svn propset -F svn-keywords svn:keyword tmp-co
    svn commit ${WORKSPACE}/tmp-co -m "${COMMIT_MSG}"

    rm -rf tmp-co svn-keywords
}

get_clean_svn_rev(){
    svn propget svn:keyword ${1} | grep 'clean_' | sed s@'clean_'@@g | awk '{print $2}'
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
    echo "${GF_WORKSPACE_URL_HTTP}/trunk/main ${SVN_REVISION}" >> ${WORKSPACE}/version-info.txt
    if [ "${BUILD_KIND}" == "weekly" ]
    then
        echo "Maven-Version: ${RELEASE_VERSION}" >> ${WORKSPACE}/version-info.txt
    fi

    printf "\n%s\n\n" "==== VERSION INFO ===="
    echo "Date: `date`" >> ${WORKSPACE}/version-info.txt
    cat ${WORKSPACE}/version-info.txt
}

create_svn_tag(){
    printf "\n%s \n\n" "===== CREATE SVN TAG ====="

    # download and unzip the workspace
    curl $PROMOTED_BUNDLES/workspace.zip > workspace.zip
    rm -rf ${WORKSPACE}/tag ; unzip -d ${WORKSPACE}/tag workspace.zip

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
    cp ${WORKSPACE}/main/appserver/distributions/glassfish/target/*.zip ${WORKSPACE}/bundles
    cp ${WORKSPACE}/main/appserver/distributions/web/target/*.zip ${WORKSPACE}/bundles
    cp ${WORKSPACE}/main/nucleus/distributions/nucleus/target/*.zip ${WORKSPACE}/bundles
}

clean_and_zip_workspace(){
    printf "\n%s \n\n" "===== ZIP THE WORKSPACE ====="
    svn status main | grep ? | awk '{print $2}' | xargs rm -rf
    zip ${WORKSPACE}/bundles/workspace.zip -r main
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
         { if (NR > 2) print $2" "$3" "$8" "$11 }' tests.html \
		| sed -e s@'"'@@g \
				  -e s@'href=/hudson/job/'@@g \
          -e s@'/testReport/><img alt='@'#'@g \
          -e s@' style=text-align:right>'@'#'@g \
					-e s@'/'@'#'@g`
    do
        jobname=`cut -d '#' -f1 <<< $i`
        buildnumber=`cut -d '#' -f2 <<< $i`
        failednumber=`cut -d '#' -f4 <<< $i`
        passednumber=`cut -d '#' -f5 <<< $i`
	printf "%s%s%s%s\n" \
            `align_column 55 "." "$jobname (#${buildnumber})"` \
            `align_column 15 "." "PASSED(${passednumber})"` \
            "FAILED(${failednumber})"
    done
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
    file=${1}
    simple_name=`echo ${1} | \
        sed -e s@"${PRODUCT_GF}-${PRODUCT_VERSION_GF}-web"@web@g \
        -e s@"${JAVAEE_VERSION}-${BUILD_ID}-"@@g \
        -e s@"-${JAVAEE_VERSION}-${BUILD_ID}"@@g \
        -e s@"${BUILD_ID}-"@@g \
        -e s@"-${BUILD_ID}"@@g \
        -e s@"-${PRODUCT_VERSION_GF}"@@g \
        -e s@"${PKG_ID}-"@@g \
        -e s@"--"@"-"@g `
    ssh ${SSH_MASTER} \
        "scp /java/re/${ARCHIVE_MASTER_BUNDLES}/${file} ${JNET_DIR}"
    ssh ${SSH_MASTER} \
        "scp /java/re/${ARCHIVE_MASTER_BUNDLES}/${file} ${JNET_DIR}/latest-${simple_name}"
}

promote_bundle(){
    printf "\n==== PROMOTE_BUNDLE (%s) ====\n\n" ${2}
    curl ${1} > ${2}
    scp ${2} ${SCP}
    scp_jnet ${2}
    simple_name=`echo ${2}| tr -d " " | sed \
        -e s@"${PRODUCT_VERSION_GF}-"@@g \
        -e s@"${BUILD_ID}-"@@g \
        -e s@"--"@"-"@g`
    echo "${simple_name} -> ${ARCHIVE_URL}/${2}" >> ${PROMOTION_SUMMARY}
}

send_notification(){
    get_svn_rev_from_version_info
    DATE=`date`
    /usr/lib/sendmail -t << MESSAGE
From: ${NOTIFICATION_FROM}
To: ${NOTIFICATION_SENDTO}
Subject: [ ${PRODUCT_GF}-${PRODUCT_VERSION_GF} ] Trunk ${BUILD_KIND} Build (${BUILD_ID}-${DATE})

Product : ${PRODUCT_GF} ${PRODUCT_VERSION_GF}
Date    : ${DATE}
Version : ${VERSION}

External: ${JNET_DIR_HTTP}
Internal: ${ARCHIVE_URL}
Hudson job: ${PROMOTED_URL}
SVN revision: ${SVN_REVISION}

Aggregated tests summary:

`aggregated_tests_summary ${PROMOTED_URL}/aggregatedTestReport/`

Promotion summary:

`cat $PROMOTION_SUMMARY`

Thanks,
RE
MESSAGE
}
