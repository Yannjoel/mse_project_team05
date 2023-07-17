from Ranker.bmtf import BM25
from Ranker.tfidf import TFIDF
from Ranker.pwsvm import PWSVM
from Ranker.neuralnetwork import NeuralNetwork

def searcher(query, df, ranker_str='bm25'):

    # TODO: Implement variable length of results
    # Not sure if this works yet. 
    # It would be great if we could read the data from the database before we start this 
    # function because I think this would be faster.     
    if ranker_str == 'bm25':
        ranker = BM25()
    elif ranker_str == 'tfidf':
        ranker = TFIDF()
    elif ranker_str == 'pwsvm':
        ranker = PWSVM()
    elif ranker_str == 'nn':
        ranker = NeuralNetwork()
    else:
        raise ValueError('Invalid ranker_str: {}'.format(ranker_str))
    
    scores = ranker.get_scores(query=query, docs=df['body'])
    results = df[['url', 'title']].iloc[scores.argsort()[::-1][:10]].values


    return results