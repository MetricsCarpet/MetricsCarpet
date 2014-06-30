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
            ]
        return self._products


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
