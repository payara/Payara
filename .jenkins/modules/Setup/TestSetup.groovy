// Perform the base behaviour
MPLModule('Test Setup', CFG);

// Perform suite specific behaviour
if(CFG.suite.equals("Payara-Samples")) {
    MPLModule('Payara Samples Setup', CFG)
} else {
    MPLModule('Quicklook Setup', CFG)
}