package com.straders.algo.cache.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import com.straders.algo.cache.model.CacheData;

public class CacheService {

	@Cacheable(value = "cache", key = "#cache.key")
	public static void cacheData(CacheData cache) {
		System.out.println(" Data Cached");
	}

	@Cacheable(value = "cache", condition = "#cache.key")
	public static void getCache(CacheData cache) {
		System.out.println(" Data Cache Got ");
	}

	@CachePut(value = "cache", key = "#cache.key")
	public static void updateCache(CacheData cache) {
		System.out.println(" Data Cache Updated ");
	}

	@CacheEvict(value = "cache", allEntries = false, key = "#cache.key")
	public static void deleteCache(CacheData cache) {
		System.out.println(" Data Cache Deleted ");
	}

	@CacheEvict(value = "cache", allEntries = true)
	public static void deleteAll(CacheData cache) {
		System.out.println(" Data Cache Deleted All ");
	}
}
