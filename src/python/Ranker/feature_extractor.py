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
        self.data = pd.DataFrame({'body': r.get_bodies(), 'title': r.get_titles(), 'url': r.get_urls()})  # TODO anchor/whole document?
        self.features = self.get_features()

        
    # functions only dependent on  single doc
    def covered_query_term_number(self, doc):
        """returns number of terms in doc that are also in query""" 
        return len(set(doc.split()).intersection(set(self.query.split())))
    
    def covered_query_term_ratio(self, doc):
        """returns ratio of terms in doc that are also in query"""
        
        return self.covered_query_term_number(doc) / len(doc.split()) if len(doc)!= 0 else 0
    
    def stream_length(self, doc):  # TODO could be precomputed (independent of query)
        """returns length of doc"""
        return len(doc.split())
     
    # # functions dependent on multiple docs
    # def idf(self, docs: pd.Series, vectorizer):
    #     """returns idf of doc"""
    #     query_idx = [vectorizer.vocabulary_.get(q_i) for q_i in self.query.split()]
    #     if None in query_idx:
    #         query_idx.remove(None)
    #     if query_idx:
    #         return np.mean(vectorizer.idf_[query_idx])
    #     else:
    #         return 0
        

    # def vector_space_model(self, docs: pd.Series, vectorizer):
    #     """returns vector space model of doc"""
    #     query_vector = vectorizer.transform([self.query])
    #     doc_vectors = vectorizer.transform(docs)
    #     scores = cosine_similarity(query_vector, doc_vectors)[0]
    #     return scores


    # def bm25(self, docs, k=1.5, b=.75):
    #     """returns bm25 of doc"""

    #     avgdl = np.mean([len(doc.split()) for doc in docs])
    #     N = len(docs)
    #     # number of docs containing query term
    #     n_q = np.array([sum([1 for doc in docs if q_i in doc]) for q_i in self.query.split()])
    #     idf = np.log((N - n_q + 0.5) / (n_q + 0.5)+1)
    #     # frequency of query term in doc
    #     f_q_d = np.array([doc.split().count(q_i) for q_i in self.query.split() for doc in docs])
    #     f_q_d = f_q_d.reshape(len(self.query.split()), len(docs))
    #     doc_len = np.array([len(doc.split()) for doc in docs])

    #     frac = (f_q_d * (k + 1)) / (f_q_d + k * (1 - b + b * doc_len / avgdl))
    #     scores = idf @ frac

    #     return scores

    def len_url(self, doc):
        """returns length of url"""
        return len(doc)

    def n_slash(self, doc):
        """returns number of slashes in url"""
        return doc.count('/')


    def get_features(self):
        """returns features for all documents in reader"""
        features = self.data.agg([self.covered_query_term_number, self.covered_query_term_ratio, self.stream_length])
        bm25 = BM25()

        for column in self.data.columns:
            vectorizer = TfIdf(self.data[column])

            features[column + '_idf'] = vectorizer.get_idf(self.query)
            features[column + '_vsm'] = vectorizer.get_similarity_scores(self.query)
            features[column + '_bm25'] = bm25.get_scores(self.query, self.data[column])
            
        
        features['url_n_slash'] = self.data['url'].apply(self.n_slash)
        features['url_len'] = self.data['url'].apply(self.len_url)

        features.columns = ["_".join(a) for a in features.columns.to_flat_index()]
        
        return features 
    

if __name__ == '__main__':
    #docs = Reader()
    features = Features(query='hello Tübingen')
 