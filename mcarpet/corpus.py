import os
from findtools.find_files import (find_files, Match)


KNOWN_LIBRARIES = [
    'Bioformats',
    'ImgLib2',
    'Vigra',
    'VTK',
    'ITK',
    'ImageMagick',
]


SOFTWARE_LOCATION = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    'ZurichCorpus',
)


class SoftwareProduct(object):

    def __init__(self, name, language, libraries, established_in_year,
                 primary_paper, urls, categories):
        self.name = name
        self.language = language
        self.libraries = libraries
        self.established_in_year = established_in_year
        self.primary_paper = primary_paper
        self.urls = urls
        self.categories = categories
        self._location = None

    @property
    def locaiton_path(self):
        if self._location is None:
            self._location = os.path.join(SOFTWARE_LOCATION, self.name)
            assert os.path.exists(self._location)
        return self._location

    @staticmethod
    def list_measurable_products():
        '''Returns  a list of available quality tools'''
        return [
            ImageJ(),
        ]

    def get_files(self):
        raise NotImplementedError()


class ImageJ(SoftwareProduct):

    def __init__(self):
        SoftwareProduct.__init__(
            self,
            name='ImageJ',
            language='java',
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
        )

    def get_files(self):
        source_code_files = Match(filetype='f', name='*.java')
        found_files = find_files(path=self.locaiton_path,
                                 match=source_code_files)

        for found_file in found_files:
            print found_file
