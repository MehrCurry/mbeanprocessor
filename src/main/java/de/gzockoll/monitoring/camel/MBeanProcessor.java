package de.gzockoll.monitoring.camel;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gzockoll.observation.Measurement;

public class MBeanProcessor implements Processor {
	private static Logger LOGGER=LoggerFactory.getLogger(MBeanProcessor.class);
	private static final char DECIMAL_SEPARATOR = DecimalFormatSymbols
			.getInstance().getDecimalSeparator();
	private DecimalFormat format = new DecimalFormat("#.##");

	@Override
	public void process(Exchange exchange) throws Exception {
		List<Measurement> results = new ArrayList<Measurement>();
		Message in = exchange.getIn();
		String serviceURL = (String) in.getHeader("jmxServiceUrl","service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi");
		String user = (String) in.getHeader("jmxUser");
		String pass = (String) in.getHeader("jmxPassword");

		ObjectName name = new ObjectName((String) in.getHeader("jmxObjectName"));
		String[] attributeNames = ((String) in
				.getHeader("jmxAttributeName", "")).split(",");
		String id = (String) in.getHeader("MeasurementID");
		Phaenomens p = Phaenomens.valueOf((String) in.getHeader("Phaenomen"));
		Units u = Units.valueOf((String) in.getHeader("Unit"));
		double scale = Double.parseDouble((String) in.getHeader("Scale", "1"));

		LOGGER.debug("Trying to connect to: " + serviceURL);
		JMXServiceURL serviceUrl = new JMXServiceURL(serviceURL);

		Map<String, Object> environment = null;
		if (!StringUtils.isEmpty(pass)) {
			environment = new HashMap<String, Object>();
			String[] credentials = new String[] { user, pass };
			environment.put(JMXConnector.CREDENTIALS, credentials);
		}

		JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl,
				environment);

		Number value = null;
		try {
			MBeanServerConnection mBeanServerConnection = jmxConnector
					.getMBeanServerConnection();

			for (String attribute : attributeNames) {
				String[] parts = attribute.split("\\.");
				Object result = mBeanServerConnection.getAttribute(name,
						parts[0]);
				if (result instanceof Number)
					value = (Number) result;
				else if (result instanceof String) {
					String s = (String) result;
					value = Double.parseDouble(s.replace(',', '.'));
				} else if (result instanceof CompositeDataSupport) {
					CompositeDataSupport cds = (CompositeDataSupport) result;
					value = (Number) ((CompositeDataSupport) result)
							.get(parts[1]);
				} else
					throw new IllegalArgumentException("Unknown type: "
							+ result);
				results.add(new Measurement(id + "." + attribute, p, u, value
						.doubleValue() / (double) scale));

			}
		} catch (Exception e) {
			LOGGER.error("An exception occurred:",e);
		} finally {
			jmxConnector.close();
		}
		exchange.getIn().setBody(results);
	}
}
