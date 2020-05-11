package com.jamesratzlaff.rawrecover.io.file.type.spi;

import java.util.Iterator;

public interface FileEndOffsetService {

	
	FindsEndOffset getEndFinder(String type);
	Iterator<FindsEndOffset> getEndFinders();
	
	Iterator<FindsEndOffset> getEndFinders(boolean refresh);
	
}
