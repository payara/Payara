#!groovy

@Library('PayaraTestLibrary') _

PRTestPipeline {
    jdk = 'zulu-8'
    agent_label = 'PR-Test-Agent'
    test_suites = [[suite_name: 'Quicklook', suite_location: 'Payara'],
                   [suite_name: 'Payara-Samples', suite_location: 'Payara'],
                   [suite_name: 'patched-src-javaee7-samples', branch: 'Payara6'],
                   [suite_name: 'patched-src-javaee8-samples', branch: 'Payara6'],
                   [suite_name: 'cargotracker', branch: 'Payara6'],
                   [suite_name: 'MicroProfile-TCK-Runners', branch: 'microprofile-5.0']]
}
