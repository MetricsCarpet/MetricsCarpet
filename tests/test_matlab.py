import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(__file__)))
from mcarpet.corpus import SoftwareProduct
from mcarpet.toolset import MatlabTool
from mcarpet.experiments import Experiment, MeasurementAggregator


MATLAB_TOOL_PATH = os.path.join(
        os.path.dirname(os.path.dirname(
            os.path.abspath(__file__))),
        'tools', 'matlab',
    )
# print MATLAB_TOOL_PATH


class FooProduct(SoftwareProduct):

    def __init__(self, location):
        SoftwareProduct.__init__(
            self,
            name='Foo',
            language='Matlab',
            libraries=['Test'],
            established_in_year='2015',
            primary_paper=(
                'John Doe',
                'Unit testing',
                'http://test.me.com'
                'html',
            ),
            urls=('http://test.me.com', ),
            categories=['unit_testing'],
            # List of all version available in the corpus.
            versions=[],
            corpus=None,
        )
        # Important
        self._location = location


def test_matlab_foo():
    experiment = Experiment('test_matlab_tool')
    tool = MatlabTool()
    tool.attach_to_experiment(experiment)
    product = FooProduct(MATLAB_TOOL_PATH)
    print 'This might take some times, since it starts MATLAB'
    tool.measure(product)


def test_matlab_on_infectio():
    experiment = Experiment('test_matlab_on_infectio')
    tool = MatlabTool()
    tool.attach_to_experiment(experiment)

