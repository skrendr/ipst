/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.cases;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import eu.itesla_project.computation.ComputationManager;
import eu.itesla_project.iidm.datasource.DataSource;
import eu.itesla_project.iidm.datasource.DataSourceFactory;
import eu.itesla_project.iidm.datasource.GenericReadOnlyDataSource;
import eu.itesla_project.iidm.import_.Importer;
import eu.itesla_project.iidm.import_.Importers;
import eu.itesla_project.iidm.network.Country;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.ucte.util.UcteFileName;
import eu.itesla_project.ucte.util.UcteGeographicalCode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Common ENTSOE case repository layout:
 * <pre>
 * CIM/SN/2013/01/15/20130115_0620_SN2_FR0.zip
 *    /FO/...
 * UCT/SN/...
 *    /FO/...
 * </pre>
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EntsoeCaseRepository implements CaseRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntsoeCaseRepository.class);

    static class EntsoeFormat {

        private final Importer importer;

        private final String dirName;

        EntsoeFormat(Importer importer, String dirName) {
            this.importer = Objects.requireNonNull(importer);
            this.dirName = Objects.requireNonNull(dirName);
        }

        Importer getImporter() {
            return importer;
        }

        String getDirName() {
            return dirName;
        }
    }

    private final EntsoeCaseRepositoryConfig config;

    private final List<EntsoeFormat> formats;

    private final DataSourceFactory dataSourceFactory;

    public static CaseRepository create(ComputationManager computationManager) {
        return new EntsoeCaseRepository(EntsoeCaseRepositoryConfig.load(), computationManager);
    }

    EntsoeCaseRepository(EntsoeCaseRepositoryConfig config, List<EntsoeFormat> formats, DataSourceFactory dataSourceFactory) {
        this.config = Objects.requireNonNull(config);
        this.formats = Objects.requireNonNull(formats);
        this.dataSourceFactory = Objects.requireNonNull(dataSourceFactory);
        LOGGER.info(config.toString());
    }

    EntsoeCaseRepository(EntsoeCaseRepositoryConfig config, ComputationManager computationManager) {
        this(config,
                Arrays.asList(new EntsoeFormat(Importers.getImporter("CIM1", computationManager), "CIM"),
                              new EntsoeFormat(Importers.getImporter("UCTE", computationManager), "UCT")), // official ENTSOE formats)
                (directory, baseName) -> new GenericReadOnlyDataSource(directory, baseName));
    }

    public EntsoeCaseRepositoryConfig getConfig() {
        return config;
    }

    private static class ImportContext {
        private final Importer importer;
        private final DataSource ds;

        private ImportContext(Importer importer, DataSource ds) {
            this.importer = importer;
            this.ds = ds;
        }
    }

    // because D1 snapshot does not exist and forecast replacement is not yet implemented
    private static Collection<UcteGeographicalCode> forCountryHacked(Country country) {
        return UcteGeographicalCode.forCountry(country).stream()
                .filter(ucteGeographicalCode -> ucteGeographicalCode != UcteGeographicalCode.D1)
                .collect(Collectors.toList());
    }

    private <R> R scanRepository(DateTime date, CaseType type, Country country, Function<List<ImportContext>, R> handler) {
        Collection<UcteGeographicalCode> geographicalCodes = country != null ? forCountryHacked(country)
                                                                             : Collections.singleton(UcteGeographicalCode.UX);
        for (EntsoeFormat format : formats) {
            Path formatDir = config.getRootDir().resolve(format.getDirName());
            if (Files.exists(formatDir)) {
                Path typeDir = formatDir.resolve(type.name());
                if (Files.exists(typeDir)) {
                    Path dayDir = typeDir.resolve(String.format("%04d", date.getYear()))
                            .resolve(String.format("%02d", date.getMonthOfYear()))
                            .resolve(String.format("%02d", date.getDayOfMonth()));
                    if (Files.exists(dayDir)) {
                        List<ImportContext> importContexts = null;
                        for (UcteGeographicalCode geographicalCode : geographicalCodes) {
                            Collection<String> forbiddenFormats = config.getForbiddenFormatsByGeographicalCode().get(geographicalCode);
                            if (!forbiddenFormats.contains(format.getImporter().getFormat())) {
                                for (int i = 9; i >= 0; i--) {
                                    String baseName = String.format("%04d%02d%02d_%02d%02d_" + type + "%01d_" + geographicalCode.name() + "%01d",
                                            date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), date.getHourOfDay(), date.getMinuteOfHour(),
                                            date.getDayOfWeek(), i);
                                    DataSource ds = dataSourceFactory.create(dayDir, baseName);
                                    if (importContexts == null) {
                                        importContexts = new ArrayList<>();
                                    }
                                    if (format.getImporter().exists(ds)) {
                                        importContexts.add(new ImportContext(format.getImporter(), ds));
                                    }
                                }
                            }
                        }
                        if (importContexts != null) {
                            R result = handler.apply(importContexts);
                            if (result != null) {
                                return result;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static DateTime toCetDate(DateTime date) {
        DateTimeZone CET = DateTimeZone.forID("CET");
        if (!date.getZone().equals(CET)) {
            return date.toDateTime(CET);
        }
        return date;
    }

    @Override
    public List<Network> load(DateTime date, CaseType type, Country country) {
        Objects.requireNonNull(date);
        Objects.requireNonNull(type);
        List<Network> networks2 = scanRepository(toCetDate(date), type, country, importContexts -> {
            List<Network> networks = null;
            if (importContexts.size() > 0) {
                networks = new ArrayList<>();
                for (ImportContext importContext : importContexts) {
                    LOGGER.info("Loading {} in {} format", importContext.ds.getBaseName(), importContext.importer.getFormat());
                    networks.add(importContext.importer.import_(importContext.ds, null));
                }
            }
            return networks;
        });
        return networks2 == null ? Collections.emptyList() : networks2;
    }

	@Override
	public boolean isDataAvailable(DateTime date, CaseType type, Country country) {
		return isNetworkDataAvailable(date, type, country);
	}

	@Override
	public Map<Country, Boolean> dataAvailable(DateTime date, CaseType type, Country country) {
		Map<Country, Boolean> dataAvailable = new HashMap<>();
		dataAvailable.put(country, isNetworkDataAvailable(date, type, country));
		return dataAvailable;
	}
	
	private boolean isNetworkDataAvailable(DateTime date, CaseType type, Country country) {
		Objects.requireNonNull(date);
        Objects.requireNonNull(type);
        return scanRepository(toCetDate(date), type, country, importContexts -> {
            if (importContexts.size() > 0) {
                for (ImportContext importContext : importContexts) {
                    if (importContext.importer.exists(importContext.ds)) {
                        return true;
                    }
                }
                return null;
            }
            return null;
        }) != null;
	}

    private void browse(Path dir, Consumer<Path> handler) {
        try (Stream<Path> stream = Files.list(dir)) {
            stream.sorted().forEach(child -> {
                if (Files.isDirectory(child)) {
                    browse(child, handler);
                } else {
                    try {
                        if (Files.size(child) > 0) {
                            handler.accept(child);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<DateTime> dataAvailable(CaseType type, Set<Country> countries, Interval interval) {
        Set<UcteGeographicalCode> geographicalCodes = new HashSet<>();
        if (countries == null) {
            geographicalCodes.add(UcteGeographicalCode.UX);
        } else {
            for (Country country : countries) {
                geographicalCodes.addAll(forCountryHacked(country));
            }
        }
        Multimap<DateTime, UcteGeographicalCode> dates = HashMultimap.create();
        for (EntsoeFormat format : formats) {
            Path formatDir = config.getRootDir().resolve(format.getDirName());
            if (Files.exists(formatDir)) {
                Path typeDir = formatDir.resolve(type.name());
                if (Files.exists(typeDir)) {
                    browse(typeDir, path -> {
                        UcteFileName ucteFileName = UcteFileName.parse(path.getFileName().toString());
                        UcteGeographicalCode geographicalCode = ucteFileName.getGeographicalCode();
                        if (geographicalCode != null
                                && !config.getForbiddenFormatsByGeographicalCode().get(geographicalCode).contains(format.getImporter().getFormat())
                                && interval.contains(ucteFileName.getDate())) {
                            dates.put(ucteFileName.getDate(), geographicalCode);
                        }
                    });
                }
            }
        }
        return dates.asMap().entrySet().stream()
                .filter(e -> new HashSet<>(e.getValue()).containsAll(geographicalCodes))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}