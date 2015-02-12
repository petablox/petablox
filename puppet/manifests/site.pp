node 'vagrant-petablox' {
	exec {'apt-update':
		command	=> '/usr/bin/apt-get update'
	}

	Exec['apt-update'] -> Package <| |>

	# java for logicblox, doop
	package {'java7-jre': 
		ensure	=> installed,
		name	=> 'openjdk-7-jre'
	}

	package {'java7-jdk':
		ensure	=> installed,
		name	=> 'openjdk-7-jdk'
	}

	# 32 bit libs needed for old JREs used for DOOP
	package {'lib32z1':
		ensure	=> installed,
		name	=> 'lib32z1'
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

	package {'unzip':
		ensure	=> installed,
		name	=> 'unzip'
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
