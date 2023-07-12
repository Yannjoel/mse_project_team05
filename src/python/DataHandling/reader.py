import sqlite3
import numpy as np


class Reader:
    
        def __init__(self):
            self.connection = sqlite3.connect("database.db")
            self.cursor = self.connection.cursor()
    
        def get_all_data(self):
            sql_statement = 'SELECT * FROM website'
            self.cursor.execute(sql_statement)
            return self.cursor.fetchall()
    
        def get_corpus(self):
            sql_statement = 'SELECT content FROM website WHERE content IS NOT NULL'
            self.cursor.execute(sql_statement)
            return np.array(self.cursor.fetchall()).flatten()
        
        def get_urls(self):
            sql_statement = 'SELECT url FROM website'
            self.cursor.execute(sql_statement)
            return self.cursor.fetchall()
        
        def get_titles(self):
            sql_statement = 'SELECT title FROM website'
            self.cursor.execute(sql_statement)
            return self.cursor.fetchall()
        
        