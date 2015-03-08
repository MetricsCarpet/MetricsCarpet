import os
from findtools.find_files import (find_files, Match)


class SoftwareProduct(object):

    def __init__(self, name, language, libraries, established_in_year,
                 primary_paper, urls, categories, versions, corpus):
        self.name = name
        self.language = language
        self.libraries = libraries
        self.established_in_year = established_in_year
        self.primary_paper = primary_paper
        self.urls = urls
        self.categories = categories
        self._location = None
        self.versions = versions
        self.corpus = corpus

    @property
    def location_path(self):
        if self._location is None:
            self._location = os.path.join(self.corpus.software_location,
                                          self.name)
            assert os.path.exists(self._location)
        return self._location

    @staticmethod
    def _find_source_files(language, location):
        if language == 'Java':
            source_code_files = Match(filetype='f', name='*.java')
        else:
            raise Exception('Unknown language: %s' % language)
        return find_files(path=location, match=source_code_files)

    def get_source_path(self, version):
        '''Returns a full path to a tested version.'''
        return os.path.join(self.location_path, version)

    def get_source_files(self, version):
        location = self.get_source_path(version)
        return SoftwareProduct._find_source_files(self.language, location)

    def get_all_source_files(self):
        for version in self.versions:
            for source_file in self.get_source_files(version):
                yield source_file


class SoftwareCorpus(object):

    def list_measurable_products(self):
        raise NotImplementedError()
