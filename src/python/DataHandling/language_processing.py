from nltk.corpus import stopwords
from nltk.corpus import wordnet
from nltk.stem import WordNetLemmatizer
from nltk.corpus import words
import string


STOPWORDS = set(stopwords.words("english"))


def preprocess(doc):
    """lemmatize and remove stopwords of a list of documents"""
    doc = doc.translate(str.maketrans("", "", string.punctuation)).lower()
    lemmatizer = WordNetLemmatizer()
    doc = " ".join(
        [lemmatizer.lemmatize(word) for word in doc.split() if word not in STOPWORDS]
    )
    return doc


def remove_stopwords(doc):
    """removes stopwords and punctuation from given doc"""
    doc = doc.translate(str.maketrans("", "", string.punctuation)).lower()
    doc = " ".join(
        [word.lower() for word in doc.split() if word.lower() not in STOPWORDS]
    )
    
    return doc
