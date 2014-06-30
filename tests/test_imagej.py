import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(__file__)))
from zurich_corpus import ZurichCorpus, ImageJ


def test_imagej_product():
    corpus = ZurichCorpus()
    ij = corpus.list_measurable_products()[0]
    #print ij.versions
    assert 'ij127' in ij.versions
    assert all([version.startswith('ij') for version in ij.versions])
