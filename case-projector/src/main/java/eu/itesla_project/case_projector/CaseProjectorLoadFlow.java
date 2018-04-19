/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.case_projector;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowResultImpl;

/**
* @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
*/
public class CaseProjectorLoadFlow implements LoadFlow {

    private final Network network;
    private final ComputationManager computationManager;

    public CaseProjectorLoadFlow(Network network, ComputationManager computationManager, int priority) {
        this.network = Objects.requireNonNull(network);
        this.computationManager = Objects.requireNonNull(computationManager);
    }

    @Override
    public String getName() {
        return "ampl-load-flow";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public LoadFlowResult run(LoadFlowParameters params) throws Exception {
        CaseProjectorLoadFlowParameters amplParams = params.getExtension(CaseProjectorLoadFlowParameters.class);
        Path generatorsDomains = amplParams.getGeneratorsDomainsFile();
        Map<String, String> metrics = new HashMap<>();
        CompletableFuture<Boolean> result = AmplTaskFactory.createAmplTask(computationManager, amplParams.getAmplHomeDir(), generatorsDomains, network, network.getStateManager().getWorkingStateId(), amplParams.isDebug());
        Boolean wres = result.join();
        return new LoadFlowResultImpl(wres, metrics, null);
    }

}
