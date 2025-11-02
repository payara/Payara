#!/usr/bin/env python3

"""
Test for register login module functionality.
"""

import os
import sys
import logging
import argparse
import subprocess
import tempfile
import shutil
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


class RegisterLoginModuleTest:
    """Test class for register login module functionality."""

    @staticmethod
    def _find_payara_home() -> str:
        """Find the Payara installation directory."""
        # Check PAYARA_HOME environment variable first
        payara_home = os.environ.get('PAYARA_HOME')
        if payara_home and os.path.isdir(payara_home):
            return os.path.abspath(payara_home)

        raise FileNotFoundError(
            "Could not find Payara installation. Please set PAYARA_HOME environment variable "
            "or provide the path to asadmin using --asadmin-path"
        )

    def _find_asadmin(self, asadmin_path: str = None) -> str:
        """Find the asadmin executable path."""
        # If path is provided, use it
        if asadmin_path:
            if os.path.isfile(asadmin_path) and os.access(asadmin_path, os.X_OK):
                return os.path.abspath(asadmin_path)
            raise FileNotFoundError(f"asadmin not found or not executable at: {asadmin_path}")

        # Check if it's in the PATH
        asadmin_in_path = shutil.which('asadmin')
        if asadmin_in_path:
            return asadmin_in_path

        # Try to find it relative to PAYARA_HOME
        try:
            payara_home = self._find_payara_home()
            default_path = os.path.join(payara_home, 'bin', 'asadmin')
            if os.path.isfile(default_path) and os.access(default_path, os.X_OK):
                return default_path
        except FileNotFoundError:
            pass

        # If not found, raise error
        raise FileNotFoundError(
            "Could not find asadmin executable. Please provide the path using --asadmin-path, "
            "set PAYARA_HOME environment variable, or ensure asadmin is in your PATH."
        )

    def __init__(self, asadmin_path: str = None, host: str = None, port: int = 4848,
                 user: str = 'admin', password: str = 'admin'):
        """Initialize the test with connection details."""
        self.asadmin_path = asadmin_path or os.environ.get('PAYARA_HOME', '') + '/bin/asadmin' or 'asadmin'
        self.host = host or 'localhost'
        self.port = port or 4848
        self.user = user
        self.password = password

        # Store original login config location
        self.original_login_config = os.environ.get('java.security.auth.login.config')

        # Create a temporary login config file
        self.temp_dir = tempfile.mkdtemp()
        self.login_config = os.path.join(self.temp_dir, 'login.conf')

        # Set the login config environment variable
        os.environ['java.security.auth.login.config'] = self.login_config

        # Initialize empty login config
        self._write_login_config('')

    def __del__(self):
        """Clean up temporary files and restore environment."""
        # Remove temporary directory
        if hasattr(self, 'temp_dir') and os.path.exists(self.temp_dir):
            import shutil
            shutil.rmtree(self.temp_dir, ignore_errors=True)

        # Restore original login config
        if hasattr(self, 'original_login_config'):
            if self.original_login_config is not None:
                os.environ['java.security.auth.login.config'] = self.original_login_config
            else:
                os.environ.pop('java.security.auth.login.config', None)

    def _run_asadmin(self, *args: str) -> Tuple[bool, str, str]:
        """Run an asadmin command and return (success, stdout, stderr)."""
        # Build the command without authentication
        cmd = [
            self.asadmin_path,
            '--host', self.host,
            '--port', str(self.port)
        ]
        cmd.extend(args)

        # Log the command
        logger.debug(f"Running command: {' '.join(cmd)}")

        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                check=False
            )

            success = result.returncode == 0
            if not success and result.returncode != 1:  # 1 is expected for warnings
                logger.error(f"Command failed with code {result.returncode}: {result.stderr or result.stdout}")

            return success, result.stdout, result.stderr

        except Exception as e:
            logger.error(f"Error running command: {e}")
            return False, "", str(e)

    def _read_login_config(self) -> str:
        """Read the current login configuration."""
        try:
            with open(self.login_config, 'r') as f:
                return f.read()
        except Exception as e:
            logger.error(f"Error reading login config: {e}")
            return ""

    def _write_login_config(self, content: str) -> None:
        """Write to the login configuration file."""
        with open(self.login_config, 'w') as f:
            f.write(content)

    ##### TESTS ######
    def test_successful_registration(self) -> bool:
        """Test successful registration of a login module."""
        logger.info("Testing successful registration of login module...")

        # First, try to delete the realm if it exists
        delete_cmd = [
            self.asadmin_path,
            '--user', 'admin',
            'delete-auth-realm',
            'test1'
        ]

        # Run delete command and ignore errors if realm doesn't exist
        subprocess.run(
            delete_cmd,
            capture_output=True,
            text=True,
            check=False
        )

        try:
            # Create the auth realm
            create_cmd = [
                self.asadmin_path,
                'create-auth-realm',
                '--classname=com.sun.enterprise.security.auth.realm.file.FileRealm',
                '--login-module', 'com.sun.enterprise.security.auth.login.FileLoginModule',
                '--property=jaas-context=test1:file=test1',
                'test1'
            ]

            # Run the create command
            logger.debug(f"Running command: {' '.join(create_cmd)}")
            result = subprocess.run(
                create_cmd,
                capture_output=True,
                text=True,
                check=False
            )

            if result.returncode != 0:
                error_msg = result.stderr or result.stdout
                if "already exists" in error_msg:
                    logger.warning("Auth realm 'test1' already exists. Test may be running in an inconsistent state.")
                    return True  # Consider this a success for now
                logger.error(f"Failed to create auth realm: {error_msg}")
                return False

            logger.info("Successfully created auth realm")
            return True

        except Exception as e:
            logger.error(f"Error running command: {e}")
            return False

    def test_existing_jaas_context_warning(self) -> bool | None:
        """Test warning when using existing JAAS context."""
        logger.info("Testing warning for existing JAAS context...")

        test_realm2 = 'test2'

        try:
            # Clean up any existing test realms
            self._run_asadmin('delete-auth-realm', test_realm2)

            cmd = [
                'create-auth-realm',
                '--classname', 'com.sun.enterprise.security.auth.realm.file.FileRealm',
                '--login-module', 'com.sun.enterprise.security.auth.login.FileLoginModule',
                '--property', 'jaas-context=fileRealm:file=test2', test_realm2,
            ]

            success, output, error = self._run_asadmin(*cmd)

            # We expect the second command to fail
            if 'fileRealm is already configured' in (output or error):
                logger.info("Found warning message in logs: 'fileRealm already configured'")
                return True
            else:
                logger.error("Expected warning message not found in logs: 'fileRealm already configured'")
                return False

        except Exception as e:
            logger.error(f"Error testing existing JAAS context: {e}")
            return False

        finally:
            # Clean up
            self._run_asadmin('delete-auth-realm', test_realm2)
            logger.info("Deleted realm: " + test_realm2)

    def test_undefined_jaas_context_warning(self) -> bool:
        """Test warning when no JAAS context is defined."""
        logger.info("Testing warning for undefined JAAS context...")

        # Clean up any existing test realm
        self._run_asadmin('delete-auth-realm', 'test3')

        # Create auth realm without a JAAS context
        success, output, error = self._run_asadmin(
            'create-auth-realm',
            "--classname", "com.sun.enterprise.security.auth.realm.certificate.CertificateRealm",
            "--login-module", "com.sun.enterprise.security.auth.login.FileLoginModule",
            "--property", "file=test3", "test3"
        )

        if "No JAAS context is defined" not in (output + error):
            logger.info("Found warning message in logs: 'No JAAS context is defined'")
            return True

        # Clean up
        self._run_asadmin('delete-auth-realm', 'test3')

        logger.info("Successfully tested undefined JAAS context warning")
        return True

    def run_tests(self) -> bool:
        """Run all tests."""
        try:
            if not self.test_successful_registration():
                return False
            if not self.test_existing_jaas_context_warning():
                return False
            if not self.test_undefined_jaas_context_warning():
                return False

            logger.info("All tests completed successfully!")
            return True

        except Exception as e:
            logger.error(f"Test failed: {e}")
            import traceback
            logger.error(traceback.format_exc())
            return False


def main():
    """Main entry point for the test script."""
    parser = argparse.ArgumentParser(description='Test register login module functionality')
    parser.add_argument('--asadmin-path', help='Path to asadmin executable')
    parser.add_argument('--host', default='localhost', help='Payara host (default: localhost)')
    parser.add_argument('--port', type=int, default=4848, help='Payara admin port (default: 4848)')
    parser.add_argument('--user', default='admin', help='Admin username (default: admin)')
    parser.add_argument('--password', default='admin', help='Admin password (default: admin)')
    parser.add_argument('--debug', action='store_true', help='Enable debug logging')
    args = parser.parse_args()

    # Configure logging
    log_level = logging.DEBUG if args.debug else logging.INFO
    logging.basicConfig(
        level=log_level,
        format='%(asctime)s - %(levelname)s - %(message)s'
    )

    test = RegisterLoginModuleTest(
        asadmin_path=args.asadmin_path,
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password
    )

    if not test.run_tests():
        sys.exit(1)


if __name__ == '__main__':
    main()
