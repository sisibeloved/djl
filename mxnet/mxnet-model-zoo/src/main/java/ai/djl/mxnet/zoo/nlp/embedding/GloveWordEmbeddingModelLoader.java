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
package ai.djl.mxnet.zoo.nlp.embedding;

import ai.djl.Application;
import ai.djl.Application.NLP;
import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.modality.nlp.SimpleVocabulary;
import ai.djl.modality.nlp.embedding.TrainableWordEmbedding;
import ai.djl.modality.nlp.embedding.WordEmbedding;
import ai.djl.mxnet.zoo.MxModelZoo;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.nn.core.Embedding;
import ai.djl.repository.Artifact;
import ai.djl.repository.MRL;
import ai.djl.repository.Repository;
import ai.djl.repository.zoo.BaseModelLoader;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.translate.TranslatorFactory;
import ai.djl.util.Pair;
import ai.djl.util.Utils;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A {@link ai.djl.repository.zoo.ModelLoader} for a {@link WordEmbedding} based on <a
 * href="https://nlp.stanford.edu/projects/glove/">GloVe</a>.
 */
public class GloveWordEmbeddingModelLoader extends BaseModelLoader {

    private static final Application APPLICATION = NLP.WORD_EMBEDDING;
    private static final String GROUP_ID = MxModelZoo.GROUP_ID;
    private static final String ARTIFACT_ID = "glove";
    private static final String VERSION = "0.0.2";

    /**
     * Constructs a {@link GloveWordEmbeddingModelLoader} given the repository.
     *
     * @param repository the repository to load the model from
     */
    public GloveWordEmbeddingModelLoader(Repository repository) {
        super(repository, MRL.model(APPLICATION, GROUP_ID, ARTIFACT_ID), VERSION, new MxModelZoo());
        factories.put(new Pair<>(String.class, NDList.class), new FactoryImpl());
    }

    private Model customGloveBlock(Model model, Artifact artifact, Map<String, Object> arguments)
            throws IOException {
        List<String> idxToToken =
                Utils.readLines(
                        resource.getRepository()
                                .openStream(artifact.getFiles().get("idx_to_token"), null));
        TrainableWordEmbedding wordEmbedding =
                TrainableWordEmbedding.builder()
                        .setEmbeddingSize(
                                Integer.parseInt(artifact.getProperties().get("dimensions")))
                        .setVocabulary(new SimpleVocabulary(idxToToken))
                        .optUnknownToken((String) arguments.get("unknownToken"))
                        .optUseDefault(true)
                        .optSparseGrad(false)
                        .build();
        model.setBlock(wordEmbedding);
        model.setProperty("unknownToken", (String) arguments.get("unknownToken"));
        return model;
    }

    /** {@inheritDoc} */
    @Override
    protected Model createModel(
            String name,
            Device device,
            Artifact artifact,
            Map<String, Object> arguments,
            String engine)
            throws IOException {
        Model model = Model.newInstance(name, device, engine);
        return customGloveBlock(model, artifact, arguments);
    }

    /**
     * Loads the model with the given search filters.
     *
     * @return the loaded model
     * @throws IOException for various exceptions loading data from the repository
     * @throws ModelNotFoundException if no model with the specified criteria is found
     * @throws MalformedModelException if the model data is malformed
     */
    public ZooModel<NDList, NDList> loadModel()
            throws IOException, ModelNotFoundException, MalformedModelException {
        Criteria<NDList, NDList> criteria =
                Criteria.builder()
                        .setTypes(NDList.class, NDList.class)
                        .optApplication(NLP.WORD_EMBEDDING)
                        .build();
        return loadModel(criteria);
    }

    private static final class FactoryImpl implements TranslatorFactory<String, NDList> {

        /** {@inheritDoc} */
        @Override
        public Translator<String, NDList> newInstance(Model model, Map<String, ?> arguments) {
            String unknownToken = (String) arguments.get("unknownToken");
            return new TranslatorImpl(unknownToken);
        }
    }

    private static final class TranslatorImpl implements Translator<String, NDList> {

        private String unknownToken;
        private Embedding<String> embedding;

        public TranslatorImpl(String unknownToken) {
            this.unknownToken = unknownToken;
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("unchecked")
        public void prepare(NDManager manager, Model model) {
            try {
                embedding = (Embedding<String>) model.getBlock();
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("The model was not an embedding", e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public NDList processOutput(TranslatorContext ctx, NDList list) {
            return list;
        }

        /** {@inheritDoc} */
        @Override
        public NDList processInput(TranslatorContext ctx, String input) {
            if (embedding.hasItem(input)) {
                return new NDList(ctx.getNDManager().create(embedding.embed(input)));
            } else {
                return new NDList(ctx.getNDManager().create(embedding.embed(unknownToken)));
            }
        }

        /** {@inheritDoc} */
        @Override
        public Batchifier getBatchifier() {
            return Batchifier.STACK;
        }
    }
}
