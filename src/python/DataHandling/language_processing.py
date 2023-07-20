from nltk.corpus import stopwords
from nltk.corpus import wordnet


STOPWORDS = set(stopwords.words("english") + ["tübingen"])


def preprocess(query, rm_stopwords=True, add_synonyms=True):
    pass


def remove_stopwords(doc):
    """returns processed query"""
    # remove stopwords from query
    doc = " ".join(
        [word.lower() for word in doc.split() if word.lower() not in STOPWORDS]
    )
    return doc


def get_synonyms(word):
    """returns list of synonyms for word"""
    synonyms = []
    for syn in wordnet.synsets(word):
        for l in syn.lemmas():
            synonyms.append(l.name())
    return synonyms
