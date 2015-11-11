from flask import Flask, render_template, request
import psycopg2
from psycopg2.extras import RealDictCursor
import sys

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
        email = request.form["user-email"]
        phone = request.form["user-phone"]
        # Put into database
        cursor.execute("INSERT INTO users (email, phone) VALUES (%s, %s) RETURNING id;", (email, phone))
        user_id = cursor.fetchone()["id"]
        cursor.execute("INSERT INTO queries (subject, verb, direct_obj, indirect_obj, loc, userid) \
                            VALUES (%s, %s, %s, %s, %s, %s);", (subject, verb, direct_obj, indirect_obj, loc, user_id))
        con.commit()

    # Get lists of query from database
    cursor.execute("SELECT id, subject, verb, direct_obj, indirect_obj, loc FROM queries;")
    queries = cursor.fetchall()

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

    # mock data
    articles = [{ "title" : "Meet the futurists: People who 'live in the future'", "source" : "CNN", "url" : "http://www.cnn.com/2015/11/06/tech/pioneers-futurists/index.html"},
                { "title" : "'I no longer see a fat little boy,' says a man overcoming body dysmorphia", "source" : "CNN", "url" : "http://www.cnn.com/2015/11/06/health/brian-cuban-body-dysmorphia-turning-points/index.html"}]

    cursor.execute("SELECT id, subject, verb, direct_obj, indirect_obj, loc FROM queries where id = %s;", id)
    query = cursor.fetchone()

    if con:
        con.close()
    return render_template("query.html", query = query, articles = articles)

@app.errorhandler(404)
def page_not_found(e):
    return render_template("404.html"), 404

if __name__ == "__main__":
    app.run(debug = True)
