"""
Deployment Group Deployment & Redeployment Tests for Payara

Tests that an application deployed to a deployment group is actually
deployed to every instance in that group, and that redeployment also
propagates to all instances.

The test suite automatically:
- Logs all operations with detailed output
- Uses clusterjsp.war from the test-apps folder

Usage:
    export PAYARA_HOME=/path/to/payara
    pytest test_deployment_group_deployment.py -v

To run specific tests:
    pytest test_deployment_group_deployment.py::TestDeploymentGroupDeployment::test_deploy_to_group_appears_on_all_instances -v

Requirements:
    - Payara Server running with DAS accessible
    - PAYARA_HOME environment variable set to Payara installation directory
    - clusterjsp.war file in the test-apps folder
"""

import os
import re
import subprocess
import time
import pytest
import logging
import requests
import socket

logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

class AsadminRunner:
    """Thin wrapper around asadmin subprocess calls."""

    def __init__(self):
        payara_home = os.environ.get("PAYARA_HOME")
        if not payara_home:
            raise RuntimeError(
                "PAYARA_HOME environment variable is not set. "
                "Please set it to your Payara installation directory."
            )
        self.asadmin = os.path.join(payara_home, "bin", "asadmin")

    def run(self, *args, check: bool = True, capture: bool = True) -> subprocess.CompletedProcess:
        cmd = [self.asadmin] + list(args)
        logger.info(f"Running asadmin command: {' '.join(args)}")
        result = subprocess.run(
            cmd,
            capture_output=capture,
            text=True,
        )
        if check and result.returncode != 0:
            logger.error(f"asadmin command failed: {' '.join(args)}")
            logger.error(f"stdout: {result.stdout}")
            logger.error(f"stderr: {result.stderr}")
            raise RuntimeError(
                f"asadmin command failed: {' '.join(args)}\n"
                f"stdout: {result.stdout}\n"
                f"stderr: {result.stderr}"
            )
        logger.info(f"asadmin command succeeded: {' '.join(args)}")
        return result

    def run_no_raise(self, *args) -> subprocess.CompletedProcess:
        """Run without raising on non-zero exit (useful for cleanup)."""
        return self.run(*args, check=False)

    def list_applications_on_instance(self, instance_name: str) -> list[str]:
        """Return names of applications deployed on the given instance."""
        result = self.run("list-applications", "--long=false", instance_name)
        apps = []
        for line in result.stdout.splitlines():
            line = line.strip()
            # Skip empty lines, the summary line and status messages
            if not line or line.startswith("Command") or line.startswith("Nothing"):
                continue
            # Output format: "<app-name>  <type>"
            parts = line.split()
            if parts:
                apps.append(parts[0])
        return apps

    def get_instance_http_port(self, instance_name: str, instance_ports: dict = None) -> str | None:
        """Get the HTTP listener port for the given instance via get command or from mapping."""
        if instance_ports and instance_name in instance_ports:
            return str(instance_ports[instance_name])
        result = self.run_no_raise(
            "get",
            f"servers.server.{instance_name}.system-property.HTTP_LISTENER_PORT.value",
        )
        for line in result.stdout.splitlines():
            m = re.search(r"=\s*(\d+)", line)
            if m:
                return m.group(1)
        return None


def check_http_app_available(host: str, port: str, app_name: str, timeout: int = 60) -> bool:
    """
    Check if the application is available via HTTP, with retries.
    
    Args:
        host: Hostname or IP address
        port: HTTP port number
        app_name: Name of the application
        timeout: Timeout in seconds for the HTTP request to succeed
    
    Returns:
        True if the application responds with HTTP 200, False otherwise
    """
    url = f"http://{host}:{port}/{app_name}"
    logger.info(f"Checking HTTP availability: {url} (up to {timeout}s)")

    start_time = time.time()
    while time.time() - start_time < timeout:
        try:
            response = requests.get(url, timeout=5)
            if response.status_code == 200:
                logger.info(f"✓ Application '{app_name}' is accessible via HTTP at {url}")
                return True
            else:
                logger.debug(f"Application '{app_name}' returned status {response.status_code}, retrying...")
        except requests.exceptions.RequestException as e:
            logger.debug(f"HTTP request failed for {url}: {e}")
        
        time.sleep(1)
        
    logger.error(f"Timed out waiting for HTTP availability at {url}")
    return False

# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture(scope="session")
def payara_domain():
    """
    Start the Payara domain before all tests and stop it after.
    """
    asadmin = AsadminRunner()
    
    # Log PAYARA_HOME
    payara_home = os.environ.get("PAYARA_HOME")
    logger.info(f"PAYARA_HOME: {payara_home}")
    
    # Log JDK version
    logger.info("Checking JDK version")
    java_result = subprocess.run(["java", "-version"], capture_output=True, text=True)
    logger.info(f"JDK version: {java_result.stderr.strip()}")


@pytest.fixture(scope="module")
def asadmin() -> AsadminRunner:
    return AsadminRunner()


@pytest.fixture(scope="module")
def test_war(tmp_path_factory) -> str:
    """
    Path to a test WAR file. Uses clusterjsp.war from test-apps folder.
    """
    war_path = os.path.join(os.path.dirname(__file__), "..", "test-apps", "clusterjsp.war")
    if not os.path.isfile(war_path):
        raise RuntimeError(f"Test WAR file not found: {war_path}")
    logger.info(f"Using test WAR: {war_path}")
    return war_path


@pytest.fixture()
def deployment_group_env(asadmin):
    """
    Create a deployment group with two standalone instances, yield the
    environment dict, then clean up everything in reverse order.

    Yielded dict keys:
        dg_name       – deployment group name
        instances     – list of instance names
        node_name     – "localhost-domain1" (default local node)
    """
    dg_name = "test-dg"
    node_name = "localhost-test-domain"
    instance_names = ["test-inst1", "test-inst2"]
    # Use fixed ports to ensure HTTP accessibility
    instance_ports = {"test-inst1": 28080, "test-inst2": 28081}

    logger.info(f"Setting up deployment group environment: {dg_name}")

    # --- Pre-setup cleanup (handle stale resources from previous runs) ---
    logger.info("Checking for and cleaning up any existing resources from previous runs")

    # Clean up any test applications at domain level
    logger.info("Cleaning up test applications from domain level")
    result = asadmin.run_no_raise("list-applications", "--long")
    test_app_prefix = "clusterjsp-dg-"
    for line in result.stdout.splitlines():
        line = line.strip()
        if not line or line.startswith("Command") or line.startswith("Nothing"):
            continue
        parts = line.split()
        if parts and parts[0].startswith(test_app_prefix):
            app_name = parts[0]
            logger.info(f"Undeploying test application from all targets: {app_name}")
            asadmin.run_no_raise("undeploy", app_name)

    # Stop and delete deployment group if it exists
    asadmin.run_no_raise("stop-deployment-group", dg_name)
    asadmin.run_no_raise("delete-deployment-group", dg_name)

    # --- Setup -----------------------------------------------------------
    # 1. Create standalone instances with explicit HTTP ports
    for inst in instance_names:
        logger.info(f"Creating instance: {inst} with HTTP port {instance_ports[inst]}")
        asadmin.run(
            "create-instance",
            f"--node={node_name}",
            f"--systemproperties=HTTP_LISTENER_PORT={instance_ports[inst]}",
            inst,
        )

    # 2. Create the deployment group
    logger.info(f"Creating deployment group: {dg_name}")
    asadmin.run("create-deployment-group", dg_name)

    # 3. Add both instances to the deployment group
    for inst in instance_names:
        logger.info(f"Adding instance {inst} to deployment group {dg_name}")
        asadmin.run(
            "add-instance-to-deployment-group",
            f"--instance={inst}",
            f"--deploymentgroup={dg_name}",
        )

    # 4. Start the deployment group (starts all member instances)
    logger.info(f"Starting deployment group: {dg_name}")
    asadmin.run("start-deployment-group", dg_name)

    # 5. Wait for instances to actually start
    logger.info("Waiting for instances to start...")
    
    logger.info(f"Deployment group environment setup complete: {dg_name}")

    yield {
        "dg_name": dg_name,
        "instances": instance_names,
        "instance_ports": instance_ports,
        "node_name": node_name,
    }

    # --- Teardown (always runs) ------------------------------------------
    logger.info(f"Tearing down deployment group environment: {dg_name}")

    # Undeploy any remaining apps from deployment group (best-effort)
    apps = asadmin.list_applications_on_instance(dg_name)
    if apps:
        logger.info(f"Undeploying remaining apps from {dg_name}: {apps}")
        for app in apps:
            asadmin.run_no_raise("undeploy", f"--target={dg_name}", app)

    # Clean up any test applications at domain level
    logger.info("Cleaning up test applications from domain level")
    result = asadmin.run_no_raise("list-applications", "--long")
    test_app_prefix = "clusterjsp-dg-"
    for line in result.stdout.splitlines():
        line = line.strip()
        if not line or line.startswith("Command") or line.startswith("Nothing"):
            continue
        parts = line.split()
        if parts and parts[0].startswith(test_app_prefix):
            app_name = parts[0]
            logger.info(f"Undeploying test application from domain: {app_name}")
            asadmin.run_no_raise("undeploy", "--target=domain", app_name)

    logger.info(f"Stopping deployment group: {dg_name}")
    asadmin.run_no_raise("stop-deployment-group", dg_name)

    for inst in instance_names:
        logger.info(f"Removing instance {inst} from deployment group {dg_name}")
        asadmin.run_no_raise("remove-instance-from-deployment-group",
                             f"--instance={inst}",
                             f"--deploymentgroup={dg_name}")
        logger.info(f"Deleting instance: {inst}")
        asadmin.run_no_raise("delete-instance", inst)

    logger.info(f"Deleting deployment group: {dg_name}")
    asadmin.run_no_raise("delete-deployment-group", dg_name)
    logger.info(f"Deployment group environment teardown complete: {dg_name}")

# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

class TestDeploymentGroupDeployment:
    """Tests for deployment group functionality."""

    def test_deploy_to_group_appears_on_all_instances(
        self, asadmin, deployment_group_env, test_war
    ):
        """Deploying to a deployment group should make the app visible on all instances."""
        dg = deployment_group_env["dg_name"]
        instances = deployment_group_env["instances"]
        instance_ports = deployment_group_env["instance_ports"]
        app_name = "clusterjsp-dg-deploy-test"

        asadmin.run("deploy", f"--target={dg}", f"--name={app_name}", f"--contextroot={app_name}", test_war)

        # Wait for application to start on instances
        logger.info("Waiting for application to start on instances...")
        time.sleep(10)

        try:
            for inst in instances:
                apps = asadmin.list_applications_on_instance(inst)
                assert app_name in apps, (
                    f"Application '{app_name}' not found on instance '{inst}'. Apps: {apps}"
                )
                
                http_port = asadmin.get_instance_http_port(inst, instance_ports)
                assert http_port is not None, f"Could not get HTTP port for '{inst}'"
                assert check_http_app_available("localhost", http_port, app_name), (
                    f"App '{app_name}' not accessible via HTTP on '{inst}'"
                )
        finally:
            asadmin.run_no_raise("undeploy", f"--target={dg}", app_name)

    def test_deploy_to_group_listed_on_group_target(
        self, asadmin, deployment_group_env, test_war
    ):
        """list-applications on the deployment group should return the deployed app."""
        dg = deployment_group_env["dg_name"]
        instance_ports = deployment_group_env["instance_ports"]
        app_name = "clusterjsp-dg-list-test"

        asadmin.run("deploy", f"--target={dg}", f"--name={app_name}", f"--contextroot={app_name}", test_war)

        # Wait for application to start on instances
        logger.info("Waiting for application to start on instances...")
        time.sleep(10)

        try:
            apps = asadmin.list_applications_on_instance(dg)
            assert app_name in apps, (
                f"App '{app_name}' not listed on deployment group '{dg}'. Apps: {apps}"
            )
            
            inst = deployment_group_env["instances"][0]
            http_port = asadmin.get_instance_http_port(inst, instance_ports)
            assert http_port is not None, f"Could not get HTTP port for '{inst}'"
            assert check_http_app_available("localhost", http_port, app_name), (
                f"App '{app_name}' not accessible via HTTP on '{inst}'"
            )
        finally:
            asadmin.run_no_raise("undeploy", f"--target={dg}", app_name)

    def test_undeploy_from_group_removes_from_all_instances(
        self, asadmin, deployment_group_env, test_war
    ):
        """Undeploying from a deployment group should remove the app from all instances."""
        dg = deployment_group_env["dg_name"]
        instances = deployment_group_env["instances"]
        instance_ports = deployment_group_env["instance_ports"]
        app_name = "clusterjsp-dg-undeploy-test"

        asadmin.run("deploy", f"--target={dg}", f"--name={app_name}", f"--contextroot={app_name}", test_war)
        
        # Wait for application to start on instances
        logger.info("Waiting for application to start on instances...")
        time.sleep(10)
        
        inst = instances[0]
        http_port = asadmin.get_instance_http_port(inst, instance_ports)
        assert http_port is not None, f"Could not get HTTP port for '{inst}'"
        assert check_http_app_available("localhost", http_port, app_name), (
            f"App '{app_name}' not accessible before undeploy"
        )
        
        asadmin.run("undeploy", f"--target={dg}", app_name)

        for inst in instances:
            apps = asadmin.list_applications_on_instance(inst)
            assert app_name not in apps, (
                f"App '{app_name}' still on instance '{inst}' after undeploy"
            )

    def test_redeploy_to_group_propagates_to_all_instances(
        self, asadmin, deployment_group_env, test_war
    ):
        """Redeploying an app to a deployment group should propagate to all instances."""
        dg = deployment_group_env["dg_name"]
        instances = deployment_group_env["instances"]
        instance_ports = deployment_group_env["instance_ports"]
        app_name = "clusterjsp-dg-redeploy-test"

        # Initial deployment
        asadmin.run("deploy", f"--target={dg}", f"--name={app_name}", f"--contextroot={app_name}", test_war)

        # Wait for application to start on instances
        logger.info("Waiting for application to start on instances...")
        time.sleep(10)

        try:
            # Redeploy the same app with force flag
            asadmin.run("deploy", "--force=true", f"--target={dg}", f"--name={app_name}", f"--contextroot={app_name}", test_war)

            # Verify redeployment propagated to all instances
            for inst in instances:
                apps = asadmin.list_applications_on_instance(inst)
                assert app_name in apps, (
                    f"App '{app_name}' not found on instance '{inst}' after redeploy. Apps: {apps}"
                )

                http_port = asadmin.get_instance_http_port(inst, instance_ports)
                assert http_port is not None, f"Could not get HTTP port for '{inst}'"
                assert check_http_app_available("localhost", http_port, app_name), (
                    f"App '{app_name}' not accessible via HTTP on '{inst}' after redeploy"
                )
        finally:
            asadmin.run_no_raise("undeploy", f"--target={dg}", app_name)
