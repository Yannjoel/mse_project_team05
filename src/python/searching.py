import os
import time
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from ranker import Ranker
from RankingAlgorithms import *
from DataHandling.db_reader import Reader
from DataHandling.language_processing import remove_stopwords


def searcher(query, df, ranker_str="BM25"):
    """
    returns top-10 results for a given query + scores of all documents
    """
    ranker = get_ranker(ranker_str)()
    query = remove_stopwords(query)
    scores = ranker.get_scores(query, df)
    top_10 = diversify_results(scores, df, threshold=0.99)

    # calculate top-10 results and return titles and urls
    # top_10 = scores.argsort()[-10:][::-1]
    titles = df["title"].iloc[top_10].values
    urls = df["url"].iloc[top_10].values
    top_10_scores = scores[top_10].round(3)

    # store top-10 results in txt.file
    path = uniquify("results/search_results.txt")
    with open(path, "w") as f:
        for i, (url, score) in enumerate(zip(urls, top_10_scores), start=1):
            f.write(f"{i}.\t{url}\t{score}\n")

    return titles, urls, scores


def diversify_results(scores, df, threshold=0.9, max_iter=20):
    """
    returns a list of indices of the top-10 results
    """
    # TODO threshold analysis for two queries and improve algorithm, find cornercases
    embedding = np.load("tsne_result.npy")
    sorted_scores = list(scores.argsort()[-50:][::-1])
    top_ten = [sorted_scores.pop(0)]
    i = 0
    while len(top_ten) < 10 and i<max_iter:
        candidate = sorted_scores.pop(0)
        if (
            abs(
                cosine_similarity(
                    embedding[candidate, :].reshape(1, -1), embedding[top_ten, :]
                )
            )
         < threshold).any():
            top_ten.append(candidate)
        i += 1
    return top_ten


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
    returns a list of the names of all subclasses
    """
    rankers = get_all_rankers()
    names = [cls for cls in rankers.keys()]
    return names


def get_ranker(name: str):
    """
    returns the subclass given the name
    """
    rankers = get_all_rankers()
    return rankers[name]


if __name__ == "__main__":
    # test get_all_rankers
    import pandas as pd

    r = Reader()

    df = pd.DataFrame(
        {"url": r.get_urls(), "body": r.get_bodies(), "title": r.get_titles()}
    )
    print("start querying on df of length", len(df))
    start = time.time()
    print(searcher(query="tübingen attractions", df=df, ranker_str="NeuralNetwork"))
    end = time.time()
    print(f"Time elapsed: {end-start}")
