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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class DemultiplexerTest {
	@Test
	public void testComponent() throws InvalidCPUException, JSONException {
		tComp(2, 1, 0, 0);
		tComp(2, 1, 0, 1);
        tComp(2, 1, 1, 0);
		tComp(2, 1, 1, 1);

        tComp(4, 32, 0xDEADBEEF, 0);
		tComp(4, 32, 0xDEADBEEF, 1);
		tComp(4, 32, 0xDEADBEEF, 2);
		tComp(4, 32, 0xDEADBEEF, 3);
	}

	private void tComp(int num, int size, int in, int sel) throws InvalidCPUException, JSONException {
		List<String> outIds = new ArrayList<>();
		for(int i = 0; i < num; i++) {
			outIds.add(i + "");
		}
		JSONObject json = new JSONObject().put("x", 0).put("y", 0)
			.put("out", new JSONArray(outIds))
			.put("size", size).put("sel", "sel").put("in", "in");

		Demultiplexer c = new Demultiplexer("test", json);
        c.getInput().setValue(in);
		c.getSelector().setValue(sel);
		c.execute();

		for(int i = 0; i < num; i++) {
            if (i == sel)
                assertEquals(in, c.getOutput(i).getValue());
            else
                assertEquals(0, c.getOutput(i).getValue());
		}
	}
}
