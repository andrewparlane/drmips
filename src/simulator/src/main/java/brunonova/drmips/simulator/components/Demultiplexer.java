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

import brunonova.drmips.simulator.*;
import brunonova.drmips.simulator.exceptions.InvalidCPUException;
import brunonova.drmips.simulator.util.Dimension;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class that represents a demultiplexer.
 *
 * @author Andrew Parlane (based on Multiplexer by Bruno Nova)
 */
public class Demultiplexer extends Component {
	private final Input selector;
	private final Input input;
	private final List<Output> outputs; // outputs

	/**
	 * Component constructor.
	 * @param id The component's identifier.
	 * @param json The JSON object representing the component that should be parsed.
	 * @throws InvalidCPUException If the component has invalid parameters.
	 * @throws JSONException If the JSON object is invalid or incomplete.
	 */
	public Demultiplexer(String id, JSONObject json) throws InvalidCPUException, JSONException {
		super(id, json, "D\nM\nU\nX", "demultiplexer", "demultiplexer_description", new Dimension(15, 50));

		// Add the outputs
		JSONArray outs = json.getJSONArray("out");
		outputs = new ArrayList<>(outs.length());
		int size = json.getInt("size");
		for(int x = 0; x < outs.length(); x++) {
			outputs.add(addOutput(outs.getString(x), new Data(size)));
		}

		selector = addInput(json.getString("sel"), new Data((outs.length() > 0) ? Data.requiredNumberOfBits(outs.length() - 1) : 1), IOPort.Direction.NORTH);
		input = addInput(json.getString("in"), new Data(size));
	}

	@Override
	public void execute() {
		int sel = getSelector().getValue();

		for(int i = 0; i < outputs.size(); i++) { // put input on selected output
			if(i == sel)
                getOutput(i).setValue(getInput().getValue());
            else
                getOutput(i).setValue(0);
		}
        input.setRelevant(true);
	}

	@Override
	protected List<Input> getLatencyInputs() {
		ArrayList<Input> inList = new ArrayList<>();
		// add control input
		inList.add(getSelector());
		// add input
		inList.add(getInput());
		return inList;
	}

	/**
	 * Returns the demultiplexer's input.
	 * @return Demultiplexer input;
	 */
	public final Input getInput() {
		return input;
	}

	/**
	 * Returns the demultiplexer's selector.
	 * @return Demultiplexer selector;
	 */
	public final Input getSelector() {
		return selector;
	}

	/**
	 * Returns the Output with the specified index.
	 * @param index Index of the output.
	 * @return The Output, or <tt>null</tt> if it doesn't exist.
	 */
	public final Output getOutput(int index) {
		if(index >= 0 && index < outputs.size())
			return outputs.get(index);
		else
			return null;
	}
}
