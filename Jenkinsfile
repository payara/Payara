#!groovy

@Library('PayaraTestLibrary') _

PRTestPipeline {
    jdk = 'zulu-11'
    agent_label = 'PR-Test-Agent'
    test_suites = [[suite_name: 'Quicklook', suite_location: 'Payara', jdk: 'zulu-11'],
                   [suite_name: 'Payara-Samples', suite_location: 'Payara', jdk: 'zulu-11'],
                   [suite_name: 'patched-src-javaee7-samples', branch: 'Payara6', jdk: 'zulu-11'],
                   [suite_name: 'patched-src-javaee8-samples', branch: 'Payara6', jdk: 'zulu-11'],
                   [suite_name: 'cargotracker', branch: 'Payara6', jdk: 'zulu-11'],
                   [suite_name: 'MicroProfile-TCK-Runners', branch: 'microprofile-5.0', jdk: 'zulu-11']]
}
