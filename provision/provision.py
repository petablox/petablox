#!/usr/bin/env python
from __future__ import print_function
import sys
import json
import os
import subprocess
import hashlib
import urllib2
import shutil

CONFIG_JSON = '/vagrant/provision/config.json'

class FlushingStream(object):
	"""File-like object wrapper that flushes on every write."""
	def __init__(self, stream):
		self.stream = stream

	def write(self, data):
		self.stream.write(data)
		self.stream.flush()

	def __getattr__(self, attr):
		return getattr(self.stream, attr)

# make stdout and stderr flush eagerly so we can see progress during provisioning
sys.stdout = FlushingStream(sys.stdout)
sys.stderr = FlushingStream(sys.stderr)


def main(args=None):
	if args is None: args = sys.argv[1:]

	with open(CONFIG_JSON) as f:
		config = json.load(f)

	print('User dir: {}'.format(os.path.expanduser('~')))

	lb_path = '/vagrant/logicblox-{}'.format(config['logicblox']['version'])
	for userdir in ('/root', '/home/vagrant'):
		set_shell_src(lb_path, userdir=userdir)
	start_logicblox(lb_path)

	setup_doop(config['doop'])
	doop_path = '/vagrant/{}'.format(config['doop'])


def md5_file(file, block_size=32768):
	"""Compute md5sum of file (as hex), read in chunks of block_size"""
	md5 = hashlib.md5()
	while True:
		data = file.read(block_size)
		if not data:
			break
		md5.update(data)
	return md5.hexdigest()


def setup_doop(conf):
	doop_path = '/vagrant/' + conf['dirname']
	decapo_file = doop_path + '/externals/' + conf['decapo_zip']
	needs_decapo = True
	if os.path.isfile(decapo_file):
		with open(decapo_file, 'rb') as input:
			md5 = md5_file(input)
		if md5 == conf['decapo_md5']:
			print('Decapo present: {}'.format(decapo_file))
			needs_decapo = False
		else:
			print('Decapo incomplete? md5: {}, expected: {}'.format(md5, conf['decapo_md5']))

	if needs_decapo:
		decapo_url = conf['decapo_url']
		print('Downloading decapo to {}'.format(decapo_file))
		print('Fetching from: {}'.format(decapo_url))
		resp = urllib2.urlopen(decapo_url)
		try:
			with open(decapo_file, 'wb') as out:
				shutil.copyfileobj(resp, out)
		finally:
			resp.close()


def start_logicblox(lb_path):
	subprocess.call(['sudo', '-u', 'vagrant', '-i', '/vagrant/provision/startlb.sh', lb_path], stdout=sys.stdout, stderr=sys.stderr)

def set_shell_src(lb_path, userdir=None):
	if userdir is None:
		userdir = os.path.expanduser('~')

	print('Creating {}/.extra_profile with path: {}'.format(userdir, lb_path))
	with open(userdir + '/.extra_profile', 'w') as out:
		out.write('''
source "{lb_path}/etc/profile.d/logicblox.sh"
export LOGICBLOX_HOME="{lb_path}/logicblox"
if [ -z "$LD_LIBRARY_PATH" ]; then
	# work around DOOP bug requiring variable to be set
	export LD_LIBRARY_PATH=
fi
		'''.format(lb_path=lb_path))

	load_line = 'if [ -f ~/.extra_profile ]; then source ~/.extra_profile; fi'
	with open(userdir + '/.profile', 'r') as input:
		for line in input:
			if load_line in line:
				break
		else:
			with open(userdir + '/.profile', 'a') as out:
				out.write(load_line)
				out.write('\n')
			print('Updated {userdir}/.profile to load {userdir}/.extra_profile'.format(userdir=userdir))



if __name__ == '__main__':
	main()
