#!groovy

@Library('DEVOPS-721') _

PRTestPipeline {
    jdk = 'zulu-8'
    agent_label = 'PR-Test-Agent'
    test_suites = [[suite_name: 'Quicklook', suite_location: 'Payara'],
                   [suite_name: 'Payara-Samples', suite_location: 'Payara'],
                   [suite_name: 'patched-src-javaee7-samples'],
                   [suite_name: 'patched-src-javaee8-samples'], 
                   [suite_name: 'cargotracker'], 
                   [suite_name: 'MicroProfile-TCK-Runners', branch: 'microprofile-4.0']]
}
