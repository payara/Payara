#!/usr/bin/env python3

"""
Test for auto-naming Payara instances with conflict resolution.
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

class AutoNameInstancesTest:
    """Test class for auto-naming instances with conflict resolution."""

    def __init__(self, asadmin_path: str = None, host: str = None, port: int = None,
                 user: str = None, password: str = None):
        """Initialize the test with connection details."""
        self.asadmin_path = asadmin_path or os.environ.get('PAYARA_HOME', '') + '/bin/asadmin' or 'asadmin'
        self.host = host or 'localhost'
        self.port = port or 4848
        self.user = user
        self.password = password
        self.conflict_instance = "Scrumptious-Swordfish"
        self.generated_instance = None
        self.domain_name = self._get_domain_name()
        self.node_name = f"localhost-{self.domain_name}"

        # For remote mode, adjust node name
        if self.host != 'localhost' and self.host != '127.0.0.1':
            self.node_name = f"{self.host}-{self.domain_name}"

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

    def _get_domain_name(self) -> str:
        """Get the current domain name."""
        success, output, _ = self._run_asadmin('list-domains')
        if not success:
            raise AsadminCommandError("Failed to list domains")

        # The output is something like:
        # domain1 running
        # domain2 not running
        for line in output.splitlines():
            if 'running' in line:
                return line.split()[0]

        raise AsadminCommandError("No running domains found")

    def _instance_exists(self, instance_name: str) -> bool:
        """Check if an instance exists."""
        success, output, _ = self._run_asadmin('list-instances')
        if not success:
            return False

        instances = [line.split()[0] for line in output.splitlines()
                    if line.strip() and not line.startswith('Command')]
        return instance_name in instances

    def setup(self):
        """Set up the test environment."""
        logger.info("Setting up test environment...")

        # Check if conflict instance exists, create if not
        if not self._instance_exists(self.conflict_instance):
            logger.info(f"Creating conflict instance: {self.conflict_instance}")
            success, _, error = self._run_asadmin(
                'create-instance',
                '--node', self.node_name,
                self.conflict_instance
            )
            if not success:
                raise AsadminCommandError(f"Failed to create conflict instance: {error}")
            self.conflict_created = True
        else:
            logger.info(f"Conflict instance {self.conflict_instance} already exists")
            self.conflict_created = False

    def run_test(self) -> bool:
        """Run the auto-naming test."""
        logger.info("Testing auto-naming with conflict...")

        # Try to create instance with auto-naming
        success, output, error = self._run_asadmin(
            'create-instance',
            '--autoname', 'true',
            '--node', self.node_name,
            '-T',
            self.conflict_instance
        )

        if not success:
            raise AsadminCommandError(f"Failed to create auto-named instance: {error}")

        # Extract the generated instance name
        self.generated_instance = output.strip()
        if not self.generated_instance:
            raise RuntimeError("Failed to get generated instance name")

        logger.info(f"Successfully created auto-named instance: {self.generated_instance}")
        return True

    def cleanup(self):
        """Clean up test resources."""
        logger.info("Cleaning up test resources...")

        # Delete the auto-named instance if it was created
        if self.generated_instance and self._instance_exists(self.generated_instance):
            try:
                logger.info(f"Deleting auto-named instance: {self.generated_instance}")
                self._run_asadmin('delete-instance', self.generated_instance)
            except Exception as e:
                logger.warning(f"Failed to delete instance {self.generated_instance}: {e}")

        # Delete the conflict instance if we created it
        if hasattr(self, 'conflict_created') and self.conflict_created:
            try:
                logger.info(f"Deleting conflict instance: {self.conflict_instance}")
                success, _, error = self._run_asadmin('delete-instance', self.conflict_instance)
                if not success:
                    logger.warning(f"Failed to delete instance {self.conflict_instance}: {error}")
            except Exception as e:
                logger.warning(f"Error while deleting instance {self.conflict_instance}: {e}")

def parse_args():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description='Test auto-naming of Payara instances')
    parser.add_argument('--asadmin', default=os.environ.get('PAYARA_HOME', '') + '/bin/asadmin' if os.environ.get('PAYARA_HOME') else 'asadmin',
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

    test = None
    try:
        # Initialize test
        test = AutoNameInstancesTest(
            asadmin_path=args.asadmin,
            host=args.host,
            port=args.port,
            user=args.user,
            password=args.password
        )

        # Run test
        test.setup()
        test.run_test()
        logger.info("Test completed successfully!")
        return 0

    except Exception as e:
        logger.error(f"Test failed: {e}", exc_info=args.debug)
        return 1

    finally:
        # Always clean up
        if test:
            test.cleanup()

if __name__ == "__main__":
    sys.exit(main())
