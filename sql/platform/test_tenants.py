#!/usr/bin/env python3
"""Run HTTP/MySQL tenant-isolation tests in a temporary MySQL 8.4 container.
Extra Maven arguments may be supplied, e.g. a local Mockito -DargLine agent.
"""
import os
from pathlib import Path
import subprocess
import sys
import time
import uuid

root = Path(__file__).resolve().parents[2]
name = 'erp-tenant-' + uuid.uuid4().hex[:10]

def docker(*args):
    return subprocess.run(['docker', *args], text=True, capture_output=True)

try:
    result = docker('run', '--rm', '-d', '--name', name, '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes',
                    '-p', '127.0.0.1::3306', 'mysql:8.4')
    if result.returncode:
        raise RuntimeError(result.stderr)
    for _ in range(90):
        if docker('exec', name, 'mysqladmin', '--protocol=TCP', '-h127.0.0.1', '-uroot', 'ping').returncode == 0:
            break
        time.sleep(1)
    else:
        raise RuntimeError('MySQL readiness timeout')
    mapping = docker('port', name, '3306').stdout.strip()
    if not mapping.startswith('127.0.0.1:'):
        raise RuntimeError('Unexpected test container port mapping')
    port = mapping.rsplit(':', 1)[1]
    environment = dict(os.environ, TENANT_TEST_JDBC_BASE=f'jdbc:mysql://127.0.0.1:{port}/')
    result = subprocess.run([str(root/'mvnw'), '-Dtest=Tenant*Tests,JwtUtilTests,CurrentUserImplTest,RestSecurityErrorHandlerTests',
                             *sys.argv[1:], 'test'], cwd=root, env=environment)
    raise SystemExit(result.returncode)
finally:
    docker('rm', '-f', name)
