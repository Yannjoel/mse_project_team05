import numpy as np
import pickle
import pandas as pd

from Ranker.bmtf import BM25
from Ranker.tfidf import TfIdf
from Ranker.neuralnetwork import NeuralNetwork
from Ranker.feature_extractor import Features
from DataHandling.db_reader import Reader

def searcher(query, df, ranker_str='bm25'):

    # TODO: Implement variable length of results
    # Not sure if this works yet. 
    # It would be great if we could read the data from the database before we start this 
    # function because I think this would be faster.     
    if ranker_str == 'bm25':
        ranker = BM25()
    elif ranker_str == 'tfidf':
        ranker = TfIdf(corpus=df.values.ravel())
    elif ranker_str == 'pwsvm':
        ranker = pickle.load(open('src/python/models/svm.pkl', 'rb'))
    elif ranker_str == 'nn':
        ranker = NeuralNetwork.load('src/python/models/nn.pth')
    else:
        raise ValueError('Invalid ranker_str: {}'.format(ranker_str))
    
    X = Features(query=query, url=df['url'], title=df['title'], body=df['body']).get_features()
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
    print(searcher('Computer Science', df, ranker_str='nn'))