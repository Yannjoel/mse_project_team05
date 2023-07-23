from flask import Flask, jsonify, render_template, request
import pandas as pd
import numpy as np
import random
import json
from DataHandling.db_reader import Reader
from searching import searcher

app = Flask(__name__)


def searcher_api(query, ranker, test=True):
    if test:
        titles = np.load('testtitles.npy', allow_pickle=True)
        urls = np.load('testurls.npy', allow_pickle=True)
        scores = np.load('testscores.npy', allow_pickle=True)
        indices = np.load('testindices.npy', allow_pickle=True)
    else:
        r = Reader()
        print("Load dataframe")
        df = pd.DataFrame(
            {"url": r.get_urls(), "body": r.get_bodies(), "title": r.get_titles()}
        )
        print("Do ranking")
        titles, urls, scores, indices = searcher(query, df, ranker)
    np.save('titlesresult.npy', titles)
    np.save('urlsresult.npy', urls)
    np.save('scoresresult.npy', scores)
    np.save('indicesresult.npy', indices)
    return titles, urls, scores, indices


def get_ranked_results(query, ranker, cut):
    #titles, urls, scores, indices = searcher_api(query, ranker)
    titles = np.load('titlesresult.npy', allow_pickle=True)
    urls = np.load('urlsresult.npy', allow_pickle=True)
    scores = np.load('scoresresult.npy', allow_pickle=True)
    indices = np.load('indicesresult.npy', allow_pickle=True)
    data = []
    for i,title,url,index in zip(indices, titles, urls, indices):
        if cut==True and len(url)>60:
            url_display = url[:60] + "..."
        else:
            url_display = url
        data.append({
            'url_display' : url_display,
            'url' : url,
            'title' : title,
            'id' : '{0}'.format(index),
            'score' : round(scores[i],3)
        })
    return data

#def zoom_scatter()

def create_scatter(query, ranker, varsize=False, list=False):
    titles = np.load('titlesresult.npy', allow_pickle=True)
    urls = np.load('urlsresult.npy', allow_pickle=True)
    scores = np.load('scoresresult.npy', allow_pickle=True)
    indices = np.load('indicesresult.npy', allow_pickle=True)
    tsne_coords = np.load('tsne_result.npy')
    urls = np.load('urls.npy', allow_pickle=True)
    titles = np.load('titles.npy', allow_pickle=True)
    max_score = np.max(scores)
    points = []
    scores = scores / max_score
    for i,(point, url, score) in enumerate(zip(tsne_coords, urls, scores)):
        if i in indices:
            size = 20
            if list:
                stroke = 'black'
                color = 'black'
            else:
                stroke = 'red'
                color = 'red'
        else:
            size = 8
            stroke = 'grey'
            color = 'grey'
        if varsize:
            size = score * 20
            if size<1:
                size=1
        t = {
            'x' : str(point[0]),
            'y' : str(point[1]),
            'url' : url,
            'size' : size,
            'stroke' : stroke,
            'color' : color,
            'score' : score,
            'id' : '{0}'.format(i)
        }
        points.append(t)
    with open('static/data/data.json', 'w') as f:
        json.dump(points, f)
        

@app.route('/')
def create():
    return render_template('start.html')

@app.route('/tsne')
def tsne():
    data = {
        'width' : 6000,
        'height' : 2900,
        'color' : False
    }
    return render_template('tsne.html', data=data)

@app.route('/', methods=['POST'])
def my_form_post():
    query = request.form['i1']
    ranker = request.form['i2']
    searcher_api(query, ranker)
    data = get_ranked_results(None, None, cut=True)
    create_scatter(query, ranker, varsize=False, list=True)
    transferred_data = {
        'data' : data,
        'search' : query,
        'width' : 6000,
        'height' : 4000,
        'color' : False
    }
    return render_template('list.html', data=transferred_data)

@app.route('/tsne', methods=['POST'])
def my_form_post_tsne():
    size = False
    if "size" in request.form:
        size = True
    
    color = False
    if "colormap" in request.form:
        color = True
    
    liste = False
    if "list" in request.form:
        liste = True
    
    ranker = 'BM25'
    if 'ranker' in request.form:
        ranker = request.form['ranker']
    
    zoom = 'Zoom-factor 3'
    if request.form['zoom'] != "":
        zoom = request.form['zoom']

    possibilities = ['Zoom-factor 1', 'Zoom-factor 2', 'Zoom-factor 3', 'Zoom-factor 4', 'Zoom-factor 5',]
    resolution = [[3000, 1450], [4500, 2150], [6000, 2900], [7500, 3600], [14000, 6700]]
    
    query = request.form['search']
    print("\n\n\n", len(query), "\n\n\n")

    if len(query) > 0:
        searcher_api(query, ranker)
    create_scatter(query, ranker, varsize=size, list=liste)
    
    res = resolution[possibilities.index(zoom)]
    data = {
        'width' : res[0],
        'height' : res[1],
        'color' : color
    }
    
    if liste == True:
        data = get_ranked_results(None, None, True)
        transferred_data = {
            'data' : data,
            'search' : "",
            'width' : 6000,
            'height' : 4000,
            'color' : False
        }
        return render_template('tsne_list.html', data=transferred_data)
    else:
        return render_template('tsne.html', data=data)

#def create_scatter_initial():


if __name__ == '__main__':
    app.run(debug=True)
    create_scatter_initial()