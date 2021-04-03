package com.straders.algo.cache.model;

import java.io.Serializable;

import lombok.Data;

public class CacheData implements Serializable {

	private static final long serialVersionUID = 1L;

	public String key;

	public Object value;

}
