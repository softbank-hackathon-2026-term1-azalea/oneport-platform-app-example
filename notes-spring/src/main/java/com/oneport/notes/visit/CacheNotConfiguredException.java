package com.oneport.notes.visit;

class CacheNotConfiguredException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	CacheNotConfiguredException() {
		super("cache is not configured");
	}

}
