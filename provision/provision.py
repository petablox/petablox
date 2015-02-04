#!/usr/bin/env python
from __future__ import print_function
import sys
import json
import os
import subprocess
import hashlib
import urllib2
import shutil
import stat
import pwd

CONFIG_JSON = '/vagrant/provision/config.json'

def main(args=None):
	if args is None: args = sys.argv[1:]

	with open(CONFIG_JSON) as f:
		config = json.load(f)

	lb_path = '/vagrant/logicblox-{}'.format(config['logicblox']['version'])
	for userdir in ('/root', '/home/vagrant'):
		set_shell_src(config, userdir=userdir)
	start_logicblox(lb_path)

	setup_doop(config['doop'])

def get_lb_path(config):
	return '/vagrant/logicblox-{}'.format(config['logicblox']['version'])

def get_doop_path(config):
	return '/vagrant/{}'.format(config['doop']['dirname'])

def md5_file(file, block_size=32768):
	"""Compute md5sum of file (as hex), read in chunks of block_size"""
	md5 = hashlib.md5()
	while True:
		data = file.read(block_size)
		if not data:
			break
		md5.update(data)
	return md5.hexdigest()


def fprint(*args, **kwargs):
	print(*args, **kwargs)
	if 'file' in kwargs:
		kwargs['file'].flush()
	else:
		sys.stdout.flush()


def setup_doop(conf):
	doop_path = '/vagrant/' + conf['dirname']

	link_doop_dirs(conf)
	get_decapo(doop_path, conf)
	unpack_jres(doop_path, conf['jres'])

def link_doop_dirs(conf):
	doop_path = '/vagrant/' + conf['dirname']
	tmp_path = '/opt/' + conf['dirname']

	vagrant_pwd = pwd.getpwnam('vagrant')

	for dirname in ('cache', 'jars', 'tmp'):
		source = os.path.join(doop_path, dirname)
		target = os.path.join(tmp_path, dirname)
		if not os.path.isdir(target):
			os.makedirs(target)
		os.chown(target, vagrant_pwd.pw_uid, vagrant_pwd.pw_gid)

		needs_link = True
		if os.path.lexists(source):
			if not os.path.islink(source):
				fprint('WARN: Non-symlink {} exists, test execution will fail with mmap errors!'.format(source), file=sys.stderr)
				needs_link = False # will fail, so avoid erroring provisioning
			elif os.readlink(source) != target:
				os.unlink(source)
			else:
				needs_link = False
		if needs_link:
			os.symlink(target, source)


def get_decapo(doop_path, conf):
	decapo_file = doop_path + '/externals/' + conf['decapo_zip']
	needs_decapo = True
	if os.path.isfile(decapo_file):
		with open(decapo_file, 'rb') as input:
			md5 = md5_file(input)
		if md5 == conf['decapo_md5']:
			fprint('Decapo present: {}'.format(decapo_file))
			needs_decapo = False
		else:
			fprint('Decapo incomplete? md5: {}, expected: {}'.format(md5, conf['decapo_md5']))

	if needs_decapo:
		decapo_url = conf['decapo_url']
		fprint('Downloading decapo to {}'.format(decapo_file))
		fprint('Fetching from: {}'.format(decapo_url))
		resp = urllib2.urlopen(decapo_url)
		try:
			with open(decapo_file, 'wb') as out:
				shutil.copyfileobj(resp, out)
		finally:
			resp.close()

def unpack_jres(doop_path, jres):
	externals = os.path.join(doop_path, 'externals')
	for jrebin, jredir in jres:
		jrebin_full = os.path.join(externals, jrebin)

		# ensure executable
		mode = os.stat(jrebin_full).st_mode
		if (mode | stat.S_IXUSR) != mode:
			fprint('Setting exec bit on {}'.format(jrebin_full))
			os.chmod(jrebin_full, mode | stat.S_IXUSR)

		jredir_full = os.path.join(externals, jredir)
		if os.path.isdir(jredir_full):
			fprint('JRE exists: {}'.format(jredir_full))
		else:
			fprint('Unpacking JRE {} from {}'.format(jredir, jrebin_full))
			proc = subprocess.Popen([jrebin_full, '-silent'], stdout=sys.stdout, stderr=sys.stderr, stdin=subprocess.PIPE, cwd=externals)
			proc.communicate('yes')
			if proc.returncode != 0:
				fprint('Failed to unpack JRE {}'.format(jredir), file=sys.stderr)


def start_logicblox(lb_path):
	subprocess.call(['sudo', '-u', 'vagrant', '-i', '/vagrant/provision/startlb.sh', lb_path], stdout=sys.stdout, stderr=sys.stderr)

def set_shell_src(config, userdir=None):
	if userdir is None:
		userdir = os.path.expanduser('~')

	lb_path = get_lb_path(config)
	doop_path = get_doop_path(config)

	fprint('Creating {}/.extra_profile with paths:\nLogicblox: {}\nDoop: {}\n'.format(userdir, lb_path, doop_path))
	with open(userdir + '/.extra_profile', 'w') as out:
		out.write('''
source "{lb_path}/etc/profile.d/logicblox.sh"
export LOGICBLOX_HOME="{lb_path}/logicblox"
export DOOP_HOME="{doop_path}"
if [ -z "$LD_LIBRARY_PATH" ]; then
	# work around DOOP bug requiring variable to be set
	export LD_LIBRARY_PATH=
fi
		'''.format(lb_path=lb_path, doop_path=doop_path))

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
