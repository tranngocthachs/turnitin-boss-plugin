package boss.turnitin;

import java.util.ArrayList;
import java.util.Collection;

import uk.ac.warwick.dcs.boss.model.ConfigurationOption;
import uk.ac.warwick.dcs.boss.plugins.spi.config.PluginConfiguration;

public class TIIWSConfig extends PluginConfiguration {

	@Override
	public Collection<ConfigurationOption> getConfigurationOptions() {
		Collection<ConfigurationOption> retval = new ArrayList<ConfigurationOption>();
		retval.add(new ConfigurationOption("tii.aid", "Your TurnItIn primary account id (associated with your school's TurnItIn license)", "1234"));
		retval.add(new ConfigurationOption("tii.secretkey", "Your TurnItIn shared secrete key (set by your TurnItIn Administrator)", "abcd1234"));
		retval.add(new ConfigurationOption("tii.apiurl", "The URL of TurnItIn web service URL", "https://submit.ac.uk/api.asp"));
		return retval;
	}

}
