/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.gradlize;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;


class MetadataExporterIvyImpl implements MetadataExporter {

	private final Bundle bundle;
	private String org;
	private final Collection<Dependency> dependencies;

	public MetadataExporterIvyImpl(Bundle bundle, String org, Collection<Dependency> dependencies) {
		this.bundle = bundle;
		this.org = org;
		this.dependencies = dependencies;
	}

	@Override
	public void exportTo(File targetDir) throws IOException {
		targetDir.mkdirs();
		File file = new File(targetDir, String.format("%s_%s.xml", bundle.name, bundle.version));
		FileUtils.writeStringToFile(file, toString(), "UTF-8", false);

	}

	@Override
	public String toString() {
		Source source = new DOMSource(toDocument());
		try (StringWriter writer = new StringWriter()) {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
			transformer.transform(source, new StreamResult(writer));
			return writer.toString();
		} catch (TransformerException | IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Document toDocument()  {
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

			Element ivyModElm = doc.createElement("ivy-module");
			doc.appendChild(ivyModElm);
			ivyModElm.setAttribute("version", "2.0");

			Element infoElm = doc.createElement("info");
			ivyModElm.appendChild(infoElm);
			infoElm.setAttribute("organisation", org);
			infoElm.setAttribute("module", bundle.name);
			infoElm.setAttribute("revision", bundle.version.toString());
			infoElm.setAttribute("status", "release");
			// infoElm.setAttribute("default", "true");

			Element confElm = doc.createElement("configurations");
			ivyModElm.appendChild(confElm);
			confElm.setAttribute("defaultconfmapping", "default");
			Element elm = doc.createElement("conf");
			confElm.appendChild(elm);
			elm.setAttribute("name", "compile");
			elm = doc.createElement("conf");
			confElm.appendChild(elm);
			elm.setAttribute("name", "default");
			elm.setAttribute("extends", "compile");

			Element depsElm = doc.createElement("dependencies");
			ivyModElm.appendChild(depsElm);
			for (Dependency dep: dependencies) {
				Element depElm = doc.createElement("dependency");
				depsElm.appendChild(depElm);
				depElm.setAttribute("org", org);
				depElm.setAttribute("name", dep.name);
				depElm.setAttribute("rev", dep.version.toString());
				depElm.setAttribute("conf", "compile->default");
			}
			return doc;
		} catch (ParserConfigurationException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
