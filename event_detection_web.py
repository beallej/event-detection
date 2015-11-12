from flask import Flask, render_template, request, redirect
import psycopg2
from psycopg2.extras import RealDictCursor
import sys
import QueryProcessorDaemon
import threading

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


@app.route("/", methods=["GET"])
def queries():
    con, cursor = connect_database()

    # Get lists of query from database with counts of associated articles
    cursor.execute("SELECT q.id, q.subject, q.verb, q.direct_obj, q.indirect_obj, \
                           q.loc, count(qa.article) as article_count \
                    FROM queries q \
                    LEFT JOIN query_articles qa on q.id = qa.query and qa.accuracy > .25 \
                    GROUP BY(q.id);")
    queries = cursor.fetchall()

    if con:
        con.close()
    return render_template("queries.html", queries = queries)

@app.route("/query", methods=["POST"])
def new_query():
    con, cursor = connect_database()

    # TODO: server side validation
    subject = request.form["subject"]
    verb = request.form["verb"]
    direct_obj = request.form["direct-object"]
    indirect_obj = request.form["indirect-object"]
    loc = request.form["location"]
    email = request.form["user-email"]
    phone = request.form["user-phone"]
    # Put into database
    cursor.execute("SELECT id from users where email = %s and phone = %s", (email, phone))
    user_id = cursor.fetchone()
    if user_id:
        user_id = user_id["id"]
    else:
        cursor.execute("INSERT INTO users (email, phone) VALUES (%s, %s) RETURNING id;", (email, phone))
        user_id = cursor.fetchone()["id"]

    try:
        cursor.execute("INSERT INTO queries (subject, verb, direct_obj, indirect_obj, loc, userid) \
                        VALUES (%s, %s, %s, %s, %s, %s);", (subject, verb, direct_obj, indirect_obj, loc, user_id))
        con.commit()

        qpd = QueryProcessorDaemon.QueryProcessorDaemon()
        thread = threading.Thread(target=qpd.run)
        thread.daemon = True
        thread.start()

    except psycopg2.IntegrityError:
        pass

    if con:
        con.close()

    return redirect("/")

@app.route("/query/<id>", methods=["GET"])
def query(id):
    # find query by id
    # if we don't find a query with that id, 404
    con, cursor = connect_database()
    cursor.execute("SELECT a.title, s.source_name as source, a.url \
                    FROM queries q \
                    INNER JOIN query_articles qa on q.id = qa.query \
                    INNER JOIN articles a on qa.article = a.id \
                    INNER JOIN sources s on s.id = a.source \
                    WHERE q.id = %s and qa.accuracy > .25;", (id,))
    articles = cursor.fetchall()

    cursor.execute("SELECT id, subject, verb, direct_obj, indirect_obj, loc FROM queries where id = %s;", (id,))
    query = cursor.fetchone()

    if con:
        con.close()

    if query is not None:
        return render_template("query.html", query = query, articles = articles)
    return render_template("404.html"), 404

@app.errorhandler(404)
def page_not_found(e):
    return render_template("404.html"), 404

if __name__ == "__main__":
    app.run(debug = True)
