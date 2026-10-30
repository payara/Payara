#!/usr/bin/env python3

"""
Test for listing health check services.
"""

import os
import sys
import logging
import argparse
import subprocess
from typing import Tuple

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class AsadminCommandError(Exception):
    """Exception raised for asadmin command failures."""
    pass

class ListHealthCheckServicesTest:
    """Test class for listing health check services."""

    def __init__(self, asadmin_path: str = None, host: str = None, port: int = None,
                 user: str = None, password: str = None):
        """Initialize the test with connection details."""
        self.asadmin_path = asadmin_path or os.environ.get('PAYARA_HOME', '') + '/bin/asadmin' or 'asadmin'
        self.host = host or 'localhost'
        self.port = port or 4848
        self.user = user
        self.password = password
        self.expected_services = [
            'healthcheck-mp',
            'healthcheck-cpu',
            'healthcheck-gc',
            'healthcheck-heap',
            'healthcheck-threads',
            'healthcheck-machinemem',
            'healthcheck-cpool',
            'healthcheck-stuck'
        ]

    def _run_asadmin(self, *args: str) -> Tuple[bool, str, str]:
        """Run an asadmin command and return (success, stdout, stderr)."""
        cmd = [self.asadmin_path]

        # Add connection parameters if not local
        if self.host != 'localhost' or self.port != 4848:
            cmd.extend(['--host', self.host, '--port', str(self.port)])

        # Add authentication if provided
        if self.user and self.password:
            cmd.extend(['--user', self.user, '--password', self.password])

        cmd.extend(args)

        try:
            logger.debug(f"Running command: {' '.join(cmd)}")
            result = subprocess.run(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                check=False
            )

            if result.returncode != 0:
                logger.error(f"Command failed with code {result.returncode}: {result.stderr.strip()}")
                return False, result.stdout.strip(), result.stderr.strip()

            return True, result.stdout.strip(), result.stderr.strip()

        except Exception as e:
            error_msg = f"Error running command: {e}"
            logger.error(error_msg)
            return False, "", error_msg

    def test_list_healthcheck_services(self) -> bool:
        """Test that health check services are listed correctly."""
        logger.info("Testing list-healthcheck-services command...")

        # Run the command
        success, output, error = self._run_asadmin('list-healthcheck-services')
        if not success:
            logger.error(f"Failed to list health check services: {error}")
            return False

        # Check the output
        if not output:
            logger.error("No output from list-healthcheck-services command")
            return False

        # Check for the header
        if "Available Health Check Services:" not in output:
            logger.error("Header not found in output")
            return False

        # Check for each expected service
        missing_services = []
        for service in self.expected_services:
            if service not in output:
                missing_services.append(service)

        if missing_services:
            logger.error(f"Missing expected services in output: {', '.join(missing_services)}")
            return False

        logger.info("All expected health check services found in output")
        return True

    def run_tests(self) -> bool:
        """Run all tests."""
        try:
            if not self.test_list_healthcheck_services():
                return False

            logger.info("All tests completed successfully!")
            return True

        except Exception as e:
            logger.error(f"Test failed: {e}")
            return False

def parse_args():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description='Test listing health check services')
    parser.add_argument('--asadmin',
                      help='Path to asadmin script (default: $PAYARA_HOME/bin/asadmin or asadmin in PATH)')
    parser.add_argument('--host', default='localhost',
                      help='DAS host (default: localhost)')
    parser.add_argument('--port', type=int, default=4848,
                      help='DAS port (default: 4848)')
    parser.add_argument('--user', help='Admin user (if authentication is required)')
    parser.add_argument('--password', help='Admin password (if authentication is required)')
    parser.add_argument('--debug', action='store_true',
                      help='Enable debug logging')
    return parser.parse_args()

def main():
    """Main function."""
    args = parse_args()

    if args.debug:
        logging.getLogger().setLevel(logging.DEBUG)

    test = ListHealthCheckServicesTest(
        asadmin_path=args.asadmin,
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password
    )

    success = test.run_tests()
    return 0 if success else 1

if __name__ == "__main__":
    sys.exit(main())
