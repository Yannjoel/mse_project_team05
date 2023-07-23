import os
import time
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from ranker import Ranker
from RankingAlgorithms import *
from DataHandling.db_reader import Reader
from DataHandling.language_processing import remove_stopwords


def searcher(query, df, ranker_str="NeuralNetwork", topk=10):
    """
    returns top-k results for a given query + scores of all documents
    """
    ranker = get_ranker(ranker_str)()
    query = remove_stopwords(query)
    scores = ranker.get_scores(query=query, df=df)
    sorted_indices = scores.argsort()[::-1]

    # calculate top-k results and return titles and urls
    top_k = diversify(df["title"].values, sorted_indices, topk)
    titles = df["title"].iloc[top_k].values
    urls = df["url"].iloc[top_k].values
    top_k_scores = scores[top_k].round(3)

    # store top-k results in txt.file
    path = uniquify("results/search_results.txt")
    with open(path, "w") as f:
        for i, (url, score) in enumerate(zip(urls, top_k_scores), start=1):
            f.write(f"{i}.\t{url}\t{score}\n")

    return titles, urls, scores, top_k


def diversify(titles, sorted_indices, topk=10):
    """
    returns a list of indices of the top-10 results by checking whether there are some duplicates in the titles
    """
    titles = titles[sorted_indices]
    top_k = []
    top_k_titles = []
    i = 0
    while len(top_k) < topk and i < len(titles):
        if titles[i] not in top_k_titles:
            top_k.append(sorted_indices[i])
            top_k_titles.append(titles[i])
        i += 1
    return top_k


def uniquify(path):
    """
    source: https://stackoverflow.com/questions/13852700/create-file-but-if-name-exists-add-number
    """
    filename, extension = os.path.splitext(path)
    counter = 1

    while os.path.exists(path):
        path = filename + " (" + str(counter) + ")" + extension
        counter += 1

    return path


def get_all_rankers():
    """
    returns all subclasses of Ranker
    """
    all_my_base_classes = {cls.__name__: cls for cls in Ranker.__subclasses__()}
    return all_my_base_classes


def get_ranker_names():
    """
    returns a list of the names of all subclasses of Ranker
    """
    rankers = get_all_rankers()
    names = [cls for cls in rankers.keys()]
    return names


def get_ranker(name: str):
    """
    returns the subclass given the ranker name
    """
    rankers = get_all_rankers()
    return rankers[name]


if __name__ == "__main__":
    # test get_all_rankers
    import pandas as pd

    df = pd.DataFrame(
        {
            "url": np.load("src/python/data/urls.npy", allow_pickle=True),
            "body": np.load("src/python/data/bodies.npy", allow_pickle=True),
            "title": np.load("src/python/data/titles.npy", allow_pickle=True),
        }
    )
    print("start querying on df of length", len(df))
    start = time.time()
    print(searcher(query="food and drinks and bananas", df=df, ranker_str="NeuralNetwork", topk=10))
    end = time.time()
    print(f"Time elapsed: {end-start}")
