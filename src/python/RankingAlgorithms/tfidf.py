from sklearn.metrics.pairwise import cosine_similarity
import numpy as np
import pickle
import scipy.sparse as sp

from ranker import Ranker


class TfIdf(Ranker):
    def __init__(self, vec_name="body"):
        self.vec_name = vec_name
        path = f"src/python/models/vectorizer_{vec_name}.pkl"
        self.vectorizer = pickle.load(open(path, "rb"))

    def get_idf(self, query):
        query_idx = [self.vectorizer.vocabulary_.get(q_i) for q_i in query.split()]
        if None in query_idx:
            query_idx.remove(None)
        if query_idx:
            return np.mean(self.vectorizer.idf_[query_idx])
        else:
            return 0

    def get_scores(self, query, **kwargs):
        """returns cosine similarity of query and docs"""

        doc_vectors = sp.load_npz(f"src/python/data/{self.vec_name}_embedding.npz")
        query_vector = sp.csr_matrix(self.vectorizer.transform([query]))
        scores = cosine_similarity(query_vector, doc_vectors)[0]
        return scores
