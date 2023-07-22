"""Run this file one time to preprocess the data. It will create new columns in 
the database and store the preprocessed data there under COLUMN_NAME_processed."""
from db_reader import Reader
from language_processing import preprocess

import numpy as np

# load data
print("Reading data...")
reader = Reader()
titles = reader.get_titles()
bodies = reader.get_bodies()
print("Reading data done")


# preprocess data
print("preprocessing...")
titles_processed = [preprocess(title) for title in titles]
bodies_processed = [preprocess(body) for body in bodies]
print("preprocessing done")

# store as npy-files
np.save("data/titles_processed.npy", titles_processed)
np.save("data/bodies_processed.npy", bodies_processed)
print("Storing data done")

reader.close()
