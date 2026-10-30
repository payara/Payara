# Payara Deployment Group Deployment Tests

This test suite verifies that deployment to a Payara deployment group works correctly, ensuring applications are deployed to all member instances and redeployment propagates properly.

## Overview

The tests verify:
- Applications deployed to a deployment group appear on every member instance
- Applications are listed when querying the deployment group target
- Undeploying from a deployment group removes the app from all instances
- Redeploying to a deployment group propagates to all instances
- Applications are accessible via HTTP on all instances

## Prerequisites

- Payara Server installed and running with DAS accessible
- Python 3.8 or higher
- Python dependencies: pytest, requests

## Setup

1. Install Python dependencies:
```bash
pip install -r requirements.txt
```

2. Set the `PAYARA_HOME` environment variable:
```bash
export PAYARA_HOME=/path/to/payara
```

3. Ensure the test WAR file exists:
```bash
../test-apps/clusterjsp.war
```

## Running the Tests

Run all tests:
```bash
export PAYARA_HOME=/path/to/payara
pytest test_deployment_group.py -v -s
```

Run specific tests:
```bash
pytest test_deployment_group.py::TestDeploymentGroupDeployment::test_deploy_to_group_appears_on_all_instances -v
```

## Test Configuration

The test suite automatically:
- Starts the Payara domain before all tests
- Stops the Payara domain after all tests complete
- Logs all operations with detailed output
- Uses `clusterjsp.war` from the ../test-apps folder

### Custom HTTP Port Base

By default, test instances use HTTP ports starting from 28080. To customize:

```bash
pytest test_deployment_group.py --instance-http-port-base=28080 -v
```

## Troubleshooting

**Error: PAYARA_HOME environment variable is not set**
- Set the PAYARA_HOME environment variable to your Payara installation directory

**Error: Test WAR file not found**
- Ensure `clusterjsp.war` exists in the `../test-apps/` folder

**HTTP requests failing**
- Ensure instances are running and accessible on the configured ports
- Check firewall settings
- Verify instance HTTP ports are correctly configured

## License

This test suite is provided as-is for testing Payara deployment group functionality.
