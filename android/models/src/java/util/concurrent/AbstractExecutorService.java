class AbstractExecutorService
{
	public  java.util.concurrent.Future<?> submit(java.lang.Runnable task) { 
		task.run();
		return null;
	}

	public <T> java.util.concurrent.Future<T> submit(java.lang.Runnable task, T result) { 
		task.run();
		return null;
	}

	public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) { 
		try{
			task.call();
		}catch(Exception e){}
		return null;
	}

}