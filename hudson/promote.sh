#!/bin/bash -e

source `dirname $0`/common.sh

printf "\n%s \n\n" "===== ENV ====="
env

###################
# ARCHIVE DISTROS #
###################

ssh $SSH_STORAGE "rm -rf $ARCHIVE_STORAGE_BUNDLES ; mkdir -p $ARCHIVE_STORAGE_BUNDLES"
cd $WORKSPACE_BUNDLES

# JAVAEE API JARS AND JAVADOCS
# XXXJAVAEE promote_bundle $PROMOTED_BUNDLES/javaee-api.jar javaee-api-7.0-$BUILD_ID.jar
# XXXJAVAEE promote_bundle $PROMOTED_BUNDLES/javaee-api-javadoc.jar javaee-api-7.0-$BUILD_ID-javadoc.jar
# XXXJAVAEE promote_bundle $PROMOTED_BUNDLES/javaee-web-api.jar javaee-web-api-7.0-$BUILD_ID.jar
# XXXJAVAEE promote_bundle $PROMOTED_BUNDLES/javaee-web-api-javadoc.jar javaee-web-api-7.0-$BUILD_ID-javadoc.jar

# COMMUNITY BUNDLES
if [ "weekly" == "${1}" ]
then
    promote_bundle $PROMOTED_BUNDLES/web-ips.zip $PRODUCT_GF-$PRODUCT_VERSION_GF-web-$BUILD_ID.zip
    promote_bundle $PROMOTED_BUNDLES/web-ips-ml.zip $PRODUCT_GF-$PRODUCT_VERSION_GF-web-$BUILD_ID-ml.zip
    promote_bundle $PROMOTED_BUNDLES/glassfish-ips.zip $PRODUCT_GF-$PRODUCT_VERSION_GF-$BUILD_ID.zip
    promote_bundle $PROMOTED_BUNDLES/glassfish-ips-ml.zip $PRODUCT_GF-$PRODUCT_VERSION_GF-$BUILD_ID-ml.zip
    promote_bundle $PROMOTED_BUNDLES/nucleus-new.zip nucleus-$RELEASE_VERSION.zip
    promote_bundle $PROMOTED_BUNDLES/version-info.txt version-info-$BUILD_ID.txt
elif [ "nightly" == "${1}" ]
then
    promote_bundle $PROMOTED_BUNDLES/web-ips-ml.zip $PRODUCT_GF-$PRODUCT_VERSION_GF-web-$BUILD_ID-ml.zip
    promote_bundle $PROMOTED_BUNDLES/glassfish-ips-ml.zip $PRODUCT_GF-$PRODUCT_VERSION_GF-$BUILD_ID-ml.zip
fi

####################
# CREATE SYMBLINKS #
####################

PROMOTE_SCRIPT=/tmp/promotebuild.sh
cat <<EOF > $PROMOTE_SCRIPT
#!/bin/bash -ex

# arg1 BUILD_ID
# arg2 PRODUCT_VERSION_GF
# arg3 ARCHIVE_MASTER_BUNDLES
# arg4 JAVAEE_VERSION

cd \$3
rm -rf latest-*

for i in \`ls\`
do
    simple_name=\`echo \$i | \
        sed -e s@"-\$1"@@g \
                -e s@"-\$4"@@g \
                -e s@"-\$2"@@g \
                -e s@"\$3-"@@g \
                -e s@"--"@"-"@g\` 
        ln -fs \$i "latest-\$simple_name"
done

cd \$3
cd ../../../
rm -rf latest
ln -s \$1 latest
EOF

scp $PROMOTE_SCRIPT $SSH_MASTER:/tmp
ssh $SSH_MASTER "chmod +x $PROMOTE_SCRIPT"
ssh $SSH_MASTER "$PROMOTE_SCRIPT $BUILD_ID $PRODUCT_VERSION_GF $ARCHIVE_MASTER_BUNDLES $JAVAEE_VERSION"

#####################
# TAG THE WORKSPACE #
#####################

if [ "weekly" == "${1}" ]
then
    curl $PROMOTED_BUNDLES/workspace.zip > workspace.zip
    unzip workspace.zip

    svn switch --relocate $GF_WORKSPACE_URL_SSH/trunk/main $GF_WORKSPACE_URL_SSH/tags/$RELEASE_VERSION
fi

########################
# SEND NOTIFICATION #
########################

datetime=`date`
/usr/lib/sendmail -t << MESSAGE
From: $NOTIFICATION_FROM
To: $NOTIFICATION_SENDTO
Subject: [ $PRODUCT_GF-$PRODUCT_VERSION_GF ] Trunk ${1} Build ($BUILD_ID)

Product : $PRODUCT_GF $PRODUCT_VERSION_GF
Date    : `date`
Version : $BUILD_ID

 *External*: $JNET_DIR_HTTP
 *Internal*: $ARCHIVE_URL

 *Aggregated tests summary*:

`aggregated_tests_summary $PROMOTED_URL/aggregatedTestReport/`

 *Promotion summary*:

`cat $WORKSPACE_BUNDLES/${1}-promotion-summary.txt`

 *Svn revisions*:

`head -1 $WORKSPACE_BUNDLES/version-info-$BUILD_ID.txt`

Thanks,
RE
MESSAGE