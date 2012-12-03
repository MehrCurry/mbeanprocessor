package de.gzockoll.monitoring.camel;

public class SimpleMeasurement {

	private String name;
	private Number value;

	public SimpleMeasurement(String name, Number value) {
		this.name=name;
		this.value=value;
	}

	public String getName() {
		return name;
	}

	public Number getValue() {
		return value;
	}

}
