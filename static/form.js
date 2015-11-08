function submitQuery() {
  var form = document.forms["query-form"];
  var invalid = false;
  var list = document.getElementById("error-list");
  list.innerHTML = "";

  if (form["subject"].value == "") {
    var entry = document.createElement("li");
    entry.appendChild(document.createTextNode("Subject is required"));
    list.appendChild(entry);
    invalid = true;
  }
  if (form["verb"].value == "") {
    var entry = document.createElement("li");
    entry.appendChild(document.createTextNode("Verb is required"));
    list.appendChild(entry);
    invalid = true;
  }
  if (form["user-phone"].value == "" && form["user-email"].value == "") {
    var entry = document.createElement("li");
    entry.appendChild(document.createTextNode("Either phone or email is required"));
    list.appendChild(entry);
    invalid = true;
  }
  if (invalid) {
    document.getElementById("form-error").className = "";
    event.preventDefault();
    return false;
  }
}
