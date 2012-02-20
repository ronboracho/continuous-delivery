package org.jenkins.plugins.continuous_delivery;

import hudson.Extension;
import hudson.Launcher;
import hudson.console.HyperlinkNote;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.parameterizedtrigger.BuildInfoExporterAction;
import hudson.plugins.parameterizedtrigger.ParameterizedDependency;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.IOException2;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class PipelineManager extends Builder implements DependecyDeclarer {

	private List<JoinTriggerConfig> configs;

	@DataBoundConstructor
	public PipelineManager(final List<JoinTriggerConfig> configs) {
		this.configs = configs;
	}

	public PipelineManager(JoinTriggerConfig... configs) {
		this(Arrays.asList(configs));
	}

	public List<JoinTriggerConfig> getConfigs() {
		return configs;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		boolean buildStepResult = true;
		try {
			listener.getLogger().println("Triggering Pipeline-Manger");

			ListMultimap<AbstractProject, Future<AbstractBuild>> futures = ArrayListMultimap
					.create();

			for (JoinTriggerConfig config : getConfigs()) {
				if (config.isFolkStep()) {
					listener.getLogger().println(
							"Triggering job " + config.getProjects());
					futures.putAll(config.perform2(build, launcher, listener));
				}
				if (config.isJoinStep()) {
					listener.getLogger().println(
							"Joining jobs " + config.getProjects());
					List<AbstractProject> joiningProjects = config
							.getProjectList(build.getEnvironment(listener));
					for (AbstractProject project : joiningProjects) {
						List<Future<AbstractBuild>> futureList = futures
								.get(project);
						for (Future<AbstractBuild> future : futureList) {

							AbstractBuild b = future.get();
							listener.getLogger().println(
									HyperlinkNote.encodeTo('/' + b.getUrl(),
											b.getFullDisplayName())
											+ " completed. Result was "
											+ b.getResult());
							build.getActions().add(
									new BuildInfoExporterAction(b.getProject()
											.getFullName(), b.getNumber()));

							if (buildStepResult
									&& config.getBlock().mapBuildStepResult(
											b.getResult())) {
								build.setResult(config.getBlock()
										.mapBuildResult(b.getResult()));
							} else {
								buildStepResult = false;
							}
						}

					}
				}
			}
		} catch (ExecutionException e) {
			throw new IOException2(e); // can't happen, I think.
		}

		return buildStepResult;
	}

	@Extension(ordinal = Integer.MAX_VALUE - 500)
	public static class PipelineBuilderDescriptor extends
			BuildStepDescriptor<Builder> {

		@Override
		public String getDisplayName() {
			return "Pipeline Manager";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {

			return true;
		}
	}

	public void buildDependencyGraph(AbstractProject owner,
			DependencyGraph graph) {
		for (JoinTriggerConfig config : configs)
			for (AbstractProject project : config.getProjectList(
					owner.getParent(), null))
				graph.addDependency(new ParameterizedDependency(owner, project,
						config) {
					@Override
					public boolean shouldTriggerBuild(AbstractBuild build,
							TaskListener listener, List<Action> actions) {
						// TriggerBuilders are inline already.
						return false;
					}
				});

	}

}
