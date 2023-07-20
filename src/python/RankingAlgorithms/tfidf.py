from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np

from ranker import Ranker


class TfIdf(Ranker):
    def __init__(self, is_ranker=True):
        self.vectorizer = TfidfVectorizer()
        self.is_ranker = is_ranker

    def get_idf(self, query):
        query_idx = [self.vectorizer.vocabulary_.get(q_i) for q_i in query.split()]
        if None in query_idx:
            query_idx.remove(None)
        if query_idx:
            return np.mean(self.vectorizer.idf_[query_idx])
        else:
            return 0

    def get_scores(self, query, df):
        """returns cosine similarity of query and docs"""
        if self.is_ranker:
            docs = df["body"]
        else:
            docs = df
        self.vectorizer.fit(docs)

        query_vector = self.vectorizer.transform([query])
        doc_vectors = self.vectorizer.transform(docs)
        scores = cosine_similarity(query_vector, doc_vectors)[0]
        return scores
