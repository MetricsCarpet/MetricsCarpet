import os
from datetime import datetime
import logging
from sqlalchemy import create_engine


logger = logging.getLogger(__name__)
# Turn on SQL logging.
# logging.basicConfig()
# logging.getLogger('sqlalchemy.engine').setLevel(logging.DEBUG)
SHOW_SQL_LOGS = True


DATABASE_PATH = os.path.join(
    os.path.dirname(os.path.dirname(__file__)),
    'var', 'measurements',
)
if not os.path.exists(DATABASE_PATH):
    logger.info('Making missing database path: ' + DATABASE_PATH)
    os.makedirs(DATABASE_PATH)


# TODO: make it a part of SQLAlchemy data model?
class Experiment(object):

    def __init__(self, name):
        self.name = name
        self._aggregator = MeasurementAggregator.factory(self)

    @property
    def aggregator(self):
        return self._aggregator

    @aggregator.setter
    def aggregator(self, value):
        self._aggregator = value

    def aggregate_frame(self, data_frame):
        '''
        Aggregate Pandas dataframe into the measurements database.
        Columns and fields a written as they are.
        '''
        self._aggregator.add_frame(data_frame)

    def select_product(name):
        '''
        Select which tested software product would analyzed.
        '''

    def apply_measure(measure_name):
        '''
        Select which measures would be applied for selected products.
        '''

    def apply_tool(measure_name):
        '''
        Use all measures that particular tool provides.
        '''


class MeasurementAggregator(object):

    def __init__(self, experiment_name, timestamp):
        self.experiment_name = experiment_name
        self.timestamp = timestamp

    @staticmethod
    def factory(experiment, timestamp=datetime.now()):
        return SqlAggregator(experiment.name, timestamp)

    def add_frame(self, data_frame):
        raise NotImplementedError()


class SqlAggregator(MeasurementAggregator):

    def __init__(self, experiment_name, timestamp):
        super(SqlAggregator, self).__init__(experiment_name, timestamp)
        self.db_location = os.path.join(
            DATABASE_PATH,
            '%s_%s.sqlite' % (
                self.experiment_name,
                self.timestamp.strftime('%y%m%d%H%M%S'),
            ),
        )
        if os.path.exists(self.db_location):
            raise IOError('Database with this name already exists at: %s' %
                          self.db_location)
        logger.info('Creating SQLite DB: ' + self.db_location)
        self.engine = create_engine('sqlite:///' + self.db_location,
                                    echo=SHOW_SQL_LOGS)

    def add_frame(self, data_frame):
        data_frame.to_sql('measurements', self.engine, if_exists='append')
