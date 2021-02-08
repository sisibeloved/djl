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
package ai.djl.mxnet.zoo.nlp.qa;

import ai.djl.Application;
import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.modality.nlp.qa.QAInput;
import ai.djl.mxnet.zoo.MxModelZoo;
import ai.djl.repository.MRL;
import ai.djl.repository.Repository;
import ai.djl.repository.zoo.BaseModelLoader;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorFactory;
import ai.djl.util.Pair;
import ai.djl.util.Progress;
import java.io.IOException;
import java.util.Map;

/**
 * Model loader for BERT QA models.
 *
 * <p>The model was trained on Gluon and loaded in DJL in MXNet Symbol Block.
 *
 * <p>See <a href="https://arxiv.org/pdf/1810.04805.pdf">the BERT paper</a> or the <a
 * href="https://github.com/awslabs/djl/blob/master/jupyter/BERTQA.ipynb">jupyter demo</a> for more
 * information about BERT.
 *
 * @see ai.djl.mxnet.engine.MxSymbolBlock
 */
public class BertQAModelLoader extends BaseModelLoader {

    private static final Application APPLICATION = Application.NLP.QUESTION_ANSWER;
    private static final String GROUP_ID = MxModelZoo.GROUP_ID;
    private static final String ARTIFACT_ID = "bertqa";
    private static final String VERSION = "0.0.1";

    /**
     * Creates the Model loader from the given repository.
     *
     * @param repository the repository to load the model from
     */
    public BertQAModelLoader(Repository repository) {
        super(repository, MRL.model(APPLICATION, GROUP_ID, ARTIFACT_ID), VERSION, new MxModelZoo());
        factories.put(new Pair<>(QAInput.class, String.class), new FactoryImpl());
    }

    /**
     * Loads the model with the given search filters.
     *
     * @param filters the search filters to match against the loaded model
     * @param device the device the loaded model should use
     * @param progress the progress tracker to update while loading the model
     * @return the loaded model
     * @throws IOException for various exceptions loading data from the repository
     * @throws ModelNotFoundException if no model with the specified criteria is found
     * @throws MalformedModelException if the model data is malformed
     */
    public ZooModel<QAInput, String> loadModel(
            Map<String, String> filters, Device device, Progress progress)
            throws IOException, ModelNotFoundException, MalformedModelException {
        Criteria<QAInput, String> criteria =
                Criteria.builder()
                        .setTypes(QAInput.class, String.class)
                        .optFilters(filters)
                        .optDevice(device)
                        .optProgress(progress)
                        .build();
        return loadModel(criteria);
    }

    private static final class FactoryImpl implements TranslatorFactory<QAInput, String> {

        /** {@inheritDoc} */
        @Override
        public Translator<QAInput, String> newInstance(Model model, Map<String, ?> arguments) {
            int seqLength;
            Object value = arguments.get("seqLength");
            if (value == null) {
                seqLength = 384;
            } else {
                seqLength = Integer.parseInt(value.toString());
            }

            return MxBertQATranslator.builder().setSeqLength(seqLength).build();
        }
    }
}
