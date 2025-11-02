"""
Wrapper script to run all asadmin test scripts in sequence.
"""

import argparse
import logging
import os
import subprocess
import sys
from typing import List, Tuple

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def find_test_scripts(test_dir: str) -> List[str]:
    """
    Find all Python test scripts in the specified directory.

    Args:
        test_dir: Directory to search for test scripts

    Returns:
        List of paths to test scripts
    """
    test_scripts = []
    for root, _, files in os.walk(test_dir):
        for file in files:
            if file.startswith('test_') and file.endswith('.py'):
                test_scripts.append(os.path.join(root, file))
    return sorted(test_scripts)

def run_test_script(script_path: str, args: List[str]) -> Tuple[bool, str]:
    """Run a single test script and return (success, output)."""
    cmd = [sys.executable, script_path] + args
    logger.info(f"Running test: {os.path.basename(script_path)}")
    logger.info("=" * 80)  # Visual separator for test output
    try:
        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            check=False
        )

        output = result.stdout
        # Log the test output with indentation for better readability
        if output.strip():
            logger.info("Test output:\n" + "\n".join(f"    {line}" for line in output.splitlines()))

        if result.returncode != 0:
            logger.error(f"❌ Test failed with code {result.returncode}: {os.path.basename(script_path)}")
            return False, output
        else:
            logger.info(f"✅ Test passed: {os.path.basename(script_path)}")
            return True, output

    except Exception as e:
        error_msg = f"Error running {script_path}: {str(e)}"
        logger.error(error_msg)
        return False, error_msg
    finally:
        logger.info("=" * 80 + "\n")

def main():
    """Main function to run all test scripts."""
    parser = argparse.ArgumentParser(description='Run all asadmin test scripts')
    parser.add_argument('--asadmin', help='Path to asadmin executable')
    parser.add_argument('--host', default='localhost', help='Domain Administration Server host')
    parser.add_argument('--port', type=int, default=4848, help='Domain Administration Server port')
    parser.add_argument('--user', help='Admin username')
    parser.add_argument('--password', help='Admin password')
    parser.add_argument('--test-dir', default=os.path.dirname(os.path.abspath(__file__)),
                        help='Directory containing test scripts')
    args = parser.parse_args()

    # Build common arguments to pass to test scripts
    test_args = []
    if args.asadmin:
        test_args.extend(['--asadmin', args.asadmin])
    if args.host:
        test_args.extend(['--host', args.host])
    if args.port:
        test_args.extend(['--port', str(args.port)])
    if args.user:
        test_args.extend(['--user', args.user])
    if args.password:
        test_args.extend(['--password', args.password])

    # Find and run all test scripts
    test_scripts = find_test_scripts(args.test_dir)
    if not test_scripts:
        logger.error(f"No test scripts found in {args.test_dir}")
        return 1

    logger.info(f"Found {len(test_scripts)} test scripts to run")

    all_passed = True
    for script in test_scripts:
        success, output = run_test_script(script, test_args)
        if not success:
            all_passed = False
            # Optionally continue running other tests even if one fails
            # Uncomment the next line to stop on first failure
            # return 1

    if all_passed:
        logger.info("All tests passed successfully!")
        return 0
    else:
        logger.error("Some tests failed")
        return 1

if __name__ == "__main__":
    sys.exit(main())
