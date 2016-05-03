package java.nio;

import edu.stanford.stamp.annotation.STAMP;
import edu.stanford.stamp.annotation.Flow;


class StampCharBuffer extends CharBuffer
{
	public java.nio.CharBuffer asReadOnlyBuffer()
	{
		return this;
	}

	public java.nio.CharBuffer compact()
	{
		return this;
	}
	
	public java.nio.CharBuffer duplicate()
	{
		return this;
	}

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public char get()
	{
		return 'a';
	}
	
	@STAMP(flows = {@Flow(from="this",to="@return")})
	public char get(int index)
	{
		return 'a';
	}

	public boolean isDirect()
	{
		return true;
	}
	
	public java.nio.ByteOrder order()
	{
		return null;//TODO?
	}
	
	@STAMP(flows = {@Flow(from="c",to="this")})
	public java.nio.CharBuffer put(char c)
	{
		return this;
	}
	
	@STAMP(flows = {@Flow(from="c",to="this")})
	public java.nio.CharBuffer put(int index, char c)
	{
		return this;
	}
	
	public java.nio.CharBuffer slice()
	{
		return this;
	}
	
	public java.nio.CharBuffer subSequence(int start, int end)
	{
		return this;
	}

	public boolean isReadOnly()
	{
		return true;
	}
}