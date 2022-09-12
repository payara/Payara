// Perform the base behaviour
MPLModule('Test Setup', CFG);

def testSuite = CFG.suite.suite_name

// Perform suite specific behaviour
if(testSuite.equals("Payara-Samples")) {
    MPLModule('Payara Samples Setup', CFG)
} else {
    MPLModule('Quicklook Setup', CFG)
}
