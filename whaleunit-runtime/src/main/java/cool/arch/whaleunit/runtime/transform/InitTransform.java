package cool.arch.whaleunit.runtime.transform;

/*
 * #%L WhaleUnit - Runtime %% Copyright (C) 2015 CoolArch %% Licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. #L%
 */

import static cool.arch.whaleunit.support.functional.Exceptions.wrap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;

import org.jvnet.hk2.annotations.Service;

import cool.arch.stateroom.State;
import cool.arch.whaleunit.annotation.DirtiesContainers;
import cool.arch.whaleunit.annotation.Logger;
import cool.arch.whaleunit.annotation.LoggerAdapterFactory;
import cool.arch.whaleunit.annotation.WhaleUnit;
import cool.arch.whaleunit.api.exception.ContainerDescriptorLoadException;
import cool.arch.whaleunit.api.exception.InitializationException;
import cool.arch.whaleunit.api.exception.TestManagementException;
import cool.arch.whaleunit.api.exception.ValidationException;
import cool.arch.whaleunit.runtime.api.ContainerFactory;
import cool.arch.whaleunit.runtime.api.Containers;
import cool.arch.whaleunit.runtime.api.DelegatingLoggerAdapterFactory;
import cool.arch.whaleunit.runtime.model.MachineModel;
import cool.arch.whaleunit.runtime.service.api.ContainerDescriptorLoaderService;
import cool.arch.whaleunit.runtime.service.api.MutableConfigService;

@Service
public final class InitTransform implements BiFunction<State<MachineModel>, MachineModel, MachineModel> {

	private static final String MISSING_WHALEUNIT_ANNOTATION_TMPL =
		"Annotation %s is required on unit test %s that is using WhaleUnit.";

	private static final String LOG_INIT_ERROR = "Error initializing LoggerAdapter";

	private final Containers containers;

	private final ContainerFactory containerFactory;

	private final Provider<MutableConfigService> configService;

	private final Provider<ContainerDescriptorLoaderService> containerDescriptorLoaderService;

	private final DelegatingLoggerAdapterFactory delegatingLoggerAdapterFactory;

	private Set<String> globallyDirtiedContainerNames = new HashSet<>();

	private Logger log;

	@Inject
	public InitTransform(final Containers containers, final ContainerFactory containerFactory,
		final Provider<MutableConfigService> configService,
		final Provider<ContainerDescriptorLoaderService> containerDescriptorLoaderService,
		final DelegatingLoggerAdapterFactory delegatingLoggerAdapterFactory) {
		this.containers = containers;
		this.containerFactory = containerFactory;
		this.configService = configService;
		this.containerDescriptorLoaderService = containerDescriptorLoaderService;
		this.delegatingLoggerAdapterFactory = delegatingLoggerAdapterFactory;
	}

	@Override
	public MachineModel apply(final State<MachineModel> state, final MachineModel model) {
		model.setGloballyDirtiedContainerNames(globallyDirtiedContainerNames);
		
		Optional.of(model.getTestClass())
			.map(tc -> tc.getAnnotation(DirtiesContainers.class))
			.map(annotation -> annotation.value())
			.map(Arrays::asList)
			.ifPresent(globallyDirtiedContainerNames::addAll);

		preInit(model);

		log.info("Initializing");
		
		try {
			init(model);
		} catch (final ContainerDescriptorLoadException e) {
			throw new TestManagementException(e);
		}

		validate(model);

		containers.createAll();

		return model;
	}

	private void validate(final MachineModel model) {
		final Class<?> testClass = model.getTestClass();
		final WhaleUnit annotation = Optional.ofNullable(testClass)
			.map(tc -> tc.getAnnotation(WhaleUnit.class))
			.orElse(null);
		final Set<String> names = new HashSet<>();

		Optional.ofNullable(testClass)
			.map(tc -> tc.getAnnotation(DirtiesContainers.class))
			.map(DirtiesContainers::value)
			.map(Arrays::stream)
			.orElse(Arrays.stream(new String[] {}))
			.forEach(names::add);

		if (testClass != null) {
			Arrays.stream(testClass.getMethods())
				.filter(m -> m.isAnnotationPresent(DirtiesContainers.class))
				.map(m -> m.getAnnotation(DirtiesContainers.class))
				.map(DirtiesContainers::value)
				.flatMap(c -> Arrays.stream(c))
				.forEach(names::add);
		}

		final String missingName = names.stream()
			.filter(name -> !containers.exists(name))
			.collect(Collectors.joining(", "));

		if (!"".equals(missingName)) {
			throw new TestManagementException("Unknown containers in DirtiesContainers annotations: " + missingName);
		}
	}

	private void preInit(final MachineModel model) {
		initLogAdapter(model);
	}

	private void init(final MachineModel model) throws ContainerDescriptorLoadException {
		final Class<?> testClass = model.getTestClass();

		containerDescriptorLoaderService.get()
			.extractDescriptors(testClass)
			.stream()
			.peek(c -> getLog().debug("Registering container " + c.getId()
				.get()))
			.map(containerFactory::apply)
			.forEach(containers::add);

		Optional.ofNullable(testClass)
			.map(tc -> tc.getAnnotation(WhaleUnit.class))
			.map(WhaleUnit::config)
			.ifPresent(config -> configService.get()
				.addConfigFile(config));
	}

	private void initLogAdapter(final MachineModel model) {
		final Class<?> testClass = model.getTestClass();
		final String whaleUnitName = WhaleUnit.class.getName();
		final String testClassName = Optional.ofNullable(testClass)
			.map(tc -> tc.getName())
			.get();
		final String message = String.format(MISSING_WHALEUNIT_ANNOTATION_TMPL, whaleUnitName, testClassName);

		final Optional<LoggerAdapterFactory> laf = Optional.ofNullable(testClass)
			.map(testClazz -> testClazz.getAnnotation(WhaleUnit.class))
			.map(annotation -> annotation.loggerAdapterFactory())
			.map(wrap(clazz -> clazz.newInstance(), e -> new InitializationException(LOG_INIT_ERROR, e)));

		laf.ifPresent(delegatingLoggerAdapterFactory::setLoggerAdapterFactory);
		laf.orElseThrow(() -> new ValidationException(message));
		laf.map(factory -> delegatingLoggerAdapterFactory.create(getClass()))
			.ifPresent(log -> this.log = log);
	}

	public Logger getLog() {
		return log;
	}

}