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
 * Class that represents a bank of saturating counters.
 *
 * @author Andrew Parlane (based on RegBank by Bruno Nova)
 */
public class SatCountBank extends Component implements Synchronous {
	private final Input         readIdx, writeIdx, update, countUp;
	private final Output        readData;
	private final Data[]        counters;
	private final boolean       forwarding;     // use internal forwarding?
	private final int           max;
	private final int           initialValue;
	private final Stack<int[]>  states = new Stack<>(); // previous values

	/**
	 * Component constructor.
	 * @param id The component's identifier.
	 * @param json The JSON object representing the component that should be parsed.
	 * @throws InvalidCPUException If the component has invalid parameters.
	 * @throws JSONException If the JSON object is invalid or incomplete.
	 */
	public SatCountBank(String id, JSONObject json) throws InvalidCPUException, JSONException {
		super(id, json, "Saturating Counters", "satcountbank", "satcountbank_description", new Dimension(80, 100));

		int numCounters = json.getInt("num_counters");
		if(numCounters <= 1 || !Data.isPowerOf2(numCounters))
			throw new InvalidCPUException("Invalid number of counters (must be a power of 2)!");

        int width = json.getInt("width");
        if(width <= 0 || width > 16)
			throw new InvalidCPUException("Invalid width (must be 1 to 16 bits)!");

        // in width bits, the maximum value is (2^width) - 1
        max = (1 << width) - 1;

        initialValue = json.getInt("initial_value");
        if (initialValue < 0 || initialValue > max)
			throw new InvalidCPUException("Initial value does not fit in the counter!");

		forwarding = json.optBoolean("forwarding");

        // Initialize counters
		int requiredBits = Data.requiredNumberOfBits(numCounters - 1);
		counters = new Data[numCounters];
		for(int i = 0; i < numCounters; i++)
			counters[i] = new Data(width, initialValue);

        // Add inputs/outputs
        readIdx     =  addInput(json.getString("read_idx"),   new Data(requiredBits),   IOPort.Direction.WEST,  true, true);
        writeIdx    =  addInput(json.getString("write_idx"),  new Data(requiredBits),   IOPort.Direction.WEST,  true, true);
        update      =  addInput(json.getString("update"),     new Data(1),              IOPort.Direction.NORTH, true, true);
        countUp     =  addInput(json.getString("count_up"),    new Data(1),              IOPort.Direction.WEST,  true, true);
		readData    = addOutput(json.getString("read_data"),  new Data(width),          IOPort.Direction.EAST,  true);
	}

	@Override
	public void execute() {
		int indexR = getReadIdx().getValue();
		int indexW = getWriteIdx().getValue();
		boolean update = getUpdate().getValue() == 1;
		boolean countUp = getCountUp().getValue() == 1;

		if(isForwarding() && update && indexR == indexW) {
            int newValue = getCounter(indexW).getValue() + (countUp ? 1 : -1);

            if (newValue < 0)
                newValue = 0;

            if (newValue > max)
                newValue = max;

			getReadData().setValue(newValue);
        }
		else {
			getReadData().setValue(getCounter(indexR).getValue());
        }

		getWriteIdx().setRelevant(update);
		getCountUp().setRelevant(update);
	}

	@Override
	public void executeSynchronous() {
		int indexW = getWriteIdx().getValue();
		boolean update = getUpdate().getValue() == 1;
		boolean countUp = getCountUp().getValue() == 1;

		if(update) {
            int newValue = getCounter(indexW).getValue() + (countUp ? 1 : -1);

            if (newValue < 0)
                newValue = 0;

            if (newValue > max)
                newValue = max;

			counters[indexW].setValue(newValue);
        }
	}

	@Override
	public void pushState() {
		int[] values = new int[getNumberOfCounters()];
		for(int i = 0; i < getNumberOfCounters(); i++)
			values[i] = counters[i].getValue();
		states.push(values);
	}

	@Override
	public void popState() {
		if(hasSavedStates()) {
			int[] values = states.pop();
			for(int i = 0; i < getNumberOfCounters(); i++)
				counters[i].setValue(values[i]);
		}
	}

	@Override
	public boolean hasSavedStates() {
		return !states.empty();
	}

	@Override
	public void clearSavedStates() {
        System.out.println("SatCountBank clearSavedStates");
		states.clear();
	}

	@Override
	public void resetFirstState() {
		if(hasSavedStates()) {
			int[] values = states.peek();
			while(hasSavedStates())
				values = states.pop();
			for(int i = 0; i < getNumberOfCounters(); i++)
				counters[i].setValue(values[i]);
		}
	}

	@Override
	public boolean isWritingState() {
		return getUpdate().getValue() == 1;
	}

	/**
	 * Returns whether the data should be updated or read first if reading and writing to the same counter.
	 * @return <tt>True</tt> if internal forwarding is enabled.
	 */
	public final boolean isForwarding() {
		return forwarding;
	}

	/**
	 * Resets the counters back to their initial state
	 */
	public final void reset() {
        System.out.println("SatCountBank reset");
		for (Data counter: counters)
			counter.setValue(initialValue);
		execute();
	}

	/**
	 * Returns the number of counters.
	 * @return The number of counters.
	 */
	public final int getNumberOfCounters() {
		return counters.length;
	}

	/**
	 * Returns how many bits are required to identify a counter.
	 * @return Number of bits required to identify a counter.
	 */
	public final int getRequiredBitsToIdentifyCounter() {
		return Data.requiredNumberOfBits(getNumberOfCounters() - 1);
	}

	/**
	 * Returns a copy of the indicated counter.
	 * @param index Index/address of the counter.
	 * @return Copy of the indicated counter.
	 * @throws ArrayIndexOutOfBoundsException If the index is invalid.
	 */
	public final Data getCounter(int index) throws ArrayIndexOutOfBoundsException {
		return counters[index].clone();
	}

	/**
	 * Updates the value of the indicated counter.
	 * <p>The new counter is propagated to the rest of the circuit if it is being read.</p>
	 * @param index Index/address of the counter.
	 * @param newValue New value.
	 * @throws ArrayIndexOutOfBoundsException If the index is invalid.
	 */
	public final void setCounter(int index, int newValue) throws ArrayIndexOutOfBoundsException {
		setCounter(index, newValue, true);
	}

	/**
	 * Updates the value of the indicated counter.
	 * @param index Index/address of the counter.
	 * @param newValue New value.
	 * @param propagate Whether the new counter is propagated to the rest of the circuit if it is being read.
	 * @throws ArrayIndexOutOfBoundsException If the index is invalid.
	 */
	public final void setCounter(int index, int newValue, boolean propagate) throws ArrayIndexOutOfBoundsException {
        counters[index].setValue(newValue);
        if(propagate) execute();
	}

	/**
	 * Returns the readIdx input.
	 * @return The readIdx input.
	 */
	public final Input getReadIdx() {
		return readIdx;
	}

	/**
	 * Returns the writeIdx input.
	 * @return The writeIdx input.
	 */
	public final Input getWriteIdx() {
		return writeIdx;
	}

	/**
	 * Returns the readData output.
	 * @return The readData output.
	 */
	public final Output getReadData() {
		return readData;
	}

	/**
	 * Returns the countUp input.
	 * @return The countUp input.
	 */
	public final Input getCountUp() {
		return countUp;
	}

	/**
	 * Returns the update control input (that controls whether to update the counter).
	 * @return The update control input.
	 */
	public final Input getUpdate() {
		return update;
	}
}
