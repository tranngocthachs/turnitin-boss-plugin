package boss.turnitin;

import java.util.ArrayList;
import java.util.Collection;

import boss.plugins.spi.config.IPluginConfiguration;

import uk.ac.warwick.dcs.boss.model.ConfigurationOption;

public class TIIWSConfig extends IPluginConfiguration {
	public static final String AID_PROP_KEY = "tii.aid";
	public static final String SECRETKEY_PROP_KEY = "tii.secretkey";
	public static final String APIURL_PROP_KEY = "tii.apiurl";
	@Override
	public Collection<ConfigurationOption> getConfigurationOptions() {
		Collection<ConfigurationOption> retval = new ArrayList<ConfigurationOption>();
		retval.add(new ConfigurationOption(AID_PROP_KEY, "Your TurnItIn primary account id (associated with your school's TurnItIn license)", "12345"));
		retval.add(new ConfigurationOption(SECRETKEY_PROP_KEY, "Your TurnItIn shared secrete key (set by your TurnItIn Administrator)", "password"));
		retval.add(new ConfigurationOption(APIURL_PROP_KEY, "The URL of TurnItIn web service URL", "http://localhost:8080/Dummy/api"));
		return retval;
	}

}
