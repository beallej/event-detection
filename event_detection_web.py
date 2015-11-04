from flask import Flask, render_template, request
import psycopg2
import sys

app = Flask(__name__)

# Establish connction with database
try:
    conn = psycopg2.connect(user="root", database="event_detection")
    cursor = conn.cursor()
except:
    print("ERROR: Cannot establish connection with SQL database")
    sys.exit()
    
@app.route("/", methods=["POST", "GET"])
def queries():
    if request.method == "POST":
        subject = request.form["subject"]
        verb = request.form["verb"]
        direct_object = request.form["direct-object"]
        indirect_object = request.form["indirect-object"]
        location = request.form["location"]
        # Put into database
        cursor.execute("INSERT INTO queries (subject, verb, direct_obj, indirect_obj, location) \
                            VALUES (%s, %s, %s, %s, %s);", (subject, verb, direct_obj, indirect_obj, location))
        
    else:
        # Get lists of query from database
        cursor.execute("SELECT subject, verb, direct_obj, indirect_obj, location FROM queries;")
        queries = cursor
        '''
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
         '''
    return render_template("queries.html", queries = queries)

@app.route("/query/<id>", methods=["GET"])
def show_query(id):
    # find query by id
    # if we don't find a query with that id, 404
    cursor.execute("SELECT q.subject, q.verb, q.direct_obj, q.indirect_obj, q.location, \
                        a.title, a.source, a.url \
                        FROM queries q \
                        INNER JOIN query_articles qa on q.query_id = qa.query_id\
                        INNER JOIN articles a on qa.article_id = a.id\
                        WHERE q.query_id = %s;", (id))
    query = cursor
    '''query = {"subject": "sub",
             "verb": "verb",
             "direct object": "dir obj",
             "indirect object": "ind obj",
             "location": "loc",
             "matched": False,
             "matching_articles": [] }
    '''
    return render_template("query.html", query = query)

@app.errorhandler(404)
def page_not_found(e):
    return render_template('404.html'), 404

if __name__ == "__main__":
    app.run(debug = True)
