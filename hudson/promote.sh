#!/bin/bash -e

source `dirname $0`/common.sh

#init_storage_area

#promote_bundle $PROMOTED_BUNDLES/web-ips-ml.zip $PRODUCT_GF-$PRODUCT_VERSION_GF-web-$BUILD_ID-ml.zip
#promote_bundle $PROMOTED_BUNDLES/glassfish-ips-ml.zip $PRODUCT_GF-$PRODUCT_VERSION_GF-$BUILD_ID-ml.zip
#promote_bundle $PROMOTED_BUNDLES/nucleus-new.zip nucleus-$PRODUCT_VERSION_GF-$BUILD_ID.zip
#promote_bundle $PROMOTED_BUNDLES/version-info.txt version-info-$BUILD_ID.txt

#create_symlinks

if [ "weekly" == "$1" ]
then
	create_svn_tag    
elif [ "nightly" == "$1" ]
then
   	printf "\n%s \n\n" "===== TODO, RECORD REVISION ====="
fi

#send_notification
