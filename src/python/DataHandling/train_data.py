import numpy as np
import pandas as pd


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
    return X, y


def transform_pairwise(X, y):
    """
    Transform data into pairwise optimization format
    """
    larger = (y[:, None] > y).astype(int)
    smaller = (y[:, None] < y).astype(int) * -1
    paired_labels = (larger + smaller).flatten()

    feature_difference = X[:, None] - X
    paired_features = feature_difference.reshape(-1, X.shape[1])
    paired_features = paired_features[paired_labels != 0]

    paired_labels = paired_labels[paired_labels != 0]
    print("n_samples after pairwise transform ", len(paired_labels))

    return paired_features, paired_labels
