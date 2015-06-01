# -*- coding: utf-8
from __future__ import unicode_literals
import os
from mcarpet.corpus import SoftwareProduct, SoftwareCorpus


KNOWN_LIBRARIES = [
    'Bioformats',
    'ImgLib2',
    'Vigra',
    'VTK',
    'ITK',
    'ImageMagick',
]


SOFTWARE_LOCATION = os.path.dirname(os.path.abspath(__file__))


class ZurichCorpus(SoftwareCorpus):

    def __init__(self):
        self.software_location = SOFTWARE_LOCATION
        self._products = None

    def list_measurable_products(self):
        '''Returns a list of available quality tools'''
        if self._products is None:
            self._products = [
                ImageJ(corpus=self),
                Infectio(corpus=self),
                CellProfiler(corpus=self),
            ]
        return self._products

    def get_product(self, name):
        for product in self.list_measurable_products():
            if product.name == name:
                return product
        raise KeyError('Product not found: %s' % name)


class ImageJ(SoftwareProduct):

    def __init__(self, corpus):
        SoftwareProduct.__init__(
            self,
            name='ImageJ',
            language='Java',
            libraries=['Bioformats'],
            established_in_year='1987',  # as NIH Image
            primary_paper=(
                'C.A. Schneider, W.S. Rasband, K.W. Eliceir',
                'NIH Image to ImageJ: 25 years of image analysis',
                'http://www.nature.com/nmeth/journal/v9/n7/full/nmeth.2089.'
                'html',
            ),
            urls=('http://imagej.nih.gov/ij/', ),
            categories=['image_processing'],
            # List of all version available in the corpus.
            versions=[],
            corpus=corpus,
        )
        self.versions = self.get_versions(self.location_path)

    def get_versions(self, location):
        return [version for version in os.listdir(location)
                if version.startswith('ij')]


class Infectio(SoftwareProduct):

    def __init__(self, corpus):
        SoftwareProduct.__init__(
            self,
            name='Infectio',
            language='Matlab',
            libraries=['MEX'],  # Dependencies
            established_in_year='2012',  # as CAPS
            primary_paper=(
                'Yakimovich A, Gumpert H, Burckhardt CJ, Lütschg VA, '
                'Jurgeit A, Sbalzarini IF, Greber UF',
                'Cell-free transmission of human adenovirus by passive mass '
                'transfer in cell culture simulated in a computer model',
                'http://www.ncbi.nlm.nih.gov/pmc/articles/PMC3446567/',
            ),
            urls=('http://infectio.github.io/', ),
            categories=['bio_simulation'],
            # List of all version available in the corpus.
            versions=[],
            corpus=corpus,
        )
        self.versions = self.get_versions(self.location_path)

    def get_versions(self, location):
        return ['master']


class CellProfiler(SoftwareProduct):

    def __init__(self, corpus):
        SoftwareProduct.__init__(
            self,
            name='CellProfiler1.0',
            language='Matlab',
            libraries=['MEX'],  # Dependencies
            established_in_year='2006',
            primary_paper=(
                'Carpenter AE, Jones TR, Lamprecht MR, Clarke C, Kang IH, '
                'Friman O, Guertin DA, Chang JH, Lindquist RA, Moffat J, '
                'Golland P, Sabatini DM',
                'CellProfiler: image analysis software for identifying and '
                'quantifying cell phenotypes',
                'http://genomebiology.com/2006/7/10/R100',
            ),
            urls=('http://www.cellprofiler.org/', ),
            categories=['image_processing', 'bioimage_informatics'],
            # List of all version available in the corpus.
            versions=[],
            corpus=corpus,
        )
        self.versions = self.get_versions(self.location_path)

    def get_versions(self, location):
        return ['latest']
