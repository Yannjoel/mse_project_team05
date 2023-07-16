from DataHandling.reader import Reader
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from tfidf import TfIdf
from bmtf import BM25


class Features:
    def __init__(self, query):
        self.query = query.lower()
        r = Reader()
        self.data = pd.DataFrame(
            {"body": r.get_bodies(), "title": r.get_titles(), "url": r.get_urls()}
        )  # TODO anchor/whole document?
        self.features = self.get_features()

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

    def get_features(self):
        """returns features for all documents in reader"""
        features = self.data.agg(
            [
                self.covered_query_term_number,
                self.covered_query_term_ratio,
                self.stream_length,
            ]
        )
        bm25 = BM25()

        for column in self.data.columns:
            vectorizer = TfIdf(self.data[column])

            features[column + "_idf"] = vectorizer.get_idf(self.query)
            features[column + "_vsm"] = vectorizer.get_similarity_scores(self.query)
            features[column + "_bm25"] = bm25.get_scores(self.query, self.data[column])

        features["url_n_slash"] = self.data["url"].apply(self.n_slash)
        features["url_len"] = self.data["url"].apply(self.len_url)

        features.columns = ["_".join(a) for a in features.columns.to_flat_index()]

        return features


if __name__ == "__main__":
    # docs = Reader()
    features = Features(query="hello Tübingen")
