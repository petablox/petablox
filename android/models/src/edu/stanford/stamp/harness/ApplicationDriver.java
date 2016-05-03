package edu.stanford.stamp.harness;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.os.Bundle;

/*
 * @author Patrick Charles Mutchler 
 * @author Saswat Anand
 */
public class ApplicationDriver 
{
	static List<Callback> callbacks = new ArrayList();

	// Singleton pattern.
	private static ApplicationDriver instance = new ApplicationDriver();

	private ApplicationDriver()
	{
	}

	public static ApplicationDriver getInstance()
	{
		return instance;
	}	

	public static void registerCallback(Callback cb)
	{
		callbacks.add(cb);
	}

	public static void callCallbacks()
	{
		//note: if analysis can distinguish between indices
		//replace 0 with random int
		callbacks.get(0).run();
	}
}

	
