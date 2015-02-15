package cool.arch.whaleunit.junit.exception;

/*
 * #%L
 * WhaleUnit - JUnit
 * %%
 * Copyright (C) 2015 CoolArch
 * %%
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */

/**
 * Exception that indicates that a test's configuration is invalid.
 */
public class ValidationException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ValidationException() {
		super();
	}
	
	public ValidationException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
	
	public ValidationException(final String message, final Throwable cause) {
		super(message, cause);
	}
	
	public ValidationException(final String message) {
		super(message);
	}
	
	public ValidationException(final Throwable cause) {
		super(cause);
	}
}
