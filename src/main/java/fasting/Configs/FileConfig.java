package fasting.Configs;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Configuration file handler 
 * @author V.K. Cody Bumgardner
 * @author Caylin Hickey
 * @since 0.1.0
 */

public class FileConfig {

    private HierarchicalINIConfiguration iniConfObj;

    public FileConfig(String configFile) throws ConfigurationException {
        iniConfObj = new HierarchicalINIConfiguration(configFile);
        iniConfObj.setDelimiterParsingDisabled(true);
        iniConfObj.setAutoSave(true);

    }

    public Map<String, Object> getConfigMap() {
        return getConfigMap("general");
    }

    public Map<String, Object> getConfigMap(String section) {

        Map<String, Object> configMap = new HashMap<>();

        SubnodeConfiguration sObj = iniConfObj.getSection(section);
        //final Map<String,String> result=new TreeMap<String,String>();
        //StringBuilder sb = new StringBuilder();
        final Iterator it = sObj.getKeys();
        while (it.hasNext()) {
            final Object key = it.next();
            final Object value = sObj.getString(key.toString());
            configMap.put(key.toString(),value);
            //result.put(key.toString(),value);
            //sb.append(key.toString() + "=" + value + ",");

        }
        //return sb.toString().substring(0, sb.length() - 1);
        //return result;
        return  configMap;
    }


}