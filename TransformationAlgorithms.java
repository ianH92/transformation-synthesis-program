/** A class holding the transformation algorithms and their helper methods
 *	The class holds three algorithms which will synthesize a reversible circuit for a reversible
 *	Boolean function using Toffoli gates. The three algorithms are the output transformation
 *	algorithm, the input transformation algorithm, and the bidirectional algorithm. All three
 *	algorithms follow the method described in the paper below:
 *	Miller, D. Michael, Dmitri Maslov, and Gerhard W. Dueck. "A transformation based algorithm for
 *		reversible logic synthesis." Design Automation Conference, 2003. Proceedings. IEEE, 2003.
 *
 * The algorithms are all designed to take a double int array containing the truth table of the
 * reversible Boolean function and return an array containing the gates of the circuit.
 *
 * @author ian (ianH92)
 * @version 2.0
 * @since June 21st, 2017
 */
public class TransformationAlgorithms {
	
	/** The birdirectional transformation algorithm
	 *	Method applies the birdirectional algorithm as described in the paper above to transformTable
	 *	a given truth table to the identity and return an array representing the circuit that
	 *	realizes the reversible Boolean function specified in the truth table.
	 
	 * @param userInput The array representation of the truth table for the reversible function.
	 * @return The array representation of the gates that realize the Boolean function.
	 */
	public static int[][] bidirectionalAlgorithm(int[][] userInput) throws UserInputException {
		try {
			int rows = userInput.length;
			int columns = userInput[0].length;
			int maxNumGates = ((columns - 1) * ((int)Math.pow(2.0, columns))) + 1;
			int[][] outputGates = new int[maxNumGates][columns];
			int[][] inputGates = new int[maxNumGates][columns];
			int numberOfInputGates = 0;
			int numberOfOutputGates = 0;
			int[][] table = userInputToTable(userInput);
			
			for(int i = 0; i < rows; i++) {
				int input = table[findIndexForInputAlgorithm(table, i)][0];
				int h1 = hammingDistance(i, table[i][1], columns);
				int h2 = hammingDistance(i, input, columns);
				
				/* Decide whether to use an input transformation or an output transformation based on 
				 * the Hamming distances of the two possible steps. If HammingDistance(input, Expansion)
				 * is smaller that HammingDistance(input, index(input)) then use the output 
				 * transformation, else use the input transformation.
				 */
				if(h1 <= h2) {
					int index = table[i][0];
					int expansion = table[i][1];
					int target;
					
					if(i == 0) {
						// First step: Flip all the bits in f+(0) that are 1 to zero
						index = 0;
						expansion = table[index][1];
						target = table[index][1];
						if(expansion != index) {
							transformTable(target, 0, table, 1);
							addGate(outputGates, numberOfOutputGates, columns, 0, target);
							numberOfOutputGates++;
						}
					} else {
						index = table[i][0];
						expansion = table[i][1];
							
						if(index == expansion) {
							// If the row index equals the expansion, nothing needs to be done
						} else {
							// p represents all the 0 bits that must be flipped to 1 in the expansion
							int p = ((index ^ expansion) & index);
							// q represents all the 1 bits that must be flipped to 0 in the expansion
							int q = ((index ^ expansion) & expansion);
								
							// Process the targets in p
							for(int j = 0; j < columns; j++) {
								index = table[i][0];
								expansion = table[i][1];
								target = (1 << j);
								
								if((p & target) != 0) {
									int[] validControls = validControlLines(target, expansion, index);
									int control = bestControlLine(validControls, target, table, 1, columns);
									transformTable(target, control, table, 1);
									addGate(outputGates, numberOfOutputGates, columns, control, target);
									numberOfOutputGates++;
								}
							}
							
							// Process the targets in q
							for(int j = 0; j < columns; j++) {
								index = table[i][0];
								expansion = table[i][1];
								target = (1 << j);
								
								if((q & target) != 0) {
									int[] validControls = validControlLines(target, expansion, index);
									int control = bestControlLine(validControls, target, table, 1, columns);
									transformTable(target, control, table, 1);
									addGate(outputGates, numberOfOutputGates, columns, control, target);
									numberOfOutputGates++;
								}
							}
						}
					} 
				} else {
					// Find the location of the expansion that matches the current index i
					int index = findIndexForInputAlgorithm(table, i);
					input = table[index][0];
					
					if(input == table[index][1]) {
						// If the input equals the expansion, nothing needs to be done
					} else {
						// p represents all the 0 bits that must be flipped to 1 in the expansion
						int p = ((i ^ input) & i);
						// q represents all the 1 bits that must be flipped to 0 in the expansion
						int q = ((i ^ input) & input);
						
						int target = 0;
						// Process the targets in p
						for(int j = 0; j < columns; j++) {
							input = table[index][0];
							target = (1 << j);
							
							if((p & target) != 0) {
								int[] validControls = validControlLines(target, input, i);
								int control = bestControlLine(validControls, target, table, 0, columns);
								transformTable(target, control, table, 0);
								addGate(inputGates, numberOfInputGates, columns, control, target);
								numberOfInputGates++;
							}
						}
						
						// Process the targets in q
						for(int j = 0; j < columns; j++) {
							input = table[index][0];
							target = (1 << j);
							
							if((q & target) != 0) {
								int[] validControls = validControlLines(target, input, i);
								int control = bestControlLine(validControls, target, table, 0, columns);
								transformTable(target, control, table, 0);
								addGate(inputGates, numberOfInputGates, columns, control, target);
								numberOfInputGates++;
							}
						}
						
						table = sortTable(table);
					}	
				}
			}
			
			int[][] newGates = reduceAndMergeGates(numberOfInputGates, numberOfOutputGates, inputGates, outputGates, columns);
			return newGates;
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new UserInputException("Error: Entered function is not reversible.");
		}
	}
	
	/** Method which sorts the table for the bidirectional algorithm.
	 *	The method assumes a double int array with each array having a length of two. The array
	 *	is sorted based on the value of the 0th index of each array.
	 *
	 * @param table The table to be sorted.
	 * @return The sorted table.
	 */
	private static int[][] sortTable(int[][] table) {
		int rows = table.length;
		int[][] newTable = new int[rows][2];
		
		for(int i = 0; i < rows; i++) {
			for(int j = 0; j < rows; j++) {
				if(table[j][0] == i) {
					newTable[i][0] = table[j][0];
					newTable[i][1] = table[j][1];
				}
			}
		}
		return newTable;
	}
	
	/** Method merges output generated gates and input generated gates for bidirectional alg.
	 *	The input gates are added to the front in forward order and the output gates are then addded 
	 *	in reverse order.
	 *
	 * @param numberOfGatesInput The number of input gates that were generated.
	 * @param numberOfGatesOutput The number of output gates that were generated.
	 * @param inputGates The array containing the input gates.
	 * @param outputGates The array containing the output gates.
	 * @param numberOfInputs The number of inputs the Boolean function has.
	 * @return The array representation of the circuit.
	 */
	private static int[][] reduceAndMergeGates(int numberOfGatesInput, int numberOfGatesOutput, 
	int[][] inputGates, int[][] outputGates, int numberOfInputs) {
		
		int[][] newGates = new int[numberOfGatesInput + numberOfGatesOutput][numberOfInputs];
		int len = newGates.length;
		
		int i;
		for(i = 0; i < numberOfGatesInput; i++) {
			// Add the input gates to the front of the circuit
			for(int j = 0; j < numberOfInputs; j++) {
				newGates[i][j] = inputGates[i][j];
			}
		}
		
		int outputIndex = (numberOfGatesOutput - 1);
		for(i = i; i < len; i++) {
			// Add the output gates in reverse order to the circuit
			for(int j = 0; j < numberOfInputs; j++) {
				newGates[i][j] = outputGates[outputIndex][j];
			}
			outputIndex--;
		}
		
		return newGates;
	}
	
	/** The input transformation algorithm; performs an input based transformation on truth table
	 *	The method takes an array representation of the truth table and performs an input based
	 *	transformation on the truth table, transforming it to the identity. The method returns the
	 *	array representation of the circuit that realizes the Boolean function.
	 *
	 * @param userInput The array representation of the truth table for the Boolean function.
	 * @return The array representation of the generated circuit.
	 */
	public static int[][] inputAlgorithm(int[][] userInput) throws UserInputException {
		try {
			int rows = userInput.length;
			int columns = userInput[0].length;
			int maxNumGates = ((columns - 1) * ((int)Math.pow(2.0, columns))) + 1;
			int[][] gates = new int[maxNumGates][columns];
			int numberOfGates = 0;
			int[][] table = userInputToTable(userInput);
			
			for(int i = 0; i < rows; i++) {
				int index = findIndexForInputAlgorithm(table, i);
				int input = table[index][0];
				
				if(input == table[index][1]) {
					// If the input equals the expansion, nothing needs to be done
				} else {
					// p represents all the 0 bits that must be flipped to 1 in the expansion
					int p = ((i ^ input) & i);
					// q represents all the 1 bits that must be flipped to 0 in the expansion
					int q = ((i ^ input) & input);
					
					int target = 0;
					// Process the targets in p
					for(int j = 0; j < columns; j++) {
						input = table[index][0];
						target = (1 << j);
						
						if((p & target) != 0) {
							int[] validControls = validControlLines(target, input, i);
							int control = bestControlLine(validControls, target, table, 0, columns);
							transformTable(target, control, table, 0);
							addGate(gates, numberOfGates, columns, control, target);
							numberOfGates++;
						}
					}
					
					// Process the targets in q
					for(int j = 0; j < columns; j++) {
						input = table[index][0];
						target = (1 << j);
						
						if((q & target) != 0) {
							int[] validControls = validControlLines(target, input, i);
							int control = bestControlLine(validControls, target, table, 0, columns);
							transformTable(target, control, table, 0);
							addGate(gates, numberOfGates, columns, control, target);
							numberOfGates++;
						}
					}
				}
			}
			
			// Process the gates
			int[][] newGates = reduceGates(gates, numberOfGates, columns);
			
			// Create the circuit object
			return newGates;
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new UserInputException("Error: Entered function is not reversible.");
		}
	}
	
	/** The output transformation algorithm; performs an output based transformation on truth table
	 *	The method takes an array representation of the truth table and performs an output based
	 *	transformation on the truth table, transforming it to the identity. The method returns the
	 *	array representation of the circuit that realizes the Boolean function.
	 *
	 * @param userInput The array representation of the truth table for the Boolean function.
	 * @return The array representation of the generated circuit.
	 */
	public static int[][] outputAlgorithm(int[][] userInput) throws UserInputException {
		try {
			int rows = userInput.length;
			int columns = userInput[0].length;
			int maxIndex = (columns - 1);
			int maxNumGates = ((columns - 1) * ((int)Math.pow(2.0, columns))) + 1;
			int[][] gates = new int[maxNumGates][columns];
			int numberOfGates = 0;
			
			int[][] table = userInputToTable(userInput);
			
			// First step: Flip all the bits in f+(0) that are 1 to zero
			int index = 0;
			int expansion = table[index][1];
			int target = table[index][1];
			if(expansion != index) {
				transformTable(target, 0, table, 1);
				addGate(gates, numberOfGates, columns, 0, target);
				numberOfGates++;
			}
			
			for(int i = 1; i < rows; i++) {
				index = table[i][0];
				expansion = table[i][1];
				
				if(index == expansion) {
					// If the row index equals the expansion, nothing needs to be done
				} else {
					// p represents all the 0 bits that must be flipped to 1 in the expansion
					int p = ((index ^ expansion) & index);
					// q represents all the 1 bits that must be flipped to 0 in the expansion
					int q = ((index ^ expansion) & expansion);
					
					
					// Process the targets in p
					for(int j = 0; j < columns; j++) {
						index = table[i][0];
						expansion = table[i][1];
						target = (1 << j);
						
						if((p & target) != 0) {
							int[] validControls = validControlLines(target, expansion, index);
							int control = bestControlLine(validControls, target, table, 1, columns);
							transformTable(target, control, table, 1);
							addGate(gates, numberOfGates, columns, control, target);
							numberOfGates++;
						}
					}
					
					// Process the targets in q
					for(int j = 0; j < columns; j++) {
						index = table[i][0];
						expansion = table[i][1];
						target = (1 << j);
						if((q & target) != 0) {
							int[] validControls = validControlLines(target, expansion, index);
							int control = bestControlLine(validControls, target, table, 1, columns);
							transformTable(target, control, table, 1);
							addGate(gates, numberOfGates, columns, control, target);
							numberOfGates++;
						}
					}
				}
			}
			
			// Process the gates
			int[][] newGates = reverseAndReduceGates(gates, numberOfGates, columns);
			
			// Create the circuit object
			return newGates;
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new UserInputException("Error: Entered function is not reversible.");
		}
	}
	
	/** Finds the index of the expansion that matched the input side for the input algorithm.
	 *	The method searches the truth table for the index of the expansion that matches the input
	 *	side and returns it; the input algorithm uses this to find the input side index it needs to
	 *	transform.
	 *
	 * @param table The array representation of the truth table.
	 * @param index The input side value that needs to be matched to an expansion.
	 */
	private static int findIndexForInputAlgorithm(int[][] table, int index) throws UserInputException {
		try {
			int rows = table.length;
			for(int i = 0; i < rows; i++) {
				if(table[i][1] == index) {
					return i;
				}
			}
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new UserInputException("Error: Entered Function not reversible.");
		}
		// Should never reach here, assuming valid input.
		return -1;
	}
	
	/** Reduces length and reverses gates generated; used by output transformation algorithm.
	 *	The method takes an array of gates and removes any unused gates, while simultaneously
	 *	reversing the gates as per the specification of the output transformation algorithm.
	 *
	 * @param oldGates The array representation of the gates generated by the output algorithm.
	 * @param numberOfGates The number of gates in oldGates.
	 * @param numberOfLines The number of lines in the circuit.
	 */
	private static int[][] reverseAndReduceGates(int[][] oldGates, int numberOfGates, int numberOfLines) {
		int[][] newGates = new int[numberOfGates][numberOfLines];
		int oldGateIndex = (numberOfGates - 1);
		for(int i = 0; i < numberOfGates; i++) {
			for(int j = 0; j < numberOfLines; j++) {
				newGates[i][j] = oldGates[oldGateIndex][j];
			}
			oldGateIndex --;
		}
		return newGates;
	}
	
	/** Reduces the length of an array representation of the gates; used by the input algorithm.
	 *	Method takes an array of gates generated by the input transformation algorithm and reduces
	 *	the length of the array.
	 *
	 * @param oldGates The array representation of the gates generated by the input algorithm.
	 * @param numberOfGates The number of gates in oldGates.
	 * @param numberOfLines The number of lines in the circuit.
	 */
	private static int[][] reduceGates(int[][] oldGates, int numberOfGates, int numberOfLines) {
		int[][] newGates = new int[numberOfGates][numberOfLines];
		for(int i = 0; i < numberOfGates; i++) {
			for(int j = 0; j < numberOfLines; j++) {
				newGates[i][j] = oldGates[i][j];
			}
		}
		return newGates;
	}
	
	/** Takes userInput representation of truth table and transforms it to algorithm version
	 * 	The method transforms the userInput form of the truth table to one that can be used by
	 *	the three transformation algorithms.
	 *
	 * @param userInput The array representation of the truth table.
	 * @return The array representation of the truth table.
	 */
	private static int[][] userInputToTable(int[][] userInput) {
		int rows = userInput.length;
		int columns = 0;
		
		// If passed an invalid table, return null
		if(userInput[0] != null) {
			columns = userInput[0].length;
		} else {
			return null;
		}
		
		int[][] vals = new int[rows][2];
		
		int maxIndex = columns - 1;
		int sum;
		for(int i = 0; i < rows; i++) {
			sum = 0;
			for(int j = 0; j < columns; j++) {
				if(userInput[i][j] == 1) {
					// Convert the user input to a number for easy manipulation.
					sum += (int) Math.pow(2, (double) (maxIndex - j));
				}
			}
			// The first index is the int representation of the input side
			vals[i][0] = i;
			// The second index is the int representation of the output side
			vals[i][1] = sum;
		}
		
		return vals;
	}
	
	/** Calculates the Hamming distance between two numbers in Binary form.
	 *	The method calcualtes the Hamming distance between two binary numbers; the Hamming distance
	 *	is the number of positions by which the two binary numbers differ.
	 *
	 * @param a The first number.
	 * @param b The second number.
	 * @param numberOfInputs The number of positions in the binary numbers.
	 * @return The Hamming distance.
	 */
	private static int hammingDistance(int a, int b, int numberOfInputs) {
		int hammingDist = 0;
		int bitMask = 0;
		for(int i = (numberOfInputs - 1); i >= 0; i--) {
			bitMask = (1 << i);
			if((a & bitMask) != (b & bitMask)) {
				hammingDist++;
			}
		}
		return hammingDist;
	}
	
	/** Calculates the complexity of given array representation of truth table.
	 *	The method calculates the Hamming Distance for every input-output pair in the table and sums
	 *	these values. This number represents the complexity of the table. This method is used in the
	 *	transformation algorithms to decide which subset of possible control lines to use; the
	 *	control lines set with the lowest complexity are the controls which bring the truth table
	 *	closest to the identity, and which will result in the lowest number of gates.
	 *
	 * @param table The array representation of the truth table.
	 * @param numberOfInputs The number of inputs to the Boolean function.
	 * @return The complexity of the table.
	 */
	private static int tableComplexity(int [][] table, int numberOfInputs) {
		int complexity = 0;
		int rows = table.length;
		for(int i = 0; i < rows; i++) {
			complexity += hammingDistance(table[i][0], table[i][1], numberOfInputs);
		}
		return complexity;
	}
	
	/** Computes the set of valid control lines for a transformation.
	 *	The method takes a number that needs to be transformed, a number to transform that number to,
	 *	and a number that represents the target bit to flip. From these values the method generates
	 *	all possible valid control lines for the given target.
	 *
	 * @param target Represents the bit(s) to be flipped with 1s, 0s in non-target positions.
	 * @param numberToMapfrom The number that needs to be transformed.
	 * @param numberToMapTo The number to transform to.
	 * @return An array holding all valid controls for the transformation.
	 */
	private static int[] validControlLines(int target, int numberToMapfrom, int numberToMapTo) {
		int[] controlLines = new int[numberToMapfrom];
		int numberOfControlsAdded = 0;
		int arrayIndex = 0;
		
		for(int i = numberToMapfrom; i >= numberToMapTo; i--) {
			if((i & target) != 0) {
				/* Not a valid subset of controls, the control lines cannot include a 1 bit where
				 * there is a target.
				 */
			} else {
				if((numberToMapfrom & i) != i) {
					/* Not a valid subset of controls, the control lines cannot include a 1 bit
					 * where the numberToMapFrom does not have a 1 bit.
					 */
				} else {
					controlLines[arrayIndex++] = i;
					numberOfControlsAdded++;
				}
			}
		}
		
		controlLines = reduceArray(controlLines, numberOfControlsAdded);		
		return controlLines;
	}
	
	/** Reduces the length of array by removing usused indices.
	 *	The method, given an array and a number of elements in the array, will generate a new array
	 *	of length numberOfValues which contains the elements in the array and no unused space. The
	 *	method returns null if the number of elements is higher more than can be contained in the
	 *	original array.
	 *
	 * @param array The array to be reduced.
	 * @param numberOfValues The number of values in the array to be reduced.
	 * @return The new array containing the old values and no extra space.
	 */
	private static int[] reduceArray(int[] array, int numberOfValues) {
		if(numberOfValues > array.length) {
			return null;
		}
		int[] newArray = new int[numberOfValues];
		for(int i = 0; i < numberOfValues; i++) {
			newArray[i] = array[i];
		}
		return newArray;
	}
	
	/** Transforms the table by applying a transform to all elements on a side that match.
	 *	The method takes an array representation of a truth table with each array having a length
	 *	of two and transforms one side of that truth table. Each element on a side is transfomed by
	 *	the target (if it matches the control lines) by flipping the bits in the element where
	 *	target is 1. The element matches if it has 1 bits where controlLines has 1 bits. The
	 *	variable tableColumn represents which side of the table to transform, input or output. 0 = 
	 *	input and 1 = output.
	 *
	 * @param target Represents the bit(s) to be flipped with 1s, 0s in non-target positions.
	 * @param controlLines Represents the control lines with 1 bits.
	 * @param table The array representation of the truth table to be transformed.
	 * @param tableColumn The side of the table to be transfomed, 0 = the input side, 1 = the output
	 */
	private static void transformTable(int target, int controlLines, int[][] table, int tableColumn) {
		int rows = table.length;
		for(int i = 0; i < rows; i++) {
			int val = table[i][tableColumn];
			if((val & controlLines) == controlLines) {
				table[i][tableColumn] = (val ^ target);
			}
		}
	}
	
	/** Method that determines control lines with the least complexity for a set of control lines.
	 *	The method applys each possible group of control lines for an array of possible control
	 *	lines and calculates the complexity each group of control lines results in; the method then
	 *	returns the control lines that result in the lowest complexity.
	 *
	 * @param controlLines The array of possible control lines.
	 * @param target Represents the bit(s) to be flipped with 1s, 0s in non-target positions.'
	 * @param table The array representation of the truth table to be transformed.
	 * @param tableColumn The side of the table to be transfomed, 0 = the input side, 1 = the output
	 * @param numberOfInputs The number of inputs to the Boolean function.
	 * @return The int rep. of the control lines with the lowest resulting complexity.
	 */
	private static int bestControlLine(int[] controlLines, int target, int[][] table, int tableColumn, int numberOfInputs) throws UserInputException {
		int bestControlLine = 0;
		try {
			int rows = controlLines.length;
			int minComplexity = Integer.MAX_VALUE;
			bestControlLine = controlLines[0];
			int currLow = numberOfOnes(controlLines[0],  numberOfInputs);
			
			for(int i = 0; i < rows; i++) {
				if(currLow > numberOfOnes(controlLines[i], numberOfInputs)) {
					bestControlLine = controlLines[i];
				}
			}
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new UserInputException("Error: Entered Function is not reversible.");
		}
		
		return bestControlLine;
	}
	
	/** Determines the number of ones in number.
	 *
	 * @param a The number.
	 * @param numberOfInputs The number of positions in the binary numbers.
	 * @return The number of ones.
	 */
	private static int numberOfOnes(int a, int numberOfInputs) {
		int numberOfOnes = 0;
		int bitMask = 0;
		for(int i = (numberOfInputs - 1); i >= 0; i--) {
			bitMask = (1 << i);
			if((a & bitMask) != 0) {
				numberOfOnes++;
			}
		}
		return numberOfOnes;
	}
	
	/** Creates a deep copy of a double int array.
	 * @param array The array to be copied.
	 * @return The deep copy of the array.
	 */
	private static int[][] copyArray(int[][] array) {
		int rows = array.length;
		int columns = array[0].length;
		int[][] arrayCopy = new int[rows][columns];
		
		for(int i = 0; i < rows; i++) {
			for(int j = 0; j < columns; j++) {
				arrayCopy[i][j] = array[i][j];
			}
		}
		return arrayCopy;
	}
	
	/** Helper method that adds a representation of a a gate to an array.
	 * @param gateArray The array of gates to add the gate to.
	 * @param row The row to add the gate to.
	 * @param numberOfColumns The number of columns in the gate array (and # inputs to function).
	 *
	 */
	private static void addGate(int[][] gateArray, int row, int numberOfColumns, int controlLines, int target) {
		for(int i = 0; i < numberOfColumns; i++) {
			if((controlLines & (1 << i)) != 0) {
				gateArray[row][i] = 1;
			}
			if((target & (1 << i)) != 0) {
				gateArray[row][i] = 2;
			}
		}
	} 
}