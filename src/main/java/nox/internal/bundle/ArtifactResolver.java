/**
 * Created by skol on 07.03.17.
 */
package nox.internal.bundle;

import java.util.Collection;

import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public interface ArtifactResolver {

	static Configurator withDependencyHelper(DependencyHandler depHandler) {
		return new ArtifactResolverImpl(depHandler);
	}

	interface Configurator {
		Configurator withConfigurationContainer(ConfigurationContainer confContainer);

		Configurator withSources(boolean withSources);

		ArtifactResolver instance();
	}


	Collection<ResolvedArtifactExt> resolve(BundleDef bundleDef);
}
