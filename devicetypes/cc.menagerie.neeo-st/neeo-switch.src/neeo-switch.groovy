/**
 * MIT License
 *
 * Copyright (c) 2018 Matthew N Henkel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

metadata {
	definition (
		name: "neeo-switch",
		namespace: "cc.menagerie.neeo-st",
		author: "Matthew N Henkel"
	) {
		capability "Switch"
		capability "Refresh"
		capability "Polling"
	}

	simulator {
		// TODO
	}

	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: "Off", action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			state "on", label: "On", action: "switch.off", icon: "st.switches.switch.off", backgroundColor: "#79b821"
		}

		standardTile("refresh", "capability.refresh", with: 1, height: 1, decoration: "flat") {
			state "default", label: "Refresh", action: "refresh.refresh", icon: "st.secondary.refresh"
		}

		main("switch")
		details(["switch"])
	}

	command "on"
	command "off"
}

preferences {
	input "roomID", "text", title: "Room ID", required: true, displayDuringSetup: true
	input "onID", "text", title: "On ID", required: true, displayDuringSetup: true
	input "offID", "text", title: "Off ID", required: true, displayDuringSetup: true
	input "statusID", "text", title: "Status ID", required: true, displayDuringSetup: true
}

def refresh(){
	executeCommand("projects/home/rooms/$roomID/recipes/$statusID/isactive")
}

// handle commands
def on() {
	log.debug("Executing 'on'")
	executeCommand("projects/home/rooms/$roomID/recipes/$onID/execute")
	sendEvent(name: "switch", value: "on", isStateChange: true)
}

def off() {
	log.debug("Executing 'off'")
	executeCommand("projects/home/rooms/$roomID/recipes/$offID/execute")
	sendEvent(name: "switch", value: "off", isStateChange: true)
}

def hubActionResponse(response){
  log.debug("Executing 'hubActionResponse': '${device.deviceNetworkId}'")
  log.debug(response.status)
	log.debug(response.header)
	log.debug(response.body)

	def body = new groovy.json.JsonSlurper().parseText(response.body)
	if (body.active != null) {
		def state = device.currentValue("switch")
		log.debug("Current switch state: '$state'")

		def neeo = body.active ? "on" : "off"
		log.debug("Neeo state: '$neeo'")

		if (neeo != state) {
			log.debug("Updating switch to $neeo")
			sendEvent(name: "switch", value: neeo, isStateChange: true)
		}
	}
}

def poll(){
	refresh()
}

private executeCommand(command){
    log.debug(device.deviceNetworkId)

    def headers = [:]
    headers.put("HOST", "192.168.7.179:3000")

    try {
      sendHubCommand(new physicalgraph.device.HubAction([
          method: "GET",
          path: "/v1/$command",
          headers: headers],
          device.deviceNetworkId,
          [callback: "hubActionResponse"]
      ))
    } catch (e) {
      log.debug(e.message)
    }
}
