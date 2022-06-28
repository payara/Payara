#!groovy

@Library('PayaraTestLibrary') _

PRTestPipeline {
    jdk = 'zulu-8'
    agent_label = 'PR-Test-Agent'
    test_suites = ['Quicklook', 'Payara-Samples', 'patched-src-javaee7-samples', 'patched-src-javaee8-samples', 'cargotracker', 'MicroProfile-TCK-Runners']
}