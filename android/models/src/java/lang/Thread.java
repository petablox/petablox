class Thread
{
    private Runnable r;

    public  Thread() 
    { 
		this.r = this;
    }

    public  Thread(java.lang.Runnable runnable) 
    { 
		this.r = runnable;
    }

	public  Thread(java.lang.Runnable runnable, java.lang.String threadName)
	{ 
		this.r = runnable;
	}

	public  Thread(java.lang.String threadName) 
	{
		this.r = this;
	}

	public  Thread(java.lang.ThreadGroup group, java.lang.Runnable runnable)
	{ 
		this.r = runnable;
	}

	public  Thread(java.lang.ThreadGroup group, java.lang.Runnable runnable, java.lang.String threadName) 
	{ 
		this.r = runnable;
	}

	public  Thread(java.lang.ThreadGroup group, java.lang.String threadName) { 
		this.r = this;
	}

	public  Thread(java.lang.ThreadGroup group, java.lang.Runnable runnable, java.lang.String threadName, long stackSize) 
	{ 
		this.r = runnable;
	}

    public synchronized  void start() 
    { 
		r.run();
    }
}
