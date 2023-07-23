from flask import Flask, jsonify, render_template, request
import pandas as pd
import numpy as np
import random
import json
from matplotlib import pyplot as plt

app = Flask(__name__)



@app.route('/')
def create():
    return "Hello"


if __name__ == '__main__':
    app.run(debug=True)
