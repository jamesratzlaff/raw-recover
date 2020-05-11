package com.jamesratzlaff.rawrecover.io.file.type;

import java.util.Iterator;
import java.util.ServiceLoader;

import com.jamesratzlaff.rawrecover.io.file.type.spi.FileEndOffsetService;
import com.jamesratzlaff.rawrecover.io.file.type.spi.FindsEndOffset;

public class FileEndOffsetServiceImpl implements FileEndOffsetService{

	public static class Instance {
		private static FileEndOffsetService impl;
		
		public static FileEndOffsetService get() {
			if(impl==null) {
				impl=new FileEndOffsetServiceImpl();
			}
			return impl;
		}
		
	}
	
	private final ServiceLoader<FindsEndOffset>  loader;
	private FileEndOffsetServiceImpl() {
		loader = ServiceLoader.load(FindsEndOffset.class);
	}


	@Override
	public FindsEndOffset getEndFinder(String type) {
		return loader.stream().map(provider->provider.get()).filter(ef->ef.test(type)).findFirst().orElse(null);
	}


	@Override
	public Iterator<FindsEndOffset> getEndFinders() {
		return getEndFinders(false);
	}


	@Override
	public Iterator<FindsEndOffset> getEndFinders(boolean refresh) {
		if(refresh) {
			loader.reload();
		}
		return loader.iterator();
	}
	
	
	
	
}
