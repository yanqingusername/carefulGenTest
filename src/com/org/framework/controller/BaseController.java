package com.org.framework.controller;

import org.springframework.beans.factory.annotation.Autowired;

import com.org.framework.service.UtilMethodService;

/**
 * 所有的controller的基类
 * 
 * @author Liu zhile
 * @since 2015-10-16
 */
public class BaseController {

	@Autowired
	protected UtilMethodService utilMethodService;

}
