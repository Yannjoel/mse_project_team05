import numpy as np
import pandas as pd


from DataHandling.db_reader import Reader
from Ranker.tfidf import TfIdf
from Ranker.bmtf import BM25


class Features:
    def __init__(self, query, **kwargs):
        self.query = query.lower()
        self.url = kwargs.get("url", None)
        self.title = kwargs.get("title", None)
        self.body = kwargs.get("body", None)


    def get_features(self):
        """returns features for all documents"""
        features = pd.DataFrame()
        for name, docs in zip(["url", "title", "body"], [self.url, self.title, self.body]):
            if docs is not None:
                features = pd.concat(
                    [features, self.get_features_for_docs(docs, name)], axis=1
                )

        return features.values

    def get_features_for_docs(self, docs, name):
        """helper function for get_features, returns features for a single document type"""
        bm25 = BM25()
        tfidf = TfIdf(corpus=docs)
        features = pd.DataFrame()
        features[name + "_bm25"] = bm25.get_scores(self.query, docs)
        features[name + "_idf"] = tfidf.get_idf(self.query)
        features[name + "_vsm"] = tfidf.get_scores(self.query, docs)

        features[name + "_covered_query_term_number"] = docs.apply(self.covered_query_term_number)
        features[name + "_covered_query_term_ratio"] = docs.apply(self.covered_query_term_ratio)
        features[name + "_stream_length"] = docs.apply(self.stream_length)

        if name == "url":
            features[name + "_len_url"] = docs.apply(self.len_url)
            features[name + "_n_slash"] = docs.apply(self.n_slash)

        return features
        
    # functions only dependent on  single doc
    def covered_query_term_number(self, doc):
        """returns number of terms in doc that are also in query"""
        return len(set(doc.split()).intersection(set(self.query.split())))

    def covered_query_term_ratio(self, doc):
        """returns ratio of terms in doc that are also in query"""

        return (
            self.covered_query_term_number(doc) / len(doc.split())
            if len(doc) != 0
            else 0
        )

    def stream_length(self, doc):  # TODO could be precomputed (independent of query)
        """returns length of doc"""
        return len(doc.split())

    def len_url(self, doc):
        """returns length of url"""
        return len(doc)

    def n_slash(self, doc):
        """returns number of slashes in url"""
        return doc.count("/")


if __name__ == "__main__":
    features = Features(query="hello Tübingen")
