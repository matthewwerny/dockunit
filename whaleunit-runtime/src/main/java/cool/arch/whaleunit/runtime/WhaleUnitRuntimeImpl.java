package cool.arch.whaleunit.runtime;

/*
 * #%L
 * WhaleUnit - Runtime
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

import static cool.arch.whaleunit.support.functional.Exceptions.wrap;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import cool.arch.whaleunit.annotation.DirtiesContainers;
import cool.arch.whaleunit.annotation.Logger;
import cool.arch.whaleunit.annotation.LoggerAdapterFactory;
import cool.arch.whaleunit.annotation.WhaleUnit;
import cool.arch.whaleunit.runtime.api.Container;
import cool.arch.whaleunit.runtime.api.ContainerFactory;
import cool.arch.whaleunit.runtime.api.Containers;
import cool.arch.whaleunit.runtime.api.DelegatingLoggerAdapterFactory;
import cool.arch.whaleunit.runtime.api.MutableTestClassHolder;
import cool.arch.whaleunit.runtime.binder.LoggerAdapterBinder;
import cool.arch.whaleunit.runtime.exception.InitializationException;
import cool.arch.whaleunit.runtime.exception.TestManagementException;
import cool.arch.whaleunit.runtime.exception.ValidationException;
import cool.arch.whaleunit.runtime.service.api.ContainerDescriptorLoaderService;

public final class WhaleUnitRuntimeImpl implements WhaleUnitRuntime {
	
	private static final String MISSING_WHALEUNIT_ANNOTATION_TMPL = "Annotation %s is required on unit test %s that is using WhaleUnit.";
	
	private final Set<String> globallyDirtiedContainerNames = new HashSet<>();
	
	@Inject
	private ContainerFactory containerFactory;
	
	@Inject
	private DelegatingLoggerAdapterFactory delegatingLoggerAdapterFactory;
	
	@Inject
	private Containers containers;
	
	@Inject
	private MutableTestClassHolder testClassHolder;
	
	@Inject
	private Provider<ContainerDescriptorLoaderService> containerDescriptorLoaderService;
	
	private Logger log;
	
	private final ServiceLocator locator;
	
	/**
	 * Constructs a new WhaleUnitRule.
	 */
	public WhaleUnitRuntimeImpl(final Class<?> testClass) {
		requireNonNull(testClass, "testClass shall not be null");
		locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
		ServiceLocatorUtilities.bind(locator, new LoggerAdapterBinder());
		locator.inject(this);
		testClassHolder.setTestClass(testClass);
	}
	
	WhaleUnitRuntimeImpl(final ServiceLocator locator) {
		this.locator = requireNonNull(locator, "locator shall not be null");
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void onInit(final Class<?> testClass) {
		Optional.of((Class) testClass)
			.map(tc -> tc.getAnnotation(DirtiesContainers.class))
			.map(annotation -> ((DirtiesContainers) annotation).value())
			.map(Arrays::asList)
			.ifPresent(globallyDirtiedContainerNames::addAll);
		
		preInit();
		validate();
		init();
	}
	
	@Override
	public void onTestStart(final String methodName) {
		containers.startAll();
	}
	
	@Override
	public void onTestSucceeded(final String methodName) {
		// Intentionally do nothing
	}
	
	@Override
	public void onTestFailed(final String methodName) {
		containers.stopAll();
	}
	
	@Override
	public void onTestEnd(final String methodName) {
		containers.stop(globallyDirtiedContainerNames);
		
		testClassHolder.getTestClass()
			.map(wrap(testClass -> testClass.getMethod(methodName), e -> new TestManagementException("Error looking up method " + methodName)))
			.map(method -> method.getAnnotation(DirtiesContainers.class))
			.map(annotation -> annotation.value())
			.ifPresent(containers::stop);
	}
	
	@Override
	public void onCleanup() {
		containers.stopAll();
		containers.destroyAll();
	}
	
	private void preInit() {
		initLogAdapter();
	}
	
	private void initLogAdapter() {
		final String whaleUnitName = WhaleUnit.class.getName();
		final String testClassName = testClassHolder.getTestClass().get().getName();
		final String message = String.format(MISSING_WHALEUNIT_ANNOTATION_TMPL, whaleUnitName, testClassName);
		
		final Optional<LoggerAdapterFactory> laf = testClassHolder.getTestClass()
			.map(testClass -> testClass.getAnnotation(WhaleUnit.class))
			.map(annotation -> annotation.loggerAdapterFactory())
			.map(wrap(clazz -> clazz.newInstance(), e -> new InitializationException("Error initializing LoggerAdapter", e)));
		
		laf.ifPresent(delegatingLoggerAdapterFactory::setLoggerAdapterFactory);
		laf.orElseThrow(() -> new ValidationException(message));
		laf.map(factory -> delegatingLoggerAdapterFactory.create(getClass()))
			.ifPresent(log -> this.log = log);
	}
	
	private void init() {
		addContainer("foo");
		addContainer("bar");
		addContainer("bat");
		addContainer("baz");
	}
	
	private void validate() {
		//		final WhaleUnit annotation = testClass.getAnnotation(WhaleUnit.class);
		//		
		//		annotation.config();
		
		// TODO - Implement
	}
	
	private void addContainer(final String name) {
		getLog().debug("Registering container " + name);
		
		final Container container = containerFactory.apply(name);
		containers.add(container);
	}
	
	public Logger getLog() {
		return log;
	}
}