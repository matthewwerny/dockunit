package cool.arch.whaleunit.runtime.api;

/*
 * #%L WhaleUnit - JUnit %% Copyright (C) 2015 CoolArch %% Licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. #L%
 */

import java.util.Collection;

import org.jvnet.hk2.annotations.Contract;

@Contract
public interface Containers {

	void add(Container container);

	void createAll();

	void destroyAll();

	boolean exists(String name);

	void restart(Collection<String> names);

	void restart(String... names);

	void restartAll();

	void start(Collection<String> names);

	void start(String... names);

	void startAll();

	void stop(Collection<String> names);

	void stop(String... names);

	void stopAll();

	Container lookup(String name);
}
