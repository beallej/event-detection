#!/usr/bin/env bash
working_dir="$(pwd)"
java_path="$(which java)"
[ "$(python --version 2>&1 | grep 'Python 3')" != "" ] && python_path="$(which python)" || python_path="$(which python3)"
if [ "$python_path" == "" ]; then
	>&2 echo "Error:  Unable to find the executable for python 3."
	exit 1
fi
$java_path -jar "$(pwd)/event-detection.jar" "$(pwd)/configuration.json" && $python_path "$(pwd)/Daemons/ArticleProcessorDaemon.py" && $python_path "$(pwd)/Daemons/ValidatorDaemon.py"
