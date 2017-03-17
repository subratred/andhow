package org.yarnandtail.andhow.internal;

import org.yarnandtail.andhow.api.Problem;
import org.yarnandtail.andhow.api.ProblemList;
import org.yarnandtail.andhow.api.LoaderValues;
import org.yarnandtail.andhow.api.PropertyGroup;
import org.yarnandtail.andhow.api.ExportGroup;
import org.yarnandtail.andhow.api.EffectiveName;
import org.yarnandtail.andhow.api.ValueMapWithContext;
import org.yarnandtail.andhow.api.ValueMap;
import org.yarnandtail.andhow.api.Property;
import org.yarnandtail.andhow.api.NamingStrategy;
import org.yarnandtail.andhow.api.Loader;
import org.yarnandtail.andhow.api.Exporter;
import org.yarnandtail.andhow.api.ConstructionDefinition;
import org.yarnandtail.andhow.api.AppFatalException;
import java.util.*;
import org.yarnandtail.andhow.*;
import org.yarnandtail.andhow.name.CaseInsensitiveNaming;
import org.yarnandtail.andhow.util.ReportGenerator;

/**
 * Actual central instance of the AndHow state after a successful startup.
 * The advertised AndHow class is really a proxy for this class, and allows
 * interaction with the AndHow framework prior to startup, reloading during unit
 * testing, and (potentially) a future implementation where reloading of production
 * data would be allowed.
 * 
 * @author eeverman
 */
public class AndHowCore implements ConstructionDefinition, ValueMap {
	//User config
	private final List<Loader> loaders = new ArrayList();
	
	//Internal state
	private final ConstructionDefinition runtimeDef;
	private final ValueMapWithContext loadedValues;
	private final ProblemList<Problem> problems = new ProblemList();
	
	public AndHowCore(NamingStrategy naming, List<Loader> loaders, 
			List<Class<? extends PropertyGroup>> registeredGroups) 
			throws AppFatalException {
		
		NamingStrategy namingStrategy = (naming != null)?naming:new CaseInsensitiveNaming();
		
		if (loaders != null) {
			for (Loader loader : loaders) {
				if (! this.loaders.contains(loader)) {
					this.loaders.add(loader);
				} else {
					problems.add(new ConstructionProblem.DuplicateLoader(loader));
				}
			}
		}		
		
		//The global options are always added to the list of registered groups
		ArrayList<Class<? extends PropertyGroup>> effRegGroups = new ArrayList();
		if (registeredGroups != null) {
			effRegGroups.addAll(registeredGroups);
		}
		effRegGroups.add(Options.class);

		ConstructionDefinitionMutable startupDef = AndHowUtil.buildDefinition(effRegGroups, loaders, namingStrategy, problems);
		runtimeDef = startupDef.toImmutable();
		
		//
		//If there are ConstructionProblems, we can't continue on to attempt to
		//load values.
		if (problems.size() > 0) {
			AppFatalException afe = new AppFatalException(
				"There is a problem with the basic setup of the " + AndHow.ANDHOW_INLINE_NAME + " framework. " +
				"Since it is the framework itself that is misconfigured, no attempt was made to load values. " +
				"See System.err, out or the log files for more details.",
					problems);
			printFailedStartupDetails(afe);
			throw afe;
		}
		
		//Continuing on to load values
		loadedValues = loadValues(runtimeDef, problems).getValueMapWithContextImmutable();

		checkForRequiredValues(runtimeDef, problems);

		if (problems.size() > 0) {
			AppFatalException afe = AndHowUtil.buildFatalException(problems);
			printFailedStartupDetails(afe);
			throw afe;
		}
		
		//Export Values if applicable
		List<ExportGroup> exportGroups = runtimeDef.getExportGroups();
		for (ExportGroup eg : exportGroups) {
			Exporter exporter = eg.getExporter();
			Class<? extends PropertyGroup> group = eg.getGroup();
			
			if (group != null) {
				exporter.export(group, runtimeDef, this);
			} else {
				for (Class<? extends PropertyGroup> grp : runtimeDef.getPropertyGroups()) {
					exporter.export(grp, runtimeDef, this);
				}
			}
		}
		
		//Print samples (if requested)
		if (Options.CREATE_SAMPLES.getValue(this)) {
			ReportGenerator.printConfigSamples(runtimeDef, System.out, loaders, false);
		}
	}
	
	private void printFailedStartupDetails(AppFatalException afe) {
		ReportGenerator.printProblems(System.err, afe, runtimeDef);
		ReportGenerator.printConfigSamples(runtimeDef, System.err, loaders, true);
	}
	
	@Override
	public boolean isExplicitlySet(Property<?> prop) {
		return loadedValues.isExplicitlySet(prop);
	}
	
	@Override
	public <T> T getExplicitValue(Property<T> prop) {
		return loadedValues.getExplicitValue(prop);
	}
	
	@Override
	public <T> T getEffectiveValue(Property<T> prop) {
		return loadedValues.getEffectiveValue(prop);
	}
	
	//TODO:  Shouldn't this be stateless and pass in the loader list?
	private ValueMapWithContext loadValues(ConstructionDefinition definition, ProblemList<Problem> problems) {
		ValueMapWithContextMutable existingValues = new ValueMapWithContextMutable();

		for (Loader loader : loaders) {
			LoaderValues result = loader.load(definition, existingValues);
			existingValues.addValues(result);
			problems.addAll(result.getProblems());
		}

		return existingValues;
	}
	

	private void checkForRequiredValues(ConstructionDefinition definition, ProblemList<Problem> problems) {
		
		for (Property<?> prop : definition.getProperties()) {
			if (prop.isRequired()) {
				if (getEffectiveValue(prop) == null) {
					
					problems.add(
						new RequirementProblem.RequiredPropertyProblem(
								definition.getGroupForProperty(prop), prop));
				}
			}
		}
		
	}

	
	//
	//ConstructionDefinition Interface
	
	@Override
	public List<EffectiveName> getAliases(Property<?> property) {
		return runtimeDef.getAliases(property);
	}

	@Override
	public String getCanonicalName(Property<?> prop) {
		return runtimeDef.getCanonicalName(prop);
	}

	@Override
	public Class<? extends PropertyGroup> getGroupForProperty(Property<?> prop) {
		return runtimeDef.getGroupForProperty(prop);
	}

	@Override
	public List<Property<?>> getPropertiesForGroup(Class<? extends PropertyGroup> group) {
		return runtimeDef.getPropertiesForGroup(group);
	}

	@Override
	public Property<?> getProperty(String name) {
		return runtimeDef.getProperty(name);
	}
	
	@Override
	public List<Class<? extends PropertyGroup>> getPropertyGroups() {
		return runtimeDef.getPropertyGroups();
	}

	@Override
	public List<Property<?>> getProperties() {
		return runtimeDef.getProperties();
	}

	@Override
	public List<ExportGroup> getExportGroups() {
		return runtimeDef.getExportGroups();
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return runtimeDef.getNamingStrategy();
	}
	
	@Override
	public Map<String, String> getSystemEnvironment() {
		return runtimeDef.getSystemEnvironment();
	}
	
		
}
