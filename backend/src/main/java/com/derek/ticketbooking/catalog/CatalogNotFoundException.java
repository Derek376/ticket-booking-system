package com.derek.ticketbooking.catalog;

public class CatalogNotFoundException extends RuntimeException {

	public CatalogNotFoundException(String resource, long id) {
		super(resource + " " + id + " was not found.");
	}

}
