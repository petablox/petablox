node 'vagrant-petablox' {

	# java for logicblox, doop
	package {'java7-jre': 
		ensure	=> installed,
		name	=> 'openjdk-7-jre'
	}

	package {'java7-jdk':
		ensure	=> installed,
		name	=> 'openjdk-7-jdk'
	}

	# python for provisioning scripts
	package {'python3':
		ensure	=> installed,
		name	=> 'python3'
	}

	package {'python3-yaml':
		ensure	=> installed,
		name	=> 'python3-yaml'
	}

	# standard repo packages
	package {'git':
		ensure	=> installed,
		name	=> 'git',
	}

	# package {'augeas':
	# 	ensure	=> installed,
	# 	name	=> 'augeas',
	# }

	package {'screen':
		ensure	=> installed,
		name	=> 'screen',
	}
}
