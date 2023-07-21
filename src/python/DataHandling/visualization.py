import numpy as np
from sklearn.manifold import TSNE
from sklearn.decomposition import PCA

from sklearn.feature_extraction.text import TfidfVectorizer
import matplotlib.pyplot as plt

from db_reader import Reader
from language_processing import remove_stopwords

### Not sure yet. Probably better if we just store the coordinates since they are fixed after crawling?

r = Reader()
bodies = r.get_bodies()
titles = r.get_titles()
urls = r.get_urls()

vectorizer = TfidfVectorizer()
# remove stopwords
bodies = [remove_stopwords(body) for body in bodies]

features = vectorizer.fit_transform(bodies)

# PCA
pca = PCA(n_components=50)
pca_result = pca.fit_transform(features.toarray())
print('PCA finished')

# TSNE
tsne = TSNE(n_components=2)
tsne_result = tsne.fit_transform(pca_result)
print('TSNE finished')

# Plot
plt.scatter(tsne_result[:,0], tsne_result[:,1])
plt.show()

# Save 
np.save('tsne_result.npy', tsne_result)
np.save('urls.npy', urls)
np.save('titles.npy', titles)