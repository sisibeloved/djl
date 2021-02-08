/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package ai.djl.serving.wlm;

import ai.djl.modality.Input;
import ai.djl.modality.Output;
import ai.djl.repository.zoo.ZooModel;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A class represent a loaded model and it's metadata. */
public final class ModelInfo implements AutoCloseable, Cloneable {

    private static final Logger logger = LoggerFactory.getLogger(ModelInfo.class);

    private String modelName;
    private String modelUrl;

    private int minWorkers;
    private int maxWorkers;
    private int queueSize;
    private int batchSize;
    private int maxBatchDelay;
    private int maxIdleTime;

    private ZooModel<Input, Output> model;

    /**
     * Constructs a new {@code ModelInfo} instance.
     *
     * @param modelName the name of the model that will be used as HTTP endpoint
     * @param modelUrl the model url
     * @param model the {@link ZooModel}
     * @param queueSize the maximum request queue size
     * @param maxIdleTime the initial maximum idle time for workers.
     * @param maxBatchDelay the initial maximum delay when scaling up before giving up.
     * @param batchSize the batch size for this model.
     */
    public ModelInfo(
            String modelName,
            String modelUrl,
            ZooModel<Input, Output> model,
            int queueSize,
            int maxIdleTime,
            int maxBatchDelay,
            int batchSize) {
        this.modelName = modelName;
        this.modelUrl = modelUrl;
        this.model = model;
        this.maxBatchDelay = maxBatchDelay;
        this.maxIdleTime = maxIdleTime; // default max idle time 60s
        this.queueSize = queueSize;
        this.batchSize = batchSize;
    }

    /**
     * Sets a new batchSize and returns a new configured ModelInfo object. You have to
     * triggerUpdates in the {@code ModelManager} using this new model.
     *
     * @param batchSize the batchSize to set
     * @return new configured ModelInfo.
     */
    public ModelInfo configureModelBatch(int batchSize) {
        ModelInfo clone;
        try {
            clone = (ModelInfo) this.clone();
            clone.batchSize = batchSize;
        } catch (CloneNotSupportedException e) {
            // this should never happen, cause we know we are cloneable.
            throw new AssertionError(e);
        }
        return clone;
    }

    /**
     * Sets new workers capcities for this model and returns a new configured ModelInfo object. You
     * have to triggerUpdates in the {@code ModelManager} using this new model.
     *
     * @param minWorkers minimum amount of workers.
     * @param maxWorkers maximum amount of workers.
     * @return new configured ModelInfo.
     */
    public ModelInfo scaleWorkers(int minWorkers, int maxWorkers) {
        ModelInfo clone;
        try {
            clone = (ModelInfo) this.clone();
            clone.minWorkers = minWorkers;
            clone.maxWorkers = maxWorkers;
        } catch (CloneNotSupportedException e) {
            // this should never happen, cause we know we are cloneable.
            throw new AssertionError(e);
        }
        return clone;
    }

    /**
     * Sets new configuration for the workerPool backing this model and returns a new configured
     * ModelInfo object. You have to triggerUpdates in the {@code ModelManager} using this new
     * model.
     *
     * @param maxIdleTime time a WorkerThread can be idle before scaling down this worker.
     * @param maxBatchDelay maximum time to wait for a free space in worker queue after scaling up
     *     workers before giving up to offer the job to the queue.
     * @return new configured ModelInfo.
     */
    public ModelInfo configurePool(int maxIdleTime, int maxBatchDelay) {
        ModelInfo clone;
        try {
            clone = (ModelInfo) this.clone();
            clone.maxIdleTime = maxIdleTime;
            clone.maxBatchDelay = maxBatchDelay;
        } catch (CloneNotSupportedException e) {
            // ..ignore cause we know we are cloneable.
            clone = this; // for the compiler
        }
        return clone;
    }

    /**
     * Returns the loaded {@link ZooModel}.
     *
     * @return the loaded {@link ZooModel}
     */
    public ZooModel<Input, Output> getModel() {
        return model;
    }

    /**
     * Returns the model name.
     *
     * @return the model name
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Returns the model url.
     *
     * @return the model url
     */
    public String getModelUrl() {
        return modelUrl;
    }

    /**
     * Returns the model cache directory.
     *
     * @return the model cache directory
     */
    public Path getModelDir() {
        return model.getModelPath();
    }

    /**
     * returns the configured maxIdleTime of workers.
     *
     * @return the maxIdleTime
     */
    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    /**
     * Returns the configured minimum number of workers.
     *
     * @return the configured minimum number of workers
     */
    public int getMinWorkers() {
        return minWorkers;
    }

    /**
     * Returns the configured maximum number of workers.
     *
     * @return the configured maximum number of workers
     */
    public int getMaxWorkers() {
        return maxWorkers;
    }

    /**
     * Returns the configured batch size.
     *
     * @return the configured batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Returns the maximum delay in milliseconds to aggregate a batch.
     *
     * @return the maximum delay in milliseconds to aggregate a batch
     */
    public int getMaxBatchDelay() {
        return maxBatchDelay;
    }

    /**
     * returns the configured size of the workers queue.
     *
     * @return requested size of the workers queue.
     */
    public int getQueueSize() {
        return queueSize;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        if (model != null) {
            logger.debug("closing model {}", modelName);
            model.close();
        }
    }
}
