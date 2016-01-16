#!/usr/bin/env bash
working_dir="$(pwd)"
java_path="$(which java)"
[ "$(python --version | grep 'Python 3')" != "" ] && python_path="$(which python)" || python_path="$(which python3)"
if [ "$python_path" == "" ]; then
	>&2 echo "Error:  Unable to find the executable for python 3."
	exit 1
fi
line="$java_path -jar $(pwd)/event-detection.jar $(pwd)/configuration.json && $python_path $(pwd)/ArticleProcessorDaemon.py && $python_path $(pwd)/ValidatorDaemon.py"
line="0 * * * * $line"
read -p "Preparing to write: $line. Is this okay? [y/N] " yn
yn=$(echo "${yn:0:1}" | tr '[:upper:]' '[:lower:]')
if [ "$yn" != "y" ]; then
	echo "Stopping."
	exit 1
else
	echo "Writing."
fi
unset yn
(crontab -l; echo "$line") | crontab -
