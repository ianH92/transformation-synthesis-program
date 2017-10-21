Ian D. (ianH92)
October 20th, 2017

TransformationSynthesisProgram README

Program Information:

	This program implements the bidirectional and enhanced transformation based synthesis method 
for completely-specified Boolean functions. The program allows a user to enter a 
completely-specified reversible Boolean function in the form of a truth table via a graphical user 
interface. The program then allows the user to select one of three possible algorithms: the 
output-based transformation algorithm, the input-based transformation algorithm, or the 
bidirectional transformation algorithm. The program then uses the specification of the Boolean 
function and the chosen algorithm to create a circuit composed of Toffoli gates that realizes the 
Boolean function (Miller et al.  318). The program will then display a visualization of the circuit. 
As is common in the field, a crosshair symbol designates a target in the Toffoli gate and a circle 
designates a control line.

Testing the Program:
	Contained in the program directory are seven .txt files named test1.txt to test7.txt. These
files contain prepared specifications that can be used to test the program and can be loaded to the 
program as described below.

Using the Program:

	In the directory containing the program's java files type "javac *.java" to compile the files.
Then type "java TransformationSynthesisProgram" to run the program. The program's GUI should appear
immediately.The program supports three methods of entering the Boolean function specification; one 
by user input and the other by file input.
	
	The first involves the user entering the function specification 
into text fields on the left of the program. The user can also increase the number of text fields. 
To do so enter the number into the text field labeled "# of Function Inputs" and click the button 
labeled "Draw Truth Table". The program will only accept values in the range of [0, 31].
	
	The second method for entering a specification is using the file input feature. This feature 
allows the user to load a file into the text fields from a saved .txt file in the same directory as 
the program. To use this option, click on File in the menu bar and then select “Open File”. A prompt 
will appear asking for a file name. Input the file name, including the extension, and click 
"Load Specification from File". This will create text fields large enough to hold the values and 
then enter those values into the text fields to await editing or circuit generation.
	
	The third input method allows the user to load the file directly to the circuit display, 
skipping the text fields. To select this option, click File on the menu bar and then select 
"Open File Directly to Display". Then enter the file name and extension in the same manner as 
described above. Both file methods will display an error if the file format is not correct.

	The file format accepted by the program consists of a .txt file with the first line containing 
the number of inputs to the program followed by a newline character. The subsequent lines consist 
of the truth table specification; each line in the file is a row of the truth table. In each row, 
the inputs are separated by a single space and ends with a newline character.

	Once a specification is entered, the user can designate which algorithm the program should use 
to generate the circuit by selecting one of the three toggle buttons to the left of the 
"Generate Circuit" button; the user can choose either the output, input, or bidirectional 
algorithms. Note: these options may be hidden if the program window is too small, and can be 
revealed by resizing the program horizontally or by clicking the >> symbol on the right-hand side 
of the toolbar.
	Finally, once the user has entered the specification they generate the circuit by clicking the 
"Generate Circuit" button on the top toolbar.


Internal Program Logic:
	I designed the program to gather the specification for the function in a double array of 
integers with a length equal to the number of rows, and where each row is an array of two values. 
The first index in each row held the numeric value of the input side of the Boolean function and the 
second index held the numeric value of the output side of the Boolean function. Because an int in 
Java is a signed number with 32 bits, in practice this design decision would limit the user to 
entering up to 2^31 = 2,147,483,648 rows (a 31 input function). I felt this design decision was 
justifiable because most computers could not run larger inputs anyway due to memory constraints. I 
designed the program to store the user inputs and outputs as single numbers to allow for easy 
manipulation by the internal program logic using bitwise AND and XOR.
	Once the program has stored the user input in the array described above, the array is passed to 
either the output-based, input-based, or bidirectional algorithm. The algorithms are all implemented 
similarly to the methods described in "A Transformation Based Algorithm for Reversible Logic 
Synthesis" by Miller, Malsov, and Dueck but there are a few modifications I made for practical 
reasons.
	The input and output based algorithms both perform in the following manner. The algorithm 
determines a value J which must be mapped to a value I. The program then calculates the following 
numbers:
	P = ((I BitwiseXOR J) BitwiseAND I)
	Q = (I BitwiseXOR J) BitwiseAND J)
P is the number with 1s in the positions where the number J has 0s that must be transformed to ones.
For each 1 in the number P, a gate with a target corresponding to the line in that position must be 
added to the circuit (Miller et al. 319). Q is the number with 1s in positions where the number J 
has 1s that must be flipped to 0s. Like P, for each 1 in Q a gate must be added to the circuit 
with a target in the position of the 1 value (319).
	For each gate that is being added to the circuit, it must be determined which control lines 
should be used. The paper described a method of determining the best control lines by choosing the 
set of valid control lines which results in the table with the lowest complexity (320). Originally, 
I implemented this as described, where each valid set of control lines was applied to the table with 
the target and compared to the others to determine the lowest complexity. I found that this method 
quickly caused the program to run out of heap memory for inputs above 5.
	The modification I introduced has the program discover the “best” control lines in the following 
manner. For a number J being transformed to a number I the program enumerates all of the possible 
valid control lines, which corresponds to all numbers x in the range [I, J] such that x does not 
have 1s in the same position as the target and x does not have 1s in positions where the number J 
does not have 1s. This is implemented in the method validControlLines in 
TransformationAlgorithms.java. Then the program simply chooses the control lines which have the 
fewest number of individual controls in them and uses these controls to construct the gate. Then the 
new gate is applied to any input/output in the table where the controls match the input/output.
	For the input algorithm the number I equals the current index and the number J equals the index 
where the expansion equals I. For the output algorithm, the number I equals the current index and 
the number J equals the expansion of I (Miller et al. 319). The bidirectional algorithm works simply 
by comparing the Hamming distance between I and J for the input transformation and the Hamming 
distance for I and J in the output transformation and then choosing the transformation with the 
smallest distance (320). 
	A final note on the design of the program is that I originally designed the program to be 
capable of accepting any reversible Boolean function with a number of inputs in the range of 1 to 3
1. The logic implemented in the transformation algorithm supports this in theory but in practice 
this would consume far too much memory for the program to handle. A further practical limit is 
imposed by the amount of memory that the user input truth tables require.

Works Cited
	Miller, D. Michael, Dmitri Maslov, and Gerhard W. Dueck. "A transformation based algorithm for 
reversible logic synthesis." Design Automation Conference, 2003. Proceedings. IEEE, 2003.