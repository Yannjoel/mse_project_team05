import numpy as np
import pandas as pd
from sklearn.preprocessing import StandardScaler


def load_data(
    path="../../../data/MSLR-WEB10K/Fold1/train.txt",
    feature_indices=[108, 18, 103, 107, 17, 102, 105, 15, 100],
    nrows=500,
):
    """loads data from file"""
    data = pd.read_csv(path, delimiter=" ", nrows=nrows, header=None)
    data.drop(columns=[1, 138], inplace=True)
    data.iloc[:, 1:] = data.iloc[:, 1:].applymap(lambda x: x.split(":", 1)[-1])
    y = data[0].astype(float).to_numpy()
    X = data.drop(columns=0).values.astype(float)

    # extract features from data
    X = X[:, feature_indices]
    X = StandardScaler().fit_transform(X)

    return X, y
