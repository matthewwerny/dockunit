package cool.arch.whaleunit.support.util;

/*
 * #%L WhaleUnit - Support %% Copyright (C) 2015 CoolArch %% Licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. #L%
 */

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 */
public final class Iterables {

	/**
	 * Converts a finite iterable to a {@link Collection}.
	 * @param iterable 
	 * @return
	 */
	public static <T> Collection<T> toCollection(final Iterable<T> iterable) {
		requireNonNull(iterable, "iterable shall not be null");

		final List<T> result = new LinkedList<>();
		iterable.forEach(result::add);

		return result;
	}
}
