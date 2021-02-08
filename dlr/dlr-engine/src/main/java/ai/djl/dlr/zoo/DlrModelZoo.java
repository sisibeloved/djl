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
package ai.djl.dlr.zoo;

import ai.djl.dlr.engine.DlrEngine;
import ai.djl.modality.cv.zoo.ImageClassificationModelLoader;
import ai.djl.repository.Repository;
import ai.djl.repository.zoo.ModelZoo;
import java.util.Collections;
import java.util.Set;

/** DlrModelZoo is a repository that contains all dlr models for DJL. */
public class DlrModelZoo implements ModelZoo {

    private static final String DJL_REPO_URL = "https://mlrepo.djl.ai/";
    private static final Repository REPOSITORY = Repository.newInstance("Dlr", DJL_REPO_URL);
    public static final String GROUP_ID = "ai.djl.dlr";
    private static final DlrModelZoo ZOO = new DlrModelZoo();

    public static final ImageClassificationModelLoader RESNET =
            new ImageClassificationModelLoader(REPOSITORY, GROUP_ID, "resnet", "0.0.1", ZOO);

    /** {@inheritDoc} */
    @Override
    public String getGroupId() {
        return GROUP_ID;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getSupportedEngines() {
        return Collections.singleton(DlrEngine.ENGINE_NAME);
    }
}
