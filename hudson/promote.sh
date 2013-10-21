#!/bin/bash -e

source `dirname $0`/common.sh
init_release_version

###################
# ARCHIVE DISTROS #
###################

rm -rf $WORKSPACE/weekly_bundles
mkdir -p $WORKSPACE/weekly_bundles
ssh $SSH_STORAGE "rm -rf `echo $WEEKLY_ARCHIVE_STORAGE`"
ssh $SSH_STORAGE "rm -rf `echo $WEEKLY_ARCHIVE_STORAGE_BUNDLES`"

cd $WORKSPACE/weekly_bundles

# JAVAEE API JARS AND JAVADOCS
# XXXJAVAEE promote_bundle $PROMOTED_BUNDLES/javaee-api.jar javaee-api-7.0-$BUILD_ID.jar
# XXXJAVAEE promote_bundle $PROMOTED_BUNDLES/javaee-api-javadoc.jar javaee-api-7.0-$BUILD_ID-javadoc.jar
# XXXJAVAEE promote_bundle $PROMOTED_BUNDLES/javaee-web-api.jar javaee-web-api-7.0-$BUILD_ID.jar
# XXXJAVAEE promote_bundle $PROMOTED_BUNDLES/javaee-web-api-javadoc.jar javaee-web-api-7.0-$BUILD_ID-javadoc.jar

# COMMUNITY BUNDLES
promote_bundle $PROMOTED_BUNDLES/web-ips.zip $PRODUCT_GF-$PRODUCT_VERSION_GF-web-$BUILD_ID.zip
promote_bundle $PROMOTED_BUNDLES/web-ips-ml.zip $PRODUCT_GF-$PRODUCT_VERSION_GF-web-$BUILD_ID-ml.zip
promote_bundle $PROMOTED_BUNDLES/glassfish-ips.zip $PRODUCT_GF-$PRODUCT_VERSION_GF-$BUILD_ID.zip
promote_bundle $PROMOTED_BUNDLES/glassfish-ips-ml.zip $PRODUCT_GF-$PRODUCT_VERSION_GF-$BUILD_ID-ml.zip
promote_bundle $PROMOTED_BUNDLES/nucleus-new.zip nucleus-$RELEASE_VERSION.zip
promote_bundle $PROMOTED_BUNDLES/glassfish-web.sh $PRODUCT_GF-$PRODUCT_VERSION_GF-web-$BUILD_ID-unix.sh
promote_bundle $PROMOTED_BUNDLES/glassfish-web.exe $PRODUCT_GF-$PRODUCT_VERSION_GF-web-$BUILD_ID-windows.exe
promote_bundle $PROMOTED_BUNDLES/glassfish-full.sh $PRODUCT_GF-$PRODUCT_VERSION_GF-$BUILD_ID-unix.sh
promote_bundle $PROMOTED_BUNDLES/glassfish-full.exe $PRODUCT_GF-$PRODUCT_VERSION_GF-$BUILD_ID-windows.exe
promote_bundle $PROMOTED_BUNDLES/glassfish-web-ml.sh $PRODUCT_GF-$PRODUCT_VERSION_GF-web-$BUILD_ID-unix-ml.sh
promote_bundle $PROMOTED_BUNDLES/glassfish-web-ml.exe $PRODUCT_GF-$PRODUCT_VERSION_GF-web-$BUILD_ID-windows-ml.exe
promote_bundle $PROMOTED_BUNDLES/glassfish-full-ml.sh $PRODUCT_GF-$PRODUCT_VERSION_GF-$BUILD_ID-unix-ml.sh
promote_bundle $PROMOTED_BUNDLES/glassfish-full-ml.exe $PRODUCT_GF-$PRODUCT_VERSION_GF-$BUILD_ID-windows-ml.exe
promote_bundle $PROMOTED_BUNDLES/svn-revisions.txt svn-revisions-$BUILD_ID.txt

####################
# CREATE SYMBLINKS #
####################

PROMOTE_WEEKLY_SCRIPT=/tmp/promoteWeeklies.sh
cat <<EOF > $PROMOTE_WEEKLY_SCRIPT
#!/bin/bash -ex

# arg1 BUILD_ID
# arg2 PRODUCT_VERSION_GF
# arg3 WEEKLY_ARCHIVE_MASTER_BUNDLES
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

scp $PROMOTE_WEEKLY_SCRIPT $SSH_MASTER:/tmp
ssh $SSH_MASTER "chmod +x $PROMOTE_WEEKLY_SCRIPT"
ssh $SSH_MASTER "$PROMOTE_WEEKLY_SCRIPT $BUILD_ID $PRODUCT_VERSION_GF $WEEKLY_ARCHIVE_MASTER_BUNDLES $JAVAEE_VERSION"

#####################
# TAG THE WORKSPACE #
#####################

curl $PROMOTED_BUNDLES/workspace.zip > workspace.zip
unzip workspace.zip

svn switch --relocate $GF_WORKSPACE_URL_SSH/trunk/main $GF_WORKSPACE_URL_SSH/tags/$RELEASE_VERSION

########################
# SEND NOTIFICATION #
########################

datetime=`date`
/usr/lib/sendmail -t << MESSAGE
From: $NOTIFICATION_FROM
To: $NOTIFICATION_SENDTO
Subject: [ $PRODUCT_GF-$PRODUCT_VERSION_GF ] Trunk WEEKLY Build ($BUILD_ID)

Product : $PRODUCT_GF $PRODUCT_VERSION_GF
Date    : `date`
Version : $BUILD_ID

 *External*: $JNET_DIR_HTTP
 *Internal*: $WEEKLY_ARCHIVE_URL

 *Aggregated tests summary*:

`aggregated_tests_summary $PROMOTED_URL/aggregatedTestReport/`

 *Promotion summary*:

`cat $WORKSPACE/weekly_bundles/weekly-promotion-summary.txt`

 *Svn revisions*:

`cat $WORKSPACE/weekly_bundles/svn-revisions-$BUILD_ID.txt`

Thanks,
RE
MESSAGE