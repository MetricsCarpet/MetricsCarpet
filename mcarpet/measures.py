'''
All metrics of software artifacts are called measures.
'''


class QualityMeasure(object):

    def __init__(self, name, description):
        self.name = name
        self.description = description

    @staticmethod
    def list_measures():
        return [
            NLOC(),
            CC(),
        ]


class NLOC(QualityMeasure):

    def __init__(self):
        QualityMeasure.__init__(
            self,
            'nloc',
            'Total number of lines of code',
        )


class CC(QualityMeasure):

    def __init__(self):
        QualityMeasure.__init__(
            self,
            'cc',
            'Cyclomatic complexity',
        )


# KNOWN_METRICS = {
#     # Structure (code -> description)
#     # Size metrics
#     'nloc': 'Total number of lines of code',
#     'nlec': 'Total number of executable lines of code',
#     'nlc': 'Total number of lines of comments from the code',
#     'nelc': 'Total number of empty lines from the code',
#     # Halstead metrics
#     # Cyclomatic complexity metrics
#     # Basic structure metrics
# }

SIZE_MEASURES = [
    NLOC(),
]
ALL_MEASURES = SIZE_MEASURES
ALL_MEASURE_NAMES = [measure.name for measure in ALL_MEASURES]
