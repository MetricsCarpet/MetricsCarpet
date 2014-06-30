from subprocess import (PIPE, Popen)


def invoke(command, _in=None):
    '''
    Invoke command as a new system process and return its output.
    '''
    process = Popen(command, stdin=PIPE, stdout=PIPE, shell=True,
                    executable='/bin/bash')
    if _in is not None:
        process.stdin.write(_in)
    return process.stdout.read()
