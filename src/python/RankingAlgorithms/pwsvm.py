import numpy as np
import pandas as pd
from sklearn import svm, linear_model
import itertools
import pickle

from ranker import Ranker
from RankingAlgorithms.feature_extractor import Features as FeatureExtractor


def transform_pairwise(X, y):
    larger = (y[:, None] > y).astype(int)
    smaller = (y[:, None] < y).astype(int) * -1
    paired_labels = (larger + smaller).flatten()

    feature_difference = X[:, None] - X
    paired_features = feature_difference.reshape(-1, X.shape[1])
    paired_features = paired_features[paired_labels != 0]

    paired_labels = paired_labels[paired_labels != 0]
    print("n_samples after pairwise transform ", len(paired_labels))

    return paired_features, paired_labels


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
        X = FeatureExtractor(
            query=query, url=df["url"], title=df["title"], body=df["body"]
        ).get_features()
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
        if hasattr(self.model, "coef_"):
            return np.dot(X, self.model.coef_.T).ravel()
        else:
            raise ValueError("Must call fit() prior to predict()")

    def save(self, path="../models/ranksvm.pkl"):
        pickle.dump(self.model, open(path, "wb"))

    def load_coef(self, path="src/python/models/ranksvm.pkl"):
        self.model = pickle.load(open(path, "rb"))
