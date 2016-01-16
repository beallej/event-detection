import subprocess

executable = subprocess.check_output("which bash", shell=True, universal_newlines=True).strip() #This just gets the location of bash

'''
Just avoids repeated code by forwarding to subprocess.check_output with shell and universal_newlines set to True.
'''
def simpleSubprocess(args, executable=executable):
	return subprocess.check_output(args, executable=executable, shell=True, universal_newlines=True)

python_path = simpleSubprocess('''[ "$(python --version | grep 'Python 3')" != "" ] && echo "$(which python)" || echo "$(which python3)"''').strip()
