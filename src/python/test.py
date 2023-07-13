from DataHandling.reader import Reader

from Ranker.tfidf import TfIdf
from sklearn.decomposition import PCA
from sklearn.manifold import TSNE

# crawler = Crawler(['https://uni-tuebingen.de/en/'])
# crawler.run()
reader = Reader()

print(len(reader.get_corpus()))

# apply PCA
# pca = PCA(n_components=50)
# X = pca.fit_transform(tfidf.get_feature_vecs())

# print("PCA finished")

# # apply TSNE
# tsne = TSNE(n_components=2)
# X_tsne = tsne.fit_transform(X)

# print("TSNE finished")

# # plot
# import matplotlib.pyplot as plt
# import numpy as np

# plt.scatter(X_tsne[:, 0], X_tsne[:, 1], s=5)
# # eliminate axis
# plt.xticks([])
# plt.yticks([])
# plt.show()

