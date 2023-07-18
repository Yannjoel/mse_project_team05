import numpy as np
import pandas as pd
from sklearn.preprocessing import StandardScaler


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
        scaler = StandardScaler()
        features = scaler.fit_transform(X=features)
        return features

    def get_features_for_docs(self, docs, name):
        """helper function for get_features, returns features for a single document type"""
        bm25 = BM25()
        tfidf = TfIdf(corpus=docs)
        features = pd.DataFrame()
        features[name + "_bm25"] = bm25.get_scores(self.query, docs)
        features[name + "_idf"] = tfidf.get_idf(self.query)
        features[name + "_vsm"] = tfidf.get_scores(self.query, docs)

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



    # def get_features(self):
    #     """returns features for all documents in reader"""
    #     features = self.data.agg(
    #         [
    #             self.covered_query_term_number,
    #             self.covered_query_term_ratio,
    #             self.stream_length,
    #         ]
    #     )
    #     bm25 = BM25()

    #     for column in self.data.columns:
    #         vectorizer = TfIdf(self.data[column])

    #         features[column + "_idf"] = vectorizer.get_idf(self.query)
    #         features[column + "_vsm"] = vectorizer.get_scores(self.query, self.data[column])
    #         features[column + "_bm25"] = bm25.get_scores(self.query, self.data[column])

    #     features["url_n_slash"] = self.data["url"].apply(self.n_slash)
    #     features["url_len"] = self.data["url"].apply(self.len_url)

    #     features.columns = ["_".join(a) for a in features.columns.to_flat_index()]

    #     return features


if __name__ == "__main__":
    features = Features(query="hello Tübingen")
