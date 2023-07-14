import pandas as pd
from sklearn.svm import SVC
from sklearn.metrics import accuracy_score
from itertools import product
import numpy as np
from joblib import dump, load

class Ranker:
    def __init__(self, data, model_path=None):
        self.model = None

    def predict(self, x):
        y = self.model.predict(x)
        return y

    def train(self, data):
        svm = SVC(kernel='linear')
        svm.fit(data.paired_features, data.paired_labels)
        self.model = svm
        return svm
    
    def score(self, pairwise_docs, pairwise_preds):
        """returns scores for each doc based on pairwise predictions"""
        scores = pd.concat([pd.DataFrame(pairwise_docs, columns=['doc1', 'doc2']), pd.DataFrame(pairwise_preds, columns=['score'])], axis=1)
        summed_scores = scores.groupby('doc1').sum().score
        return summed_scores
    
    def rank(self, scores):
        ranked_docs = scores.sort_values(ascending=False)
        return ranked_docs
    
    def ndcg(self, relevances, ranked_docs, k=10):  # TODO: compare to sklearn.metrics.ndcg_score
        relevances_of_topk_ranked = relevances[ranked_docs.index][:k]
        DCG = sum(2**relevances_of_topk_ranked / np.log2(np.array(range(2, k+2))))
        top_k_relevances = np.array(sorted(relevances, reverse=True)[:k])
        IDCG = sum(2**top_k_relevances / np.log2(np.array(range(2, k+2))))
        print('DCG: ', DCG)
        print('IDCG: ', IDCG)
        return DCG / IDCG
    

    def save(self, model_path='svm.joblib'):
        dump(self.model, model_path) 
        print('model saved in ', model_path)

    def load(self, model_path='svm.joblib'):
        self.model = load(model_path)
        return self.model

    def evaluate(self, y_true, y_pred):
        accuracy = accuracy_score(y_true, y_pred)
        return accuracy

        


class Data:
    def __init__(self, data_path, n_features=40, n_rows=100):
        self.n_features = n_features
        self.n_rows = n_rows
        self.df, self.relevance, self.features,  = self.load(data_path)
        self.paired_docs, self.paired_labels, self.paired_features = self.pair()

    def load(self, data_path, n_features=40):
        
        data = pd.read_csv(data_path, delimiter=" ", nrows=self.n_rows).iloc[:, :self.n_features+2]      
        feature_names = [f'feature_{i}' for i in range(1, self.n_features+1)]
        data.columns = ['relevance', 'query_id']+feature_names

        # remove colon and feature id from feature values
        data.iloc[:, 1:] = data.iloc[:, 1:].applymap(lambda x: x.split(":", 1)[-1])  # TODO make more efficient
        data = data.astype(float)

        return data, np.array(data.relevance), np.array(data[feature_names])
    
    def pair(self):
        larger = (self.relevance[:, None] > self.relevance).astype(int)
        smaller = (self.relevance[:, None] < self.relevance).astype(int) * -1
        paired_labels = (larger + smaller).flatten()

        feature_difference = (self.features[:, None] - self.features)
        paired_features = feature_difference.reshape(-1, self.features.shape[1])
        paired_features = paired_features[paired_labels != 0]

        paired_docs = list(product(range(self.n_rows), range(self.n_rows)))
        paired_docs = np.array(paired_docs)[paired_labels != 0]

        paired_labels = paired_labels[paired_labels != 0]

        return paired_docs, paired_labels, paired_features
    

if __name__ == '__main__':
    train_data = Data('data/MSLR-WEB10K/Fold1/train.txt', n_rows=100)
    print('loaded data')
    Ranker = Ranker(train_data)
    Ranker.train(train_data)
    print('trained model')
    preds = Ranker.predict(train_data.paired_features)
    accuracy = Ranker.evaluate(train_data.paired_labels, preds)
    print('accuracy: ', accuracy)

    model_scores = Ranker.score(pairwise_docs=train_data.paired_docs, pairwise_preds=preds)
    ranking_model = Ranker.rank(model_scores)

    # true scores could be based on relevance or based on the paired comparison labels
    true_scores = pd.Series(train_data.relevance)
    ranking_true = Ranker.rank(Ranker.score(train_data.paired_docs, train_data.paired_labels)) 
    ranking_true = Ranker.rank(true_scores)


    # print top 10 ranked docs and their scores
    print('top 10 ranked docs and their scores')
    print('ranking_model: ', ranking_model[:10])
    print('ranking_true: ', ranking_true[:10])

    # TODO: Problem/error with ndcg score
    ndcg = Ranker.ndcg(true_scores, ranking_model, k=10)
    from sklearn.metrics import ndcg_score
    ndcg_sklearn = ndcg_score([true_scores], [ranking_model], k=10)
    print('ndcg sklearn: ', ndcg_sklearn)
    print('ndcg: ', ndcg)


    ## test on test data
    test_data = Data('data/MSLR-WEB10K/Fold1/test.txt', n_rows=50)
    print('loaded test data')

    preds = Ranker.predict(test_data.paired_features)
    test_accuracy = Ranker.evaluate(test_data.paired_labels, preds)
    print('test accuracy: ', test_accuracy)

    model_scores = Ranker.score(pairwise_docs=test_data.paired_docs, pairwise_preds=preds)
    ranking_model = Ranker.rank(model_scores)

    # true scores could be based on relevance or based on the paired comparison labels
    true_scores = pd.Series(test_data.relevance)
    ranking_true = Ranker.rank(Ranker.score(test_data.paired_docs, train_data.paired_labels)) 
    ranking_true = Ranker.rank(true_scores)


    # print top 10 ranked docs and their scores
    print('top 10 ranked docs and their scores')
    print('ranking_model: ', ranking_model[:10])
    print('ranking_true: ', ranking_true[:10])

    # TODO: Problem/error with ndcg score
    ndcg = Ranker.ndcg(true_scores, ranking_model, k=10)
    from sklearn.metrics import ndcg_score
    ndcg_sklearn = ndcg_score([true_scores], [ranking_model], k=10)
    print('test ndcg sklearn: ', ndcg_sklearn)
    print('test ndcg: ', ndcg)