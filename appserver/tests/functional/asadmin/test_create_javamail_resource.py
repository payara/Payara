#!/usr/bin/env python3

"""
Test for creating JavaMail resources with deployment group references.
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

class JavaMailResourceTest:
    """Test class for JavaMail resource creation with deployment group references."""

    def __init__(self, asadmin_path: str = None, host: str = None, port: int = None,
                 user: str = None, password: str = None):
        """Initialize the test with connection details."""
        self.asadmin_path = asadmin_path or os.environ.get('PAYARA_HOME', '') + '/bin/asadmin' or 'asadmin'
        self.host = host or 'localhost'
        self.port = port or 4848
        self.user = user
        self.password = password
        self.deployment_group = "create-javamail-resource-test-dg"
        self.javamail_resource = "mail/create-javamail-resource-test"

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

    def _resource_exists(self, resource_name: str) -> bool:
        """Check if a resource exists."""
        success, output, _ = self._run_asadmin('list-javamail-resources')
        return success and resource_name in output

    def _deployment_group_exists(self) -> bool:
        """Check if the test deployment group exists."""
        success, output, _ = self._run_asadmin('list-deployment-groups')
        return success and self.deployment_group in output

    def _delete_resources(self):
        """Clean up test resources."""
        logger.info("Cleaning up test resources...")

        # Try to delete the resource reference from all possible targets
        targets = [self.deployment_group, 'domain', 'server', 'default']
        for target in targets:
            success, _, error = self._run_asadmin(
                'delete-resource-ref',
                f'--target={target}',
                self.javamail_resource
            )
            if success or "not found" in error or "does not exist" in error:
                break
            else:
                logger.warning(f"Failed to delete resource ref from {target}: {error}")

        # Try to delete the JavaMail resource from all possible targets
        for target in ['domain', 'server', 'default']:
            success, _, error = self._run_asadmin(
                'delete-javamail-resource',
                f'--target={target}',
                self.javamail_resource
            )
            if success or "not found" in error or "does not exist" in error or "is not referenced" in error:
                break
            else:
                logger.warning(f"Failed to delete JavaMail resource from {target}: {error}")

        # Finally, delete the deployment group if it exists
        if self._deployment_group_exists():
            # First, stop the deployment group if it's running
            success, _, _ = self._run_asadmin('stop-deployment-group', self.deployment_group)

            # Then delete it
            success, _, error = self._run_asadmin('delete-deployment-group', self.deployment_group)
            if not success and "not found" not in error and "does not exist" not in error:
                logger.warning(f"Failed to delete deployment group: {error}")

    def run_test(self) -> bool:
        """Run the JavaMail resource creation test."""
        try:
            # Clean up any existing resources first
            self._delete_resources()

            # Create deployment group
            logger.info(f"Creating deployment group: {self.deployment_group}")
            success, _, error = self._run_asadmin('create-deployment-group', self.deployment_group)
            if not success:
                raise AsadminCommandError(f"Failed to create deployment group: {error}")

            # Create JavaMail resource
            logger.info(f"Creating JavaMail resource: {self.javamail_resource}")
            success, _, error = self._run_asadmin(
                'create-javamail-resource',
                '--debug=false',
                '--storeProtocol=imap',
                '--auth=false',
                '--transportProtocol=smtp',
                '--host=localhost',
                '--storeProtocolClass=org.eclipse.angus.mail.imap.IMAPStore',
                '--from=ratatosk@payara.fish',
                '--transportProtocolClass=org.eclipse.angus.mail.smtp.SMTPTransport',
                '--enabled=true',
                '--target=domain',
                '--mailhost=localhost',
                '--mailuser=ratatosk',
                self.javamail_resource
            )
            if not success:
                raise AsadminCommandError(f"Failed to create JavaMail resource: {error}")

            # Create resource reference
            logger.info("Creating resource reference...")
            success, output, error = self._run_asadmin(
                'create-resource-ref',
                '--enabled=true',
                f'--target={self.deployment_group}',
                self.javamail_resource
            )
            if not success:
                raise AsadminCommandError(f"Failed to create resource reference: {error}")

            logger.info("Test completed successfully!")
            return True

        except Exception as e:
            logger.error(f"Test failed: {e}")
            return False
        finally:
            # Clean up resources
            self._delete_resources()

def parse_args():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description='Test JavaMail resource creation with deployment group references')
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

    test = None
    try:
        # Initialize test
        test = JavaMailResourceTest(
            asadmin_path=args.asadmin,
            host=args.host,
            port=args.port,
            user=args.user,
            password=args.password
        )

        # Run test
        success = test.run_test()
        return 0 if success else 1

    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        return 1

if __name__ == "__main__":
    sys.exit(main())
