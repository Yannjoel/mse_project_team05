from sklearn.feature_extraction.text import TfidfVectorizer
from DataHandling.reader import Reader

class TfIdf:

    def __init__(self):
        self.reader = Reader()
        self.corpus = self.reader.get_corpus()


    def get_feature_vecs(self):
        vectorizer = TfidfVectorizer()
        X = vectorizer.fit_transform(self.corpus)
        return X.toarray()