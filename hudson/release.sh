#!/bin/bash -e

source `dirname $0`/common.sh
init ${1}

kill_glassfish
kill_ips_repo
print_env_info
get_svn_rev
create_version_info
install_uc_toolkit
start_ips_repository

if [ "${BUILD_KIND}" == "weekly" ]
then
    delete_svn_tag ${RELEASE_VERSION}
    svn_checkout ${SVN_REVISION}
    release_prepare
    build_release "clean deploy" "release-phase2,ips,embedded,javaee-api"
elif [ "${BUILD_KIND}" == "nightly" ]
then
    svn_checkout ${SVN_REVISION}
    run_findbugs
    build_release "clean install" "ips,javaee-api"
fi

archive_bundles
clean_and_zip_workspace