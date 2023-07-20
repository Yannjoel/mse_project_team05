import numpy as np
import pandas as pd
from sklearn import svm
import pickle

from ranker import Ranker
from RankingAlgorithms.feature_extractor import Features as FeatureExtractor
from DataHandling.train_data import transform_pairwise


class RankSVM(Ranker):
    def __init__(self, load=True):
        self.model = svm.LinearSVC()
        if load:
            self.load_coef()

    def fit(self, X, y):
        """
        Fit a pairwise ranking model.
        """
        X_trans, y_trans = transform_pairwise(X, y)
        return self.model.fit(X_trans, y_trans)

    def get_scores(self, query, df):
        """Predict scores using the pairwise ranking model."""
        X = FeatureExtractor(
            query=query, url=df["url"], title=df["title"], body=df["body"]
        ).get_features()
        return self.predict(X)

    def predict(self, X):
        """
        Helper function to predict scores using the pairwise ranking model.
        """
        if hasattr(self.model, "coef_"):
            return np.dot(X, self.model.coef_.T).ravel()
        else:
            raise ValueError("Must call fit() prior to predict()")

    def save(self, path="../models/ranksvm.pkl"):
        pickle.dump(self.model, open(path, "wb"))

    def load_coef(self, path="src/python/models/ranksvm.pkl"):
        self.model = pickle.load(open(path, "rb"))
