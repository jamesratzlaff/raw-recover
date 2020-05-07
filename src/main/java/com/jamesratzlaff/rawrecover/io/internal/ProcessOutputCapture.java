package com.jamesratzlaff.rawrecover.io.internal;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ProcessOutputCapture {
	private static final Predicate<String> defFilter = s->true;
	
	private final InputStream is;
	private final InputStream err;
	private final boolean ensureCapture;
	private List<String> outLines;
	private List<String> errLines;
	
	/**
	 * Used to filter out unwanted lines when retrieving the output or error stream (only does this when returning outLines or errLines; 
	 */
	private Predicate<String> filter=defFilter;
	
	public ProcessOutputCapture(Process p) {
		this(p, false);
	}
	
	public ProcessOutputCapture(Process p, boolean ensureCapture) {
		this.is=p.getInputStream();
		this.err=p.getErrorStream();
		this.ensureCapture=ensureCapture;
		
	}
	
	
	
	
	public List<String> getOutLines(){
		if(outLines==null) {
			outLines=new ArrayList<String>();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			reader.lines().forEach(outLines::add);
		}
		
		List<String> result = this.outLines;
		if(!Objects.equals(this.filter, defFilter)&&this.filter!=null) {
			result = new ArrayList<String>(result.stream().filter(filter).collect(Collectors.toList()));
		}
		
		return result;
		
	}
	
	public String getOutputAsSingleString() {
		List<String> lines = getOutLines();
		return String.join(System.getProperty("line.seperator"), lines);
	}
	
	public String getErrorAsSingleString() {
		List<String> lines = getErrorLines();
		return String.join(System.getProperty("line.seperator"), lines);
	}
	
	public List<String> getErrorLines(){
		if(errLines==null) {
			errLines=new ArrayList<String>();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(err));
			reader.lines().forEach(errLines::add);
		}
		List<String> result = this.errLines;
		if(!Objects.equals(this.filter, defFilter)&&this.filter!=null) {
			result = new ArrayList<String>(result.stream().filter(filter).collect(Collectors.toList()));
		}
		return result;
	}
	
	public void setFilter(Predicate<String> filter) {
		this.filter=filter;
	}
	
	public ProcessOutputCapture withFilter(Predicate<String> filter) {
		setFilter(filter);
		return this;
	}
	
	public Predicate<String> getFilter(){
		if(this.filter==null) {
			this.filter=defFilter;
		}
		return this.filter;
	}
	
	public void clearOutputLines() {
		if(outLines!=null) {
			outLines.clear();
			outLines=null;
		}
	}
	
	
	public void clearErrorLines() {
		if(errLines!=null) {
			errLines.clear();
			errLines=null;
		}
	}

	public void populate() {
		getOutLines();
		getErrorLines();
	}
	

	public void close() {
		if(ensureCapture) {
			getOutLines();
			getErrorLines();
		}
		close(is);
		close(err);
		
	}
	
	private static void close(Closeable c) {
		if(c!=null) {
			try {
				c.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	
	
}
