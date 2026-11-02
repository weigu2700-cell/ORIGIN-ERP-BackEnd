#!/usr/bin/env python3
"""Expand the checked-in SQL manifest before invoking mysql in fail-fast batch mode.

Run from any directory: python3 sql/bootstrap/run.py schema -h127.0.0.1 -uroot -p
Credentials remain in mysql's prompt / option file, never in this script.
"""
import argparse
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
MANIFESTS = {'schema': '01_schema.sql', 'seed-dev': '02_seed_dev.sql', 'verify': '03_verify.sql'}


def expand(path):
    chunks = []
    for line in path.read_text(encoding='utf-8').splitlines(keepends=True):
        source = re.fullmatch(r'SOURCE (sql/[\w/.-]+);\s*', line)
        chunks.append(expand(ROOT / source[1]) if source else line)
    return ''.join(chunks)


def main():
    parser = argparse.ArgumentParser(description=__doc__, add_help=False)
    parser.add_argument("--help", action="help")
    parser.add_argument('step', choices=MANIFESTS)
    args, mysql_args = parser.parse_known_args()
    if any(arg in ('-f', '--force') or arg.startswith('--force=') for arg in mysql_args):
        parser.error('--force would continue after SQL errors')
    # SOURCE errors may otherwise let mysql continue to later files. Inline the
    # manifest so the first SQL error aborts and the connection rolls back.
    sql = expand(Path(__file__).parent / MANIFESTS[args.step])
    raise SystemExit(subprocess.run(['mysql', *mysql_args, '--default-character-set=utf8mb4',
                                    '--batch'], input=sql, text=True).returncode)


if __name__ == '__main__':
    main()
