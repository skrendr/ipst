package eu.itesla_project.case_projector;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;

public class CaseProjectorLoadFlowFactory implements LoadFlowFactory {

    @Override
    public LoadFlow create(Network network, ComputationManager computationManager, int priority) {

        return new CaseProjectorLoadFlow(network, computationManager, priority);
    }

}
