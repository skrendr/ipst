/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.case_projector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.powsybl.ampl.converter.AmplExportConfig;
import com.powsybl.ampl.converter.AmplNetworkReader;
import com.powsybl.ampl.converter.AmplNetworkWriter;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.converter.AmplUtil;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.computation.AbstractExecutionHandler;
import com.powsybl.computation.Command;
import com.powsybl.computation.CommandExecution;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.ExecutionEnvironment;
import com.powsybl.computation.ExecutionReport;
import com.powsybl.computation.SimpleCommandBuilder;
import com.powsybl.iidm.network.Network;

/**
* @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
*/
public final class AmplTaskFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmplTaskFactory.class);
    private static final String WORKING_DIR_PREFIX = "itesla_projector_";
    private static final Set<String> AMPL_MODEL_FILE_NAMES = ImmutableSet.<String>builder()
            .add("projector.run")
            .add("projector.mod")
            .add("projector.dat")
            .add("projectorOutput.run")
            .build();

    private static final String AMPL_GENERATORS_DOMAINS_FILE_NAME = "ampl_generators_domains.txt";

    private AmplTaskFactory() {
    }

    static CompletableFuture<Boolean> createAmplTask(ComputationManager computationManager, Path amplHome,
            Path generatorsDomains, Network network, String workingStateId, boolean debug) {
        Objects.requireNonNull(computationManager);
        Objects.requireNonNull(amplHome);
        Objects.requireNonNull(generatorsDomains);
        Objects.requireNonNull(network);

        return computationManager.execute(
                new ExecutionEnvironment(ImmutableMap.of("PATH", amplHome.toString()), WORKING_DIR_PREFIX, debug),
                new AbstractExecutionHandler<Boolean>() {

                    private StringToIntMapper<AmplSubset> mapper;

                    @Override
                    public List<CommandExecution> before(Path workingDir) throws IOException {
                        if (workingStateId != null) {
                            network.getStateManager().setWorkingState(workingStateId);
                        }

                        // copy AMPL model
                        for (String amplModelFileName : AMPL_MODEL_FILE_NAMES) {
                            Files.copy(getClass().getResourceAsStream("/ampl/projector/" + amplModelFileName),
                                    workingDir.resolve(amplModelFileName));
                        }

                        // copy the generators domains file
                        Files.copy(generatorsDomains, workingDir.resolve(AMPL_GENERATORS_DOMAINS_FILE_NAME));

                        // write input data
                        mapper = AmplUtil.createMapper(network);
                        mapper.dump(workingDir.resolve("mapper.csv"));

                        new AmplNetworkWriter(network, new FileDataSource(workingDir, "ampl"),
                                mapper, new AmplExportConfig(AmplExportConfig.ExportScope.ALL, true,
                                        AmplExportConfig.ExportActionType.CURATIVE)).write();

                        Command command = new SimpleCommandBuilder().id("projector")
                                .program(amplHome.resolve("ampl").toString()).args("projector.run").build();
                        return Arrays.asList(new CommandExecution(command, 1, 0));
                    }

                    @Override
                    public Boolean after(Path workingDir, ExecutionReport report) throws IOException {
                        report.log();

                        if (report.getErrors().isEmpty()) {
                            if (workingStateId != null) {
                                network.getStateManager().setWorkingState(workingStateId);
                            }

                            Map<String, String> metrics = new HashMap<>();
                            new AmplNetworkReader(
                                    new FileDataSource(workingDir, "projector_results"/* "ampl_network_" */), network, mapper)
                                        .readBuses()
                                        .readGenerators()
                                        .readBranches()
                                        .readLoads()
                                        .readPhaseTapChangers()
                                        .readRatioTapChangers()
                                        .readShunts()
                                        .readStaticVarcompensator()
                                        .readHvdcLines()
                                        .readLccConverterStations()
                                        .readVscConverterStations()
                                        .readMetrics(metrics);

                            LOGGER.debug("Projector metrics: {}", metrics);
                        }

                        return report.getErrors().isEmpty();
                    }
                });
    }
}
