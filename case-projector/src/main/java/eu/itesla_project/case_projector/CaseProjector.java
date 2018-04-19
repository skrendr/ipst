/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.case_projector;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.simulation.SimulationParameters;
import com.powsybl.simulation.SimulatorFactory;
import com.powsybl.simulation.Stabilization;
import com.powsybl.simulation.StabilizationStatus;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class CaseProjector {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProjector.class);

    private static final LoadFlowParameters LOAD_FLOW_PARAMETERS = LoadFlowParameters.load();

    private static final LoadFlowParameters LOAD_FLOW_PARAMETERS2 = LoadFlowParameters.load().setNoGeneratorReactiveLimits(true);

    private final Network network;

    private final ComputationManager computationManager;

    private final LoadFlowFactory loadFlowFactory;

    private final SimulatorFactory simulatorFactory;

    private final CaseProjectorConfig config;

    private final LoadFlow loadFlow;

    private final Stabilization stabilization;

    public CaseProjector(Network network, ComputationManager computationManager, LoadFlowFactory loadFlowFactory,
                         SimulatorFactory simulatorFactory, CaseProjectorConfig config) throws Exception {
        this.network = Objects.requireNonNull(network);
        this.computationManager = Objects.requireNonNull(computationManager);
        this.loadFlowFactory = Objects.requireNonNull(loadFlowFactory);
        this.simulatorFactory = Objects.requireNonNull(simulatorFactory);
        this.config = Objects.requireNonNull(config);
        loadFlow = loadFlowFactory.create(network, computationManager, 0);
        stabilization = simulatorFactory.createStabilization(network, computationManager, 0);
        stabilization.init(SimulationParameters.load(), new HashMap<>());
    }

    static class StopException extends RuntimeException {
        public StopException(String message) {
            super(message);
        }
    }

    private void reintegrateLfState(String workingStateId) {
        reintegrateLfState(workingStateId, false);
    }

    private void reintegrateLfState(String workingStateId, boolean onlyVoltage) {
        network.getStateManager().setWorkingState(workingStateId);
        for (Generator g : network.getGenerators()) {
            Terminal t = g.getTerminal();
            if (!onlyVoltage) {
                if (!Float.isNaN(t.getP())) {
                    float oldTargetP = g.getTargetP();
                    float newTargetP = -t.getP();
                    if (oldTargetP != newTargetP) {
                        g.setTargetP(newTargetP);
                        LOGGER.debug("LF result reintegration: targetP {} -> {}", oldTargetP, newTargetP);
                    }
                }
                if (!Float.isNaN(t.getQ())) {
                    float oldTargetQ = g.getTargetQ();
                    float newTargetQ = -t.getQ();
                    if (oldTargetQ != newTargetQ) {
                        g.setTargetQ(newTargetQ);
                        LOGGER.debug("LF result reintegration: targetQ {} -> {}", oldTargetQ, newTargetQ);
                    }
                }
            }
            Bus b = t.getBusView().getBus();
            if (b != null) {
                if (!Float.isNaN(b.getV())) {
                    float oldV = g.getTargetV();
                    float newV = b.getV();
                    if (oldV != newV) {
                        g.setTargetV(newV);
                        LOGGER.debug("LF result reintegration: targetV {} -> {}", oldV, newV);
                    }
                }
            }
        }
    }

    public CompletableFuture<Boolean> project(String workingStateId) throws Exception {
        return loadFlow.runAsync(workingStateId, LOAD_FLOW_PARAMETERS)
                .thenComposeAsync(loadFlowResult -> {
                    LOGGER.debug("Pre-projector load flow metrics: {}", loadFlowResult.getMetrics());
                    if (!loadFlowResult.isOk()) {
                        throw new StopException("Pre-projector load flow diverged");
                    }
                    return AmplTaskFactory.createAmplTask(computationManager, config.getAmplHomeDir(), config.getGeneratorsDomainsFile(), network, workingStateId, config.isDebug());
                }, computationManager.getExecutor())
                .thenComposeAsync(ok -> {
                    if (!Boolean.TRUE.equals(ok)) {
                        throw new StopException("Projector failed");
                    }
                    return loadFlow.runAsync(workingStateId, LOAD_FLOW_PARAMETERS2);
                }, computationManager.getExecutor())
                .thenAcceptAsync(loadFlowResult -> {
                    LOGGER.debug("Post-projector load flow metrics: {}", loadFlowResult.getMetrics());
                    if (!loadFlowResult.isOk()) {
                        throw new StopException("Post-projector load flow diverged");
                    }
                    reintegrateLfState(workingStateId);
                }, computationManager.getExecutor())
                .thenComposeAsync(aVoid -> stabilization.runAsync(workingStateId), computationManager.getExecutor())
                .thenApplyAsync(stabilizationResult -> {
                    if (stabilizationResult.getStatus() == StabilizationStatus.COMPLETED) {
                        LOGGER.debug("Stabilization metrics: {}", stabilizationResult.getMetrics());
                        return Boolean.TRUE;
                    } else {
                        return Boolean.FALSE;
                    }
                }, computationManager.getExecutor())
                .exceptionally(throwable -> {
                    if (!(throwable instanceof CompletionException && throwable.getCause() instanceof StopException)) {
                        LOGGER.error(throwable.toString(), throwable);
                    }
                    return Boolean.FALSE;
                });
    }

}
