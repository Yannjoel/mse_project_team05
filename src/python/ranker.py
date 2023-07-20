### this file implements the abstract class Ranker from which all rankers inherit
### the Ranker class is responsible for ranking the documents in the corpus
import abc
from nltk.tokenize import sent_tokenize, word_tokenize
from nltk.corpus import stopwords
import nltk

nltk.download('stopwords')
STOPWORDS = set(stopwords.words('english'))

class Ranker(object):

    @abc.abstractmethod
    def get_scores(self, query, corpus):
        """returns scores for docs"""
        pass

    def process_query(self, query):
        """returns processed query"""
        # remove stopwords from query
        query = " ".join([word.lower() for word in query.split() if word.lower() not in STOPWORDS])
        return query
