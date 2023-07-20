from ranker import Ranker
from RankingAlgorithms import *
from DataHandling.db_reader import Reader


def searcher(query, df, ranker_str="BM25"):
    # TODO: Implement variable length of results
    # TODO: TFIDF does not work yet (see RankingAlgorithms/tfidf.py)
    ranker = get_ranker(ranker_str)()
    query = ranker.process_query(query)
    scores = ranker.get_scores(query, df)

    # calculate top-10 results and return titles and urls
    top_10 = scores.argsort()[-10:][::-1]
    titles = df["title"].iloc[top_10].values
    urls = df["url"].iloc[top_10].values

    return titles, urls, scores


def get_all_rankers():
    """
    returns all subclasses of Match
     Subclasses in ML have to be imported
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

    print(get_all_rankers())
    r = Reader()
    df = pd.DataFrame(
        {"url": r.get_urls(), "body": r.get_bodies(), "title": r.get_titles()}
    )

    print(searcher(query="sports", df=df, ranker_str="RankSVM"))
