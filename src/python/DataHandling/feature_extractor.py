import reader
import numpy as np
import pandas as pd

class Features:
    def __init__(self, query, documents):   # TODO change structure (reader or data as input?)
        self.query = query
        self.feature_functions = [self.covered_query_term_number, self.covered_query_term_ratio, self.stream_length]
        self.features = self.get_features(documents)
        

    def covered_query_term_number(self, doc: str):
        """returns number of terms in doc that are also in query""" # TODO write tests and check if that is really meant?
        return len(set(doc.split()).intersection(set(self.query.split())))
    
    def covered_query_term_ratio(self, doc: str):
        """returns ratio of terms in doc that are also in query"""
        if len(doc.split()) == 0:
            return np.nan
        return self.covered_query_term_number(doc) / len(doc.split())
    
    def stream_length(self, doc: str):  # TODO could be precomputed (independent of query)
        """returns length of doc"""
        return len(doc.split())
    
    def idf(self, doc: str):
        """returns idf of doc"""
        pass

    def bm25(self, doc: str):
        """returns bm25 of doc"""
        pass
     

    def get_features(self, reader):
        """returns features for all documents in reader"""
        data = pd.DataFrame({'body': reader.body, 'title': reader.title, 'url': reader.url})  # TODO structure: where to put data?
        # TODO is this a problem from reader or database?
        data = data.applymap(lambda x: x[0] if x else np.nan)
        features = data.agg(self.feature_functions)
        return features
        

if __name__ == '__main__':
    docs = reader.Reader()
    features = Features(query='Tübingen', documents=docs)
    print(features.features)