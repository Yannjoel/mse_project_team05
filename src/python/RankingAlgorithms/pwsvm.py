import numpy as np
import pandas as pd
from sklearn import svm, linear_model
import itertools
import pickle

from ranker import Ranker
from RankingAlgorithms.feature_extractor import Features as FeatureExtractor


def transform_pairwise(X, y):
    X_new = []
    y_new = []
    y = np.asarray(y)
    if y.ndim == 1:
        y = np.c_[y, np.ones(y.shape[0])]
    comb = itertools.combinations(range(X.shape[0]), 2)
    for k, (i, j) in enumerate(comb):
        if y[i, 0] == y[j, 0] or y[i, 1] != y[j, 1]:
            # skip if same target or different group
            continue
        X_new.append(X[i] - X[j])
        y_new.append(np.sign(y[i, 0] - y[j, 0]))
        # output balanced classes
        if y_new[-1] != (-1) ** k:
            y_new[-1] = - y_new[-1]
            X_new[-1] = - X_new[-1]
    return np.asarray(X_new), np.asarray(y_new).ravel()


class RankSVM(Ranker):

    def __init__(self, load=True):

        self.model = svm.LinearSVC()
        if load:
            self.load_coef()

    def fit(self, X, y):
        """
        Fit a pairwise ranking model.
        Parameters
        ----------
        X : array, shape (n_samples, n_features)
        y : array, shape (n_samples,) or (n_samples, 2)
        Returns
        -------
        self
        """
        X_trans, y_trans = transform_pairwise(X, y)
        return self.model.fit(X_trans, y_trans)
    
    def get_scores(self, query, df):
        X = FeatureExtractor(query=query, url=df["url"], title=df["title"], body=df["body"]).get_features()
        return self.predict(X)

    def predict(self, X):
        """
        Predict an ordering on X. For a list of n samples, this method
        returns a list from 0 to n-1 with the relative order of the rows of X.
        Parameters
        ----------
        X : array, shape (n_samples, n_features)
        Returns
        -------
        ord : array, shape (n_samples,)
            Returns a list of integers representing the relative order of
            the rows in X.
        """
        if hasattr(self.model, 'coef_'):
            return np.dot(X, self.model.coef_.T).ravel()
        else:
            raise ValueError("Must call fit() prior to predict()")

    def save_coef(self, path="../models/ranksvm_coef.pkl"):
        pickle.dump(self.model.coef_, open(path, "wb"))

    def load_coef(self, path="src/python/models/ranksvm_coef.pkl"):
        self.model.coef_ = pickle.load(open(path, "rb"))
