#!/usr/bin/env python3
"""Run the isolated Flyway operator main; credentials stay in external config/environment."""
import argparse
from pathlib import Path
import shlex
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('action', choices=['platform-init', 'platform-adopt', 'new', 'adopt', 'upgrade', 'verify'])
parser.add_argument('config', type=Path, help='External tenancy .properties file shared with the service')
parser.add_argument('tenant_id', nargs='?')
parser.add_argument('--offline', action='store_true', help='Use already-cached Maven dependencies')
args = parser.parse_args()
platform = args.action.startswith('platform-')
if platform == bool(args.tenant_id):
    parser.error('Platform actions take no tenant; all other actions require one explicit tenant ID')
if not args.config.is_file():
    parser.error('External config file does not exist')
root = Path(__file__).resolve().parents[2]
arguments = [args.action, str(args.config.resolve())] + ([args.tenant_id] if args.tenant_id else [])
command = [str(root / 'mvnw'), '--no-transfer-progress', '-Dmaven.test.skip=true',
           'spring-boot:run', '-Dspring-boot.run.main-class=org.smart.erp.tenant.migration.TenantMigrationCommand',
           '-Dspring-boot.run.arguments=' + shlex.join(arguments)]
if args.offline:
    command.insert(1, '-o')
raise SystemExit(subprocess.run(command, cwd=root).returncode)
