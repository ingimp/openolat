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
package org.olat.ims.qti21.model.xml;

import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.olat.imscp.xml.manifest.ManifestMetadataType;
import org.olat.imscp.xml.manifest.MetadataType;
import org.olat.imsmd.xml.manifest.DescriptionType;
import org.olat.imsmd.xml.manifest.GeneralType;
import org.olat.imsmd.xml.manifest.LangstringType;
import org.olat.imsmd.xml.manifest.LomType;
import org.olat.imsmd.xml.manifest.TechnicalType;
import org.olat.imsmd.xml.manifest.TitleType;
import org.olat.imsqti.xml.manifest.QTIMetadataType;
import org.olat.oo.xml.manifest.OpenOLATMetadataType;

/**
 * 
 * Initial date: 23.02.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class ManifestMetadataBuilder {
	
	protected static final org.olat.oo.xml.manifest.ObjectFactory ooObjectFactory = new org.olat.oo.xml.manifest.ObjectFactory();
	protected static final org.olat.imscp.xml.manifest.ObjectFactory cpObjectFactory = new org.olat.imscp.xml.manifest.ObjectFactory();
	protected static final org.olat.imsmd.xml.manifest.ObjectFactory mdObjectFactory = new org.olat.imsmd.xml.manifest.ObjectFactory();
	protected static final org.olat.imsqti.xml.manifest.ObjectFactory qtiObjectFactory = new org.olat.imsqti.xml.manifest.ObjectFactory();
	
	public static final String ASSESSMENTTEST_MIMETYPE = "text/x-imsqti-test-xml";
	public static final String ASSESSMENTITEM_MIMETYPE = "text/x-imsqti-item-xml";
	
	private MetadataType metadata;
	private ManifestMetadataType manifestMetadata;
	
	
	
	public ManifestMetadataBuilder() {
		metadata = cpObjectFactory.createMetadataType();
	}
	
	public ManifestMetadataBuilder(MetadataType metadata) {
		this.metadata = metadata;
	}
	
	public MetadataType getMetadata() {
		return metadata;
	}
	
	public String getTitle() {
		GeneralType general = getGeneral(false);
		if(general != null) {
			TitleType type = getFromAny(TitleType.class, general.getContent());
			return type == null ? null : getFirstString(type.getLangstring());
		}
		return null;
	}
	
	public void setTitle(String title, String lang) {
		GeneralType general = getGeneral(true);
		if(general != null) {
			TitleType type = getFromAny(TitleType.class, general.getContent());
			if(type == null) {
				type = mdObjectFactory.createTitleType();
				general.getContent().add(mdObjectFactory.createTitle(type));
			}
			createOrUpdateFirstLangstring(type.getLangstring(), title, lang);
		}
	}
	
	public String getDescription() {
		GeneralType general = getGeneral(false);
		if(general != null) {
			DescriptionType type = getFromAny(DescriptionType.class, general.getContent());
			return type == null ? null : getFirstString(type.getLangstring());
		}
		return null;
	}
	
	public void setDescription(String description, String lang) {
		GeneralType general = getGeneral(true);
		if(general != null) {
			DescriptionType type = getFromAny(DescriptionType.class, general.getContent());
			if(type == null) {
				type = mdObjectFactory.createDescriptionType();
				general.getContent().add(mdObjectFactory.createDescription(type));
			}
			createOrUpdateFirstLangstring(type.getLangstring(), description, lang);
		}
	}
	
	public void setTechnicalFormat(String... formats) {
		if(formats != null && formats.length > 0 && formats[0] != null) {
			TechnicalType technical = getTechnical(true);
			clearFromAny("format", technical.getContent());
			for(int i=0; i<formats.length; i++) {
				technical.getContent().add(mdObjectFactory.createFormat(formats[i]));
			}
		}
	}
	
	public void createOrUpdateFirstLangstring(List<LangstringType> langStrings, String value, String lang) {
		if(langStrings.isEmpty()) {
			langStrings.add(createString(value, lang));
		} else {
			langStrings.get(0).setValue(value);
			langStrings.get(0).setLang(lang);
		}
	}
	
	public LangstringType createString(String value, String lang) {
		LangstringType string = mdObjectFactory.createLangstringType();
		string.setLang(lang);
		string.setValue(value);
		return string;
	}
	
	public String getFirstString(List<LangstringType> langStrings) {
		String firstString = null;
		if(langStrings != null && langStrings.size() > 0) {
			firstString = langStrings.get(0).getValue();
		}
		return firstString;
	}
	
	public TechnicalType getTechnical(boolean create) {
		LomType lom = getLom(create);
		if(lom == null) return null;
		
		TechnicalType technical = lom.getTechnical();
		if(technical == null && create) {
			technical = mdObjectFactory.createTechnicalType();
			lom.setTechnical(technical);
		}
		return technical;
	}

	public GeneralType getGeneral(boolean create) {
		LomType lom = getLom(create);
		if(lom == null) return null;
		
		GeneralType general = lom.getGeneral();
		if(general == null && create) {
			general = mdObjectFactory.createGeneralType();
			lom.setGeneral(general);
		}
		return general;
	}
	
	public LomType getLom(boolean create) {
		LomType lom = getFromAny(LomType.class, getMetadataList());
		if(lom == null && create) {
			lom = mdObjectFactory.createLomType();
			getMetadataList().add(mdObjectFactory.createLom(lom));
		}
        return lom;
	}
	
	/**
	 * Return the openolat metadata if it exists or, if specified, create
	 * one and append it to the metadata of the resource.
	 * 
	 * @param resource The resource with the metadata
	 * @param create True create the qtiMetadata
	 * @return
	 */
	public OpenOLATMetadataType getOpenOLATMetadata(boolean create) {
		List<Object> anyMetadataList = getMetadataList();
		OpenOLATMetadataType ooMetadata = null;
		for(Object anyMetadata:anyMetadataList) {
			if(anyMetadata instanceof JAXBElement<?>
				&& ((JAXBElement<?>)anyMetadata).getValue() instanceof OpenOLATMetadataType) {
				ooMetadata = (OpenOLATMetadataType)((JAXBElement<?>)anyMetadata).getValue();
			}
		}
		
		if(ooMetadata == null && create) {
			ooMetadata = ooObjectFactory.createOpenOLATMetadataType();
			getMetadataList().add(ooObjectFactory.createOoMetadata(ooMetadata));
		}
		return ooMetadata;
	}
	
	public void setOpenOLATMetadata(String questionType) {
		OpenOLATMetadataType qtiMetadata = getOpenOLATMetadata(true);
		qtiMetadata.setQuestionType(questionType);
	}
	
	/**
	 * Return the qti metadata if it exists or if specified, create
	 * one and append it to the metadata of the resource.
	 * 
	 * @param resource The resource with the metadata
	 * @param create True create the qtiMetadata
	 * @return
	 */
	public QTIMetadataType getQtiMetadata(boolean create) {
		List<Object> anyMetadataList = getMetadataList();
		QTIMetadataType qtiMetadata = null;
		for(Object anyMetadata:anyMetadataList) {
			if(anyMetadata instanceof JAXBElement<?>
				&& ((JAXBElement<?>)anyMetadata).getValue() instanceof QTIMetadataType) {
				qtiMetadata = (QTIMetadataType)((JAXBElement<?>)anyMetadata).getValue();
			}
		}
		
		if(qtiMetadata == null && create) {
			qtiMetadata = qtiObjectFactory.createQTIMetadataType();
			getMetadataList().add(qtiObjectFactory.createQtiMetadata(qtiMetadata));
		}
		return qtiMetadata;
	}
	
	public void setQtiMetadata(List<String> interactions) {
		QTIMetadataType qtiMetadata = getQtiMetadata(true);
		
		qtiMetadata.getInteractionType().clear();
		for(String interaction:interactions) {
			qtiMetadata.getInteractionType().add(interaction);
		}
	}
	
	@SuppressWarnings("unchecked")
	private <U> U getFromAny(Class<U> type, List<Object> anyList) {
		U object = null;
		for(Object any:anyList) {
			if(any instanceof JAXBElement<?>
				&& ((JAXBElement<?>)any).getValue().getClass().equals(type)) {
				object = (U)((JAXBElement<?>)any).getValue();
			}
		}
		return object;
	}

	private void clearFromAny(String type, List<Object> anyList) {
		for(Iterator<Object> anyIterator=anyList.iterator(); anyIterator.hasNext(); ) {
			Object any = anyIterator.next();
			if(any instanceof JAXBElement<?>
				&& ((JAXBElement<?>)any).getName().getLocalPart().equals(type)) {
				anyIterator.remove();
			}
		}
	}
	
	public List<Object> getMetadataList() {
		return metadata == null ? manifestMetadata.getAny() : metadata.getAny();
	}

}