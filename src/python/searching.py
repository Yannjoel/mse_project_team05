import numpy as np
from Ranker.bmtf import BM25
from Ranker.tfidf import TfIdf
from Ranker.feature_extractor import Features
import pickle
import pandas as pd
from DataHandling.db_reader import Reader

def searcher(query, df, ranker_str='bm25'):

    # TODO: Implement variable length of results
    # Not sure if this works yet. 
    # It would be great if we could read the data from the database before we start this 
    # function because I think this would be faster.     
    if ranker_str == 'bm25':
        ranker = BM25()
    elif ranker_str == 'tfidf':
        ranker = TfIdf()
    elif ranker_str == 'pwsvm':
        ranker = pickle.load(open('src/python/models/svm.pkl', 'rb'))
    elif ranker_str == 'nn':
        raise NotImplementedError('Neural Network not implemented yet')
        #ranker = NeuralNetwork()
        #ranker.load_model()
    else:
        raise ValueError('Invalid ranker_str: {}'.format(ranker_str))
    
    X = Features(query='europe', url=df['url'], title=df['title'], body=df['body']).get_features()
    indices = ranker.get_scores(query=query, docs=df['body'], X=X).argsort()[::-1][:10]
    results = df[['url', 'title']].iloc[indices].values

    # return results, scores as a numpy array

    return np.hstack([results, indices.reshape(-1, 1)])


if __name__ == '__main__':
    print('imports successful')
    r = Reader()
    titles = r.get_titles()
    bodies = r.get_bodies()
    urls = r.get_urls()

    df = pd.DataFrame({'title': titles, 'body': bodies, 'url': urls})
    print(searcher('europe', df, ranker_str='pwsvm'))