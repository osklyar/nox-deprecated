/**
 * Created by skol on 07.03.17.
 */
package nox.internal.bundle;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.jar.Manifest;
import java.util.zip.ZipError;

import com.google.common.collect.Maps;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Analyzer;


class BundlizerImpl implements Bundlizer {

	private static final Logger logger = LoggerFactory.getLogger(BundlizerImpl.class);

	private final File targetDir;

	BundlizerImpl(File targetDir) {
		this.targetDir = targetDir;
	}

	@Override
	public File bundleJar(File originalJar, Manifest manifest) throws IOException {
		return bundleJar(originalJar, manifest, null);
	}

	@Override
	public File bundleJar(File originalJar, Manifest manifest, String classifier) throws IOException {
		String symbolicName = manifest.getMainAttributes().getValue("Bundle-SymbolicName").replaceAll(";singleton:=true", "");
		String version = manifest.getMainAttributes().getValue("Bundle-Version");
		File targetFile;
		if (StringUtils.isNotBlank(classifier)) {
			targetFile = new File(targetDir, String.format("%s.%s_%s.jar", symbolicName, classifier, version));
		} else {
			targetFile = new File(targetDir, String.format("%s_%s.jar", symbolicName, version));
		}

		FileUtils.copyFile(originalJar, targetFile);

		try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + targetFile.toURI()), Maps.newHashMap()))
		{
			Path nf = fs.getPath("META-INF");
			if (Files.notExists(nf)) {
				Files.createDirectory(nf);
			}
			nf = fs.getPath("META-INF", "MANIFEST.MF");
			try (OutputStream os = Files.newOutputStream(nf, StandardOpenOption.CREATE)) {
				manifest.write(os);
			}
		} catch (ZipError ex) {
			String message = String.format("Failed to write manifest to %s", targetFile);
			if (!manifest.getMainAttributes().getValue(Analyzer.BUNDLE_SYMBOLICNAME).endsWith(".source")) {
				throw new GradleException(message, ex);
			}
			logger.error(message + ": {}", ex.getMessage());
			return null;
		}
		return targetFile;
	}
}
