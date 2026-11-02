#!/usr/bin/env python3
"""Run tenant-isolation tests in temporary MySQL 8.4 and Redis 7.4 containers.
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
redis_name = name + '-redis'

def docker(*args):
    return subprocess.run(['docker', *args], text=True, capture_output=True)

try:
    result = docker('run', '--rm', '-d', '--name', name, '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes',
                    '-p', '127.0.0.1::3306', 'mysql:8.4')
    if result.returncode:
        raise RuntimeError(result.stderr)
    result = docker('run', '--rm', '-d', '--name', redis_name,
                    '-p', '127.0.0.1::6379', 'redis:7.4-alpine')
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
    redis_mapping = docker('port', redis_name, '6379').stdout.strip()
    if not redis_mapping.startswith('127.0.0.1:'):
        raise RuntimeError('Unexpected Redis test container port mapping')
    for _ in range(30):
        if docker('exec', redis_name, 'redis-cli', 'ping').stdout.strip() == 'PONG':
            break
        time.sleep(1)
    else:
        raise RuntimeError('Redis readiness timeout')
    environment = dict(os.environ, TENANT_TEST_JDBC_BASE=f'jdbc:mysql://127.0.0.1:{port}/',
                       TENANT_TEST_REDIS_PORT=redis_mapping.rsplit(':', 1)[1])
    tests = 'Tenant*Tests,JwtUtilTests,CurrentUserImplTest,RestSecurityErrorHandlerTests,Ai*Test,QueryToolsTest,NotificationInfrastructureTests,BOMRedisTests,PurchaseInStockServiceImplTests,SalesDeliveryServiceImplTests'
    result = subprocess.run([str(root/'mvnw'), '-Dtest=' + tests,
                             *sys.argv[1:], 'test'], cwd=root, env=environment)
    raise SystemExit(result.returncode)
finally:
    docker('rm', '-f', name)
    docker('rm', '-f', redis_name)
