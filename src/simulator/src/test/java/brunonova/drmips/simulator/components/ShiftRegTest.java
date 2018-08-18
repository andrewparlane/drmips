/*
    DrMIPS - Educational MIPS simulator
    Copyright (C) 2013-2015 Bruno Nova <brunomb.nova@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package brunonova.drmips.simulator.components;

import brunonova.drmips.simulator.exceptions.InvalidCPUException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShiftRegTest{
    private JSONObject json;
    private ShiftReg comp;

	@Test
	public void testComponent() throws InvalidCPUException, JSONException {

        // test without forwarding
        createComponent(false);
        tComp(0, 0, 0, false);      // initial value is 0
        tComp(0, 0, 0, true);       // still 0
        tComp(1, 1, 0, true);       // shift in some values
        tComp(1, 1, 0x01, true);
        tComp(1, 1, 0x03, true);
        tComp(1, 0, 0x07, true);
        tComp(0, 0, 0x0E, true);
        tComp(0, 0, 0x0E, true);    // stays constant when update is 0
        tComp(1, 1, 0x0E, true);
        tComp(1, 1, 0x1D, true);
        tComp(1, 0, 0x3B, true);
        tComp(1, 0, 0x76, true);
        tComp(1, 1, 0xEC, true);
        tComp(1, 1, 0xD9, true);    // bits starting to be shifted out
        tComp(1, 0, 0xB3, true);
        tComp(1, 0, 0x66, true);
        tComp(1, 0, 0xCC, true);
        tComp(1, 0, 0x98, true);
        tComp(1, 0, 0x30, true);
        tComp(1, 0, 0x60, true);
        tComp(1, 0, 0xC0, true);
        tComp(1, 0, 0x80, true);
        tComp(1, 0, 0x00, true);
        tComp(1, 0, 0x00, true);
        tComp(1, 0, 0x00, true);

        // test with forwarding
        createComponent(true);
        tComp(0, 0, 0, false);      // initial value is 0
        tComp(0, 0, 0, true);       // still 0
        tComp(1, 1, 0x01, true);    // shift in some values
        tComp(1, 1, 0x03, true);
        tComp(1, 1, 0x07, true);
        tComp(1, 0, 0x0E, true);
        tComp(0, 0, 0x0E, true);    // stays constant when update is 0
        tComp(0, 0, 0x0E, true);
        tComp(1, 1, 0x1D, true);
        tComp(1, 1, 0x3B, true);
        tComp(1, 0, 0x76, true);
        tComp(1, 0, 0xEC, true);
        tComp(1, 0, 0xD8, true);
        tComp(1, 0, 0xB0, true);
        tComp(1, 0, 0x60, true);
        tComp(1, 0, 0xC0, true);
        tComp(1, 0, 0x80, true);
        tComp(1, 0, 0x00, true);
        tComp(1, 0, 0x00, true);
        tComp(1, 0, 0x00, true);
	}


    private void createComponent(boolean forwarding) throws InvalidCPUException, JSONException {
		json = new JSONObject().put("x", 0)
                               .put("y", 0)
                               .put("forwarding", forwarding ? "true" : "false")
                               .put("width", 8)
                               .put("write", "write")
                               .put("write_data", "writeData")
                               .put("read_data", "readData");

		comp = new ShiftReg("test", json);
    }

	private void tComp(int write, int writeData, int expectedReadData, boolean sync) {
		comp.getWrite().setValue(write);
		comp.getWriteData().setValue(writeData);
		comp.execute();
		assertEquals(expectedReadData, comp.getReadData().getValue());
        if (sync)
            comp.executeSynchronous();
	}
}
