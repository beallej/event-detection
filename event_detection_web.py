from flask import Flask, render_template, request
app = Flask(__name__)

@app.route("/", methods=["POST", "GET"])
def queries():
    if request.method == "POST":
        subject = request.form["subject"]
        verb = request.form["verb"]
        direct_object = request.form["direct-object"]
        indirect_object = request.form["indirect-object"]
        location = request.form["location"]
    else:
        # we'll want to pull these from the database when we can!
        queries = [
                    {"subject": "sub",
                     "verb": "verb",
                     "direct object": "dir obj",
                     "indirect object": "ind obj",
                     "location": "loc",
                     "matched": False },
                    {"subject": "meat",
                     "verb": "causes",
                     "direct object": "cancer",
                     "indirect object": None,
                     "location": None,
                     "matched": True }
                  ]
    return render_template("queries.html", queries = queries)

@app.route("/query/<id>", methods=["GET"])
def show_query(id):
    # find query by id
    # if we don't find a query with that id, 404
    query = {"subject": "sub",
             "verb": "verb",
             "direct object": "dir obj",
             "indirect object": "ind obj",
             "location": "loc",
             "matched": False,
             "matching_articles": [] }
    return render_template("query.html", query = query)

@app.errorhandler(404)
def page_not_found(e):
    return render_template('404.html'), 404

if __name__ == "__main__":
    app.run(debug = True)
