import mysql.connector as database
import numpy as np


class Reader:
    def __init__(self):
        self.connection = database.connect(
            user="python", password="python", host="localhost", database="mse_project"
        )
        self.cursor = self.connection.cursor()

    def get_urls(self):
        sql_statement = "SELECT url FROM website WHERE relevantForSearch = 1"
        self.cursor.execute(sql_statement)
        return np.array(self.cursor.fetchall(), dtype=object).flatten()

    def get_titles(self):
        sql_statement = "SELECT title FROM website WHERE relevantForSearch = 1"
        self.cursor.execute(sql_statement)
        return np.array(self.cursor.fetchall(), dtype=object).flatten()

    def get_bodies(self):
        sql_statement = "SELECT body FROM website WHERE relevantForSearch = 1"
        self.cursor.execute(sql_statement)
        return np.array(self.cursor.fetchall(), dtype=object).flatten()

    def close(self):
        self.cursor.close()
        self.connection.close()
