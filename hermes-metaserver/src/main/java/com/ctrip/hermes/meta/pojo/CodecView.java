package com.ctrip.hermes.meta.pojo;

import java.util.ArrayList;
import java.util.List;

import com.ctrip.hermes.meta.entity.Codec;
import com.ctrip.hermes.meta.entity.Property;

public class CodecView {
	private String type;

	private List<Property> properties;

	public CodecView() {

	}

	public CodecView(Codec codec) {
		this.type = codec.getType();
		properties = new ArrayList<Property>();
		for (Property property : codec.getProperties().values()) {
			properties.add(property);
		}
	}

	public List<Property> getProperties() {
		return properties;
	}

	public String getType() {
		return type;
	}

	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Codec toMetaCodec() {
		Codec codec = new Codec();
		codec.setType(this.type);
		if (this.properties != null) {
			for (Property prop : this.properties) {
				if (prop != null)
					codec.addProperty(prop);
			}
		}
		return codec;
	}

}
