# Payara Server Admin Commands Functional Tests

This directory contains functional tests for Payara Server's asadmin commands. These tests verify the functionality of various administrative commands.

## Prerequisites

- Python 3.6 or higher
- Payara Server 6 or later
- Access to a running Payara Server instance

## Setup
The script expects a defined PAYARA_HOME environment variable to point to the Payara Server installation directory.
e.g.:
```bash
    export PAYARA_HOME=/path/to/payara6
```

Navigate to the tests directory:
```bash
    cd appserver/tests/functional/asadmin
    python3 run_all_tests.py
```


