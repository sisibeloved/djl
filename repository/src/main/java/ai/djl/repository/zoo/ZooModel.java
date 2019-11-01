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

import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.translate.Translator;
import ai.djl.util.PairList;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

public class ZooModel<I, O> implements Model {

    private Model model;
    private Translator<I, O> translator;

    public ZooModel(Model model, Translator<I, O> translator) {
        this.model = model;
        this.translator = translator;
    }

    @Override
    public void load(Path modelPath, String modelName, Map<String, String> options) {
        throw new IllegalArgumentException("ZooModel should not be re-loaded.");
    }

    @Override
    public void save(Path modelPath, String modelName) throws IOException {
        model.save(modelPath, modelName);
    }

    @Override
    public Block getBlock() {
        return model.getBlock();
    }

    @Override
    public void setBlock(Block block) {
        model.setBlock(block);
    }

    @Override
    public String getProperty(String key) {
        return null;
    }

    @Override
    public void setProperty(String key, String value) {}

    /** {@inheritDoc} */
    @Override
    public Trainer newTrainer(TrainingConfig trainingConfig) {
        return model.newTrainer(trainingConfig);
    }

    public Predictor<I, O> newPredictor() {
        return newPredictor(translator);
    }

    /** {@inheritDoc} */
    @Override
    public <P, Q> Predictor<P, Q> newPredictor(Translator<P, Q> translator) {
        return model.newPredictor(translator);
    }

    public Translator<I, O> getTranslator() {
        return translator;
    }

    public void quantize() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /** {@inheritDoc} */
    @Override
    public PairList<String, Shape> describeInput() {
        return model.describeInput();
    }

    /** {@inheritDoc} */
    @Override
    public PairList<String, Shape> describeOutput() {
        return model.describeOutput();
    }

    /** {@inheritDoc} */
    @Override
    public String[] getArtifactNames() {
        return model.getArtifactNames();
    }

    /** {@inheritDoc} */
    @Override
    public <T> T getArtifact(String name, Function<InputStream, T> function) throws IOException {
        return model.getArtifact(name, function);
    }

    /** {@inheritDoc} */
    @Override
    public URL getArtifact(String name) throws IOException {
        return model.getArtifact(name);
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getArtifactAsStream(String name) throws IOException {
        return model.getArtifactAsStream(name);
    }

    /** {@inheritDoc} */
    @Override
    public NDManager getNDManager() {
        return model.getNDManager();
    }

    /** {@inheritDoc} */
    @Override
    public void setDataType(DataType dataType) {
        model.setDataType(dataType);
    }

    /** {@inheritDoc} */
    @Override
    public DataType getDataType() {
        return model.getDataType();
    }

    /** {@inheritDoc} */
    @Override
    public void cast(DataType dataType) {
        model.cast(dataType);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        model.close();
    }
}
