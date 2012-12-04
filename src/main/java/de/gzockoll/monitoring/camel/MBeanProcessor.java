package de.gzockoll.monitoring.camel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.AttributeNotFoundException;
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

public class MBeanProcessor implements Processor {
	private static Logger LOGGER=LoggerFactory.getLogger(MBeanProcessor.class);
	private static final char DECIMAL_SEPARATOR = DecimalFormatSymbols
			.getInstance().getDecimalSeparator();
	private DecimalFormat format = new DecimalFormat("#.##");

	@Override
	public void process(Exchange exchange) throws Exception {
		List<SimpleMeasurement> results = new ArrayList<SimpleMeasurement>();
		Message in = exchange.getIn();
		String serviceURL = (String) in.getHeader("jmxServiceUrl","service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi");
		String user = (String) in.getHeader("jmxUser");
		String pass = (String) in.getHeader("jmxPassword");

		JMXConnector jmxConnector = connect(serviceURL, user, pass);

		ObjectName name = new ObjectName((String) in.getHeader("jmxObjectName"));
		String[] attributeNames = ((String) in
				.getHeader("jmxAttributeName", "")).split(",");
		String id = (String) in.getHeader("MeasurementID");
		double scale = Double.parseDouble((String) in.getHeader("Scale", "1"));


		Number value = null;
		try {
			MBeanServerConnection mBeanServerConnection = jmxConnector
					.getMBeanServerConnection();

			for (String attribute : attributeNames) {
				String[] parts = attribute.split("\\.");
				try {
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
					results.add(new SimpleMeasurement(id + "." + attribute, value
							.doubleValue() / (double) scale));
				} catch (AttributeNotFoundException e) {
					LOGGER.warn("Warning:",e);
				}

			}
		} catch (Exception e) {
			LOGGER.error("An exception occurred:",e);
		} finally {
			jmxConnector.close();
		}
		exchange.getIn().setBody(results);
	}

	private JMXConnector connect(String serviceURL, String user, String pass)
			throws MalformedURLException, IOException {
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
		return jmxConnector;
	}
}
