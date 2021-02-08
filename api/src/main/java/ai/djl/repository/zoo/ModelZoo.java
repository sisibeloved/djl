/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package ai.djl.repository.zoo;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.repository.Artifact;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An interface represents a collection of models. */
public interface ModelZoo {

    /**
     * Returns the global unique identifier of the {@code ModelZoo}.
     *
     * <p>We recommend to use reverse DNS name as your model zoo group ID to make sure it's not
     * conflict with other ModelZoos.
     *
     * @return the global unique identifier of the {@code ModelZoo}
     */
    String getGroupId();

    /**
     * Lists the available model families in the ModelZoo.
     *
     * @return the list of all available model families
     */
    default List<ModelLoader> getModelLoaders() {
        List<ModelLoader> list = new ArrayList<>();
        try {
            Field[] fields = getClass().getDeclaredFields();
            for (Field field : fields) {
                if (ModelLoader.class.isAssignableFrom(field.getType())) {
                    list.add((ModelLoader) field.get(null));
                }
            }
        } catch (ReflectiveOperationException e) {
            // ignore
        }
        return list;
    }

    /**
     * Returns the {@link ModelLoader} based on the model name.
     *
     * @param name the name of the model
     * @return the {@link ModelLoader} of the model
     */
    default ModelLoader getModelLoader(String name) {
        for (ModelLoader loader : getModelLoaders()) {
            if (name.equals(loader.getArtifactId())) {
                return loader;
            }
        }
        return null;
    }

    /**
     * Returns all supported engine names.
     *
     * @return all supported engine names
     */
    Set<String> getSupportedEngines();

    /**
     * Returns whether a model zoo with the group id is available.
     *
     * @param groupId the model zoo group id to check for
     * @return whether a model zoo with the group id is available
     */
    static boolean hasModelZoo(String groupId) {
        ServiceLoader<ZooProvider> providers = ServiceLoader.load(ZooProvider.class);
        for (ZooProvider provider : providers) {
            ModelZoo zoo = provider.getModelZoo();
            if (zoo.getGroupId().equals(groupId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the {@link ModelLoader} based on the model name.
     *
     * @param criteria the requirements for the model
     * @param <I> the input data type for preprocessing
     * @param <O> the output data type after postprocessing
     * @return the model that matches the criteria
     * @throws IOException for various exceptions loading data from the repository
     * @throws ModelNotFoundException if no model with the specified criteria is found
     * @throws MalformedModelException if the model data is malformed
     */
    static <I, O> ZooModel<I, O> loadModel(Criteria<I, O> criteria)
            throws IOException, ModelNotFoundException, MalformedModelException {
        Logger logger = LoggerFactory.getLogger(ModelZoo.class);
        logger.debug("Loading model with {}", criteria);

        String artifactId = criteria.getArtifactId();
        ModelZoo modelZoo = criteria.getModelZoo();
        String groupId = criteria.getGroupId();
        String engine = criteria.getEngine();
        Application application = criteria.getApplication();

        List<ModelZoo> list = new ArrayList<>();
        if (modelZoo != null) {
            logger.debug("Searching model in specified model zoo: {}", modelZoo.getGroupId());
            if (groupId != null && !modelZoo.getGroupId().equals(groupId)) {
                throw new ModelNotFoundException(
                        "groupId conflict with ModelZoo criteria."
                                + modelZoo.getGroupId()
                                + " v.s. "
                                + groupId);
            }
            Set<String> supportedEngine = modelZoo.getSupportedEngines();
            if (engine != null && !supportedEngine.contains(engine)) {
                throw new ModelNotFoundException(
                        "ModelZoo doesn't support specified with engine: " + engine);
            }
            list.add(modelZoo);
        } else {
            ServiceLoader<ZooProvider> providers = ServiceLoader.load(ZooProvider.class);
            for (ZooProvider provider : providers) {
                logger.debug("Searching model in zoo provider: {}", provider.getName());
                ModelZoo zoo = provider.getModelZoo();
                if (zoo == null) {
                    logger.debug("No model zoo found in zoo provider: {}", provider.getName());
                    continue;
                }
                if (groupId != null && !zoo.getGroupId().equals(groupId)) {
                    // filter out ModelZoo by groupId
                    logger.debug("Ignore ModelZoo {} by groupId: {}", zoo.getGroupId(), groupId);
                    continue;
                }
                Set<String> supportedEngine = zoo.getSupportedEngines();
                if (engine != null && !supportedEngine.contains(engine)) {
                    logger.debug("Ignore ModelZoo {} by engine: {}", zoo.getGroupId(), engine);
                    continue;
                }
                list.add(zoo);
            }
        }

        Exception lastException = null;
        for (ModelZoo zoo : list) {
            String loaderGroupId = zoo.getGroupId();
            for (ModelLoader loader : zoo.getModelLoaders()) {
                Application app = loader.getApplication();
                String loaderArtifactId = loader.getArtifactId();
                logger.debug("Checking ModelLoader: {}", loader);
                if (artifactId != null && !artifactId.equals(loaderArtifactId)) {
                    // filter out by model loader artifactId
                    logger.debug(
                            "artifactId mismatch for ModelLoader: {}:{}",
                            loaderGroupId,
                            loaderArtifactId);
                    continue;
                }
                if (application != Application.UNDEFINED
                        && app != Application.UNDEFINED
                        && !app.matches(application)) {
                    // filter out ModelLoader by application
                    logger.debug(
                            "application mismatch for ModelLoader: {}:{}",
                            loaderGroupId,
                            loaderArtifactId);
                    continue;
                }

                try {
                    return loader.loadModel(criteria);
                } catch (ModelNotFoundException e) {
                    lastException = e;
                    logger.trace("", e);
                    logger.debug(
                            "{} for ModelLoader: {}:{}",
                            e.getMessage(),
                            loaderGroupId,
                            loaderArtifactId);
                }
            }
        }
        throw new ModelNotFoundException(
                "No matching model with specified Input/Output type found.", lastException);
    }

    /**
     * Returns the available {@link Application} and their model artifact metadata.
     *
     * @return the available {@link Application} and their model artifact metadata
     * @throws IOException if failed to download to repository metadata
     * @throws ModelNotFoundException if failed to parse repository metadata
     */
    static Map<Application, List<Artifact>> listModels()
            throws IOException, ModelNotFoundException {
        return listModels(Criteria.builder().build());
    }

    /**
     * Returns the available {@link Application} and their model artifact metadata.
     *
     * @param criteria the requirements for the model
     * @return the available {@link Application} and their model artifact metadata
     * @throws IOException if failed to download to repository metadata
     * @throws ModelNotFoundException if failed to parse repository metadata
     */
    static Map<Application, List<Artifact>> listModels(Criteria<?, ?> criteria)
            throws IOException, ModelNotFoundException {
        String artifactId = criteria.getArtifactId();
        ModelZoo modelZoo = criteria.getModelZoo();
        String groupId = criteria.getGroupId();
        String engine = criteria.getEngine();
        Application application = criteria.getApplication();

        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<Application, List<Artifact>> models =
                new TreeMap<>(Comparator.comparing(Application::getPath));
        ServiceLoader<ZooProvider> providers = ServiceLoader.load(ZooProvider.class);
        for (ZooProvider provider : providers) {
            ModelZoo zoo = provider.getModelZoo();
            if (zoo == null) {
                continue;
            }
            if (modelZoo != null) {
                if (groupId != null && !modelZoo.getGroupId().equals(groupId)) {
                    continue;
                }
                Set<String> supportedEngine = modelZoo.getSupportedEngines();
                if (engine != null && !supportedEngine.contains(engine)) {
                    continue;
                }
            }
            List<ModelLoader> list = zoo.getModelLoaders();
            for (ModelLoader loader : list) {
                Application app = loader.getApplication();
                String loaderArtifactId = loader.getArtifactId();
                if (artifactId != null && !artifactId.equals(loaderArtifactId)) {
                    // filter out by model loader artifactId
                    continue;
                }
                if (application != Application.UNDEFINED
                        && app != Application.UNDEFINED
                        && !app.matches(application)) {
                    // filter out ModelLoader by application
                    continue;
                }
                final List<Artifact> artifacts = loader.listModels();
                models.compute(
                        app,
                        (key, val) -> {
                            if (val == null) {
                                val = new ArrayList<>();
                            }
                            val.addAll(artifacts);
                            return val;
                        });
            }
        }
        return models;
    }
}
