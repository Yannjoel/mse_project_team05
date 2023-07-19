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


class RankSVM(svm.LinearSVC, Ranker):

    def __init__(
        self,
        penalty="l2",
        loss="squared_hinge",
        *,
        dual="warn",
        tol=1e-4,
        C=1.0,
        multi_class="ovr",
        fit_intercept=True,
        intercept_scaling=1,
        class_weight=None,
        verbose=0,
        random_state=None,
        max_iter=1000,
        load = True
    ):

        super(RankSVM, self).__init__(penalty=penalty, loss=loss, dual=dual, tol=tol, C=C, multi_class=multi_class, fit_intercept=fit_intercept, intercept_scaling=intercept_scaling, class_weight=class_weight, verbose=verbose, random_state=random_state, max_iter=max_iter)
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
        super(RankSVM, self).fit(X_trans, y_trans)
        return self
    
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
        if hasattr(self, 'coef_'):
            return np.dot(X, self.coef_.T).ravel()
        else:
            raise ValueError("Must call fit() prior to predict()")

    def get_accuracy(self, X, y):
        """
        Because we transformed into a pairwise problem, chance level is at 0.5
        """
        X_trans, y_trans = transform_pairwise(X, y)
        return np.mean(super(RankSVM, self).predict(X_trans) == y_trans)

    def save_coef(self, path="../models/ranksvm_coef.pkl"):
        pickle.dump(self.coef_, open(path, "wb"))

    def load_coef(self, path="src/python/models/ranksvm_coef.pkl"):
        self.coef_ = pickle.load(open(path, "rb"))
