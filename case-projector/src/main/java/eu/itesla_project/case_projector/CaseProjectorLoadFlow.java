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
