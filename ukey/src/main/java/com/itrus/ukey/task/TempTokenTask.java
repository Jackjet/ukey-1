package com.itrus.ukey.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.ukey.service.TemTokenService;

@Service("tempTokenTask")
public class TempTokenTask {

	@Autowired
	private TemTokenService temTokenService;

	public void deleteTempToken() {
		temTokenService.deleteTemToken();
	}
}
