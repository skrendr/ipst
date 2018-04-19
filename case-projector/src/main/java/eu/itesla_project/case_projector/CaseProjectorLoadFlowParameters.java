package eu.itesla_project.case_projector;

import java.nio.file.Path;
import java.util.Objects;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.loadflow.LoadFlowParameters;


public class CaseProjectorLoadFlowParameters extends AbstractExtension<LoadFlowParameters> {

    private Path amplHomeDir;
    private Path generatorsDomainsFile;
    private boolean debug;

    public CaseProjectorLoadFlowParameters(PlatformConfig config) {
        Objects.requireNonNull(config);

        if (config.moduleExists("caseProjector")) {
            ModuleConfig amplConfig = config.getModuleConfig("caseProjector");
            this.amplHomeDir = amplConfig.getPathProperty("amplHomeDir");
            this.generatorsDomainsFile =  amplConfig.getPathProperty("generatorsDomainsFile");
            this.debug = amplConfig.getBooleanProperty("debug");
        }
    }

    public CaseProjectorLoadFlowParameters(Path amplHomeDir, Path generatorsDomainsFile, boolean debug) {
        this.amplHomeDir = Objects.requireNonNull(amplHomeDir);
        this.generatorsDomainsFile = Objects.requireNonNull(generatorsDomainsFile);
        this.debug = debug;
    }

    @AutoService(LoadFlowParameters.ConfigLoader.class)
    public static class AmplLoadFlowConfigLoader implements LoadFlowParameters.ConfigLoader<CaseProjectorLoadFlowParameters> {

        @Override
        public CaseProjectorLoadFlowParameters load(PlatformConfig config) {
            return new CaseProjectorLoadFlowParameters(config);
        }

        @Override
        public String getCategoryName() {
            return "loadflow-parameters";
        }

        @Override
        public Class<? super CaseProjectorLoadFlowParameters> getExtensionClass() {
            return CaseProjectorLoadFlowParameters.class;
        }

        @Override
        public String getExtensionName() {
            return "ampl";
        }
    }

    @Override
    public String getName() {
        return "ampl";
    }

    public Path getAmplHomeDir() {
        return amplHomeDir;
    }

    public boolean isDebug() {
        return debug;
    }

    public Path getGeneratorsDomainsFile() {
        return generatorsDomainsFile;
    }

}
