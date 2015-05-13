import time
import logging
from subprocess import (PIPE, Popen)


logger = logging.getLogger(__name__)


def invoke(command, _in=None):
    '''
    Invoke command as a new system process and return its output.
    '''
    process = Popen(command, stdin=PIPE, stdout=PIPE, stderr=PIPE, shell=True,
                    executable='/bin/bash')
    logger.info('Command: %s' % command)
    if _in is not None:
        return process.communicate(input=_in)
    stdoutdata = process.stdout.read()
    stderrdata = process.stderr.read()
    if len(stdoutdata) > 0:
        logger.info('-STDOUT-' + '-' * 80 + '\n%s' % stdoutdata)
    if len(stderrdata) > 0:
        logger.error('-STDERR-' + '-' * 80 + '\n%s' % stderrdata)
    return (stdoutdata, stderrdata)


class Timer(object):
    '''
    timer = Timer()

    with timer:
        # Whatever you want to measure goes here
        time.sleep(2)

    print timer.duration_in_seconds()
    '''
    def __enter__(self):
        self.__start = time.time()

    def __exit__(self, type, value, traceback):
        # Error handling here
        self.__finish = time.time()

    def duration_in_seconds(self):
        return self.__finish - self.__start
