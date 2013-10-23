/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.core.util.openxml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.image.Size;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 
 * Initial date: 04.09.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class OpenXMLUtils {
	
	private static final OLog log = Tracing.createLoggerFor(OpenXMLUtils.class);

	public static final double emusPerInch = 914400.0d;
	
	public static final Size convertPixelToEMUs(Size img, int dpi) {
		int widthPx = img.getWidth();
		int heightPx = img.getHeight();
		double horzRezDpi = dpi * 1.0d;
		double vertRezDpi = dpi * 1.0d;
		
		double widthEmus = (widthPx / horzRezDpi) * emusPerInch;
		double heightEmus = (heightPx / vertRezDpi) * emusPerInch;
		
		return new Size((int)widthEmus, (int)heightEmus, true);
	}
	
	public static int getSpanAttribute(String name, Attributes attrs) {
		name = name.toLowerCase();
		int span = -1;
		for(int i=attrs.getLength(); i-->0; ) {
			String attrName = attrs.getQName(i);
			if(name.equals(attrName.toLowerCase())) {
				String val = attrs.getValue(i);
				if(StringHelper.isLong(val)) {
					return Integer.parseInt(val);
				}
			}	
		}
		return span < 1 ? 1 : span;
	}
	
	public static boolean contains(Node parent, String nodeName) {
		boolean found = false;
		for(Node node=parent.getFirstChild(); node!=null; node=node.getNextSibling()) {
			if(nodeName.equals(node.getNodeName())) {
				found = true;
			}
		}
		return found;
	}
	
	public static final Document createDocument() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// Turn on validation, and turn on namespaces
			factory.setValidating(true);
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			return doc;
		} catch (ParserConfigurationException e) {
			log.error("", e);
			return null;
		}
	}
	
	/**
	 * Create a document from a template, validation is turned off. The
	 * method doesn't close the input stream.
	 * @param in
	 * @return
	 */
	public static final Document createDocument(InputStream in) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// Turn on validation, and turn on namespaces
			factory.setValidating(false);
			factory.setNamespaceAware(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(in);
			return doc;
		} catch (ParserConfigurationException e) {
			log.error("", e);
			return null;
		} catch (IOException e) {
			log.error("", e);
			return null;
		} catch (SAXException e) {
			log.error("", e);
			return null;
		}
	}
	
	public static final Document createDocument(String in) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// Turn on validation, and turn on namespaces
			factory.setValidating(false);
			factory.setNamespaceAware(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(in)));
			return doc;
		} catch (ParserConfigurationException e) {
			log.error("", e);
			return null;
		} catch (IOException e) {
			log.error("", e);
			return null;
		} catch (SAXException e) {
			log.error("", e);
			return null;
		}
	}
	
	public static final void writeTo(Document document, OutputStream out, boolean indent) {
		try {
			// Use a Transformer for output
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			if(indent) {
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			}

			DOMSource source = new DOMSource(document);
			Result result = new StreamResult(out);
			transformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			log.error("", e);
		} catch (TransformerFactoryConfigurationError e) {
			log.error("", e);
		} catch (TransformerException e) {
			log.error("", e);
		}
	}

}
