from ranker import Ranker
from RankingAlgorithms import *
from DataHandling.db_reader import Reader


def searcher(query, df, ranker_str="BM25"):
    # TODO: Implement variable length of results
    ranker = get_ranker(ranker_str)()
    scores = ranker.get_scores(query, df["body"])
    return scores


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
    print(get_all_rankers())
