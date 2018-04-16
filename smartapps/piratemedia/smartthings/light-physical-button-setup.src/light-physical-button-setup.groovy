/**
 *  Light Physical Button Setup
 *
 *  Copyright 2018 Eliot Stocker
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Light Physical Button Setup",
    namespace: "piratemedia/smartthings",
    author: "Eliot Stocker",
    description: "Application to enable direction of lights based on physical/virtual Buttons",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo-small.png",
    iconX2Url: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo.png",
    parent: "piratemedia/smartthings:Light Physical Control",
)


preferences {
    page(name: "LightSettingsPage")
}

def LightSettingsPage() {
    dynamicPage(name: "LightSettingsPage", install: true, uninstall: true) {
    	section("Select the button you wish to use") {
            input "button", "capability.button", title: "Button"
        }
        section("Select Light(s) to turn on/off") {
            input "lights", "capability.switch", title: "Lights", multiple: true, submitOnChange: true
        }
    
        section("Select Lights initial Settings when turned on") {
            if(canControlLevel()) {
                input "level", "number", title: "Light Brightness", range: "(1..100)", required: false
            }
            if(canControlColorTemperature()) {
                input "temp", "number", title: "Light Color Temperature", range: "(2200..6500)", required: false
            }
            if(canControlColor()) {
                input "color", "enum", title: "Color", options: ["Red", "Orange", "Yellow", "Green", "Blue", "Indigo", "Violet"], required: false
            }
        }
        
        section("Time specific settings (Overrides above values within selected time frames)") {
        }
        section() {
        	app(name: "timeSetup", appName: "Light Physical Button Time Settings", namespace: "piratemedia/smartthings", title: "New Time Specific Setting", multiple: true)
        }
        
        section("Lighting Setup Name") {
        	label title: "Setup Name", required: true, defaultValue: app.label
        }
    }
}

def checkForCapability(capability) {
	def found = false
    lights.each { light ->
        def capabilites = light.getCapabilities()
        capabilites.each {cap ->
            if(cap.name == capability) {
                found = true
            }
        }
    }
    return found
}

def checkDeviceForCapabilityById(id, capability) {
	def selected;
    lights.each { light ->
    	if(light.id == id) {
        	selected = light
        }
    }
    if(selected != null) {
    	return checkDeviceForCapability(selected, capability);
    }
    return false;
}

def checkDeviceForCapability(dev, capability) {
	def found = false
	def capabilites = dev.getCapabilities()
    capabilites.each {cap ->
        if(cap.name == capability) {
            found = true
        }
    }
    return found
}

def canControlLevel() {
	return checkForCapability('Switch Level')
}

def canControlColorTemperature() {
	return checkForCapability('Color Temperature')
}

def canControlColor() {
	return checkForCapability('Color Control')
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(button, "button", buttonPress)
}

def buttonPress(evt) {
	def isHeld = false;
    if(evt.value == "held") {
    	isHeld = true;
    }
	def anyOn = false;
    def timedSettingsActive = false;
	for(light in lights) {
    	if(light.currentValue("switch") == "on") anyOn = true;
    }
    if(anyOn) {
    	lights.off()
    } else {
    	if(!isHeld) {
            def children = getChildApps()
            children.each { child ->
                if(child.isActive()) {
                    timedSettingsActive = true
                    def settings = child.getSettings()
                    if(child.hasSpecificSettings()) {
                        lights.each{ light ->
                            def data = child.getSpecificLightSetting(light.label);
                            if(data != null) {
                                def lightOff = false
                                log.debug data
                                if(data.on != null && data.on.toString() != "null") {
                                    if(!data.on) {
                                        light.off()
                                        lightOff = true;
                                    }
                                }
                                if(!lightOff) {
                                    light.on()
                                    if(data.level != null && data.level.toString() != "null"
                                        || data.temp != null && data.temp.toString() != "null"
                                        || data.color != null && data.color.toString() != "null") {
                                        setLightConfig(light, data);
                                    }
                                }
                            } else {
                                light.on()
                                if(settings.level != null && settings.level.toString() != "null"
                                   || settings.temp != null && settings.temp.toString() != "null"
                                   || settings.color != null && settings.color.toString() != "null") {
                                    setLightConfig(light, settings);
                                }
                            }
                        }
                    } else {
                        lights.on()
                        if(settings.level != null && settings.level.toString() != "null"
                            || settings.temp != null && settings.temp.toString() != "null"
                            || settings.color != null && settings.color.toString() != "null") {
                            setLightsConfig(settings);
                        }
                    }
                    return
                }
            }
		}
        
        if(!timedSettingsActive) {
            lights.on()
            if(level != null || temp != null || color != null) {
                setLightsConfig([level: level, temp: temp, color: color]);
            }
        }
    }
}

def setLightsConfig(data) {
    log.debug "set all lights"
	for(light in lights) {
    	setLightConfig(light, data);
    }
}

def setLightConfig(light, data) {
    //set config for single Bulb
    if(data.color != null && data.color.toString() != "null") {
        try {
            def hue = 0;
            switch(data.color) {
                case "Red":
                hue = 0;
                break;
                case "Orange":
                hue = 8.3;
                break;
                case "Yellow":
                hue = 16;
                break;
                case "Green":
                hue = 33;
                break;
                case "Blue":
                hue = 66;
                break;
                case "Indigo":
                hue = 77;
                break;
                case "Violet":
                hue = 88;
                break;
            }
            def lvl = data.level ?: 100
            light.setColor([
                hue: hue, saturation: 100, level: lvl
            ], [delay: 250]);
        } catch(e) {
            if(data.temp != null && data.temp.toString() != "null") {
                light.setColorTemperature(data.temp);
            }
            if(data.level != null && data.level.toString() != "null") {
                light.setLevel(data.level);
            }
        }
    } else {
        if(data.temp != null && data.temp.toString() != "null") {
            light.setColorTemperature(data.temp);
        }
        if(data.level != null && data.level.toString() != "null") {
            light.setLevel(data.level);
        }
    }
}

def getLightDevices() {
	def vals = []
	lights.each{ light ->
    	def l = [
        id: light.id,
        label: light.label
        ]
    	vals.add(l)
    }
	return vals
}

def getDeviceByID(id) {
	def out = null
	lights.each { light ->
    	if(light.id == id) out = light
    }
    
    return out
}

def getDeviceByLabel(label) {
	def out = null
	lights.each { light ->
    	if(light.label == label) out = light
    }
    return out
}