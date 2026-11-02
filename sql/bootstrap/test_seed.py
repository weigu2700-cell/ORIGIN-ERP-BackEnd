#!/usr/bin/env python3
"""Real MySQL regression: fresh install, exact permission coverage, replay safety."""
from pathlib import Path
import importlib.util
import re
import subprocess
import sys
import time
import uuid

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('bootstrap', Path(__file__).with_name('run.py'))
bootstrap = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bootstrap)
name = 'erp-seed-test-' + uuid.uuid4().hex[:10]


def docker(*args, **kwargs):
    return subprocess.run(['docker', *args], text=True, capture_output=True, **kwargs)


def sql(statement):
    result = docker('exec', '-i', name, 'mysql', '-uroot', '--batch', '--skip-column-names',
                    '--default-character-set=utf8mb4', input=statement)
    if result.returncode:
        raise AssertionError(result.stderr)
    return result.stdout.strip()


def script(filename):
    return sql(bootstrap.expand(Path(__file__).parent / filename))


def snapshot():
    tables = ['sys_dept', 'sys_user', 'sys_role', 'sys_permission', 'sys_menu',
              'sys_user_role', 'sys_role_menu', 'sys_role_permission']
    return [sql(f'SELECT COUNT(*) FROM `smart-erp`.{t};') for t in tables]


try:
    started = docker('run', '--rm', '-d', '--name', name,
                     '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes', 'mysql:8.4')
    if started.returncode:
        raise RuntimeError(started.stderr)
    for _ in range(90):
        # Wait for TCP: the image starts a temporary socket-only server first.
        ping = docker('exec', name, 'mysqladmin', '--protocol=TCP', '-h127.0.0.1', '-uroot', 'ping')
        if ping.returncode == 0:
            break
        time.sleep(1)
    else:
        raise RuntimeError('MySQL readiness timeout')
    script('01_schema.sql')
    assert sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='smart-erp' AND table_name='flyway_schema_history';") == '0'
    script('02_seed_dev.sql')
    script('03_verify.sql')
    required = {code for p in (ROOT/'src/main/java').rglob('*.java')
                for expression in re.findall(r'@PreAuthorize\("([^"\n]+)"\)', p.read_text())
                for code in re.findall(r"'([^']+)'", expression)}
    actual = set(sql('SELECT code FROM `smart-erp`.sys_permission WHERE status=1 AND deleted=0;').splitlines())
    assert actual == required, (required - actual, actual - required)
    before = snapshot()
    script('02_seed_dev.sql')
    assert snapshot() == before, 'replay inserted duplicate data'
    # A deliberate password change must survive replay byte for byte.
    changed = '$2a$10$changed-password-must-survive-seed-replay'
    sql(f"UPDATE `smart-erp`.sys_user SET password='{changed}' WHERE username='admin';")
    script('02_seed_dev.sql')
    assert sql("SELECT password FROM `smart-erp`.sys_user WHERE username='admin';") == changed
    sql("UPDATE `smart-erp`.sys_permission SET status=0 WHERE code='master:material:list';")
    sql("UPDATE `smart-erp`.sys_menu SET deleted=1 WHERE path='/master/material';")
    script('02_seed_dev.sql')
    assert sql("SELECT status FROM `smart-erp`.sys_permission WHERE code='master:material:list';") == '0'
    assert sql("SELECT deleted FROM `smart-erp`.sys_menu WHERE path='/master/material';") == '1'
    # Existing installations may use entirely different user / role / department IDs.
    sql('''UPDATE `smart-erp`.sys_dept SET id=90001 WHERE code='DEFAULT';
UPDATE `smart-erp`.sys_user SET id=90002, dept_id=90001 WHERE username='admin';
UPDATE `smart-erp`.sys_role SET id=90003 WHERE code='admin';
DELETE FROM `smart-erp`.sys_user_role;
DELETE FROM `smart-erp`.sys_role_menu;
DELETE FROM `smart-erp`.sys_role_permission;''')
    script('02_seed_dev.sql')
    assert sql('SELECT CONCAT(user_id, ":", role_id) FROM `smart-erp`.sys_user_role;') == '90002:90003'
    # Validate exact menu components when the companion client is available locally.
    views = ROOT.parent / 'origin-erp-client/src/views'
    if views.exists():
        for component in sql("SELECT component FROM `smart-erp`.sys_menu WHERE component<>'';").splitlines():
            assert (views / (component + '.vue')).is_file(), component
    print(f'PASS: {len(required)} exact permissions; fresh install, replay, password/status preservation, natural IDs and menu components')
finally:
    docker('rm', '-f', name)
