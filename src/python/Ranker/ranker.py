### this file implements the abstract class Ranker from which all rankers inherit
### the Ranker class is responsible for ranking the documents in the corpus
import abc


class Ranker(object):

    @abc.abstractmethod
    def get_scores(self, query, corpus):
        """returns scores for docs"""
        pass
