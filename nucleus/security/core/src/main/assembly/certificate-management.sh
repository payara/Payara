#!/bin/bash
copy_certificates() {
  FILE=${JAVA_HOME}/jre/lib/security/cacerts
  DEST=$1
  if [ -f "$FILE" ]; then
    echo "Copying cerificates from ${JAVA_HOME}/jre/lib/security/cacerts"
    keytool -importkeystore \-srckeystore $FILE \
    -destkeystore $DEST \
    -srcstoretype JKS \-deststoretype JKS \-srcstorepass changeit \
    -deststorepass changeit \
    -v \
    -noprompt \
    2>/dev/null
  fi

}
remove_certificates() {
  FN=$1
  echo "Finding expired certs in $FN"
  ALIASES=`keytool -list -v -keystore $FN -storepass changeit 2>/dev/null | grep -i 'alias\|until' `

  echo "$ALIASES" > aliases.txt

  i=1
  # Split dates and aliases to different arrays
  while read p; do
      if ! ((i % 2)); then
          arr_date+=("$p")
      else
          arr_cn+=("$p")
      fi
      i=$((i+1))
  done < aliases.txt
  i=0

  # Parse until-dates ->
  # convert until-dates to "seconds from 01-01-1970"-format ->
  # compare until-dates with today-date ->
  # delete expired aliases
  for date_idx in $(seq 0 $((${#arr_date[*]}-1)));
  do
      a_date=`echo ${arr_date[$date_idx]} | awk -F"until: " '{print $2}'`
      if [ `date +%s --date="$a_date"` -lt `date +%s` ];
      then
          echo "removing ${arr_cn[$i]} expired: $a_date"
          alias_name=`echo "${arr_cn[$i]}" | awk -F"name: " '{print $2}'`
          keytool -delete -alias "$alias_name" -keystore $FN -storepass changeit
      fi
      i=$((i+1))
  done
  echo "Done."
  rm -rf aliases.txt
}
copy_certificates src/main/resources/config/cacerts.jks
remove_certificates src/main/resources/config/keystore.jks
remove_certificates src/main/resources/config/cacerts.jks