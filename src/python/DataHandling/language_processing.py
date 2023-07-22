from nltk.corpus import stopwords
from nltk.corpus import wordnet
from nltk.stem import WordNetLemmatizer
from nltk.corpus import words
import string


STOPWORDS = set(stopwords.words("english") + ["tübingen"])


def preprocess(doc):
    """lemmatize and remove stopwords of a list of documents"""
    # remove punctuation
    doc = doc.translate(str.maketrans("", "", string.punctuation)).lower()
    lemmatizer = WordNetLemmatizer()
    doc = " ".join(
        [
            lemmatizer.lemmatize(word)
            for word in doc.split()
            if word not in STOPWORDS #and word in words.words()
        ]
    )
    return doc


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
