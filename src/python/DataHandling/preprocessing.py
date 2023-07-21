"""Run this file one time to preprocess the data. It will create new columns in 
the database and store the preprocessed data there under COLUMN_NAME_processed."""
from db_reader import Reader
from language_processing import preprocess

import numpy as np

test = np.array(range(10))

# load data
reader = Reader()
# titles = reader.get_titles()
# bodies = reader.get_bodies()


# # preprocess data
# titles_processed = [preprocess(title) for title in titles]
# bodies_processed = [preprocess(body) for body in bodies]


# # write data to database
# reader.write_column('title_processed', titles_processed)
# reader.write_column('body_processed', bodies_processed)
reader.write_column('test', test)

print(reader.read_column('test'))

reader.close()

