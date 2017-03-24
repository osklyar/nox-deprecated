/**
 * Created by skol on 07.03.17.
 */
package nox.internal.bundle;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import nox.internal.entity.Version;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.LenientConfiguration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedModuleVersion;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.DefaultResolvedArtifact;
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyArtifact;
import org.gradle.api.internal.artifacts.ivyservice.dynamicversions.DefaultResolvedModuleVersion;
import org.gradle.internal.component.external.model.DefaultModuleComponentArtifactIdentifier;
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier;
import org.gradle.internal.component.model.DefaultIvyArtifactName;
import org.gradle.internal.component.model.IvyArtifactName;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


class ArtifactResolverImpl implements ArtifactResolver, ArtifactResolver.Configurator {

	private final DependencyHandler depHandler;
	private ConfigurationContainer confContainer;
	private boolean withSources = true;

	ArtifactResolverImpl(DependencyHandler depHandler) {
		this.depHandler = depHandler;
	}

	@Override
	public Configurator withConfigurationContainer(ConfigurationContainer confContainer) {
		this.confContainer = confContainer;
		return this;
	}

	@Override
	public Configurator withSources(boolean withSources) {
		this.withSources = withSources;
		return this;
	}

	@Override
	public ArtifactResolver instance() {
		Preconditions.checkNotNull(depHandler);
		Preconditions.checkNotNull(confContainer);
		return this;
	}

	@Override
	public Collection<ResolvedArtifactExt> resolve(BundleDef bundleDef) {
		if (bundleDef.getJarFile() != null) {
			return Lists.newArrayList(wrapLocalJars(bundleDef));
		}

		Map<ModuleVersionIdentifier, ResolvedArtifactExt> resolved = Maps.newHashMap();
		Map<ModuleVersionIdentifier, ResolvedArtifact> toResolve = Maps.newHashMap();

		toResolve.putAll(getArtifacts(bundleDef.toDependencyString()));
		Preconditions.checkArgument(!toResolve.isEmpty(), "No artifacts found for %s", bundleDef.toDependencyString());

		while (!toResolve.isEmpty()) {
			ModuleVersionIdentifier moduleId = toResolve.keySet().iterator().next();
			ResolvedArtifact artifact = toResolve.remove(moduleId);

			String depString = String.format("%s:%s:%s", moduleId.getGroup(), moduleId.getName(), moduleId.getVersion());
			Map<ModuleVersionIdentifier, ResolvedArtifact> incoming = getArtifacts(depString);
			for (ModuleVersionIdentifier incomingModule : incoming.keySet()) {
				if (!resolved.containsKey(incomingModule)) {
					toResolve.put(incomingModule, incoming.get(incomingModule));
				}
			}

			// exclude self
			Set<ModuleVersionIdentifier> requiredModules = Sets.filter(incoming.keySet(),
				depId -> !(moduleId.getGroup().equals(depId.getGroup()) && moduleId.getName().equals(depId.getName())));
			ResolvedArtifactExt artifactExt = new ResolvedArtifactExt(artifact, requiredModules);
			if (withSources) {
				ResolvedArtifact sourceArtifact = getSourceArtifact(moduleId);
				if (sourceArtifact != null) {
					artifactExt.withSourceJar(sourceArtifact.getFile());
				}
			}
			resolved.put(moduleId, artifactExt);
		}
		return resolved.values();
	}

	private Map<ModuleVersionIdentifier, ResolvedArtifact> getArtifacts(String depString) {
		Dependency dep = depHandler.create(depString);
		LenientConfiguration lenientConf = confContainer
			.detachedConfiguration(dep)
			.setDescription(depString)
			.getResolvedConfiguration()
			.getLenientConfiguration();
		Map<ModuleVersionIdentifier, ResolvedArtifact> res = Maps.newHashMap();
		for (ResolvedArtifact artifact : lenientConf.getArtifacts(spec -> true)) {
			ModuleVersionIdentifier moduleId = artifact.getModuleVersion().getId();
			res.put(moduleId, artifact);
		}
		return res;
	}

	private ResolvedArtifact getSourceArtifact(ModuleVersionIdentifier moduleId) {
		String depString = String.format("%s:%s:%s", moduleId.getGroup(), moduleId.getName(), moduleId.getVersion());
		ModuleDependency dep = (ModuleDependency) depHandler.create(depString);

		DefaultDependencyArtifact sourceDep = new DefaultDependencyArtifact(dep.getName(), "source", "jar", "sources", null);
		dep = dep.addArtifact(sourceDep).setTransitive(false);
		LenientConfiguration lenientConf = confContainer
			.detachedConfiguration(dep)
			.setDescription("sources")
			.getResolvedConfiguration()
			.getLenientConfiguration();
		Set<ResolvedArtifact> res = lenientConf.getArtifacts(spec -> true);
		if (res.isEmpty()) {
			return null;
		} else if (res.size() == 1) {
			return res.iterator().next();
		}
		throw new GradleException(String.format("Found more than 1 source artifact for %s: %s", moduleId, res));
	}

	private ResolvedArtifactExt wrapLocalJars(BundleDef bundleDef) {
		String version = new Version(bundleDef.getVersion()).toString();
		String groupId = bundleDef.getGroupId();
		String artifactId = bundleDef.getArtifactId();

		ModuleVersionIdentifier moduleId = new DefaultModuleVersionIdentifier(groupId, artifactId, version);

		ResolvedModuleVersion owner = new DefaultResolvedModuleVersion(moduleId);
		IvyArtifactName ivyArtifactName = new DefaultIvyArtifactName(artifactId, "jar", "jar");
		ModuleComponentIdentifier modCompId = new DefaultModuleComponentIdentifier(groupId, artifactId, version);
		ComponentArtifactIdentifier compArtifactId = new DefaultModuleComponentArtifactIdentifier(modCompId, ivyArtifactName);
		ResolvedArtifact artifact = new DefaultResolvedArtifact(owner, ivyArtifactName, compArtifactId, () -> bundleDef.getJarFile());
		return new ResolvedArtifactExt(artifact).withSourceJar(bundleDef.getSourceJarFile());
	}
}
