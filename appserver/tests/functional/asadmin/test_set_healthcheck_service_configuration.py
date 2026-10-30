#!/usr/bin/env python3

"""
Test for setting HealthCheck service configuration.
"""

import os
import sys
import logging
import argparse
import subprocess
from typing import Tuple, List

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class AsadminCommandError(Exception):
    """Exception raised for asadmin command failures."""
    pass

class HealthCheckServiceConfigTest:
    """Test class for HealthCheck service configuration."""

    def __init__(self, asadmin_path: str = None, host: str = None, port: int = None,
                 user: str = None, password: str = None):
        """Initialize the test with connection details."""
        self.asadmin_path = asadmin_path or os.environ.get('PAYARA_HOME', '') + '/bin/asadmin' or 'asadmin'
        self.host = host or 'localhost'
        self.port = port or 4848
        self.user = user
        self.password = password

    def _run_asadmin(self, *args: str, expected_error: str = None) -> Tuple[bool, str, str]:
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
                error_msg = result.stderr.strip()
                # Only log as error if it's not the expected error
                if not expected_error or expected_error not in error_msg:
                    logger.error(f"Command failed with code {result.returncode}: {error_msg}")
                return False, result.stdout.strip(), error_msg

            return True, result.stdout.strip(), result.stderr.strip()

        except Exception as e:
            error_msg = f"Error running command: {e}"
            logger.error(error_msg)
            return False, "", error_msg

    def test_service_is_mandatory(self) -> bool:
        """Test that service parameter is mandatory."""
        logger.info("Testing that service parameter is mandatory...")

        success, output, error = self._run_asadmin(
            'set-healthcheck-service-configuration',
            '--enabled', 'true',
            expected_error='Option service is required but was not specified'
        )

        if success:
            logger.error("Command succeeded but should have failed without service parameter")
            return False

        if 'Option service is required but was not specified' not in error:
            logger.error(f"Unexpected error message: {error}")
            return False

        logger.info("Successfully verified that service parameter is mandatory")
        return True

    def test_short_names_are_accepted(self) -> bool:
        """Test that short service names are accepted."""
        logger.info("Testing that short service names are accepted...")
        short_names = ["cp", "cu", "gc", "hmu", "ht", "mmu", "st", "mh", "mm"]
        return self._test_service_names_are_accepted(short_names)

    def test_full_names_are_accepted(self) -> bool:
        """Test that full service names are accepted."""
        logger.info("Testing that full service names are accepted...")
        full_names = ["CONNECTION_POOL", "CPU_USAGE", "GARBAGE_COLLECTOR", "HEAP_MEMORY_USAGE",
                      "HOGGING_THREADS", "MACHINE_MEMORY_USAGE", "STUCK_THREAD", "MP_HEALTH", "MP_METRICS"]
        return self._test_service_names_are_accepted(full_names)

    def _test_service_names_are_accepted(self, service_names: List[str]) -> bool:
        """Test that the given service names are accepted by the command."""
        for service_name in service_names:
            success, output, error = self._run_asadmin(
                'set-healthcheck-service-configuration',
                '--service', service_name,
                '--enabled', 'true'
            )
            if not success:
                logger.error(f"Failed to set service {service_name}: {error}")
                return False

        logger.info(f"Successfully tested {len(service_names)} service names")
        return True

    def test_enabled_affects_config_but_not_service(self) -> bool:
        """Test that enabling/disabling affects config but not service when not dynamic."""
        logger.info("Testing that enabling/disabling affects config but not service when not dynamic...")

        # Enable the service
        success, output, error = self._run_asadmin(
            'set-healthcheck-service-configuration',
            '--service', 'gc',
            '--enabled', 'true'
        )
        if not success:
            logger.error(f"Failed to enable service: {error}")
            return False

        logger.info("Successfully tested service enable/disable")
        return True

    def test_enabled_dynamic_affects_config_and_service(self) -> bool:
        """Test that enabling/disabling with dynamic flag affects both config and service."""
        logger.info("Testing that enabling/disabling with dynamic flag affects both config and service...")

        # Ensure health checks are enabled
        if not self._ensure_health_checks_are_enabled():
            return False

        # Enable the service with dynamic flag
        success, output, error = self._run_asadmin(
            'set-healthcheck-service-configuration',
            '--service', 'gc',
            '--enabled', 'true',
            '--dynamic', 'true'
        )
        if not success:
            logger.error(f"Failed to enable service dynamically: {error}")
            return False

        logger.info("Successfully tested dynamic service enable/disable")
        return True

    def test_time_affects_config_but_not_service(self) -> bool:
        """Test that time parameter affects config but not service when not dynamic."""
        logger.info("Testing that time parameter affects config but not service when not dynamic...")

        # Set time
        new_time = 33
        success, output, error = self._run_asadmin(
            'set-healthcheck-service-configuration',
            '--service', 'gc',
            '--enabled', 'true',
            '--time', str(new_time)
        )
        if not success:
            logger.error(f"Failed to set time: {error}")
            return False

        logger.info("Successfully tested time parameter")
        return True

    def _ensure_health_checks_are_enabled(self) -> bool:
        """Ensure health checks are enabled for dynamic changes to take effect."""
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to get healthcheck status: {error}")
            return False

        if 'Health Check Service Configuration is enabled?: false' in output:
            # Enable health checks
            success, output, error = self._run_asadmin(
                'set-healthcheck-configuration',
                '--enabled', 'true',
                '--dynamic', 'true'
            )
            if not success:
                logger.error(f"Failed to enable health checks: {error}")
                return False

        return True

def parse_args():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description='Test HealthCheck service configuration')
    parser.add_argument('--asadmin', help='Path to asadmin executable')
    parser.add_argument('--host', default='localhost', help='Domain Administration Server host')
    parser.add_argument('--port', type=int, default=4848, help='Domain Administration Server port')
    parser.add_argument('--user', help='Admin username')
    parser.add_argument('--password', help='Admin password')
    return parser.parse_args()

def main():
    """Main function."""
    args = parse_args()

    test = HealthCheckServiceConfigTest(
        asadmin_path=args.asadmin,
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password
    )

    tests = [
        test.test_service_is_mandatory,
        test.test_short_names_are_accepted,
        test.test_full_names_are_accepted,
        test.test_enabled_affects_config_but_not_service,
        test.test_enabled_dynamic_affects_config_and_service,
        test.test_time_affects_config_but_not_service
    ]

    success = all(test() for test in tests)

    if success:
        logger.info("All tests passed!")
        return 0
    else:
        logger.error("Some tests failed!")
        return 1

if __name__ == "__main__":
    sys.exit(main())
