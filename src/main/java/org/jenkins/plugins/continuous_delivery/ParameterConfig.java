package org.jenkins.plugins.continuous_delivery;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;

import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class ParameterConfig extends AbstractDescribableImpl<ParameterConfig>{
    public final  List<AbstractBuildParameterFactory>  configFactories;
    public List<AbstractBuildParameterFactory> getConfigFactories() {
		return configFactories;
	}

	public List<AbstractBuildParameters> getConfigs() {
		return configs;
	}

	public final List<AbstractBuildParameters> configs;

    @DataBoundConstructor
    public ParameterConfig( List<AbstractBuildParameterFactory> configFactories,List<AbstractBuildParameters> configs) {
        this.configFactories = configFactories;
        this.configs = configs;
    }
    
    public List<Descriptor<AbstractBuildParameters>> getBuilderConfigDescriptors() {
        return Hudson.getInstance().<AbstractBuildParameters,
          Descriptor<AbstractBuildParameters>>getDescriptorList(AbstractBuildParameters.class);
    }

    public List<Descriptor<AbstractBuildParameterFactory>> getBuilderConfigFactoryDescriptors() {
        return Hudson.getInstance().<AbstractBuildParameterFactory,
          Descriptor<AbstractBuildParameterFactory>>getDescriptorList(AbstractBuildParameterFactory.class);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ParameterConfig> {
        @Override
        public String getDisplayName() {
            return ""; // unused
        }

    }

}
