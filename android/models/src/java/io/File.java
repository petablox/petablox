class File
{
	@STAMP(flows={@Flow(from="!FILE",to="this"), @Flow(from="dir",to="this"), @Flow(from="name",to="this")})
	public  File(java.io.File dir, java.lang.String name) 
	{ 
	}

	@STAMP(flows={@Flow(from="$FILE",to="this"), @Flow(from="!FILE",to="this"), @Flow(from="path",to="this")})
	public  File(java.lang.String path) 
	{ 
	}

	@STAMP(flows={@Flow(from="$FILE",to="this"), @Flow(from="!FILE",to="this"), @Flow(from="dirPath",to="this"),@Flow(from="name",to="this")})
	public  File(java.lang.String dirPath, java.lang.String name) 
	{ 
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String getAbsolutePath() 
	{ 
		return new String();
	}

	public  java.io.File getAbsoluteFile() 
	{ 
		return this;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String getCanonicalPath() throws java.io.IOException 
	{ 
		return new String();
	}
	
	public  java.io.File getCanonicalFile() throws java.io.IOException 
	{ 
		return this;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String getName() 
	{ 
		return new String();
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String getParent() 
	{ 
		return new String();
	}

	public  java.io.File getParentFile() 
	{ 
		return this;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String getPath() 
	{ 
		return new String();
	}

	public  java.io.File[] listFiles() 
	{ 
		File[] fs = new File[0];
		fs[0] = this;
		return fs;
	}
	
	public  java.io.File[] listFiles(java.io.FilenameFilter filter) 
	{ 
		File[] fs = new File[0];
		fs[0] = this;
		return fs;
	}

	public  java.io.File[] listFiles(java.io.FileFilter filter) 
	{ 
		File[] fs = new File[0];
		fs[0] = this;
		return fs;
	}	

	@STAMP(flows={@Flow(from="this",to="@return")})	
	public  java.lang.String toString() { 
		return new String();
	}
}