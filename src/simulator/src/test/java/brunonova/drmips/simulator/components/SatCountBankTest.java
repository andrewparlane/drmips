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

public class SatCountBankTest {
    private JSONObject json;
    private SatCountBank comp;

	@Test
	public void testComponent() throws InvalidCPUException, JSONException {
		// test initial values
        createComponent(false, 0);
        tComp(0, 0, 0, 0, 0, false);
        tComp(1, 0, 0, 0, 0, false);
        tComp(2, 0, 0, 0, 0, false);
        tComp(3, 0, 0, 0, 0, false);
        createComponent(false, 3);
        tComp(0, 0, 0, 0, 3, false);
        tComp(1, 0, 0, 0, 3, false);
        tComp(2, 0, 0, 0, 3, false);
        tComp(3, 0, 0, 0, 3, false);

        // test count up / down without forwarding
        createComponent(false, 0);
        tComp(0, 0, 1, 1, 0, false); // expect 0 out
        tComp(0, 0, 1, 1, 0, true);  // count up, expect 0 because didn't do executeSynchronous() on the last call
        tComp(0, 0, 1, 1, 1, true);  // now we should see the update from the last call
        tComp(0, 0, 1, 1, 2, true);
        tComp(0, 0, 1, 1, 3, true);  // reached max
        tComp(0, 0, 1, 1, 3, true);  // saturated
        tComp(0, 0, 1, 0, 3, true);  // start counting down
        tComp(0, 0, 1, 0, 2, true);
        tComp(0, 0, 1, 0, 1, true);
        tComp(0, 0, 1, 0, 0, true);  // reached min
        tComp(0, 0, 1, 1, 0, true);  // saturated
        tComp(0, 0, 1, 1, 1, true);  // counter 0 should be 2 after this call

        // check counter 3
        tComp(3, 3, 1, 1, 0, false); // expect 0 out
        tComp(3, 3, 1, 1, 0, true);  // count up
        tComp(3, 3, 0, 0, 1, false); // should be at 1

        // check counter 0 is at 2 still
        tComp(0, 0, 0, 0, 2, false);

        // update counter 3 while reading counter 0
        tComp(0, 3, 1, 1, 2, true);
        tComp(0, 3, 1, 1, 2, true);
        tComp(3, 3, 0, 0, 3, true);
        tComp(3, 3, 0, 0, 3, true);

        // test count up / down with forwarding
        createComponent(true, 0);
        tComp(0, 0, 1, 1, 1, false); // expect 1 out
        tComp(0, 0, 1, 1, 1, true);  // count up, expect 1 because didn't do executeSynchronous() on the last call
        tComp(0, 0, 1, 1, 2, true);  // now we should see the update from the last call
        tComp(0, 0, 1, 1, 3, true);  // reached max
        tComp(0, 0, 1, 1, 3, true);  // saturated
        tComp(0, 0, 1, 0, 2, true);  // start counting down
        tComp(0, 0, 1, 0, 1, true);
        tComp(0, 0, 1, 0, 0, true);  // reached min
        tComp(0, 0, 1, 0, 0, true);  // saturated
        tComp(0, 0, 1, 1, 1, true);  // count back up
        tComp(0, 0, 1, 1, 2, true);  // counter 0 should be 2 after this call

        // check counter 3
        tComp(3, 3, 1, 1, 1, false); // expect 1 out
        tComp(3, 3, 1, 1, 1, true);  // count up
        tComp(3, 3, 0, 0, 1, false); // should be at 1

        // check counter 0 is at 2 still
        tComp(0, 0, 0, 0, 2, false);

        // update counter 3 while reading counter 0
        tComp(0, 3, 1, 1, 2, true);
        tComp(0, 3, 1, 1, 2, true);
        tComp(3, 3, 0, 0, 3, true);
        tComp(3, 3, 0, 0, 3, true);
	}


    private void createComponent(boolean forwarding, int initialValue) throws InvalidCPUException, JSONException {
		json = new JSONObject().put("x", 0)
                               .put("y", 0)
                               .put("forwarding", forwarding ? "true" : "false")
                               .put("num_counters", 4)
                               .put("width", 2)
                               .put("initial_value", initialValue)
                               .put("read_idx", "readIdx")
                               .put("write_idx", "writeIdx")
                               .put("update", "update")
                               .put("count_up", "countUp")
                               .put("read_data", "readData");

		comp = new SatCountBank("test", json);
    }

	private void tComp(int readIdx, int writeIdx, int update, int countUp, int expectedReadData, boolean sync) {
		comp.getReadIdx().setValue(readIdx);
		comp.getWriteIdx().setValue(writeIdx);
		comp.getUpdate().setValue(update);
		comp.getCountUp().setValue(countUp);
		comp.execute();
		assertEquals(expectedReadData, comp.getReadData().getValue());
        if (sync)
            comp.executeSynchronous();
	}
}
