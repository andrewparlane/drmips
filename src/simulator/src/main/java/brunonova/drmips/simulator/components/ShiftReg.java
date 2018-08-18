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
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class that represents a shift register
 *
 * @author Andrew Parlane (based on RegBank by Bruno Nova)
 */
public class ShiftReg extends Component implements Synchronous {
	private final Input         write, writeData;
	private final Output        readData;
	private final Data          shiftReg;
	private final boolean       forwarding;     // use internal forwarding?
	private final Stack<int[]>  states = new Stack<>(); // previous values

	/**
	 * Component constructor.
	 * @param id The component's identifier.
	 * @param json The JSON object representing the component that should be parsed.
	 * @throws InvalidCPUException If the component has invalid parameters.
	 * @throws JSONException If the JSON object is invalid or incomplete.
	 */
	public ShiftReg(String id, JSONObject json) throws InvalidCPUException, JSONException {
		super(id, json, "Shift Register", "shiftreg", "shiftreg_description", new Dimension(40, 20));

        int width = json.getInt("width");
        if(width <= 0 || width > 16)
			throw new InvalidCPUException("Invalid width (must be 1 to 16 bits)!");

		forwarding = json.optBoolean("forwarding");

        // Initialize shiftReg
        shiftReg = new Data(width, 0);

        // Add inputs/outputs
        write       =  addInput(json.getString("write"),      new Data(1),              IOPort.Direction.WEST,  true, true);
        writeData   =  addInput(json.getString("write_data"), new Data(1),              IOPort.Direction.WEST,  true, true);
		readData    = addOutput(json.getString("read_data"),  new Data(width),          IOPort.Direction.NORTH, true);
	}

	@Override
	public void execute() {
		boolean write = getWrite().getValue() == 1;
		int write_bit = getWriteData().getValue();

		if(isForwarding() && write) {
            int newValue = (shiftReg.getValue() << 1) | (write_bit & 1);
			getReadData().setValue(newValue);
        }
		else {
			getReadData().setValue(shiftReg.getValue());
        }

		getWriteData().setRelevant(write);
	}

	@Override
	public void executeSynchronous() {
		boolean write = getWrite().getValue() == 1;
		int write_bit = getWriteData().getValue();

		if(write) {
            int newValue = (shiftReg.getValue() << 1) | (write_bit & 1);
			shiftReg.setValue(newValue);
        }
	}

	@Override
	public void pushState() {
		int[] values = new int[1];
        values[0] = shiftReg.getValue();
		states.push(values);
	}

	@Override
	public void popState() {
		shiftReg.setValue(states.pop()[0]);
	}

	@Override
	public boolean hasSavedStates() {
		return !states.empty();
	}

	@Override
	public void clearSavedStates() {
		states.clear();
	}

	@Override
	public void resetFirstState() {
		if(hasSavedStates()) {
			while(hasSavedStates())
                shiftReg.setValue(states.pop()[0]);
		}
	}

	@Override
	public boolean isWritingState() {
		return getWrite().getValue() == 1;
	}

	/**
	 * Returns whether the data should be updated or read first.
	 * @return <tt>True</tt> if internal forwarding is enabled.
	 */
	public final boolean isForwarding() {
		return forwarding;
	}

	/**
	 * Resets the shift register back to it's initial state
	 */
	public final void reset() {
		shiftReg.setValue(0);
		execute();
	}

	/**
	 * Returns a copy of the shift register.
	 * @return Copy of the shift register.
	 * @throws ArrayIndexOutOfBoundsException If the index is invalid.
	 */
	public final Data getShiftReg() throws ArrayIndexOutOfBoundsException {
		return shiftReg.clone();
	}

	/**
	 * Updates the value of the shift register.
	 * <p>The new value is propagated to the rest of the circuit if it is being read.</p>
	 * @param newValue New value.
	 * @throws ArrayIndexOutOfBoundsException If the index is invalid.
	 */
	public final void setShiftReg(int newValue) throws ArrayIndexOutOfBoundsException {
		setShiftReg(newValue, true);
	}

	/**
	 * Updates the value of the shift register
	 * @param newValue New value.
	 * @param propagate Whether the new value is propagated to the rest of the circuit if it is being read.
	 * @throws ArrayIndexOutOfBoundsException If the index is invalid.
	 */
	public final void setShiftReg(int newValue, boolean propagate) throws ArrayIndexOutOfBoundsException {
        shiftReg.setValue(newValue);
        if(propagate) execute();
	}

	/**
	 * Returns the write input.
	 * @return The write input.
	 */
	public final Input getWrite() {
		return write;
	}

	/**
	 * Returns the writeData input.
	 * @return The writeData input.
	 */
	public final Input getWriteData() {
		return writeData;
	}

	/**
	 * Returns the readData output.
	 * @return The readData output.
	 */
	public final Output getReadData() {
		return readData;
	}
}
