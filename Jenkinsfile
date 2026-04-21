#!groovy
// Jenkinsfile for building a PR and running a subset of tests against it
def pom
def DOMAIN_NAME
def payaraBuildNumber
def buildId

pipeline {
    agent {
        label 'general-purpose'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '5', daysToKeepStr: '14'))
    }
    environment {
        MP_METRICS_TAGS='tier=integration'
        MP_CONFIG_CACHE_DURATION=0
        JAVA_HOME = tool("zulu-21")
        DOMAIN_NAME = "test-domain"
    }
    tools {
        jdk "zulu-21"
        maven "maven-3.6.3"
    }
    stages {
        stage('Report') {
            steps {
                script {
                    pom = readMavenPom file: 'pom.xml'
                    payaraBuildNumber = "PR${env.ghprbPullId}#${currentBuild.number}"
                    echo "Payara pom version is ${pom.version}"
                    echo "Build number is ${payaraBuildNumber}"
                    echo "Domain name is ${DOMAIN_NAME}"
                }
            }
        }
        stage('Build') {
            steps {
                script {
                    echo 'Fetching Build Job Artifacts'

                    def specificBranchCommitOrTag = env.CHANGE_BRANCH ?: env.BRANCH_NAME
                    def repoOrg = env.CHANGE_FORK ?: 'Payara'

                    def buildJob = build job: 'Build/Build',
                    wait: true,
                    parameters: [
                        string(name: 'specificBranchCommitOrTag', value: specificBranchCommitOrTag),
                        string(name: 'repoOrg', value: repoOrg),
                        string(name: 'jdkVer', value: 'zulu-21'),
                        string(name: 'stream', value: 'Community'),
                        string(name: 'profiles', value: 'BuildEmbedded'),
                        booleanParam(name: 'skipTests', value: false),
                        string(name: 'multiThread', value: '1'),
                        booleanParam(name: 'archiveMavenRepository', value: true)
                    ]

                    buildId = buildJob.getNumber().toString()
                }
            }
        }
        stage('Run Tests'){
            parallel {
                stage('Quicklook Tests') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps {

                        processPayaraArtifacts(buildId)

                        setupDomain()

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """rm  ~/test\\|sa.mv.db  || true"""
                        sh """mvn -B -V -ff -e clean test --strict-checksums -Pall \
                        -Dglassfish.home=\"${pwd()}/payara7/glassfish\" \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -f appserver/tests/quicklook/pom.xml"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            junit 'appserver/tests/quicklook/test-output/QuickLookTests/*.xml'
                            stopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'quicklook-log.zip'
                        }
                    }
                }
                stage('Payara Samples Tests') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps {

                        processPayaraArtifacts(buildId, true)
                        setupDomain()

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -V -B -ff clean install --strict-checksums -Ppayara-server-remote,playwright \
                         -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                         -Djavax.xml.accessExternalSchema=all \
                         -Dsurefire.rerunFailingTestsCount=2 \
                         -Dfailsafe.rerunFailingTestsCount=2 \
                         -f appserver/tests/payara-samples """
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            processReportAndStopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'samples-log.zip'
                        }
                    }
                }
                stage('MicroProfile Config TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    steps {
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running MP Config TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            build job: 'TCKs/MP-TCKs',
                            parameters: [
                                string(name: 'buildProject', value: "Build"),
                                string(name: 'payaraBuildNumber', value: buildId),
                                string(name: 'repoOrg', value: 'payara'),
                                string(name: 'testBranchCommitOrTag', value: 'microprofile-6.1-Payara7'),
                                string(name: 'suites', value: 'Config'),
                                string(name: 'jdkVer', value: 'zulu-21'),
                                string(name: 'distribution', value: 'full')
                            ]
                        }
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran MP Config TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                }
                stage('MicroProfile Fault Tolerance TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    steps {
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running MP Fault Tolerance TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            build job: 'TCKs/MP-TCKs',
                            parameters: [
                                string(name: 'buildProject', value: 'Build'),
                                string(name: 'payaraBuildNumber', value: buildId),
                                string(name: 'repoOrg', value: 'payara'),
                                string(name: 'testBranchCommitOrTag', value: 'microprofile-6.1-Payara7'),
                                string(name: 'suites', value: 'Fault-Tolerance'),
                                string(name: 'jdkVer', value: 'zulu-21'),
                                string(name: 'distribution', value: 'full')
                            ]
                        }
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran MP Fault Tolerance TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                }
                stage('MicroProfile Health TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    steps {
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running MP Health TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            build job: 'TCKs/MP-TCKs',
                            parameters: [
                                string(name: 'buildProject', value: 'Build'),
                                string(name: 'payaraBuildNumber', value: buildId),
                                string(name: 'repoOrg', value: 'payara'),
                                string(name: 'testBranchCommitOrTag', value: 'microprofile-6.1-Payara7'),
                                string(name: 'suites', value: 'Health'),
                                string(name: 'jdkVer', value: 'zulu-21'),
                                string(name: 'distribution', value: 'full')
                            ]
                        }
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran MP Health TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                }
                stage('MicroProfile JWT Auth TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    steps {
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running MP JWT Auth TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            build job: 'TCKs/MP-TCKs',
                            parameters: [
                                string(name: 'buildProject', value: 'Build'),
                                string(name: 'payaraBuildNumber', value: buildId),
                                string(name: 'repoOrg', value: 'payara'),
                                string(name: 'testBranchCommitOrTag', value: 'microprofile-6.1-Payara7'),
                                string(name: 'suites', value: 'JWT-Auth'),
                                string(name: 'jdkVer', value: 'zulu-21'),
                                string(name: 'distribution', value: 'full')
                            ]
                        }
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran MP JWT Auth TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                }
                stage('MicroProfile Metrics TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    steps {
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running MP Metrics TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            build job: 'TCKs/MP-TCKs',
                            parameters: [
                                string(name: 'buildProject', value: 'Build'),
                                string(name: 'payaraBuildNumber', value: buildId),
                                string(name: 'repoOrg', value: 'payara'),
                                string(name: 'testBranchCommitOrTag', value: 'microprofile-6.1-Payara7'),
                                string(name: 'suites', value: 'Metrics'),
                                string(name: 'jdkVer', value: 'zulu-21'),
                                string(name: 'distribution', value: 'full')
                            ]
                        }
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran MP Metrics TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                }
                stage('MicroProfile OpenAPI TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    steps {
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running MP OpenAPI TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            build job: 'TCKs/MP-TCKs',
                            parameters: [
                                string(name: 'buildProject', value: 'Build'),
                                string(name: 'payaraBuildNumber', value: buildId),
                                string(name: 'repoOrg', value: 'payara'),
                                string(name: 'testBranchCommitOrTag', value: 'microprofile-6.1-Payara7'),
                                string(name: 'suites', value: 'OpenAPI'),
                                string(name: 'jdkVer', value: 'zulu-21'),
                                string(name: 'distribution', value: 'full')
                            ]
                        }
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran MP OpenAPI TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                }
                stage('MicroProfile OpenTelemetry Tracing TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    steps {
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running MP OpenTelemetry Tracing TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            build job: 'TCKs/MP-TCKs',
                            parameters: [
                                string(name: 'buildProject', value: 'Build'),
                                string(name: 'payaraBuildNumber', value: buildId),
                                string(name: 'repoOrg', value: 'payara'),
                                string(name: 'testBranchCommitOrTag', value: 'microprofile-6.1-Payara7'),
                                string(name: 'suites', value: 'OpenTelemetry-Tracing'),
                                string(name: 'jdkVer', value: 'zulu-21'),
                                string(name: 'distribution', value: 'full')
                            ]
                        }
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran MP OpenTelemetry Tracing TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                }
                stage('MicroProfile OpenTracing TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    steps {
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running MP OpenTracing TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            build job: 'TCKs/MP-TCKs',
                            parameters: [
                                string(name: 'buildProject', value: 'Build'),
                                string(name: 'payaraBuildNumber', value: buildId),
                                string(name: 'repoOrg', value: 'payara'),
                                string(name: 'testBranchCommitOrTag', value: 'microprofile-6.1-Payara7'),
                                string(name: 'suites', value: 'OpenTracing'),
                                string(name: 'jdkVer', value: 'zulu-21'),
                                string(name: 'distribution', value: 'full')
                            ]
                        }
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran MP OpenTracing TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                }
                stage('MicroProfile REST Client TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    steps {
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running MP REST Client TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            build job: 'TCKs/MP-TCKs',
                            parameters: [
                                string(name: 'buildProject', value: 'Build'),
                                string(name: 'payaraBuildNumber', value: buildId),
                                string(name: 'repoOrg', value: 'payara'),
                                string(name: 'testBranchCommitOrTag', value: 'microprofile-6.1-Payara7'),
                                string(name: 'suites', value: 'Rest-Client'),
                                string(name: 'jdkVer', value: 'zulu-21'),
                                string(name: 'distribution', value: 'full')
                            ]
                        }
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran MP REST Client TCK  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                }
                stage('EE8 Tests') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps {
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running EE8 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            build job: 'Miscellaneous/Run-EE8-Samples',
                            parameters: [
                                string(name: 'payaraBuildNumber', value: "${buildId}"),
                                string(name: 'buildProject', value: "Build/Build"),
                                string(name: 'repoOrg', value: 'Payara'),
                                string(name: 'buildSpecificBranchCommitOrTag', value: 'Payara7'),
                                string(name: 'jdkChoice', value: 'zulu-21'),
                                string(name: 'arquillianProfile', value: 'payara-server-remote')
                            ]
                        }
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                }
                stage('CargoTracker Tests') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps {
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running CargoTracker tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        build job: 'Miscellaneous/Run-CargoTracker',
                        parameters: [
                            string(name: 'payaraBuildNumber', value: "${buildId}"),
                            string(name: 'buildProject', value: "Build/Build"),
                            string(name: 'repoOrg', value: 'Payara'),
                            string(name: 'buildSpecificBranchCommitOrTag', value: 'Payara7'),
                            string(name: 'jdkChoice', value: 'zulu-21'),
                            string(name: 'arquillianProfile', value: 'payara-server-remote')
                        ]
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                }
                stage('EE7 Tests') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps {
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running EE7 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            build job: 'Miscellaneous/Run-EE7-Samples',
                            parameters: [
                                string(name: 'payaraBuildNumber', value: "${buildId}"),
                                string(name: 'buildProject', value: "Build/Build"),
                                string(name: 'repoOrg', value: 'Payara'),
                                string(name: 'buildSpecificBranchCommitOrTag', value: 'Payara7'),
                                string(name: 'jdkChoice', value: 'zulu-21'),
                                string(name: 'arquillianProfile', value: 'payara-server-remote')
                            ]
                        }
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                }
                stage('Payara Functional Tests') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps {
                        processPayaraArtifacts(buildId, true)

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Building dependencies  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -V -B -ff clean install --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -DskipTests \
                        -f appserver/tests/payara-samples -pl fish.payara.samples:payara-samples \
                        -pl fish.payara.samples:samples-test-utils -pl fish.payara.samples:test-domain-setup \
                        -pl fish.payara.samples:payara-samples-profiled-tests"""

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test with Payara Micro  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -V -B -ff clean install --strict-checksums -Ppayara-micro-managed,install-deps \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -f appserver/tests/functional/payara-micro """

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test with Payara Embedded  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -V -B -ff clean verify --strict-checksums -PFullProfile \
                        -Dversion=${pom.version} \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -f appserver/tests/functional/embeddedtest """

                        sh """mvn -V -B -ff clean verify --strict-checksums -PWebProfile \
                        -Dversion=${pom.version} \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -f appserver/tests/functional/embeddedtest """

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running asadmin tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        setupDomain()
                        sh """python3 appserver/tests/functional/asadmin/run_all_tests.py \
                        --asadmin ${pwd()}/payara7/bin/asadmin"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            stopDomain()
                        }
                        cleanup {
                            processReport()
                            saveLogsAndCleanup 'asadmin-log.zip'
                        }
                    }
                }
            }
        }
    }
    post {
        unsuccessful {
            script {
                if (currentBuild.result == 'UNSTABLE') {
                    currentBuild.result = 'SUCCESS'
                    echo "Build result changed from UNSTABLE to SUCCESS for GitHub reporting"
                }
            }
        }
    }
}

void makeDomain() {
    script{
        ASADMIN = "./payara7/bin/asadmin"
        DOMAIN_NAME = "test-domain"
    }
    sh "${ASADMIN} create-domain --nopassword ${DOMAIN_NAME}"
}

void setupDomain() {
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Setting up domain  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
    makeDomain()
    sh "${ASADMIN} start-domain ${DOMAIN_NAME}"
    sh "${ASADMIN} start-database || true"
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Domain setup complete  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
}

void processReportAndStopDomain() {
    junit '**/target/*-reports/*.xml'
    sh "${ASADMIN} stop-domain ${DOMAIN_NAME}"
    sh "${ASADMIN} stop-database || true"
}

void processReport() {
    junit '**/target/*-reports/*.xml'
}

void stopDomain() {
    sh "${ASADMIN} stop-domain ${DOMAIN_NAME}"
    sh "${ASADMIN} stop-database || true"
}

void saveLogsAndCleanup(String logArchiveName) {
    zip archive: true, dir: "./payara7/glassfish/domains/${DOMAIN_NAME}/logs", glob: 'server.*', zipFile: logArchiveName
    echo 'tidying up after tests: '
    sh "rm -f -v *.zip"
    sh "${ASADMIN} delete-domain ${DOMAIN_NAME}"
}

void updatePomPayaraVersion(String payaraVersion) {
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Updating pom.xml payara.version property for Shrinkwrap resolver  *#*#*#*#*#*#*#*#*#*#*#*#'
    echo "Setting payara.version to: ${payaraVersion}"
    sh script: "sed -i \"s/payara\\.version>.*<\\/payara\\.version>/payara\\.version>${payaraVersion}<\\/payara\\.version>/g\" pom.xml", label: "Update pom.xml payara.version property"
}

void processPayaraArtifacts(String buildId, boolean restoreMavenRepo = false) {
    // Grab Payara artifact from given job
    echo "Grabbing artifacts from Build/Build: ${buildId}"

    // Determine filter based on whether we need Maven repository
    def artifactFilter = 'payara-bom.pom,payara-embedded-all.jar,payara-embedded-web.jar,payara-micro.jar,payara-web.zip,payara.zip'
    if (restoreMavenRepo) {
        artifactFilter += ',maven-repository.zip'
    }

    copyArtifacts(projectName: "Build/Build",
        selector: specific("${buildId}"),
        filter: artifactFilter,
        target: 'artifacts/')

    // If Maven repository is included, restore it
    if (restoreMavenRepo && fileExists('artifacts/maven-repository.zip')) {
        echo "Restoring Maven repository"
        sh 'unzip -o artifacts/maven-repository.zip -d ~/.m2/repository'
        echo "Maven repository restored to ~/.m2/repository"
    }

    def payaraHome = "${env.WORKSPACE}/payara7"
    echo "payaraHome = ${payaraHome}"

    // Get the Payara version from the extracted distribution
    def payaraVersion = getPayaraVersion()
    echo "Extracted Payara Version from ${payaraHome}/glassfish/config/branding/glassfish-version.properties: ${payaraVersion}"

    if (!payaraVersion) {
        error("Failed to determine Payara version from glassfish-version.properties")
    }

    // Install the distributions for any jobs that try to pull directly from Maven
    // Move into a different directory to stop Maven using the current pom
    echo "Installing copied artifacts to local maven repo"
    def installArtifactsScript = """
        mkdir tmp
        cd tmp
        mvn install:install-file -DgeneratePom=true -DgroupId=fish.payara.distributions -DartifactId=payara -Dversion=${payaraVersion} -Dpackaging=zip -Dfile=${env.WORKSPACE}/artifacts/payara.zip
        mvn install:install-file -DgeneratePom=true -DgroupId=fish.payara.distributions -DartifactId=payara-web -Dversion=${payaraVersion} -Dpackaging=zip -Dfile=${env.WORKSPACE}/artifacts/payara-web.zip
        mvn install:install-file -DgeneratePom=true -DgroupId=fish.payara.extras -DartifactId=payara-micro -Dversion=${payaraVersion} -Dpackaging=jar -Dfile=${env.WORKSPACE}/artifacts/payara-micro.jar
        mvn install:install-file -DgroupId=fish.payara.api -DartifactId=payara-bom -Dversion=${payaraVersion} -Dpackaging=pom -Dfile=${env.WORKSPACE}/artifacts/payara-bom.pom
        cd ..
    """
    sh installArtifactsScript

    echo "Updating pom.xml payara.version property for Shrinkwrap resolver"
    def updatePayaraVersionScript = """
        sed -i "s/payara\\.version>.*<\\/payara\\.version>/payara\\.version>${payaraVersion}<\\/payara\\.version>/g" pom.xml
    """
    sh updatePayaraVersionScript
}

def extractPayara() {
    if (!fileExists('payara7')) {
        echo 'Extracting payara.zip...'
        sh 'unzip -q artifacts/payara.zip -d .'
    } else {
        echo 'payara7 directory already exists'
    }
}

def getPayaraVersion() {
    extractPayara()

    def versionFile = "${pwd()}/payara7/glassfish/config/branding/glassfish-version.properties"
    if (!fileExists(versionFile)) {
        error("glassfish-version.properties not found at: ${versionFile}")
    }

    def props = readProperties file: versionFile
    return "${props.major_version}.${props.minor_version}.${props.update_version}"
}