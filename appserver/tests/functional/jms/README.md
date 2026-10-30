# Payara JMS Functional Tests #

This directory contains functional tests for Payara Server's asadmin commands. These tests verify the functionality of jms and its relationship with OpenMQ that is bundled with Payara Server.

# Prerequisites #

- Python 3.6 or higher
- Payara Server 7 or later
- Access to a running Payara Server instance

## Setup
The script expects a defined PAYARA_HOME environment variable to point to the Payara Server installation directory.
e.g.:
```bash
    export PAYARA_HOME=/path/to/payara7
```

Navigate to the tests directory:

```bash
    cd appserver/tests/functional/jms
    python3 test_jms_ping.py
```

By default, the test_jms_pings.py test is using a timeout sleep of 3 seconds to wait between calls, and that value can be changed by using the property **--sleep_time**