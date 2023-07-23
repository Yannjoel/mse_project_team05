from flask import Flask, jsonify, render_template, request
import pandas as pd
import numpy as np
import random
import json
from DataHandling.db_reader import Reader
from searching import searcher

app = Flask(__name__)


def searcher_api(query, ranker, test=False):
    if test:
        titles = np.load("testtitles.npy", allow_pickle=True)
        urls = np.load("testurls.npy", allow_pickle=True)
        scores = np.load("testscores.npy", allow_pickle=True)
        indices = np.load("testindices.npy", allow_pickle=True)
    else:
        #r = Reader()
        print("Load dataframe")
        df = pd.DataFrame(
            {
                "url": np.load("data/urls.npy", allow_pickle=True),
                "body": np.load("data/bodies.npy", allow_pickle=True),
                "title": np.load("data/titles.npy", allow_pickle=True),
            }
        )
        print("Do ranking")
        print(query, ranker)
        print(df)
        titles, urls, scores, indices = searcher(query, df, ranker)
        print(titles)

    print(titles)
    np.save("titlesresult.npy", titles)
    np.save("urlsresult.npy", urls)
    np.save("scoresresult.npy", scores)
    np.save("indicesresult.npy", indices)
    return titles, urls, scores, indices


def get_ranked_results(query, ranker, cut):
    # titles, urls, scores, indices = searcher_api(query, ranker)
    titles = np.load("titlesresult.npy", allow_pickle=True)
    urls = np.load("urlsresult.npy", allow_pickle=True)
    scores = np.load("scoresresult.npy", allow_pickle=True)
    indices = np.load("indicesresult.npy", allow_pickle=True)
    data = []
    for i, title, url, index in zip(indices, titles, urls, indices):
        if cut == True and len(url) > 60:
            url_display = url[:60] + "..."
        else:
            url_display = url
        data.append(
            {
                "url_display": url_display,
                "url": url,
                "title": title,
                "id": "{0}".format(index),
                "score": round(scores[i], 3),
            }
        )
    return data


# def zoom_scatter()


def create_scatter(query, ranker, varsize=False, list=False):
    titles = np.load("titlesresult.npy", allow_pickle=True)
    urls = np.load("urlsresult.npy", allow_pickle=True)
    scores = np.load("scoresresult.npy", allow_pickle=True)
    indices = np.load("indicesresult.npy", allow_pickle=True)
    tsne_coords = np.load("data/tsne_result.npy")
    urls = np.load("data/urls.npy", allow_pickle=True)
    titles = np.load("data/titles.npy", allow_pickle=True)
    max_score = np.max(scores)
    points = []
    if max_score > 0:
        scores = scores / max_score
    import time
    start = time.time()
    for i, (point, url, score) in enumerate(zip(tsne_coords, urls, scores)):
        if i in indices:
            size = 20
            if list:
                stroke = "black"
                color = "black"
            else:
                stroke = "red"
                color = "red"
        else:
            size = 8
            stroke = "grey"
            color = "grey"
        if varsize:
            size = score * 20
            if size < 3:
                size = 3
        t = {
            "x": str(point[0]),
            "y": str(point[1]),
            "url": url,
            "size": size,
            "stroke": stroke,
            "color": color,
            "score": score,
            "id": "{0}".format(i),
        }
        points.append(t)
    end = time.time()
    print("\n\n", end-start, "\n\n")
    with open("static/data/data.json", "w") as f:
        json.dump(points, f)


@app.route("/")
def create():
    return render_template("start.html")


@app.route("/tsne")
def tsne():
    data = {"width": 8000, "height": 3000, "color": False}
    return render_template("tsne.html", data=data)


@app.route("/", methods=["POST"])
def my_form_post():
    query = request.form["search"]
    ranker = request.form["ranker"]
    
    if ranker == "NN" or ranker=="":
        ranker="NeuralNetwork"
    elif ranker == "RkSVM":
        ranker="RankSVM"
    
    if len(query) > 0:
        print(query)
        searcher_api(query, ranker)
    
    size = False
    if "size" in request.form:
        size = True

    color = False
    if "colormap" in request.form:
        color = True

    liste = False
    if "list" in request.form:
        liste = True

    zoom = "Zoom-factor 3"
    if "zoom" in request.form:
        if request.form['zoom'] != "":
            zoom = request.form["zoom"]

    possibilities = [
        "Zoom-factor 1",
        "Zoom-factor 2",
        "Zoom-factor 3",
        "Zoom-factor 4",
        "Zoom-factor 5",
    ]
    
    resolution = [[4000, 2250], [6000, 3350], [8000, 4500], [12000, 6700], [15000, 8400]]

    create_scatter(query, ranker, varsize=size, list=False)

    res = resolution[possibilities.index(zoom)]

    data = get_ranked_results(None, None, True)
    transferred_data = {
            "data": data,
            "search": "",
            "width": res[0],
            "height": res[1],
            "color": color,
    }
    
    return render_template("tsne_list.html", data=transferred_data)
   
    


@app.route("/tsne", methods=["POST"])
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

    ranker = "BM25"
    if "ranker" in request.form:
        ranker = request.form["ranker"]

    zoom = "Zoom-factor 3"
    if request.form["zoom"] != "":
        zoom = request.form["zoom"]

    possibilities = [
        "Zoom-factor 1",
        "Zoom-factor 2",
        "Zoom-factor 3",
        "Zoom-factor 4",
        "Zoom-factor 5",
    ]
    resolution = [[3000, 1450], [4500, 2150], [8000, 3300], [7500, 3600], [14000, 6700]]

    query = request.form["search"]
    print("\n\n\n", len(query), "\n\n\n")

    if len(query) > 0:
        searcher_api(query, ranker)
    create_scatter(query, ranker, varsize=size, list=liste)

    res = resolution[possibilities.index(zoom)]
    data = {"width": res[0], "height": res[1], "color": color}

    if liste == True:
        data = get_ranked_results(None, None, True)
        transferred_data = {
            "data": data,
            "search": "",
            "width": 6000,
            "height": 4000,
            "color": False,
        }
        return render_template("tsne_list.html", data=transferred_data)
    else:
        return render_template("tsne.html", data=data)


# def create_scatter_initial():


if __name__ == "__main__":
    app.run(debug=True)
    create_scatter_initial()
