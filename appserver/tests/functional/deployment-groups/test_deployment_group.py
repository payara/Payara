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

    def get_das_admin_port(self) -> str:
        """Return the DAS admin-listener port (defaults to 4848 if it cannot be read)."""
        result = self.run_no_raise(
            "get",
            "configs.config.server-config.network-config.network-listeners."
            "network-listener.admin-listener.port",
        )
        for line in result.stdout.splitlines():
            m = re.search(r"=\s*(\d+)", line)
            if m:
                return m.group(1)
        return "4848"

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


def check_http_content(host: str, port: str, app_name: str, expected_content: str, timeout: int = 60) -> bool:
    """
    Check if the application returns expected content via HTTP, with retries.
    
    Args:
        host: Hostname or IP address
        port: HTTP port number
        app_name: Name of the application
        expected_content: Expected content in the response
        timeout: Timeout in seconds for the HTTP request to succeed
    
    Returns:
        True if the application responds with HTTP 200 and contains expected content, False otherwise
    """
    url = f"http://{host}:{port}/{app_name}"
    logger.info(f"Checking HTTP content at {url} for '{expected_content}' (up to {timeout}s)")

    start_time = time.time()
    while time.time() - start_time < timeout:
        try:
            response = requests.get(url, timeout=5)
            if response.status_code == 200:
                content = response.text
                if expected_content in content:
                    logger.info(f"✓ Application '{app_name}' contains expected content '{expected_content}'")
                    return True
                else:
                    logger.debug(f"Application '{app_name}' does not contain expected content. Response: {content[:200]}")
            else:
                logger.debug(f"Application '{app_name}' returned status {response.status_code}, retrying...")
        except requests.exceptions.RequestException as e:
            logger.debug(f"HTTP request failed for {url}: {e}")

        time.sleep(1)

    logger.error(f"Timed out waiting for expected content at {url}")
    return False


def das_management_post(port: str, path: str, data: dict, timeout: int = 60):
    """
    POST to the DAS management REST interface the way the admin console does, including
    the X-Requested-By header the interface requires for mutating requests. ``path`` is
    relative to /management/domain/ (e.g. "deployment-groups/add-instance-to-deployment-group").
    """
    url = f"http://localhost:{port}/management/domain/{path}"
    headers = {"X-Requested-By": "deployment-group-tests", "Accept": "application/json"}
    logger.info(f"POST {url} data={data}")
    return requests.post(url, data=data, headers=headers, timeout=timeout)


def find_instance_domain_xml(node: str, instance: str) -> str | None:
    """
    Locate an instance's own config file (nodes/<node>/<instance>/config/domain.xml).

    The nodes directory lives under the GlassFish base, but PAYARA_HOME may point either
    at the distribution root (which has bin/asadmin as a thin wrapper and nodes under
    glassfish/) or directly at the glassfish base (nodes/ alongside bin/). Try both
    layouts rather than assuming one, so the check works on CI and on a local build.
    """
    payara_home = os.environ.get("PAYARA_HOME")
    if not payara_home:
        return None
    candidates = [
        os.path.join(payara_home, "glassfish", "nodes", node, instance, "config", "domain.xml"),
        os.path.join(payara_home, "nodes", node, instance, "config", "domain.xml"),
    ]
    for candidate in candidates:
        if os.path.isfile(candidate):
            return candidate
    return None


def read_instance_dg_members(node: str, instance: str) -> list[str]:
    """
    Return the set of deployment-group member references (dg-server-ref) recorded in
    an instance's own live config (nodes/<node>/<instance>/config/domain.xml).

    This reflects the instance's in-memory membership as replicated live by the DAS,
    which is exactly what FISH-14056 requires running members to converge on without a
    restart. Reading the file directly (rather than a remote command) keeps the check
    deterministic.
    """
    domain_xml = find_instance_domain_xml(node, instance)
    if domain_xml is None:
        return []
    with open(domain_xml, encoding="utf-8") as handle:
        content = handle.read()
    return re.findall(r'<dg-server-ref\s+ref="([^"]+)"', content)


def wait_for_instance_dg_members(node: str, instance: str, expected: set[str],
                                 timeout: int = 30) -> list[str]:
    """
    Poll the instance's live config until its dg-server-ref set matches ``expected``
    (or the timeout elapses). Returns the last observed member list.
    """
    start_time = time.time()
    members = read_instance_dg_members(node, instance)
    while time.time() - start_time < timeout:
        members = read_instance_dg_members(node, instance)
        if set(members) == expected:
            return members
        time.sleep(1)
    return members

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
def local_node(asadmin) -> str:
    """
    Resolve the built-in local CONFIG node for the running domain.

    The default local node is named "localhost-<domainName>" (e.g.
    "localhost-test-domain", "localhost-domain1"), so it varies with the domain
    the tests run against. Hardcoding it makes the suite pass only on one domain
    name; resolving it dynamically keeps the tests portable.
    """
    result = asadmin.run("list-nodes", "--long")
    for line in result.stdout.splitlines():
        parts = line.split()
        # Format: "<NODE NAME>  <TYPE>  <NODE HOST>  ..."
        if len(parts) >= 3 and parts[1] == "CONFIG" and parts[0].startswith("localhost-"):
            logger.info(f"Resolved local CONFIG node: {parts[0]}")
            return parts[0]
    raise RuntimeError(
        f"Could not resolve local CONFIG node from list-nodes output:\n{result.stdout}"
    )


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

@pytest.fixture(scope="module", params=["1.0.0", "1.0.1"])
def sample_app(request) -> tuple[str, str]:
    """
    Path to sample-app WAR file for the requested version.
    Returns a tuple of (version, file_path).
    """
    version = request.param
    war_path = os.path.join(os.path.dirname(__file__), "..", "test-apps", f"sample-app-{version}.war")
    if not os.path.isfile(war_path):
        raise RuntimeError(f"Sample app {version} WAR file not found: {war_path}")
    logger.info(f"Using sample app {version} WAR: {war_path}")
    return version, war_path

@pytest.fixture()
def single_instance_deployment_group_env(asadmin, local_node):
    """
    Create a deployment group with a single standalone instance with a dedicated config,
    yield the environment dict, then clean up everything in reverse order.

    Yielded dict keys:
        dg_name       – deployment group name
        instance      – instance name
        instance_port – HTTP port for the instance
        config_name   – dedicated config name for the instance
        node_name     – the local CONFIG node (resolved via the local_node fixture)
    """
    dg_name = "test-dg-single"
    node_name = local_node
    instance_name = "test-inst-single"
    config_name = "test-inst-single-config"
    instance_port = 28090

    logger.info(f"Setting up single instance deployment group environment: {dg_name}")

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

    # Stop and delete instance if it exists
    asadmin.run_no_raise("stop-instance", instance_name)
    asadmin.run_no_raise("delete-instance", instance_name)

    # Delete config if it exists
    asadmin.run_no_raise("delete-config", config_name)

    logger.info(f"Creating config: {config_name} by copying default-config")
    asadmin.run("copy-config", "default-config", config_name)

    logger.info(f"Creating instance: {instance_name} with config {config_name} and HTTP port {instance_port}")
    asadmin.run(
        "create-instance",
        f"--node={node_name}",
        f"--config={config_name}",
        f"--systemproperties=HTTP_LISTENER_PORT={instance_port}",
        instance_name,
    )

    logger.info(f"Creating deployment group: {dg_name}")
    asadmin.run("create-deployment-group", dg_name)

    logger.info(f"Adding instance {instance_name} to deployment group {dg_name}")
    asadmin.run(
        "add-instance-to-deployment-group",
        f"--instance={instance_name}",
        f"--deploymentgroup={dg_name}",
    )

    logger.info(f"Starting deployment group: {dg_name}")
    asadmin.run("start-deployment-group", dg_name)

    logger.info("Waiting for instance to start...")
    time.sleep(10)

    logger.info(f"Single instance deployment group environment setup complete: {dg_name}")

    yield {
        "dg_name": dg_name,
        "instance": instance_name,
        "instance_port": instance_port,
        "config_name": config_name,
        "node_name": node_name,
    }

    logger.info(f"Tearing down single instance deployment group environment: {dg_name}")

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

    logger.info(f"Removing instance {instance_name} from deployment group {dg_name}")
    asadmin.run_no_raise("remove-instance-from-deployment-group",
                         f"--instance={instance_name}",
                         f"--deploymentgroup={dg_name}")
    logger.info(f"Deleting instance: {instance_name}")
    asadmin.run_no_raise("delete-instance", instance_name)

    logger.info(f"Deleting config: {config_name}")
    asadmin.run_no_raise("delete-config", config_name)

    logger.info(f"Deleting deployment group: {dg_name}")
    asadmin.run_no_raise("delete-deployment-group", dg_name)
    logger.info(f"Single instance deployment group environment teardown complete: {dg_name}")


@pytest.fixture()
def deployment_group_env(asadmin, local_node):
    """
    Create a deployment group with two standalone instances, yield the
    environment dict, then clean up everything in reverse order.

    Yielded dict keys:
        dg_name       – deployment group name
        instances     – list of instance names
        node_name     – the local CONFIG node (resolved via the local_node fixture)
    """
    dg_name = "test-dg"
    node_name = local_node
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

    for inst in instance_names:
        logger.info(f"Creating instance: {inst} with HTTP port {instance_ports[inst]}")
        asadmin.run(
            "create-instance",
            f"--node={node_name}",
            f"--systemproperties=HTTP_LISTENER_PORT={instance_ports[inst]}",
            inst,
        )

    logger.info(f"Creating deployment group: {dg_name}")
    asadmin.run("create-deployment-group", dg_name)

    for inst in instance_names:
        logger.info(f"Adding instance {inst} to deployment group {dg_name}")
        asadmin.run(
            "add-instance-to-deployment-group",
            f"--instance={inst}",
            f"--deploymentgroup={dg_name}",
        )

    logger.info(f"Starting deployment group: {dg_name}")
    asadmin.run("start-deployment-group", dg_name)

    logger.info("Waiting for instances to start...")

    logger.info(f"Deployment group environment setup complete: {dg_name}")

    yield {
        "dg_name": dg_name,
        "instances": instance_names,
        "instance_ports": instance_ports,
        "node_name": node_name,
    }

    logger.info(f"Tearing down deployment group environment: {dg_name}")

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


@pytest.fixture()
def offline_nonmember_instance(asadmin, local_node):
    """
    Create a single standalone instance, bring it up and then stop it so it is
    OFFLINE, and deliberately do NOT add it to any deployment group.

    Regression environment for FISH-14056: creating or deleting a deployment
    group must not try to replicate the command to instances that are not
    members of the group. The instance is started then stopped (rather than
    left NEVER_STARTED) to match the reproduction steps in the ticket.

    Yielded dict keys:
        instance   – the offline, non-member instance name
        node_name  – the local CONFIG node (resolved via the local_node fixture)
    """
    node_name = local_node
    instance_name = "test-inst-offline-nonmember"
    instance_port = 28095

    logger.info("Setting up offline non-member instance environment (FISH-14056)")

    # --- Pre-setup cleanup (handle stale resources from previous runs) ---
    asadmin.run_no_raise("stop-instance", instance_name)
    asadmin.run_no_raise("delete-instance", instance_name)

    logger.info(f"Creating instance: {instance_name} with HTTP port {instance_port}")
    asadmin.run(
        "create-instance",
        f"--node={node_name}",
        f"--systemproperties=HTTP_LISTENER_PORT={instance_port}",
        instance_name,
    )

    # Start then stop so the instance is genuinely OFFLINE (not NEVER_STARTED),
    # which is the state that triggered the misleading replication warning.
    logger.info(f"Starting instance {instance_name} then stopping it to make it offline")
    asadmin.run("start-instance", instance_name)
    time.sleep(10)
    asadmin.run("stop-instance", instance_name)
    time.sleep(5)

    logger.info(f"Offline non-member instance environment ready: {instance_name}")

    yield {
        "instance": instance_name,
        "node_name": node_name,
    }

    logger.info("Tearing down offline non-member instance environment (FISH-14056)")
    asadmin.run_no_raise("start-instance", instance_name)
    time.sleep(5)
    asadmin.run_no_raise("stop-instance", instance_name)
    asadmin.run_no_raise("delete-instance", instance_name)
    logger.info(f"Offline non-member instance environment teardown complete: {instance_name}")

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
            asadmin.run("deploy", "--force=true", f"--target={dg}", f"--name={app_name}", f"--contextroot={app_name}",
                        test_war)

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

    def test_deploy_redeploy_to_offline_instance(
            self, asadmin, deployment_group_env, test_war
    ):
        """Test that applications are deployed/redeployed to an instance that was offline when the deployment/redeployment took place."""
        dg = deployment_group_env["dg_name"]
        instances = deployment_group_env["instances"]
        instance_ports = deployment_group_env["instance_ports"]
        app_name = "clusterjsp-dg-offline-test"

        # Use the second instance as the one to be taken offline
        offline_inst = instances[1]
        online_inst = instances[0]

        logger.info(f"Stopping instance {offline_inst} to test offline deployment")
        asadmin.run("stop-instance", offline_inst)
        time.sleep(5)

        try:
            # Deploy to the deployment group while one instance is offline
            logger.info(f"Deploying app {app_name} to deployment group {dg} while {offline_inst} is offline")
            asadmin.run("deploy", f"--target={dg}", f"--name={app_name}", f"--contextroot={app_name}", test_war)

            # Wait for application to start on the online instance
            logger.info("Waiting for application to start on online instance...")
            time.sleep(10)

            # Verify app is on the online instance
            apps = asadmin.list_applications_on_instance(online_inst)
            assert app_name in apps, (
                f"Application '{app_name}' not found on online instance '{online_inst}'. Apps: {apps}"
            )

            http_port = asadmin.get_instance_http_port(online_inst, instance_ports)
            assert http_port is not None, f"Could not get HTTP port for '{online_inst}'"
            assert check_http_app_available("localhost", http_port, app_name), (
                f"App '{app_name}' not accessible via HTTP on online instance '{online_inst}'"
            )

            # Start the offline instance
            logger.info(f"Starting offline instance {offline_inst}")
            asadmin.run("start-instance", offline_inst)
            time.sleep(10)

            # Verify the app gets deployed to the previously offline instance
            logger.info(f"Verifying app {app_name} is deployed to previously offline instance {offline_inst}")
            apps = asadmin.list_applications_on_instance(offline_inst)
            assert app_name in apps, (
                f"Application '{app_name}' not found on previously offline instance '{offline_inst}'. Apps: {apps}"
            )

            http_port = asadmin.get_instance_http_port(offline_inst, instance_ports)
            assert http_port is not None, f"Could not get HTTP port for '{offline_inst}'"
            assert check_http_app_available("localhost", http_port, app_name), (
                f"App '{app_name}' not accessible via HTTP on previously offline instance '{offline_inst}'"
            )

            # Now test redeployment scenario: stop the instance again
            logger.info(f"Stopping instance {offline_inst} again to test offline redeployment")
            asadmin.run("stop-instance", offline_inst)
            time.sleep(5)

            # Redeploy the app while instance is offline
            logger.info(f"Redeploying app {app_name} to deployment group {dg} while {offline_inst} is offline")
            asadmin.run("deploy", "--force=true", f"--target={dg}", f"--name={app_name}", f"--contextroot={app_name}",
                        test_war)

            # Wait for redeployment on online instance
            logger.info("Waiting for redeployment on online instance...")
            time.sleep(10)

            # Verify app is still on the online instance after redeploy
            apps = asadmin.list_applications_on_instance(online_inst)
            assert app_name in apps, (
                f"Application '{app_name}' not found on online instance '{online_inst}' after redeploy. Apps: {apps}"
            )

            http_port = asadmin.get_instance_http_port(online_inst, instance_ports)
            assert http_port is not None, f"Could not get HTTP port for '{online_inst}'"
            assert check_http_app_available("localhost", http_port, app_name), (
                f"App '{app_name}' not accessible via HTTP on online instance '{online_inst}' after redeploy"
            )

            # Start the offline instance again
            logger.info(f"Starting offline instance {offline_inst} after redeployment")
            asadmin.run("start-instance", offline_inst)
            time.sleep(10)

            # Verify the redeployed app gets deployed to the previously offline instance
            logger.info(f"Verifying redeployed app {app_name} is deployed to previously offline instance {offline_inst}")
            apps = asadmin.list_applications_on_instance(offline_inst)
            assert app_name in apps, (
                f"Application '{app_name}' not found on previously offline instance '{offline_inst}' after redeploy. Apps: {apps}"
            )

            http_port = asadmin.get_instance_http_port(offline_inst, instance_ports)
            assert http_port is not None, f"Could not get HTTP port for '{offline_inst}'"
            assert check_http_app_available("localhost", http_port, app_name), (
                f"App '{app_name}' not accessible via HTTP on previously offline instance '{offline_inst}' after redeploy"
            )

        finally:
            # Ensure the offline instance is started before cleanup
            logger.info(f"Ensuring instance {offline_inst} is started before cleanup")
            asadmin.run_no_raise("start-instance", offline_inst)
            time.sleep(5)
            asadmin.run_no_raise("undeploy", f"--target={dg}", app_name)


    def test_deploy_then_add_instance_propagates_app(
            self, asadmin, deployment_group_env, test_war
    ):
        """Test that applications deployed to a deployment group are propagated to a running instance when added to the group after deployment."""
        dg = deployment_group_env["dg_name"]
        instances = deployment_group_env["instances"]
        instance_ports = deployment_group_env["instance_ports"]
        node_name = deployment_group_env["node_name"]
        app_name = "clusterjsp-dg-add-instance-test"

        # Use only the first instance for initial deployment
        initial_inst = instances[0]

        # Remove the second instance from the deployment group temporarily
        logger.info(f"Removing instance {instances[1]} from deployment group {dg}")
        asadmin.run(
            "remove-instance-from-deployment-group",
            f"--instance={instances[1]}",
            f"--deploymentgroup={dg}",
        )

        try:
            # Deploy application to deployment group (only has one instance now)
            logger.info(f"Deploying app {app_name} to deployment group {dg}")
            asadmin.run("deploy", f"--target={dg}", f"--name={app_name}", f"--contextroot={app_name}", test_war)

            # Wait for application to start on the initial instance
            logger.info("Waiting for application to start on initial instance...")
            time.sleep(10)

            # Verify app is on the initial instance
            apps = asadmin.list_applications_on_instance(initial_inst)
            assert app_name in apps, (
                f"Application '{app_name}' not found on initial instance '{initial_inst}'. Apps: {apps}"
            )

            http_port = asadmin.get_instance_http_port(initial_inst, instance_ports)
            assert http_port is not None, f"Could not get HTTP port for '{initial_inst}'"
            assert check_http_app_available("localhost", http_port, app_name), (
                f"App '{app_name}' not accessible via HTTP on initial instance '{initial_inst}'"
            )

            # Create a new instance
            new_inst = "test-inst-new"
            new_port = 28082
            logger.info(f"Creating new instance: {new_inst} with HTTP port {new_port}")
            asadmin.run(
                "create-instance",
                f"--node={node_name}",
                f"--systemproperties=HTTP_LISTENER_PORT={new_port}",
                new_inst,
            )

            try:
                # Start the new instance
                logger.info(f"Starting new instance: {new_inst}")
                asadmin.run("start-instance", new_inst)
                time.sleep(10)

                # Add the new instance to the deployment group
                logger.info(f"Adding instance {new_inst} to deployment group {dg}")
                asadmin.run(
                    "add-instance-to-deployment-group",
                    f"--instance={new_inst}",
                    f"--deploymentgroup={dg}",
                )

                # Wait for the application to propagate to the new instance
                logger.info("Waiting for application to propagate to newly added instance...")
                time.sleep(15)

                # Verify the app is deployed to the newly added instance
                apps = asadmin.list_applications_on_instance(new_inst)
                assert app_name in apps, (
                    f"Application '{app_name}' not found on newly added instance '{new_inst}'. Apps: {apps}"
                )

                new_instance_ports = {new_inst: new_port}
                http_port = asadmin.get_instance_http_port(new_inst, new_instance_ports)
                assert http_port is not None, f"Could not get HTTP port for '{new_inst}'"
                assert check_http_app_available("localhost", http_port, app_name), (
                    f"App '{app_name}' not accessible via HTTP on newly added instance '{new_inst}'"
                )

            finally:
                # Cleanup the new instance
                logger.info(f"Removing instance {new_inst} from deployment group {dg}")
                asadmin.run_no_raise(
                    "remove-instance-from-deployment-group",
                    f"--instance={new_inst}",
                    f"--deploymentgroup={dg}",
                )
                logger.info(f"Stopping instance: {new_inst}")
                asadmin.run_no_raise("stop-instance", new_inst)
                time.sleep(5)
                logger.info(f"Deleting instance: {new_inst}")
                asadmin.run_no_raise("delete-instance", new_inst)

        finally:
            # Re-add the second instance to the deployment group for cleanup
            logger.info(f"Re-adding instance {instances[1]} to deployment group {dg}")
            asadmin.run_no_raise(
                "add-instance-to-deployment-group",
                f"--instance={instances[1]}",
                f"--deploymentgroup={dg}",
            )
            asadmin.run_no_raise("undeploy", f"--target={dg}", app_name)

    def _get_sample_app_path(self, version: str) -> str:
        """Helper method to get the path to a sample app version."""
        war_path = os.path.join(os.path.dirname(__file__), "..", "test-apps", f"sample-app-{version}.war")
        if not os.path.isfile(war_path):
            raise RuntimeError(f"Sample app {version} WAR file not found: {war_path}")
        logger.info(f"Using sample app {version} WAR: {war_path}")
        return war_path

    def test_versioned_deployment_undeploy_old_version_keeps_new_accessible(
            self, asadmin, deployment_group_env
    ):
        """
        Test versioned deployment scenario:
        1. Deploy version 1.0.0
        2. Deploy version 1.0.1 (same context root)
        3. Verify 1.0.1 is accessible
        4. Undeploy old version 1.0.0
        5. Verify 1.0.1 remains accessible
        """
        dg = deployment_group_env["dg_name"]
        instances = deployment_group_env["instances"]
        instance_ports = deployment_group_env["instance_ports"]
        
        # Get paths to both versions
        sample_app_1_0_0 = self._get_sample_app_path("1.0.0")
        sample_app_1_0_1 = self._get_sample_app_path("1.0.1")

        try:
            # Deploy version 1.0.0
            logger.info("Deploy version 1.0.0:")
            logger.info("asadmin deploy --contextroot sample-app --name sample-app:1.0.0 sample-app-1.0.0.war")
            asadmin.run("deploy", f"--target={dg}", "--contextroot", "sample-app", "--name", "sample-app:1.0.0", sample_app_1_0_0)
            time.sleep(5)

            # Deploy version 1.0.1
            logger.info("Deploy version 1.0.1:")
            logger.info("asadmin deploy --contextroot sample-app --name sample-app:1.0.1 sample-app-1.0.1.war")
            asadmin.run("deploy", f"--target={dg}", "--contextroot", "sample-app", "--name", "sample-app:1.0.1", sample_app_1_0_1)
            time.sleep(5)

            # Access app and verify it works (shows version 1.0.1)
            logger.info("Access app:")
            logger.info("http://localhost:8080/sample-app → works (shows version 1.0.1)")
            
            inst = instances[0]
            http_port = asadmin.get_instance_http_port(inst, instance_ports)
            assert http_port is not None, f"Could not get HTTP port for '{inst}'"
            
            assert check_http_content("localhost", http_port, "sample-app", "Hello from version 1.0.1"), (
                f"App not accessible or doesn't show version 1.0.1 content on '{inst}'"
            )
            logger.info("✓ App accessible and shows version 1.0.1")

            # Undeploy old version
            logger.info("Undeploy old version:")
            logger.info("asadmin undeploy sample-app:1.0.0")
            asadmin.run("undeploy", f"--target={dg}", "sample-app:1.0.0")
            time.sleep(5)

            # Verify app is still accessible after undeploying old version
            assert check_http_content("localhost", http_port, "sample-app", "Hello from version 1.0.1"), (
                f"App not accessible after undeploying old version on '{inst}'"
            )
            logger.info("✓ App remains accessible after undeploying old version")

        finally:
            # Cleanup
            logger.info("Cleaning up test applications")
            asadmin.run_no_raise("undeploy", f"--target={dg}", "sample-app:1.0.0")
            asadmin.run_no_raise("undeploy", f"--target={dg}", "sample-app:1.0.1")

    def test_redeploy_preserves_virtual_server_default_module(
            self, asadmin, single_instance_deployment_group_env, test_war
    ):
        """
        Test that redeploying an application to a deployment group does not break the virtual server
        when the application is configured as the Default Web Module.

        Regression test for bug: After setting a deployed app as the Default Web Module for an instance's
        virtual server (via the default-web-module attribute), redeploying the app with --force=true used to
        cause the virtual server endpoint to fail with an exception in the server log. This test verifies the fix.
        """
        dg = single_instance_deployment_group_env["dg_name"]
        inst = single_instance_deployment_group_env["instance"]
        config_name = single_instance_deployment_group_env["config_name"]
        http_port = str(single_instance_deployment_group_env["instance_port"])
        app_name = "clusterjsp-dg-default-module-test"
        context_root = "/myapp"

        virtual_server = "server"

        logger.info(f"Instance {inst} uses config: {config_name}")

        try:
            logger.info(f"Deploying {app_name} to deployment group {dg} with context root {context_root}")
            asadmin.run("deploy", f"--target={dg}", f"--name={app_name}", f"--contextroot={context_root}", test_war)

            logger.info("Waiting for application to start...")
            time.sleep(10)

            # Verify app is accessible at its context root
            app_url = f"http://localhost:{http_port}{context_root}/"
            try:
                response = requests.get(app_url, timeout=5)
                assert response.status_code == 200, (
                    f"App '{app_name}' not accessible via HTTP on '{inst}' at {app_url} (status: {response.status_code})"
                )
                logger.info(f"✓ {app_name} is accessible at {app_url}")
            except requests.exceptions.RequestException as e:
                pytest.fail(f"App '{app_name}' not accessible via HTTP on '{inst}' at {app_url}: {e}")

            # Configure the app as the default web module for the virtual server
            logger.info(f"Setting {app_name} as default-web-module for virtual server {virtual_server} in config {config_name}")
            asadmin.run("set", f"configs.config.{config_name}.http-service.virtual-server.{virtual_server}.default-web-module={app_name}")

            # Wait for the configuration change to take effect
            time.sleep(5)

            # Verify the app is now accessible at the root URL via the default web module
            root_url = f"http://localhost:{http_port}/"
            try:
                response = requests.get(root_url, timeout=5)
                assert response.status_code == 200, (
                    f"App '{app_name}' not accessible via HTTP on '{inst}' at root URL as default web module (status: {response.status_code})"
                )
                logger.info(f"✓ {app_name} is accessible via root URL as default web module")
            except requests.exceptions.RequestException as e:
                pytest.fail(f"App '{app_name}' not accessible via HTTP on '{inst}' at root URL as default web module: {e}")

            logger.info(f"Redeploying {app_name} to deployment group {dg} with --force=true")
            asadmin.run("deploy", "--force=true", f"--target={dg}", f"--name={app_name}", f"--contextroot={context_root}",
                        test_war)

            logger.info("Waiting for redeployment to complete...")
            time.sleep(10)

            # Verify the virtual server endpoint still works after redeploy
            logger.info(f"Testing virtual server endpoint after redeploy: {root_url}")
            try:
                response = requests.get(root_url, timeout=5)
                assert response.status_code == 200, (
                    f"Virtual server endpoint returned status {response.status_code} after redeploy. "
                    f"This indicates the bug is not fixed."
                )
                logger.info(f"✓ Virtual server endpoint is accessible after redeploy (status: {response.status_code})")
            except requests.exceptions.RequestException as e:
                pytest.fail(f"Virtual server endpoint failed after redeploy: {e}. This indicates the bug is not fixed.")

        finally:
            # Clear the default-web-module configuration
            logger.info(f"Clearing default-web-module for virtual server {virtual_server} in config {config_name}")
            asadmin.run_no_raise("set", f"configs.config.{config_name}.http-service.virtual-server.{virtual_server}.default-web-module=")
            asadmin.run_no_raise("undeploy", f"--target={dg}", app_name)


class TestDeploymentGroupReplicationWarning:
    """
    Regression tests for FISH-14056.

    Deployment-group membership commands must not attempt to replicate to
    instances that are not members of the group. Previously
    create-deployment-group, delete-deployment-group,
    add-instance-to-deployment-group and remove-instance-from-deployment-group
    were annotated with @ExecuteOn(RuntimeType.ALL), which replicated the command
    to every instance in the domain (via Target.getAllInstances()) and produced a
    misleading "Instance <x> seems to be offline; command ... was not replicated
    to that instance" warning for unrelated offline instances.

    The fix:
    - create-deployment-group / delete-deployment-group run @ExecuteOn(RuntimeType.DAS)
      (there are no members to replicate to at that point).
    - add-instance-to-deployment-group / remove-instance-from-deployment-group run
      @ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE}) with
      @TargetType(CommandTarget.DEPLOYMENT_GROUP), so replication is scoped to the
      group's own members (Target.getInstances(<group>)) instead of every instance in
      the domain. Offline NON-members are therefore never contacted and no misleading
      warning is produced, while running members learn the membership change live
      without a restart.
    - On add, the command implements UndoableCommand and uses prepare() on the DAS to
      widen the replicated "instance" parameter to the group's FULL membership, so a
      newly joined running instance converges to the complete member set live rather
      than only recording its own reference.

    The DAS domain.xml remains the single source of truth; a removed instance (which is
    already excluded from the group at replication time) reconciles its now-inert refs on
    the next startup via the full config sync.
    """

    # Exact signatures of the buggy replication warning.
    WARNING_MARKERS = [
        "was not replicated to that instance",
        "seems to be offline",
    ]

    def _assert_no_replication_warning(self, result, command):
        output = f"{result.stdout}\n{result.stderr}"
        for marker in self.WARNING_MARKERS:
            assert marker not in output, (
                f"'{command}' produced an unexpected replication warning "
                f"(marker: {marker!r}) for an offline non-member instance. "
                f"Output:\n{output}"
            )

    def test_create_deployment_group_no_warning_for_offline_nonmember(
            self, asadmin, offline_nonmember_instance
    ):
        """create-deployment-group must not warn about an unrelated offline instance."""
        dg_name = "test-dg-fish14056-create"
        asadmin.run_no_raise("delete-deployment-group", dg_name)
        try:
            result = asadmin.run("create-deployment-group", dg_name)
            self._assert_no_replication_warning(result, "create-deployment-group")
        finally:
            asadmin.run_no_raise("delete-deployment-group", dg_name)

    def test_delete_deployment_group_no_warning_for_offline_nonmember(
            self, asadmin, offline_nonmember_instance
    ):
        """delete-deployment-group must not warn about an unrelated offline instance."""
        dg_name = "test-dg-fish14056-delete"
        asadmin.run_no_raise("delete-deployment-group", dg_name)
        asadmin.run("create-deployment-group", dg_name)
        result = asadmin.run("delete-deployment-group", dg_name)
        self._assert_no_replication_warning(result, "delete-deployment-group")

    def test_add_instance_to_deployment_group_no_warning_for_offline_nonmember(
            self, asadmin, deployment_group_env, offline_nonmember_instance
    ):
        """add-instance-to-deployment-group must not warn about an unrelated offline instance."""
        dg = deployment_group_env["dg_name"]
        member = deployment_group_env["instances"][0]
        # Remove first so the add actually changes membership, then assert the
        # add produces no replication warning for the offline non-member.
        asadmin.run_no_raise("remove-instance-from-deployment-group",
                             f"--instance={member}", f"--deploymentgroup={dg}")
        result = asadmin.run("add-instance-to-deployment-group",
                             f"--instance={member}", f"--deploymentgroup={dg}")
        self._assert_no_replication_warning(result, "add-instance-to-deployment-group")

    def test_remove_instance_from_deployment_group_no_warning_for_offline_nonmember(
            self, asadmin, deployment_group_env, offline_nonmember_instance
    ):
        """remove-instance-from-deployment-group must not warn about an unrelated offline instance."""
        dg = deployment_group_env["dg_name"]
        member = deployment_group_env["instances"][0]
        try:
            result = asadmin.run("remove-instance-from-deployment-group",
                                 f"--instance={member}", f"--deploymentgroup={dg}")
            self._assert_no_replication_warning(result, "remove-instance-from-deployment-group")
        finally:
            # Restore membership so the fixture teardown starts from a known state.
            asadmin.run_no_raise("add-instance-to-deployment-group",
                                 f"--instance={member}", f"--deploymentgroup={dg}")

    def test_add_running_instance_converges_full_membership_live(
            self, asadmin, deployment_group_env
    ):
        """
        A running instance joining a group must converge to the group's FULL membership
        live (no restart), not just record its own reference. Both the pre-existing member
        and the newly added instance must end up listing every member in their own live
        config.
        """
        dg = deployment_group_env["dg_name"]
        node = deployment_group_env["node_name"]
        existing, joining = deployment_group_env["instances"]

        # Fail loudly (rather than as a confusing empty membership set) if the instance
        # config file cannot be located under PAYARA_HOME on this environment.
        assert find_instance_domain_xml(node, existing) is not None, (
            f"Could not locate config/domain.xml for instance '{existing}' on node "
            f"'{node}' under PAYARA_HOME={os.environ.get('PAYARA_HOME')!r}"
        )

        # Start from a single-member group: existing member only, joining instance running
        # but not a member.
        asadmin.run_no_raise("remove-instance-from-deployment-group",
                             f"--instance={joining}", f"--deploymentgroup={dg}")
        wait_for_instance_dg_members(node, existing, {existing})

        # Add the still-running instance back; it must learn about the pre-existing member.
        result = asadmin.run("add-instance-to-deployment-group",
                             f"--instance={joining}", f"--deploymentgroup={dg}")
        self._assert_no_replication_warning(result, "add-instance-to-deployment-group")

        expected = {existing, joining}
        joining_members = wait_for_instance_dg_members(node, joining, expected)
        existing_members = wait_for_instance_dg_members(node, existing, expected)

        assert set(joining_members) == expected, (
            f"Newly added instance '{joining}' did not converge to the full membership "
            f"live. Expected {expected}, got {set(joining_members)}"
        )
        assert set(existing_members) == expected, (
            f"Existing member '{existing}' did not learn the new member live. "
            f"Expected {expected}, got {set(existing_members)}"
        )

    def test_add_instance_via_management_rest_succeeds(
            self, asadmin, deployment_group_env
    ):
        """
        Reproduce the admin console path. The console POSTs to
        /management/domain/deployment-groups/add-instance-to-deployment-group with the
        group passed under the 'deploymentGroup' parameter (not 'target'). Because the
        command is @TargetType(DEPLOYMENT_GROUP), the framework must still resolve the
        deployment group as the replication target when it arrives under 'deploymentGroup'
        rather than 'target' — otherwise the target defaults to 'server' and the command
        fails with "Target server is not a supported type". Regression test for that.
        """
        dg = deployment_group_env["dg_name"]
        node = deployment_group_env["node_name"]
        existing, joining = deployment_group_env["instances"]
        port = asadmin.get_das_admin_port()

        # Remove the joining instance first so we can re-add it through the REST path.
        asadmin.run_no_raise("remove-instance-from-deployment-group",
                             f"--instance={joining}", f"--deploymentgroup={dg}")
        wait_for_instance_dg_members(node, existing, {existing})

        response = das_management_post(
            port, "deployment-groups/add-instance-to-deployment-group",
            {"deploymentGroup": dg, "instance": joining})
        # A failed command (the regression) comes back as HTTP 500 with the action report,
        # so a 200 is the primary success gate; the message is surfaced for diagnosis.
        assert response.status_code == 200, (
            f"REST add-instance-to-deployment-group returned HTTP {response.status_code} "
            f"(this is the console regression): {response.text[:500]}"
        )
        body = response.json()
        exit_code = body.get("exit_code") or body.get("exitCode")
        if exit_code is not None:
            assert exit_code == "SUCCESS", (
                f"REST add-instance-to-deployment-group did not succeed: "
                f"{exit_code} - {body.get('message')}"
            )

        expected = {existing, joining}
        joining_members = wait_for_instance_dg_members(node, joining, expected)
        assert set(joining_members) == expected, (
            f"Instance '{joining}' added via the console/REST path did not converge to "
            f"the full membership live. Expected {expected}, got {set(joining_members)}"
        )

    def test_add_running_instance_to_group_created_after_start_converges(
            self, asadmin, local_node
    ):
        """
        A running instance added to a deployment group that was created *after* the
        instance started must still converge to the group membership live, without a
        restart.

        This is the exact admin-console workflow Andrew reported: create an instance,
        start it, then create a deployment group and add the running instance to it.
        Because create-deployment-group runs on the DAS only, the group is absent from
        the already-running instance's in-memory config, so the replicated
        add-instance-to-deployment-group must create the group on the instance instead
        of silently dropping the change. Without the fix the instance never records the
        membership until its next restart. Regression test for FISH-14056.
        """
        node = local_node
        instance = "test-inst-dg-after-start"
        dg = "test-dg-created-after-start"
        instance_port = 28096

        # Pre-setup cleanup (handle stale resources from previous runs).
        asadmin.run_no_raise("remove-instance-from-deployment-group",
                             f"--instance={instance}", f"--deploymentgroup={dg}")
        asadmin.run_no_raise("delete-deployment-group", dg)
        asadmin.run_no_raise("stop-instance", instance)
        asadmin.run_no_raise("delete-instance", instance)

        try:
            # Create and start the instance BEFORE the group exists, so the running
            # instance has no knowledge of it.
            asadmin.run("create-instance", f"--node={node}",
                        f"--systemproperties=HTTP_LISTENER_PORT={instance_port}",
                        instance)
            asadmin.run("start-instance", instance)

            assert find_instance_domain_xml(node, instance) is not None, (
                f"Could not locate config/domain.xml for instance '{instance}' on node "
                f"'{node}' under PAYARA_HOME={os.environ.get('PAYARA_HOME')!r}"
            )
            # The freshly started instance is not a member of anything yet.
            assert instance not in read_instance_dg_members(node, instance), (
                f"Instance '{instance}' unexpectedly already appears as a deployment-group "
                f"member before the group was created"
            )

            # Create the group only now (DAS-only), then add the still-running instance.
            asadmin.run("create-deployment-group", dg)
            result = asadmin.run("add-instance-to-deployment-group",
                                 f"--instance={instance}", f"--deploymentgroup={dg}")
            self._assert_no_replication_warning(result, "add-instance-to-deployment-group")

            # The running instance must learn its membership live even though the group
            # did not exist in its config when it started.
            deadline = time.time() + 30
            members = read_instance_dg_members(node, instance)
            while instance not in members and time.time() < deadline:
                time.sleep(1)
                members = read_instance_dg_members(node, instance)
            assert instance in members, (
                f"Instance '{instance}' added to a group created after it started did not "
                f"converge to its membership live. Expected '{instance}' in its dg-server-refs, "
                f"got {members}"
            )
        finally:
            asadmin.run_no_raise("remove-instance-from-deployment-group",
                                 f"--instance={instance}", f"--deploymentgroup={dg}")
            asadmin.run_no_raise("delete-deployment-group", dg)
            asadmin.run_no_raise("stop-instance", instance)
            asadmin.run_no_raise("delete-instance", instance)

    def test_remove_running_instance_updates_remaining_member_live(
            self, asadmin, deployment_group_env
    ):
        """
        Removing an instance from a group must drop it from the remaining running members'
        live config without a restart.
        """
        dg = deployment_group_env["dg_name"]
        node = deployment_group_env["node_name"]
        remaining, removed = deployment_group_env["instances"]

        # Fail loudly (rather than as a confusing empty membership set) if the instance
        # config file cannot be located under PAYARA_HOME on this environment.
        assert find_instance_domain_xml(node, remaining) is not None, (
            f"Could not locate config/domain.xml for instance '{remaining}' on node "
            f"'{node}' under PAYARA_HOME={os.environ.get('PAYARA_HOME')!r}"
        )

        # Both instances are members after the fixture setup; confirm the remaining member
        # sees both before the removal.
        wait_for_instance_dg_members(node, remaining, {remaining, removed})

        result = asadmin.run("remove-instance-from-deployment-group",
                             f"--instance={removed}", f"--deploymentgroup={dg}")
        self._assert_no_replication_warning(result, "remove-instance-from-deployment-group")

        try:
            remaining_members = wait_for_instance_dg_members(node, remaining, {remaining})
            assert set(remaining_members) == {remaining}, (
                f"Remaining member '{remaining}' did not drop '{removed}' live. "
                f"Expected {{{remaining}}}, got {set(remaining_members)}"
            )
        finally:
            # Restore membership so the fixture teardown starts from a known state.
            asadmin.run_no_raise("add-instance-to-deployment-group",
                                 f"--instance={removed}", f"--deploymentgroup={dg}")

    def test_remove_running_instance_drops_own_ref_live(
            self, asadmin, deployment_group_env
    ):
        """
        A running instance removed from a group must drop the membership from its OWN live
        config without a restart, not just from the remaining members' configs.

        The framework replicates the removal only to the group's CURRENT members, and the
        removed instance is no longer a member at replication time, so it would otherwise
        keep its now-stale reference until its next restart. The command therefore also
        replicates the removal to the departed running instance so it converges live.
        Regression test for FISH-14056.
        """
        dg = deployment_group_env["dg_name"]
        node = deployment_group_env["node_name"]
        remaining, removed = deployment_group_env["instances"]

        # Fail loudly (rather than as a confusing empty membership set) if the instance
        # config file cannot be located under PAYARA_HOME on this environment.
        assert find_instance_domain_xml(node, removed) is not None, (
            f"Could not locate config/domain.xml for instance '{removed}' on node "
            f"'{node}' under PAYARA_HOME={os.environ.get('PAYARA_HOME')!r}"
        )

        # Both instances are members after the fixture setup; confirm the instance that is
        # about to be removed currently lists itself.
        wait_for_instance_dg_members(node, removed, {remaining, removed})

        result = asadmin.run("remove-instance-from-deployment-group",
                             f"--instance={removed}", f"--deploymentgroup={dg}")
        self._assert_no_replication_warning(result, "remove-instance-from-deployment-group")

        try:
            # The removed, still-running instance must no longer list itself as a member.
            deadline = time.time() + 30
            members = read_instance_dg_members(node, removed)
            while removed in members and time.time() < deadline:
                time.sleep(1)
                members = read_instance_dg_members(node, removed)
            assert removed not in members, (
                f"Removed running instance '{removed}' did not drop its own membership live. "
                f"Expected '{removed}' absent from its dg-server-refs, got {members}"
            )
        finally:
            # Restore membership so the fixture teardown starts from a known state.
            asadmin.run_no_raise("add-instance-to-deployment-group",
                                 f"--instance={removed}", f"--deploymentgroup={dg}")

    def test_rejoining_instance_reconciles_stale_membership(
            self, asadmin, deployment_group_env
    ):
        """
        A running instance that rejoins a group whose membership shrank while it was not a
        member must reconcile to the DAS membership, not merely add itself on top of its
        stale copy.

        The DAS widens the replicated ``instance`` parameter to the group's full membership,
        so on a member that list is authoritative. Only adding the missing references would
        leave the members that departed while this instance was outside the group as phantom
        entries in its own config until its next restart.

        Sequence (with the fixture's two running instances A and B):
          1. remove B  -> DAS [A]; B drops its own ref live, so B's copy is [A]
          2. remove A  -> DAS [];  B is no longer a member, so it never hears this and keeps [A]
          3. add B     -> DAS [B]; B must end up with exactly [B], not [A, B]
        Regression test for FISH-14056.
        """
        dg = deployment_group_env["dg_name"]
        node = deployment_group_env["node_name"]
        first, second = deployment_group_env["instances"]

        assert find_instance_domain_xml(node, second) is not None, (
            f"Could not locate config/domain.xml for instance '{second}' on node "
            f"'{node}' under PAYARA_HOME={os.environ.get('PAYARA_HOME')!r}"
        )

        # Both instances are members after the fixture setup.
        wait_for_instance_dg_members(node, second, {first, second})

        try:
            # 1. Take the second instance out; it drops its own ref and is left holding
            #    a copy of the membership that is correct at this point.
            asadmin.run("remove-instance-from-deployment-group",
                        f"--instance={second}", f"--deploymentgroup={dg}")
            wait_for_instance_dg_members(node, second, {first})

            # 2. Take the first instance out too. The second instance is not a member any
            #    more, so the removal is not replicated to it and its copy goes stale.
            asadmin.run("remove-instance-from-deployment-group",
                        f"--instance={first}", f"--deploymentgroup={dg}")
            stale_members = read_instance_dg_members(node, second)
            assert first in stale_members, (
                f"Test precondition not met: '{second}' was expected to still hold the stale "
                f"reference to '{first}', got {stale_members}"
            )

            # 3. Rejoin. The instance must converge to the DAS membership exactly.
            result = asadmin.run("add-instance-to-deployment-group",
                                 f"--instance={second}", f"--deploymentgroup={dg}")
            self._assert_no_replication_warning(result, "add-instance-to-deployment-group")

            members = wait_for_instance_dg_members(node, second, {second})
            assert set(members) == {second}, (
                f"Rejoining instance '{second}' did not reconcile its membership. "
                f"Expected {{{second}}}, got {set(members)} — '{first}' is a phantom member "
                f"left over from the copy the instance held while it was outside the group"
            )
        finally:
            # Restore membership so the fixture teardown starts from a known state.
            asadmin.run_no_raise("add-instance-to-deployment-group",
                                 f"--instance={first}", f"--deploymentgroup={dg}")
