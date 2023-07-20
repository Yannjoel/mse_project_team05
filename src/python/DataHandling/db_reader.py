import mysql.connector as database
import numpy as np


class Reader:
    
        def __init__(self):
            self.connection = database.connect(
                 user='python', password='python', host="localhost", database="mse_project")
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
            sql_statement = 'SELECT url FROM website WHERE relevantForSearch = 1'
            self.cursor.execute(sql_statement)
            return np.array(self.cursor.fetchall()).flatten()
        
        def get_titles(self):
            sql_statement = 'SELECT title FROM website WHERE relevantForSearch = 1'
            self.cursor.execute(sql_statement)
            return np.array(self.cursor.fetchall()).flatten()
        
        def get_bodies(self):
            sql_statement = 'SELECT body FROM website WHERE relevantForSearch = 1'
            self.cursor.execute(sql_statement)
            return np.array(self.cursor.fetchall()).flatten()
        
        def get_documents(self):  # Frage: wollen wir das so? oder wie?
            sql_statement = 'SELECT title, body FROM website'
            self.cursor.execute(sql_statement)
            return np.array(self.cursor.fetchall()).flatten()
        

if __name__=='__main__':
    reader = Reader()
    #corpus = reader.get_corpus()
    urls = reader.get_urls()
    titles = reader.get_titles()
    bodies = reader.get_bodies()
    print(titles)