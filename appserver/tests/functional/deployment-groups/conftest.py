"""
Pytest configuration for deployment group deployment tests.
"""

import pytest
import subprocess
import os
import logging

logger = logging.getLogger(__name__)


def pytest_addoption(parser):
    parser.addoption(
        "--instance-http-port-base",
        default="28080",
        type=int,
        help="Base HTTP port for created test instances (default: 28080)",
    )


@pytest.fixture(scope="session", autouse=True)
def cleanup_domain_applications(payara_domain):
    """
    Clean up any test applications deployed at the domain level after test runs.
    This prevents stale deployments from causing deployment failures.
    Depends on payara_domain to ensure the domain is running before cleanup.
    """
    payara_home = os.environ.get("PAYARA_HOME")
    if not payara_home:
        logger.warning("PAYARA_HOME not set, skipping domain application cleanup")
        return

    asadmin = os.path.join(payara_home, "bin", "asadmin")

    def cleanup_test_apps():
        """Undeploy all test applications from the domain."""
        logger.info("Cleaning up test applications from domain...")
        result = subprocess.run(
            [asadmin, "list-applications", "--long"],
            capture_output=True,
            text=True,
        )
        
        if result.returncode != 0:
            logger.warning(f"Failed to list applications: {result.stderr}")
            return

        # Parse applications and undeploy any matching test pattern
        test_app_prefix = "clusterjsp-dg-"
        for line in result.stdout.splitlines():
            line = line.strip()
            if not line or line.startswith("Command") or line.startswith("Nothing"):
                continue
            
            # Output format: "<app-name>  <type>"
            parts = line.split()
            if parts and parts[0].startswith(test_app_prefix):
                app_name = parts[0]
                logger.info(f"Undeploying test application: {app_name}")
                subprocess.run(
                    [asadmin, "undeploy", "--target=domain", app_name],
                    capture_output=True,
                    text=True,
                )

    yield
    
    # Cleanup after tests (domain will be stopped by payara_domain fixture)
    cleanup_test_apps()
