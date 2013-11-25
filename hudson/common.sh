#!/bin/bash -e

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
UC2_BUILD=2.3-b56
UC_HOME_URL=http://${STORAGE_HOST_HTTP}/java/re/updatecenter/2.3/promoted/B56/archive/uc2/build

if [ "weekly" == "${1}" ]
then
    if [ ! -z ${RELEASE_VERSION} ] && [ ${#RELEASE_VERSION} -gt 0 ]
    then
        VERSION_QUALIFIER=`cut -d '-' -f2- <<< ${RELEASE_VERSION}`
        BUILD_ID=${VERSION_QUALIFIER}
    else
        RELEASE_VERSION="${PRODUCT_VERSION_GF-$BUILD_ID}"
        VERSION_QUALIFIER=${BUILD_ID}
    fi

    printf "\n%s \n\n" "===== RELEASE_VERSION ====="
    printf "%s\n\n" "VERSION : ${RELEASE_VERSION} - QUALIFIER: ${VERSION_QUALIFIER}"
    
    ARCHIVE_PATH=${PRODUCT_GF}/${PRODUCT_VERSION_GF}
    if [ ${#VERSION_QUALIFIER} -gt 0 ]
    then
    	ARCHIVE_PATH=${ARCHIVE_PATH}/promoted
    else
        ARCHIVE_PATH=${ARCHIVE_PATH}/release
    fi
    ARCHIVE_MASTER_BUNDLES=${ARCHIVE_PATH}/${VERSION_QUALIFIER}/archive/bundles
elif [ "nightly" == "${1}" ]
then
    ARCHIVE_PATH=${PRODUCT_GF}/${PRODUCT_VERSION_GF}/nightly
    MDATE=$(date +%m_%d_%Y)
    BUILD_ID=`cat /net/bat-sca.us.oracle.com/repine/export2/hudson/promote-trunk.version`
    ARCHIVE_MASTER_BUNDLES=${ARCHIVE_PATH}/${BUILD_ID}-${MDATE}
else
    printf "\n%s \n\n" "Error: wrong argument passed, please pass either weekly or nightly as parameter to the script"
    exit 1
fi

BUILD_KIND=$1
WORKSPACE_BUNDLES=${WORKSPACE}/${BUILD_KIND}_bundles
if [ ! -d "${WORKSPACE_BUNDLES}" ]
then
    mkdir -p "${WORKSPACE_BUNDLES}"
fi

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
	VERSION_QUALIFIER \
	BUILD_ID \
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

align_column(){
    max=$1
    char=$2
    string=$3
    stringlength=${#string}
    y=$((max-stringlength))
    while [ $y -gt 0 ] ; do	string="$string$char" ; y=$((y-1)) ; done
    echo "$string"
}

aggregated_tests_summary(){
    # Hudson rest API does not give the report
    # parsing html manually...

    TESTS_HTML="tests.html"
    UNSORTED="unsorted.txt"
    RESULTS="results.txt"
    JOBS="jobs.txt"

    rm -f $TESTS_HTML $UNSORTED $RESULTS $JOBS
    wget -o /dev/null -O $TESTS_HTML $1

    # DO THE MAGIC
    job_name=`echo $1 | sed -e 's/.*\/job\/\([^\/]*\).*/\1/'`
    for i in `cat tests.html | ggrep -2 "testReport" | grep -v footer ` ; do echo $i ; done > $UNSORTED
    cat unsorted.txt |  grep "testReport" | grep -v $job_name | sed -e 's/.*\/job\/\([^\/]*\)\/\([0-9]*\).*/\1 \2/' > $JOBS
    cat unsorted.txt | grep "text-align" | sed s@">"@" "@g | awk '{print $2}' | grep -v "</td" > $RESULTS
    rm -f $TESTS_HTML $UNSORTED

    cpt=1
    while read job
    do
        failednumber=`awk "NR==$cpt" $RESULTS`
        passednumber=`awk "NR==$((cpt+1))" $RESULTS`
        jobname=`echo $job | awk '{print $1}'`
        buildnumber=`echo $job | awk '{print $2}'`

        echo "`align_column 55 "." "$jobname (#$buildnumber)"` `align_column 12 " " "PASSED($passednumber)"`FAILED($failednumber)"
        cpt=$((cpt+2))
    done < $JOBS
    rm -f $RESULTS $JOBS
}

create_symlinks(){
    PROMOTE_SCRIPT=/tmp/promotebuild.sh
    cat <<EOF > $PROMOTE_SCRIPT
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
                -e s@"-\${4}"@@g \
                -e s@"-\${2}"@@g \
                -e s@"\${3}-"@@g \
                -e s@"--"@"-"@g\` 
        ln -fs \$i "latest-\${simple_name}"
done

cd /java/re/\${5}
rm -rf latest
ln -s \${1} latest
EOF
    scp ${PROMOTE_SCRIPT} ${SSH_MASTER}:/tmp
    ssh ${SSH_MASTER} "chmod +x ${PROMOTE_SCRIPT}"
    ssh ${SSH_MASTER} "${PROMOTE_SCRIPT} ${BUILD_ID} ${PRODUCT_VERSION_GF} /java/re/${ARCHIVE_MASTER_BUNDLES} ${JAVAEE_VERSION} ${ARCHIVE_PATH}"
}

kill_clean(){ if [ ${#1} -ne 0 ] ; then kill -9 ${1} ; fi }

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
    ssh ${SSH_MASTER} "scp /java/re/${ARCHIVE_MASTER_BUNDLES}/${file} ${JNET_DIR}"
    ssh ${SSH_MASTER} "scp /java/re/${ARCHIVE_MASTER_BUNDLES}/${file} ${JNET_DIR}/latest-${simple_name}"
}

init_storage_area(){
	ssh ${SSH_STORAGE} "rm -rf ${ARCHIVE_STORAGE_BUNDLES} ; mkdir -p ${ARCHIVE_STORAGE_BUNDLES}"
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

create_svn_tag(){
    printf "\n%s \n\n" "===== CREATE SVN TAG ====="
	
    # download and unzip the workspace
    curl $PROMOTED_BUNDLES/workspace.zip > workspace.zip
    rm -rf ${WORKSPACE}/tag ; unzip -d ${WORKSPACE}/tag workspace.zip
    
    # delete tag (for promotion forcing)
    set +e
    svn del ${GF_WORKSPACE_URL_SSH}/tags/${RELEASE_VERSION} -m "del tag ${RELEASE_VERSION}"
    set -e
    
    # copy the exact trunk used to run the release
    SVN_REVISION=`svn info ${WORKSPACE}/tag/main | grep 'Revision:' | awk '{print $2}'`
    svn cp ${GF_WORKSPACE_URL_SSH}/trunk/main@${SVN_REVISION} ${GF_WORKSPACE_URL_SSH}/tags/${RELEASE_VERSION} -m "create tag ${RELEASE_VERSION} based on r${SVN_REVISION}"
    
    # switch the workspace
    svn switch ${GF_WORKSPACE_URL_SSH}/tags/${RELEASE_VERSION} ${WORKSPACE}/tag/main
    
    # commit the local changes
    svn commit ${WORKSPACE}/tag/main -m "commit tag ${RELEASE_VERSION}"
}

send_notification(){
    /usr/lib/sendmail -t << MESSAGE
From: ${NOTIFICATION_FROM}
To: ${NOTIFICATION_SENDTO}
Subject: [ ${PRODUCT_GF}-${PRODUCT_VERSION_GF} ] Trunk ${BUILD_KIND} Build (${BUILD_ID})

Product : ${PRODUCT_GF} ${PRODUCT_VERSION_GF}
Date    : `date`
Version : ${BUILD_ID}

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