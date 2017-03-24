/**
 * Created by skol on 07.03.17.
 */
package nox.internal.bundle;

import com.google.common.collect.Sets;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;

import java.io.File;
import java.util.Collection;
import java.util.Set;

public class ResolvedArtifactExt {

	public final ResolvedArtifact artifact;

	public final Set<ModuleVersionIdentifier> requiredModules = Sets.newHashSet();

	public File sourceJar = null;

	public ResolvedArtifactExt(ResolvedArtifact artifact) {
		this.artifact = artifact;
	}

	public ResolvedArtifactExt(ResolvedArtifact artifact, Collection<ModuleVersionIdentifier> requiredModules) {
		this.artifact = artifact;
		this.requiredModules.addAll(requiredModules);
	}

	public ResolvedArtifactExt withSourceJar(File sourceJar) {
		this.sourceJar = sourceJar;
		return this;
	}
}
