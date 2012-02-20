package org.jenkins.plugins.continuous_delivery;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Run;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.ResultCondition;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class JoinTriggerConfig extends BuildTriggerConfig {
	private final BlockingBehaviour block;
	private boolean buildAllNodesWithLabel;
	private final boolean folkStep;

	public boolean isFolkStep() {
		return folkStep;
	}

	public boolean isJoinStep() {
		return joinStep;
	}

	private final boolean joinStep;
	private final ParameterConfig parameterConfig;

	public ParameterConfig getParameterConfig() {
		return parameterConfig;
	}

	public boolean isTrigger() {
		return folkStep;
	}

	public JoinTriggerConfig(String projects, BlockingBehaviour block,
			List<AbstractBuildParameters> configs) {
		super(projects, ResultCondition.ALWAYS, false, configs);
		this.block = block;
		this.folkStep = false;
		this.joinStep = false;
		this.parameterConfig = new ParameterConfig(null, configs);
	}

	@DataBoundConstructor
	public JoinTriggerConfig(String projects, BlockingBehaviour block,
			ParameterConfig parameterConfig) {
		super(projects, ResultCondition.ALWAYS, false, null, null);

		this.block = block;
		this.joinStep = (block != null);
		this.folkStep = (parameterConfig != null);
		this.parameterConfig = parameterConfig;
	}

	public BlockingBehaviour getBlock() {
		return block;
	}

	@Override
	public List<Future<AbstractBuild>> perform(AbstractBuild<?, ?> build,
			Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		List<Future<AbstractBuild>> r = Collections.emptyList();
		if (folkStep) {
			r = super.perform(build, launcher, listener);
		}
		return r;
	}

	@Override
	public ListMultimap<AbstractProject, Future<AbstractBuild>> perform2(
			AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		ListMultimap<AbstractProject, Future<AbstractBuild>> futures = ArrayListMultimap
				.create();
		if (folkStep) {
			futures = super.perform2(build, launcher, listener);
		}
		return futures;
	}

	@Override
	protected Future schedule(AbstractBuild<?, ?> build,
			AbstractProject project, List<Action> list)
			throws InterruptedException, IOException {
		if (folkStep) {
			while (true) {
				// if we fail to add the item to the queue, wait and retry.
				// it also means we have to force quiet period = 0, or else
				// it'll never leave the queue
				Future f = project.scheduleBuild2(0, new UpstreamCause(
						(Run) build), list.toArray(new Action[list.size()]));
				if (f != null)
					return f;
				Thread.sleep(1000);
			}
		}
		return null;
	}

	public Collection<Node> getNodes() {
		return Hudson.getInstance().getLabel("asrt").getNodes();
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<BuildTriggerConfig> {

		@Override
		public String getDisplayName() {
			// TODO Auto-generated method stub
			return "";
		}
		
	}
}