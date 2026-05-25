#!/usr/bin/env python3
"""
Functional test: start Payara, run jms-ping, validate server log entry.
"""

import argparse
import logging
import os
import platform
import subprocess
import sys
import time
from pathlib import Path
from typing import Tuple

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

EXPECTED_LOG_TEXT = (
    "The default broker instance for OpenMQ is using the default admin password"
)


class AsadminCommandError(Exception):
    pass


class TestJmsPing:
    def __init__(self, asadmin_path: str, domain_name: str, log_dir: str):
        self.asadmin_path = asadmin_path
        self.domain_name = domain_name
        self.log_dir = Path(log_dir)

    def _run_asadmin(self, *args: str) -> Tuple[bool, str, str]:
        cmd = [self.asadmin_path] + list(args)
        try:
            logger.debug(f"Running: {' '.join(cmd)}")
            result = subprocess.run(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                check=False
            )
            if result.returncode != 0:
                logger.error(
                    f"Command failed (code {result.returncode}): {result.stderr.strip()}"
                )
                return False, result.stdout.strip(), result.stderr.strip()
            return True, result.stdout.strip(), result.stderr.strip()
        except Exception as e:
            msg = f"Error running asadmin: {e}"
            logger.error(msg)
            return False, "", msg

    def setup(self):
        logger.info(f"Starting domain: {self.domain_name}")
        success, stdout, stderr = self._run_asadmin('start-domain', self.domain_name)
        if not success:
            raise AsadminCommandError(f"start-domain failed: {stderr}")
        logger.info("Domain started. Waiting 3 seconds...")
        time.sleep(3)

    def run_test(self) -> bool:
        logger.info("Running jms-ping...")
        success, stdout, stderr = self._run_asadmin('jms-ping')
        if not success:
            logger.error(f"jms-ping failed: {stderr}")
            return False
        logger.info(f"jms-ping output: {stdout}")

        logger.info("Waiting 3 seconds...")
        time.sleep(3)

        log_file = self.log_dir / 'server.log'
        logger.info(f"Reading log file: {log_file}")
        if not log_file.exists():
            raise FileNotFoundError(f"Log file not found: {log_file}")

        content = log_file.read_text(encoding='utf-8', errors='replace')
        if EXPECTED_LOG_TEXT in content:
            logger.info("PASS: Expected log entry found.")
            return True

        logger.error("FAIL: Expected log entry not found.")
        logger.error(f"Expected: {EXPECTED_LOG_TEXT!r}")
        logger.error("Last 20 lines of log:")
        for line in content.splitlines()[-20:]:
            logger.error(f"  {line}")
        return False

    def cleanup(self):
        logger.info(f"Stopping domain: {self.domain_name}")
        success, _, stderr = self._run_asadmin('stop-domain', self.domain_name)
        if not success:
            logger.error(f"stop-domain failed (ignored): {stderr}")


def _resolve_asadmin(asadmin_arg: str) -> str:
    if asadmin_arg:
        return asadmin_arg
    payara_home = os.environ.get('PAYARA_HOME')
    if not payara_home:
        sys.exit("ERROR: --asadmin-path not provided and PAYARA_HOME is not set.")
    ext = '.bat' if platform.system() == 'Windows' else ''
    return str(Path(payara_home) / 'bin' / f'asadmin{ext}')


def _resolve_log_dir(log_dir_arg: str, domain_name: str) -> str:
    if log_dir_arg:
        return log_dir_arg
    payara_home = os.environ.get('PAYARA_HOME')
    if not payara_home:
        sys.exit("ERROR: --log-dir not provided and PAYARA_HOME is not set.")
    return str(Path(payara_home) / 'glassfish' / 'domains' / domain_name / 'logs')


def main() -> int:
    parser = argparse.ArgumentParser(
        description='Functional test: start Payara, run jms-ping, validate server log.'
    )
    parser.add_argument(
        '--asadmin-path',
        help='Path to asadmin binary (default: $PAYARA_HOME/bin/asadmin[.bat])'
    )
    parser.add_argument(
        '--domain-name', default='domain1',
        help='Domain name (default: domain1)'
    )
    parser.add_argument(
        '--log-dir',
        help='Path to domain log directory '
             '(default: $PAYARA_HOME/glassfish/domains/<domain-name>/logs)'
    )
    parser.add_argument('--debug', action='store_true', help='Enable debug logging')
    args = parser.parse_args()

    if args.debug:
        logging.getLogger().setLevel(logging.DEBUG)

    asadmin_path = _resolve_asadmin(args.asadmin_path)
    log_dir = _resolve_log_dir(args.log_dir, args.domain_name)

    test = None
    passed = False
    try:
        test = TestJmsPing(
            asadmin_path=asadmin_path,
            domain_name=args.domain_name,
            log_dir=log_dir,
        )
        test.setup()
        passed = test.run_test()
    except Exception as e:
        logger.error(f"Test error: {e}", exc_info=args.debug)
        passed = False
    finally:
        if test:
            test.cleanup()

    if passed:
        logger.info("TEST RESULT: PASSED")
        return 0
    else:
        logger.error("TEST RESULT: FAILED")
        return 1


if __name__ == '__main__':
    sys.exit(main())
