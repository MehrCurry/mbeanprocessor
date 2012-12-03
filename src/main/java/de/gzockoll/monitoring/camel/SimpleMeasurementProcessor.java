package de.gzockoll.monitoring.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleMeasurementProcessor implements Processor {
	private static Logger LOGGER=LoggerFactory.getLogger(SimpleMeasurementProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		Message in = exchange.getIn();
		String id = (String) in.getHeader("MeasurementID");
		Double scale = Double.parseDouble((String) in.getHeader("Scale", "1"));
		Double value = Double.parseDouble((String) in.getBody(String.class));

		exchange.getIn().setBody(new SimpleMeasurement(id, value
						.doubleValue() / (double) scale));
	}
}
