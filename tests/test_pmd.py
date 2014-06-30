import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(__file__)))
from zurich_corpus import ZurichCorpus, ImageJ
from mcarpet.toolset import PmdTool
from mcarpet.experiments import Experiment, MeasurementAggregator


def test_pmd_tool():
    # Get single source file.
    corpus = ZurichCorpus()
    ij = corpus.list_measurable_products()[0]
    #print ij.versions
    #print os.path.join(ij.location_path, ij.versions[0])
    version = ij.versions[0]
    source_files = [filename for filename in ij.get_source_files(version)]
    #print source_files
    source_file = source_files[0]
    #print source_file
    # Bake instrument instance and do some analysis
    experiment = Experiment('test_pmd_tool')
    tool = PmdTool()
    tool.attach_to_experiment(experiment)
    tool.measure_file(source_file)
    #assert False
