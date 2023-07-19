from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np

from ranker import Ranker

class TfIdf(Ranker):

    def __init__(self, corpus):
        self.corpus = corpus
        self.vectorizer = TfidfVectorizer()
        self.vectorizer.fit(self.corpus)
    
    def get_tfidf(self, query):
        return self.vectorizer.transform([query])
    
    def get_tfidf_scores(self, query):
        return self.vectorizer.transform([query]).toarray()[0]
    
    def get_idf(self, query):
        query_idx = [self.vectorizer.vocabulary_.get(q_i) for q_i in query.split()]
        if None in query_idx:
            query_idx.remove(None)
        if query_idx:
            return np.mean(self.vectorizer.idf_[query_idx])
        else:
            return 0
        

    def get_scores(self, query, docs, **kwargs):
        query_vector = self.vectorizer.transform([query])
        doc_vectors = self.vectorizer.transform(docs)
        scores = cosine_similarity(query_vector, doc_vectors)[0]
        return scores
