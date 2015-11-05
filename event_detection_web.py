from flask import Flask, render_template, request
import psycopg2
from psycopg2.extras import RealDictCursor
import sys
import pdb

app = Flask(__name__)

def connect_database():
    # Establish connction with database
    try:
        conn = psycopg2.connect(user="root", database="event_detection")
        cursor = conn.cursor(cursor_factory=RealDictCursor)

    except:
        print("ERROR: Cannot establish connection with SQL database")
        sys.exit()
    return conn, cursor


@app.route("/", methods=["POST", "GET"])
def queries():
    con, cursor = connect_database()
    if request.method == "POST":
        subject = request.form["subject"]
        verb = request.form["verb"]
        direct_obj = request.form["direct-object"]
        indirect_obj = request.form["indirect-object"]
        loc = request.form["location"]
        # Put into database
        cursor.execute("INSERT INTO queries (subject, verb, direct_obj, indirect_obj, loc) \
                            VALUES (%s, %s, %s, %s, %s);", (subject, verb, direct_obj, indirect_obj, loc))
        con.commit()

    # Get lists of query from database
    cursor.execute("SELECT id, subject, verb, direct_obj, indirect_obj, loc FROM queries;")
    queries = cursor.fetchall()
    # pdb.set_trace()

    if con:
        con.close()
    return render_template("queries.html", queries = queries)

@app.route("/query/<id>", methods=["GET"])
def query(id):
    # find query by id
    # if we don't find a query with that id, 404
    con, cursor = connect_database()
    cursor.execute("SELECT q.subject, q.verb, q.direct_obj, q.indirect_obj, q.loc, \
                        a.title, a.source, a.url \
                        FROM queries q \
                        INNER JOIN query_articles qa on q.id = qa.query\
                        INNER JOIN articles a on qa.article = a.id\
                        WHERE q.id = %s;", id)
    articles = cursor.fetchall()

    cursor.execute("SELECT id, subject, verb, direct_obj, indirect_obj, loc FROM queries where id = %s;", id)
    query = cursor.fetchone()

    if con:
        con.close()
    return render_template("query.html", query = query)

@app.errorhandler(404)
def page_not_found(e):
    return render_template("404.html"), 404

if __name__ == "__main__":
    app.run(debug = True)
