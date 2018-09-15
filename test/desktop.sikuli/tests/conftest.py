import org.sikuli.script.SikulixForJython
from sikuli import *
import pytest
import requests
import re
from urllib import urlretrieve

from tests.report import save_test_result, TEST_REPORT_DIR


def pytest_addoption(parser):
    parser.addoption("--nightly",
                     action="store",
                     default=True)
    parser.addoption('--dmg',
                     action='store',
                     default=None,
                     help='Url or local path to dmg')


def pytest_configure(config):
    if config.getoption('nightly'):
        raw_data = requests.request('GET', 'https://status-im.github.io/nightly/').text
        dmg_url = re.findall('href="(.*dmg)', raw_data)[0]
        urlretrieve(dmg_url, 'nightly.dmg')


@pytest.mark.hookwrapper
def pytest_runtest_makereport(item, call):
    import logging
    logging.basicConfig(filename='%s/%s.log' % (TEST_REPORT_DIR, item.name), filemode='w', level=logging.INFO)
    outcome = yield
    report = outcome.get_result()
    if report.when == 'call':
        save_test_result(item, report)
