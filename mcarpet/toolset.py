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

    def __init__(self, name, languages, metrics):
        self.name = name
        self.languages = languages
        self.metrics = metrics

    @staticmethod
    def list_quality_measuring_tools():
        '''Returns  a list of available quality tools'''
        T = QualityMeasuringTool
        return [
            T(
                name='',
                languages=['java'],
                metrics=[''],
            ),
        ]




