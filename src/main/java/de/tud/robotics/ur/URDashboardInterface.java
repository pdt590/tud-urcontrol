package de.tud.robotics.ur;

import de.tud.robotics.ur.api.ExpectedReturn;

public interface URDashboardInterface {

	@ExpectedReturn("Shutting down")
	boolean shutdown();
	@ExpectedReturn("Powering on")
	boolean power_on();
	@ExpectedReturn("Powering off")
	boolean power_off();
	@ExpectedReturn("Brake releasing")
	boolean brake_release();
	@ExpectedReturn("Protective stop releasing")
	boolean unlock_protective_stop();
	@ExpectedReturn("closing safety popup")
	boolean close_safety_popup();
	@ExpectedReturn(value="Loading installation:", parameters ={"name"} )
	boolean load_installation(String name);
	@ExpectedReturn(value="Starting program")
	boolean play();
	
	default boolean load_installation() {
		return load_installation("/programs/default.installation");
	}
}
