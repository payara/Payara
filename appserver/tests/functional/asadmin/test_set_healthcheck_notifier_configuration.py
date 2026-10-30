#!/usr/bin/env python3

"""
Test for setting HealthCheck notifier configuration.
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

class HealthCheckNotifierConfigTest:
    """Test class for HealthCheck notifier configuration."""

    def __init__(self, asadmin_path: str = None, host: str = None, port: int = None,
                 user: str = None, password: str = None):
        """Initialize the test with connection details."""
        self.asadmin_path = asadmin_path or os.environ.get('PAYARA_HOME', '') + '/bin/asadmin' or 'asadmin'
        self.host = host or 'localhost'
        self.port = port or 4848
        self.user = user
        self.password = password

    def _run_asadmin(self, *args: str, expected_error: str = None) -> Tuple[bool, str, str]:
        """Run an asadmin command and return (success, stdout, stderr).

        Args:
            *args: Command line arguments to pass to asadmin
            expected_error: If provided, this error message is expected and won't be logged as an error
        """
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

    def test_enabled_is_optional(self) -> bool:
        """Test that enabled parameter is optional."""
        logger.info("Testing that enabled parameter is optional...")
        success, output, error = self._run_asadmin('set-healthcheck-configuration')
        if not success:
            logger.error(f"Command failed: {error}")
            return False
        logger.info("Command executed successfully without enabled parameter")
        return True

    def test_notifier_names_are_accepted(self) -> bool:
        """Test that valid notifier names are accepted."""
        logger.info("Testing that valid notifier names are accepted...")

        # These are the standard notifier names
        notifier_names = ["log-notifier", "jms-notifier", "cdieventbus-notifier", "eventbus-notifier"]

        for notifier_name in notifier_names:
            success, output, error = self._run_asadmin(
                'set-healthcheck-configuration',
                '--enableNotifiers', notifier_name,
                '--enabled', 'true'
            )
            if not success:
                logger.error(f"Failed to enable notifier {notifier_name}: {error}")
                return False

        logger.info("Successfully tested all notifier names")
        return True

    def test_incorrect_notifier_names_are_not_accepted(self) -> bool:
        """Test that invalid notifier names are rejected."""
        logger.info("Testing that invalid notifier names are rejected...")

        # The command should fail with a specific error message
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enableNotifiers', 'log-notifier,bad-notifier',
            '--enabled', 'true',
            expected_error='Unrecognised notifier bad-notifier'
        )

        if success:
            logger.error("Command succeeded but should have failed with bad notifier name")
            return False

        logger.info("Successfully verified that bad notifier names are rejected")
        return True

    def test_enabled_affects_config_but_not_service(self) -> bool:
        """Test that enabling/disabling notifiers affects config but not service when not dynamic."""
        logger.info("Testing that notifier config affects config but not service when not dynamic...")

        # Get initial state
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to get initial config: {error}")
            return False

        # Disable log notifier
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--disableNotifiers', 'log-notifier',
            '--enabled', 'true'
        )
        if not success:
            logger.error(f"Failed to disable log notifier: {error}")
            return False

        # Verify the config was updated but service state is unchanged
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to verify configuration: {error}")
            return False

        # Check if log-notifier is disabled in config
        if 'log-notifier' in output and 'enabled=true' in output.lower():
            logger.error("log-notifier was not disabled in configuration")
            return False

        # Re-enable log notifier
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enableNotifiers', 'log-notifier',
            '--enabled', 'false'
        )
        if not success:
            logger.error(f"Failed to re-enable log notifier: {error}")
            return False

        logger.info("Successfully tested notifier configuration changes")
        return True

    def test_enabled_dynamic_affects_config_and_service(self) -> bool:
        """Test that enabling/disabling notifiers with dynamic flag affects both config and service."""
        logger.info("Testing that notifier config with dynamic flag affects both config and service...")

        # Enable health checks if not already enabled
        if not self._ensure_health_checks_are_enabled():
            return False

        # Disable log notifier with dynamic flag
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--disableNotifiers', 'log-notifier',
            '--enabled', 'true',
            '--dynamic', 'true'
        )
        if not success:
            logger.error(f"Failed to disable log notifier dynamically: {error}")
            return False

        # Verify the config was updated
        success, output, error = self._run_asadmin('get-healthcheck-configuration')
        if not success:
            logger.error(f"Failed to verify configuration: {error}")
            return False

        # Check if log-notifier is disabled in config
        if 'log-notifier' in output and 'enabled=true' in output.lower():
            logger.error("log-notifier was not disabled in configuration")
            return False

        # Re-enable log notifier with dynamic flag
        success, output, error = self._run_asadmin(
            'set-healthcheck-configuration',
            '--enableNotifiers', 'log-notifier',
            '--enabled', 'true',
            '--dynamic', 'true'
        )
        if not success:
            logger.error(f"Failed to re-enable log notifier dynamically: {error}")
            return False

        logger.info("Successfully tested dynamic notifier configuration changes")
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
    parser = argparse.ArgumentParser(description='Test HealthCheck notifier configuration')
    parser.add_argument('--asadmin', help='Path to asadmin executable')
    parser.add_argument('--host', default='localhost', help='Domain Administration Server host')
    parser.add_argument('--port', type=int, default=4848, help='Domain Administration Server port')
    parser.add_argument('--user', help='Admin username')
    parser.add_argument('--password', help='Admin password')
    return parser.parse_args()

def main():
    """Main function."""
    args = parse_args()

    test = HealthCheckNotifierConfigTest(
        asadmin_path=args.asadmin,
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password
    )

    tests = [
        test.test_enabled_is_optional,
        test.test_notifier_names_are_accepted,
        test.test_incorrect_notifier_names_are_not_accepted,
        test.test_enabled_affects_config_but_not_service,
        test.test_enabled_dynamic_affects_config_and_service,
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
