import numpy as np
import pickle
import pandas as pd
import torch

from Ranker.bmtf import BM25
from Ranker.tfidf import TfIdf
from Ranker.neuralnetwork import NeuralNetwork
from Ranker.feature_extractor import Features
from DataHandling.db_reader import Reader


def searcher(query, df, ranker_str="bm25"):
    # TODO: Implement variable length of results
    scores = None
    if ranker_str == "bm25":
        bm25 = BM25()
        scores = bm25.get_scores(query, df)
    elif ranker_str == "tfidf":
        tfidf = TfIdf()
        scores = tfidf.get_scores(query, df)
    elif ranker_str == "nn":
        nn = NeuralNetwork.load("src/python/models/nn.pth")
        X = torch.tensor(
            Features(
                query, url=df["url"], title=df["title"], body=df["body"]
            ).get_features(),
            dtype=torch.float32,
        )
        scores = nn.get_scores(X).detach().numpy().flatten()
    elif ranker_str == "pwsvm":
        pwsvm = pickle.load(open("src/python/models/svm.pkl", "rb"))
        X = Features(
            query, url=df["url"], title=df["title"], body=df["body"]
        ).get_features()
        scores = pwsvm.get_scores(X=X)
    else:
        raise ValueError("Invalid Ranker chosen")

    return df[["title", "url"]].iloc[np.argsort(scores)[::-1]][:10], scores


if __name__ == "__main__":
    r = Reader()
    titles = r.get_titles()
    bodies = r.get_bodies()
    urls = r.get_urls()

    df = pd.DataFrame({"title": titles, "body": bodies, "url": urls})
    print(searcher("Computer Science", df, ranker_str="nn"))
