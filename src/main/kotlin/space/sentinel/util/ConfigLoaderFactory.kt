package space.sentinel.util

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

class ConfigLoaderFactory {

    companion object SettingsLoader {
        val WindowsConf = "windows"
        val LinuxARMConf = "linux-arm"
        val DefaultConf = "default"
    }

    val os = System.getProperty("os.name")

    /**
     * Loads settings from 'default.conf'
     * and additional settings from 'windows.conf' or 'linux-arm.conf'
     * depending on the os.
     * @return
     */
    fun load(): Config {
        return ConfigFactory.load(determineConfigFile())
                .withFallback(ConfigFactory.load(DefaultConf))
                .resolve()

    }

    private fun determineConfigFile() =
            if (os.toLowerCase().startsWith(WindowsConf)) WindowsConf
            else LinuxARMConf

}