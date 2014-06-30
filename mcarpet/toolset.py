import os
from mcarpet import measures
from mcarpet.utils import invoke
import pandas as pd
from cStringIO import StringIO


KNOWN_LANGUAGES = [
    'C',
    'C++'
    'Java',
    'python',
    'javascript',
    'Matlab',
    'R',
]


class QualityMeasuringTool(object):

    def __init__(self, name, languages, measures):
        self.__experiment = None
        self.name = name
        # Supported langues
        self.languages = languages
        # Supported measures
        self.measures = measures

    @property
    def experiment(self):
        if self.__experiment is None:
            raise Exception('This measurement tool has not been attached to '
                            'any experiment. See documentation for details.')
        return self.__experiment

    def attach_to_experiment(self, experiment):
        self.__experiment = experiment

    @staticmethod
    def list_quality_measuring_tools():
        '''Returns a list of available quality tools'''
        return [
            PmdTool(),
        ]

    def extract_measures(self, source_file_path):
        pass

    @property
    def tools_location_path(self):
        return os.path.join(
            os.path.dirname(os.path.dirname(__file__)),
            'tools',
        )


class PmdTool(QualityMeasuringTool):

    def __init__(self):
        QualityMeasuringTool.__init__(
            self,
            name='pmd',
            languages=['java'],
            measures=[measures.CC().name],
        )
        # MAP MC measures to PMD measures
        self.measures_map = {
            # measure -> ruleset
            measures.CC().name: 'CyclomaticComplexity',
        }
        # MAP PMD measures to rulesets
        self.ruleset_map = {
            'CyclomaticComplexity', 'java-codesize',
        }
        self.executable = os.path.join('pmd-bin-5.1.1', 'bin', 'run.sh')

    def get_ruleset(self, measure_name):
        return self.ruleset_map[self.measures_map[measure_name]]

    def measure_file(self, source_file_path):
        command = ('{executable} pmd -f csv -R {custom_ruleset} '
                   '-d {source_file_path}')
        command = command.format(
            executable=os.path.join(self.tools_location_path,
                                    self.executable),
            custom_ruleset=os.path.join(self.tools_location_path,
                                        'pmd-rulesets', 'java',
                                        'metricscarpet.xml'),
            source_file_path=source_file_path,
        )
        result_csv_data = invoke(command)
        self.process_data(pd.read_csv(StringIO(result_csv_data)))

    def process_data(self, raw_data_frame):
        '''
        We expect and parse PMD data in CSV format according to:

        "Problem","Package","File","Priority","Line","Description",
        "Rule set","Rule".

        In fact we just use pandas to make it straightforward.
        '''
        #
        # Adjust data columns and values as necessary.
        data_frame = raw_data_frame
        # Finally, collect the data by attached experiment.
        self.experiment.aggregate_frame(data_frame)
