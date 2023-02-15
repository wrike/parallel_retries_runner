package com.wrike.runner.provider.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Util class for loading services implementations via SPI.
 * Errors are logged, but they does not interrupt execution.
 *
 * @author daniil.shylko on 09.02.2023
 */
public final class ServiceLoaderUtil {

    private static final Logger LOG = LogManager.getLogger(ServiceLoaderUtil.class);

    private ServiceLoaderUtil() {
    }

    public static <T> List<T> load(final Class<T> type) {
        return ServiceLoader.load(type, Thread.currentThread().getContextClassLoader()).stream()
                .map(ServiceLoaderUtil::getSafely)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    private static <T> Optional<T> getSafely(ServiceLoader.Provider<T> provider) {
        try {
            return Optional.of(provider.get());
        } catch (ServiceConfigurationError | Exception e) {
            LOG.error(String.format("Can't load %s", provider.type()), e);
            return Optional.empty();
        }
    }
}
