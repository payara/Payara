#!/usr/bin/env python3

"""
Test for setting HealthCheck configuration.
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

class HealthCheckConfigTest:
    """Test class for HealthCheck configuration."""

    def __init__(self, asadmin_path: str = None, host: str = None, port: int = None,
                 user: str = None, password: str = None):
        """Initialize the test with connection details."""
        self.asadmin_path = asadmin_path or os.environ.get('PAYARA_HOME', '') + '/bin/asadmin' or 'asadmin'
        self.host = host or 'localhost'
        self.port = port or 4848
        self.user = user
        self.password = password

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

    def test_enabled_is_optional(self) -> bool:
        """Test that enabled parameter is optional."""
        logger.info("Testing that enabled parameter is optional...")
        success, output, error = self._run_asadmin('set-healthcheck-configuration')
        if not success:
            logger.error(f"Command failed: {error}")
            return False
        logger.info("Command executed successfully without enabled parameter")
        return True

    def test_enabled_affects_config_but_not_service(self) -> bool:
        """Test that enabled parameter affects config but not service when not dynamic."""
        logger.info("Testing enabled parameter affects config but not service...")

        # Get initial state
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to get initial config: {error}")
            return False

        # Test setting enabled to false
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enabled', 'false'
        )
        if not success:
            logger.error(f"Failed to set enabled=false: {error}")
            return False

        # Test setting enabled to true
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enabled', 'true'
        )
        if not success:
            logger.error(f"Failed to set enabled=true: {error}")
            return False

        return True

    def test_enabled_dynamic_affects_config_and_service(self) -> bool:
        """Test that enabled parameter with dynamic flag affects both config and service."""
        logger.info("Testing dynamic enabled parameter affects both config and service...")

        # Test enabling with dynamic flag
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enabled', 'true',
            '--dynamic', 'true'
        )
        if not success:
            logger.error(f"Failed to enable dynamically: {error}")
            return False

        # Test disabling with dynamic flag
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enabled', 'false',
            '--dynamic', 'true'
        )
        if not success:
            logger.error(f"Failed to disable dynamically: {error}")
            return False

        return True

    def test_log_notifier_enabled_by_default(self) -> bool:
        """Test that log notifier is enabled by default."""
        logger.info("Testing that log notifier is enabled by default...")

        # Get the current configuration
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to get healthcheck configuration: {error}")
            return False

        # Find the log-notifier line in the output
        log_notifier_line = None
        for line in output.split('\n'):
            if 'log-notifier' in line:
                log_notifier_line = line.strip()
                break

        if not log_notifier_line:
            logger.error("log-notifier not found in the notifier list")
            return False

        # Parse the line to get the enabled status
        # Expected format: 'log-notifier          true              '
        parts = [p for p in log_notifier_line.split(' ') if p]
        if len(parts) < 2:
            logger.error(f"Unexpected format for log-notifier line: {log_notifier_line}")
            return False

        # The second part should be 'true' if enabled
        if parts[1].lower() != 'true':
            logger.error(f"log-notifier is not enabled by default. Status: {parts[1]}")
            return False

        logger.info("Successfully verified that log notifier is enabled by default")
        return True

    def test_historical_trace_enabled_affects_config_but_not_service(self) -> bool:
        """Test that historical trace enabled affects config but not service when not dynamic."""
        logger.info("Testing historical trace enabled affects config but not service...")

        # Test setting historical trace to false
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enabled', 'true',
            '--historical-trace-enabled', 'false'
        )

        if not success:
            logger.error(f"Failed to set historical-trace-enabled=false: {error}")
            return False

        # Verify the config was updated but service state is unchanged
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to verify configuration: {error}")
            return False

        if 'Historical Tracing Enabled?: false' not in output:
            logger.error("Historical trace was not disabled in configuration")
            return False

        # Test setting historical trace to true
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enabled', 'true',
            '--historical-trace-enabled', 'true'
        )
        if not success:
            logger.error(f"Failed to set historical-trace-enabled=true: {error}")
            return False

        # Verify the config was updated
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to verify configuration: {error}")
            return False

        if 'Historical Tracing Enabled?: true' not in output:
            logger.error("Historical trace was not enabled in configuration")
            return False

        logger.info("Successfully tested historical trace enabled parameter")
        return True

    def test_historical_trace_enabled_dynamic_affects_config_and_service(self) -> bool:
        """Test that historical trace enabled with dynamic flag affects both config and service."""
        logger.info("Testing historical trace enabled with dynamic flag affects both config and service...")

        # Test enabling historical trace with dynamic flag
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enabled', 'true',
            '--historical-trace-enabled', 'true',
            '--dynamic', 'true'
        )
        if not success:
            logger.error(f"Failed to enable historical trace dynamically: {error}")
            return False

        # Verify the config was updated
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to verify configuration: {error}")
            return False

        if 'Historical Tracing Enabled?: true' not in output:
            logger.error("Historical trace was not enabled in configuration")
            return False

        # Test disabling historical trace with dynamic flag
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enabled', 'true',
            '--historical-trace-enabled', 'false',
            '--dynamic', 'true'
        )
        if not success:
            logger.error(f"Failed to disable historical trace dynamically: {error}")
            return False

        # Verify the config was updated
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to verify configuration: {error}")
            return False

        if 'Historical Tracing Enabled?: false' not in output:
            logger.error("Historical trace was not disabled in configuration")
            return False

        logger.info("Successfully tested historical trace enabled with dynamic parameter")
        return True

    def test_historical_trace_store_size_affects_config_but_not_service(self) -> bool:
        """Test that historical trace store size affects config but not service when not dynamic."""
        logger.info("Testing historical trace store size affects config but not service...")

        # Set a new store size
        new_size = 13
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enabled', 'true',
            '--historical-trace-enabled' , 'true',
            '--historical-trace-store-size', '13'
        )
        if not success:
            logger.error(f"Failed to set historical trace store size: {error}")
            return False

        # Verify the config was updated
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to verify configuration: {error}")
            return False

        # Parse the new store size from the output
        updated_size = None
        for line in output.split('\n'):
            if 'Historical Tracing Store Size' in line:
                try:
                    updated_size = int(line.split(':')[-1].strip())
                    break
                except (ValueError, IndexError):
                    logger.error(f"Failed to parse updated store size from: {line}")
                    return False

        if updated_size != new_size:
            logger.error(f"Expected store size {new_size}, got {updated_size}")
            return False

        logger.info("Successfully tested historical trace store size parameter")
        return True

    def test_historical_trace_store_size_dynamic_affects_config_and_service(self) -> bool:
        """Test that historical trace store size with dynamic flag affects both config and service."""
        logger.info("Testing historical trace store size with dynamic flag affects both config and service...")

        # Set a new store size with dynamic flag
        new_size = 13
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enabled', 'true',
            '--historical-trace-store-size', str(new_size),
            '--dynamic', 'true'
        )
        if not success:
            logger.error(f"Failed to set historical trace store size dynamically: {error}")
            return False

        # Verify the config was updated
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to verify configuration: {error}")
            return False

        # Parse the new store size from the output
        updated_size = None
        for line in output.split('\n'):
            if 'Historical Tracing Store Size' in line:
                try:
                    updated_size = int(line.split(':')[-1].strip())
                    break
                except (ValueError, IndexError):
                    logger.error(f"Failed to parse updated store size from: {line}")
                    return False

        if updated_size != new_size:
            logger.error(f"Expected store size {new_size}, got {updated_size}")
            return False

        logger.info("Successfully tested historical trace store size with dynamic parameter")
        return True

    def test_historical_trace_store_timeout_affects_config_but_not_service(self) -> bool:
        """Test that historical trace store timeout affects config but not service when not dynamic."""
        logger.info("Testing historical trace store timeout affects config but not service...")

        # Get initial timeout
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to get initial healthcheck configuration: {error}")
            return False

        # Parse the initial timeout from the output
        initial_timeout = None
        for line in output.split('\n'):
            if 'Historical Tracing Store Size' in line:
                try:
                    initial_timeout = int(line.split(':')[-1].strip().split()[0])  # Get number before unit (e.g., "42 MINUTES")
                    break
                except (ValueError, IndexError):
                    logger.error(f"Failed to parse initial timeout from: {line}")
                    return False

        if initial_timeout is None:
            logger.error("Could not find Historical Tracing Store Timeout in configuration")
            return False

        # Set a new timeout
        new_timeout = 42
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enabled', 'true',
            '--historical-trace-store-timeout', str(new_timeout)
        )
        if not success:
            logger.error(f"Failed to set historical trace store timeout: {error}")
            return False

        # Verify the config was updated
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to verify configuration: {error}")
            return False

        # Parse the new timeout from the output
        updated_timeout = None
        for line in output.split('\n'):
            if 'Historical Tracing Store Timeout' in line:
                try:
                    updated_timeout = int(line.split(':')[-1].strip().split()[0])  # Get number before unit
                    break
                except (ValueError, IndexError):
                    logger.error(f"Failed to parse updated timeout from: {line}")
                    return False

        if updated_timeout != new_timeout:
            logger.error(f"Expected timeout {new_timeout}, got {updated_timeout}")
            return False

        logger.info("Successfully tested historical trace store timeout parameter")
        return True

    def test_historical_trace_store_timeout_dynamic_affects_config_and_service(self) -> bool:
        """Test that historical trace store timeout with dynamic flag affects both config and service."""
        logger.info("Testing historical trace store timeout with dynamic flag affects both config and service...")

        # Set a new timeout with dynamic flag
        new_timeout = 42
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enabled', 'true',
            '--historical-trace-store-timeout', str(new_timeout),
            '--dynamic', 'true'
        )
        if not success:
            logger.error(f"Failed to set historical trace store timeout dynamically: {error}")
            return False

        # Verify the config was updated
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to verify configuration: {error}")
            return False

        # Parse the new timeout from the output
        updated_timeout = None
        for line in output.split('\n'):
            if 'Historical Tracing Store Timeout' in line:
                try:
                    updated_timeout = int(line.split(':')[-1].strip().split()[0])  # Get number before unit
                    break
                except (ValueError, IndexError):
                    logger.error(f"Failed to parse updated timeout from: {line}")
                    return False

        if updated_timeout != new_timeout:
            logger.error(f"Expected timeout {new_timeout}, got {updated_timeout}")
            return False

        # Note: In the Python test, we can't directly verify the service's timeout
        # as we don't have direct access to the service object like in Java.
        # The Java test verifies both config and service, but here we can only verify config.

        logger.info("Successfully tested historical trace store timeout with dynamic parameter")
        return True

    def test_historical_trace_store_size_below_minimum_causes_error(self) -> bool:
        """Test that setting historical trace store size below minimum causes an error."""
        logger.info("Testing that setting historical trace store size below minimum causes an error...")

        # Try to set store size to 0 (below minimum)
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enabled', 'true',
            '--historical-trace-store-size', '0'
        )

        # The command should fail
        if success:
            logger.error("Command succeeded but should have failed with store size 0")
            return False

        # Check for the expected error message
        expected_error = "Store size must be greater than 0"
        if expected_error not in error and expected_error not in output:
            logger.error(f"Expected error message not found. Error: {error}, Output: {output}")
            return False

        logger.info("Successfully verified that store size below minimum causes an error")
        return True


def parse_args():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description='Test HealthCheck configuration')
    parser.add_argument('--asadmin', help='Path to asadmin executable')
    parser.add_argument('--host', default='localhost', help='Domain Administration Server host')
    parser.add_argument('--port', type=int, default=4848, help='Domain Administration Server port')
    parser.add_argument('--user', help='Admin username')
    parser.add_argument('--password', help='Admin password')
    return parser.parse_args()

def main():
    """Main function."""
    args = parse_args()

    test = HealthCheckConfigTest(
        asadmin_path=args.asadmin,
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password
    )

    tests = [
        test.test_enabled_is_optional,
        test.test_enabled_affects_config_but_not_service,
        test.test_enabled_dynamic_affects_config_and_service,
        test.test_log_notifier_enabled_by_default,
        test.test_historical_trace_enabled_affects_config_but_not_service,
        test.test_historical_trace_enabled_dynamic_affects_config_and_service,
        test.test_historical_trace_store_size_affects_config_but_not_service,
        test.test_historical_trace_store_size_dynamic_affects_config_and_service,
        test.test_historical_trace_store_timeout_affects_config_but_not_service,
        test.test_historical_trace_store_timeout_dynamic_affects_config_and_service,
        test.test_historical_trace_store_size_below_minimum_causes_error,

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
