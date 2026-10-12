# Copyright 2026 Goldman Sachs
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Summarize Surefire XML test reports as Markdown for a GitHub Actions job.

Usage: test-summary.py [--title TITLE] REPORT_PATH...

Each REPORT_PATH is a Surefire XML report, or a directory whose *.xml files
are read. The Markdown is appended to the file named by GITHUB_STEP_SUMMARY,
which is what puts it on the run's summary page, or written to standard
output when that is not set (for trying it out locally).

This only reports: it exits 0 whatever the tests did, because the test step
has already decided whether the job passes. Standard library only, and no
syntax newer than Python 3.8, so it runs on any hosted runner's Python as-is.
"""

import argparse
import glob
import html
import os
import sys
import xml.etree.ElementTree as ElementTree

PASSED_MARK = chr(0x2705)
FAILED_MARK = chr(0x274C)
WARNING_MARK = chr(0x26A0)

# GitHub caps a step summary at 1 MiB. These keep a badly broken build well
# under that while still showing more failures than anyone reads in one go;
# the uploaded reports have the rest.
MAX_FAILURES_SHOWN = 50
MAX_MESSAGE_CHARS = 300
MAX_TRACE_LINES = 40
MAX_TRACE_CHARS = 6000
SLOWEST_SUITES_SHOWN = 10


class Results:
    def __init__(self):
        self.unreadable = []
        self.tests = 0
        self.passed = 0
        self.failures = 0
        self.errors = 0
        self.skipped = 0
        self.flaky = 0
        self.problems = []
        self.suites = []


def report_files(paths):
    for path in paths:
        if os.path.isdir(path):
            for file in sorted(glob.glob(os.path.join(path, '*.xml'))):
                yield file
        elif os.path.isfile(path):
            yield path


def seconds(value):
    # Older Surefire versions write times with a thousands separator.
    try:
        return float((value or '0').replace(',', ''))
    except ValueError:
        return 0.0


def read_reports(paths):
    results = Results()
    for file in report_files(paths):
        try:
            root = ElementTree.parse(file).getroot()
        except (ElementTree.ParseError, OSError):
            results.unreadable.append(os.path.basename(file))
            continue
        if root.tag not in ('testsuite', 'testsuites'):
            continue
        for suite in root.iter('testsuite'):
            cases = suite.findall('testcase')
            if cases:
                results.suites.append((suite.get('name') or '', len(cases), seconds(suite.get('time'))))
            for case in cases:
                read_case(case, results)
    return results


def read_case(case, results):
    results.tests += 1
    problem = case.find('failure')
    if problem is not None:
        results.failures += 1
    else:
        problem = case.find('error')
        if problem is not None:
            results.errors += 1
    if problem is not None:
        results.problems.append((case.get('classname') or '', case.get('name') or '', problem))
    elif case.find('skipped') is not None:
        results.skipped += 1
    else:
        results.passed += 1
        if case.find('flakyFailure') is not None or case.find('flakyError') is not None:
            results.flaky += 1


def duration(total_seconds):
    total_seconds = int(round(total_seconds))
    hours, rest = divmod(total_seconds, 3600)
    minutes, secs = divmod(rest, 60)
    if hours:
        return '%dh %dm' % (hours, minutes)
    if minutes:
        return '%dm %ds' % (minutes, secs)
    return '%ds' % secs


def plural(count, word):
    return '%s %s%s' % ('{:,}'.format(count), word, '' if count == 1 else 's')


def code(text):
    return '<code>%s</code>' % html.escape(text).replace('|', '&#124;')


def first_line(text, limit):
    line = (text or '').strip().split('\n', 1)[0].strip()
    return line if len(line) <= limit else line[:limit - 3] + '...'


def trimmed_trace(text):
    lines = (text or '').strip().splitlines()
    shown = lines[:MAX_TRACE_LINES]
    trace = '\n'.join(shown)
    if len(trace) > MAX_TRACE_CHARS:
        trace = trace[:MAX_TRACE_CHARS]
    if len(shown) < len(lines) or len(trace) < len('\n'.join(shown)):
        trace += '\n... (trimmed; the full trace is in the uploaded reports)'
    return trace


def headline(title, results):
    prefix = title + ': ' if title else ''
    if results.tests == 0:
        return '%s %sno tests reported' % (WARNING_MARK, prefix)
    broken = results.failures + results.errors
    if broken:
        return '%s %s%s of %s failed' % (FAILED_MARK, prefix, '{:,}'.format(broken), plural(results.tests, 'test'))
    return '%s %s%s passed' % (PASSED_MARK, prefix, plural(results.tests, 'test'))


def summary(title, results):
    out = ['### ' + headline(title, results), '']
    if results.tests == 0:
        out.append('The tests did not run, or stopped before writing any reports.')
        out.append('')
    else:
        total_time = sum(time for _, _, time in results.suites)
        out.append('| Tests | Passed | Failures | Errors | Skipped | Time in tests |')
        out.append('|---:|---:|---:|---:|---:|---:|')
        out.append('| %s | %s | %s | %s | %s | %s |' % (
            '{:,}'.format(results.tests), '{:,}'.format(results.passed), '{:,}'.format(results.failures),
            '{:,}'.format(results.errors), '{:,}'.format(results.skipped), duration(total_time)))
        out.append('')
        out.append('Time in tests is summed across parallel test JVMs.')
        out.append('')
    if results.flaky:
        out.append('%s passed only on a rerun.' % plural(results.flaky, 'test'))
        out.append('')
    if results.unreadable:
        out.append('Could not read %s: %s' % (
            plural(len(results.unreadable), 'report file'), ', '.join(code(name) for name in results.unreadable)))
        out.append('')
    if results.problems:
        out.extend(problem_details(results.problems))
    if results.suites:
        out.extend(slowest_suites(results.suites))
    return '\n'.join(out) + '\n'


def problem_details(problems):
    out = ['#### Failures and errors', '']
    for classname, name, problem in sorted(problems, key=lambda p: (p[0], p[1]))[:MAX_FAILURES_SHOWN]:
        test = classname + '.' + name if classname else name
        message = first_line(problem.get('message'), MAX_MESSAGE_CHARS)
        kind = (problem.get('type') or problem.tag).rsplit('.', 1)[-1]
        detail = kind + (': ' + message if message else '')
        out.append('<details><summary>%s: %s</summary>' % (code(test), html.escape(detail)))
        out.append('')
        out.append('<pre>%s</pre>' % html.escape(trimmed_trace(problem.text)))
        out.append('')
        out.append('</details>')
        out.append('')
    if len(problems) > MAX_FAILURES_SHOWN:
        out.append('')
        out.append('...and %d more; the uploaded reports have them all.' % (len(problems) - MAX_FAILURES_SHOWN))
        out.append('')
    return out


def slowest_suites(suites):
    out = ['<details><summary>Slowest test classes</summary>', '',
           '| Class | Tests | Time |', '|---|---:|---:|']
    for name, tests, time in sorted(suites, key=lambda s: -s[2])[:SLOWEST_SUITES_SHOWN]:
        out.append('| %s | %s | %s |' % (code(name), '{:,}'.format(tests), duration(time)))
    out.extend(['', '</details>', ''])
    return out


def main():
    parser = argparse.ArgumentParser(description='Summarize Surefire XML test reports as Markdown.')
    parser.add_argument('--title', default='', help='what these results are for, such as "JDK 25"')
    parser.add_argument('paths', nargs='+', help='Surefire XML report files, or directories of them')
    args = parser.parse_args()

    text = summary(args.title, read_reports(args.paths))
    summary_file = os.environ.get('GITHUB_STEP_SUMMARY')
    if summary_file:
        with open(summary_file, 'a', encoding='utf-8') as out:
            out.write(text)
    else:
        sys.stdout.buffer.write(text.encode('utf-8'))
    return 0


if __name__ == '__main__':
    sys.exit(main())
