import numpy as np
from ranker import Ranker


class BM25(Ranker):
    def __init__(self, k=1.2, b=0.75, is_ranker=True):
        self.k = k
        self.b = b
        self.is_ranker = is_ranker

    def get_scores(self, query, df):
        """returns bm25 of doc"""
        if self.is_ranker:
            docs = df["body"]
        else:
            docs = df
        avgdl = np.mean([len(doc.split()) for doc in docs])
        N = len(docs)
        # number of docs containing query term
        n_q = np.array(
            [sum([1 for doc in docs if q_i in doc]) for q_i in query.split()]
        )
        idf = np.log((N - n_q + 0.5) / (n_q + 0.5) + 1)
        # frequency of query term in doc
        f_q_d = np.array(
            [doc.split().count(q_i) for q_i in query.split() for doc in docs]
        )
        f_q_d = f_q_d.reshape(len(query.split()), len(docs))
        doc_len = np.array([len(doc.split()) for doc in docs])

        frac = (f_q_d * (self.k + 1)) / (
            f_q_d + self.k * (1 - self.b + self.b * doc_len / avgdl)
        )
        scores = idf @ frac

        return scores
