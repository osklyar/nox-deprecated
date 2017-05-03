/**
 * Created by skol on 07.03.17.
 */
package nox.internal.bundle;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.jar.Manifest;

import org.gradle.api.artifacts.ModuleVersionIdentifier;


public interface ManifestConverter {

	static Configurator withModuleId(ModuleVersionIdentifier moduleId) throws IOException {
		return new ManifestConverterImpl(moduleId);
	}

	interface Configurator {

		Configurator withManifest(Manifest manifest);

		Configurator withClassesJarOrDir(File classesJarOrDir);

		Configurator withClasspath(Collection<File> classpath);

		Configurator withRequiredModules(Collection<ModuleVersionIdentifier> moduleIds);

		Configurator withModuleDef(ModuleDef moduleDef);

		Configurator withRuleDefs(List<RuleDef> ruleDefs);

		Configurator withUses(boolean uses);

		ManifestConverter instance();
	}

	Manifest convertToOSGiManifest() throws IOException;
}
