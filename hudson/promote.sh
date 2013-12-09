#!/bin/bash -e

source `dirname $0`/common.sh
init_common ${1}
init_storage_area

if [ "${BUILD_KIND}" == "weekly" ]
then
    promote_bundle ${PROMOTED_BUNDLES}/web-ips-ml.zip ${PRODUCT_GF}-${PRODUCT_VERSION_GF}-web-${BUILD_ID}-ml.zip
    promote_bundle ${PROMOTED_BUNDLES}/glassfish-ips-ml.zip ${PRODUCT_GF}-${PRODUCT_VERSION_GF}-${BUILD_ID}-ml.zip
    promote_bundle ${PROMOTED_BUNDLES}/nucleus-new.zip nucleus-${PRODUCT_VERSION_GF}-${BUILD_ID}.zip
    promote_bundle ${PROMOTED_BUNDLES}/version-info.txt version-info-${PRODUCT_VERSION_GF}-${BUILD_ID}.txt

    VERSION_INFO="version-info-${PRODUCT_VERSION_GF}-${BUILD_ID}.txt"
    create_svn_tag    
elif [ "${BUILD_KIND}" == "nightly" ]
then
    promote_bundle ${PROMOTED_BUNDLES}/web-ips-ml.zip ${PRODUCT_GF}-${PRODUCT_VERSION_GF}-web-${BUILD_ID}-${MDATE}-ml.zip
    promote_bundle ${PROMOTED_BUNDLES}/glassfish-ips-ml.zip ${PRODUCT_GF}-${PRODUCT_VERSION_GF}-${BUILD_ID}-${MDATE}-ml.zip
    promote_bundle ${PROMOTED_BUNDLES}/nucleus-new.zip nucleus-${PRODUCT_VERSION_GF}-${BUILD_ID}-${MDATE}.zip
    promote_bundle ${PROMOTED_BUNDLES}/version-info.txt version-info-${PRODUCT_VERSION_GF}-${BUILD_ID}-${MDATE}.txt

    VERSION_INFO="version-info-${PRODUCT_VERSION_GF}-${BUILD_ID}-${MDATE}.txt"
    SVN_REVISION=`awk '{print $2}' <<<  ${VERSION_INFO}`
    record_svn_rev ${SVN_REVISION}
fi

create_symlinks
send_notification