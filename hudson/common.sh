#!/bin/bash -ex

#RE_USER=
#HUDSON_MASTER_HOST=
#STORAGE_HOST=
#JNET_USER=
#JNET_STORAGE_HOST=
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
PRODUCT_VERSION_GF=$MAJOR_VERSION.$MINOR_VERSION.$MICRO_VERSION

SSH_MASTER=${RE_USER}@${HUDSON_MASTER_HOST}
SSH_STORAGE=${RE_USER}@${STORAGE_HOST}
WEEKLY_ARCHIVE_STORAGE=/onestop/$PRODUCT_GF/$PRODUCT_VERSION_GF/promoted/$BUILD_ID
WEEKLY_ARCHIVE_STORAGE_BUNDLES=$WEEKLY_ARCHIVE_STORAGE/archive/bundles
WEEKLY_ARCHIVE_MASTER=/java/re/$PRODUCT_GF/$PRODUCT_VERSION_GF/promoted/$BUILD_ID
WEEKLY_ARCHIVE_MASTER_BUNDLES=$WEEKLY_ARCHIVE_MASTER/archive/bundles
WEEKLY_SCP=$SSH_STORAGE:$WEEKLY_ARCHIVE_STORAGE_BUNDLES
JNET_DIR=${JNET_USER}@${JNET_STORAGE_HOST}:/export/nfs/dlc/$PRODUCT_GF/$PRODUCT_VERSION_GF/promoted
JNET_DIR_HTTP=http://download.java.net/$PRODUCT_GF/$PRODUCT_VERSION_GF/promoted
WEEKLY_ARCHIVE_URL=http://javaweb.us.oracle.com/$WEEKLY_ARCHIVE_MASTER_BUNDLES
PROMOTED_BUNDLES=$PROMOTED_URL/artifact/bundles/
GF_WORKSPACE_URL_SSH=svn+ssh://${RE_USER}@svn.java.net/glassfish~svn
GF_WORKSPACE_URL_HTTP=https://svn.java.net/svn/glassfish~svn

IPS_REPO_URL=http://localhost
IPS_REPO_DIR=$WORKSPACE/promorepo
IPS_REPO_PORT=16500
IPS_REPO_TYPE=sunos-sparc
UC2_BUILD=2.3-b56
 
UC_HOME_URL=http://${STORAGE_HOST}/java/re/updatecenter/2.3/promoted/B56/archive/uc2/build 

init_release_version(){
    if [ ! -z $RELEASE_VERSION ] && [ ${#RELEASE_VERSION} -gt 0 ]
    then
        printf "\n%s\n\n" "Using provided RELEASE_VERSION: $RELEASE_VERSION"
    else
        RELEASE_VERSION="$PRODUCT_VERSION_GF-$BUILD_ID"
    fi
}
align_column(){
    max=$1
    char=$2
    string=$3
    stringlength=${#string}
    y=$((max-stringlength))
    while [ $y -gt 0 ]
    do
         string="$string$char"
         y=$((y-1))
    done
    echo "$string"
}
aggregated_tests_summary(){	
    # Hudson rest API does not give the report
    # parsing html with xpath
    curl $1 2> /dev/null 1> aggregatedTestReport
    XPATH_REQUEST="//table[@class='pane sortable']/tr[position()>1]//*[not(child::a)]/text()"

    # each line is a column value
    COLUMN=1
    EMPTY="true"
    for RESULT_LINE in `xpath -q -e "$XPATH_REQUEST" aggregatedTestReport`
    do
            case "$COLUMN" in
            "1")
                    JOB_NAME="$RESULT_LINE"
                ;;
            "2")
                    JOB_NUMBER="$RESULT_LINE"
                    ;;
            "3")
                    FAILED_NUMBER=`echo $RESULT_LINE | awk '{print $1}' | tr -d ' '`
                ;;
            "4")
                    TOTAL_NUMBER=`echo $RESULT_LINE | awk '{print $1}' | tr -d ' '`

                    if [ $TOTAL_NUMBER != "N/A" ] && [ $FAILED_NUMBER != "N/A" ]
                    then
                            PASSED_NUMBER=$((TOTAL_NUMBER-FAILED_NUMBER))
                            printf "%s%s%s \n" \
                                    "`align_column 55 "." "$JOB_NAME ($JOB_NUMBER)"`" \
                                    "`align_column 12 ' ' "PASSED($PASSED_NUMBER)"`" \
                                    "FAILED($FAILED_NUMBER)"
                            EMPTY="false"
                    fi

                    # RESET COLUM
                    COLUMN=0
                ;;
            esac
            COLUMN=$((COLUMN+1))
    done
    if [ $EMPTY = "true" ]
    then
            printf "No downstream test result found."
    fi
    rm aggregatedTestReport
}
scp_jnet(){
    file=$1
    simple_name=`echo $1 | \
                 sed -e s@"$PRODUCT_GF-$PRODUCT_VERSION_GF-web"@web@g \
                     -e s@"$JAVAEE_VERSION-$BUILD_ID-"@@g \
                     -e s@"-$JAVAEE_VERSION-$BUILD_ID"@@g \
                     -e s@"$BUILD_ID-"@@g \
                     -e s@"-$BUILD_ID"@@g \
                     -e s@"-$PRODUCT_VERSION_GF"@@g \
                     -e s@"$PKG_ID-"@@g \
                     -e s@"--"@"-"@g `
    ssh $SSH_MASTER "scp `echo $WEEKLY_ARCHIVE_MASTER_BUNDLES/$file $JNET_DIR`"
    ssh #SSH_MASTER "scp `echo $WEEKLY_ARCHIVE_MASTER_BUNDLES/$file $JNET_DIR/latest-$simple_name`"
}
promote_bundle(){
    curl $1 > $2
    scp $2 $WEEKLY_SCP
    scp_jnet $2
    simple_name=`echo $i | tr -d " " | sed \
        -e s@"$PRODUCT_VERSION_GF-"@@g \
        -e s@"$BUILD_ID-"@@g \
        -e s@"--"@"-"@g`
    echo "$simple_name -> $WEEKLY_ARCHIVE_URL/$i" >> weekly-promotion-summary.txt
}