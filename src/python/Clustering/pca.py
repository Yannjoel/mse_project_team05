from sklearn.decomposition import PCA
from Ranker.tfidf import TfIdf

class PCA:

    def __init__(self):
        self.tfidf = TfIdf()

    def get_pca(self, n_components):
        pca = PCA(n_components=n_components)
        X = pca.fit_transform(self.tfidf.get_feature_vecs())
        return X

    def get_pca_components(self, n_components):
        pca = PCA(n_components=n_components)
        pca.fit(self.tfidf.get_feature_vecs())
        return pca.components_