import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.event.ActionEvent; 
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Modality;
import javafx.scene.control.ScrollPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Color;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.RadioButton;
import javafx.geometry.Pos;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.NoSuchElementException;
import java.util.InputMismatchException;

/** A class which creates a UI for entering a reversible Boolean function and generating a circuit.
 *	The class allows the user to graphically enter a reversible Boolean function in the form of a
 *	truth table and then realize that function as a graphical representation of a circuit using
 *	either the output, input, or bidirectional transformation based synthesis algorithms. The UI 
 *	also supports loading a Boolean function specification from a file.
 *
 * @author ian (ianH92)
 * @version 2.0
 * @since June 20th, 2017
 */
public class TransformationSynthesisProgram extends Application {
	// Use this flag to determine what method of user input is being used
	private int algorithmFlag = 0;
	
	// This is the current number of function inputs
	private int inputs = 3;
	
	// Variables that hold the truth tables
	private TextField[] userInput;
	private GridPane truthTable;
	private Canvas t;
	private Group r;
	
	// The labels used for inputs and outputs
	private String[] labels = {"a", "b", "c", "d", "e",
							"f", "g", "h", "i", "j",
							"k", "l", "m", "n", "o",
							"p", "q", "r", "s", "t",
							"u", "v", "w", "x", "y", "z"};
	
	
	/** The main method of the program.
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		launch(args);
	}
	
	/** The start method of the program.
	 *	Here all the components of the UI and most of their functions are created.
	 * @param primaryStage The main stage of the program.
	 */
	@Override
	public void start(Stage primaryStage) {
		final double WIDTH = 800.0;
		final double HEIGHT = 600.0;
		final double INSET = 10.0;
		final double TTABLEWIDTH = (WIDTH / 4.0);
		final double TTABLEHEIGHT = (HEIGHT / 2.0);
		
		// Set the title of the program
		primaryStage.setTitle("Transformation Based Synthesis Program");
		
		// Create truthTable
		userInput = userInputFieldsSimple(inputs);
		truthTable = truthTableSimple(TTABLEWIDTH, TTABLEHEIGHT, inputs, userInput);
		
		// Create the truth table scroll pane and put the complex truth table in it	
		ScrollPane sL = new ScrollPane();
		sL.setContent(truthTable);
		sL.setFitToHeight(true);
		sL.setMinHeight(350.0);
		sL.setMinWidth(250.0);
		
		// Create initial circuit display
		this.t = CircuitDisplay(500, 200, null);
		this.r = new Group();
		this.r.getChildren().add(t);
		
		// Create the circuit display ScollPane and add the initial ciruit display to it
		ScrollPane sB = new ScrollPane();
		sB.setContent(r);
		sB.setFitToHeight(true);
		sB.setFitToWidth(true);
		
		// Create the top menu, menu options, and menu items
		MenuBar mainMenu = new MenuBar();
		
		// Create the file menu and file menu items
		final Menu file = new Menu("File");
		MenuItem load1 = new MenuItem("Open File");
		load1.setOnAction(e -> {
			// Create the prompt stage for the file
			Stage filePrompt = new Stage();
			filePrompt.initModality(Modality.WINDOW_MODAL);
			VBox fileName = new VBox(10);
			fileName.setAlignment(Pos.CENTER);
			Label f = new Label("Enter full file name including extension.");
			TextField fileNme = new TextField("<file_name>.txt");
			Button fileButton = new Button("Load specification from File.");
			
			fileButton.setOnAction(eV -> {
				String name = fileNme.getText();
				try {
					String[][] specification = readSpecificationFileToString(name);
					int rows = specification.length;
					int columns = specification[0].length;
					inputs = columns;
					
					// Create the simple truthTable
					userInput = userInputFieldsSimple(inputs);
					truthTable = truthTableSimple(TTABLEWIDTH, TTABLEHEIGHT, inputs, userInput);
					
					for(int i = 0; i < rows; i++) {
						String tmp = "";
						for(int j = 0; j < columns; j++) {
							tmp += specification[i][j];
						}
						userInput[i].setText(tmp);
					}
					
					sL.setContent(truthTable);
					filePrompt.close();
				} catch(UserInputException err) {
					errorDisplay(err);
				}
			});
			
			fileName.getChildren().addAll(f, fileNme, fileButton);
			filePrompt.setScene(new Scene(fileName, 400, 400));
			filePrompt.show();
		});
		//
		MenuItem load2 = new MenuItem("Open File Directly to Display");
		load2.setOnAction(e -> {
			// Create the prompt stage for the file
			Stage filePrompt = new Stage();
			filePrompt.initModality(Modality.WINDOW_MODAL);
			VBox fileName = new VBox(10);
			fileName.setAlignment(Pos.CENTER);
			Label f = new Label("Enter full file name including extension.");
			TextField fileNme = new TextField("<file_name>.txt");
			Button fileButton = new Button("Load specification from File.");
			
			fileButton.setOnAction(eV -> {
				String name = fileNme.getText();
				try {
					int[][] input = readSpecificationFile(name);
					
					if(this.algorithmFlag == 0) {
						input = TransformationAlgorithms.outputAlgorithm(input);
					} else if(this.algorithmFlag == 1) {
						input = TransformationAlgorithms.inputAlgorithm(input);
					} else {
						input = TransformationAlgorithms.bidirectionalAlgorithm(input);
					}
					
					this.t = null;
					this.r.getChildren().clear();
					this.t = CircuitDisplay(WIDTH, HEIGHT, input);
					this.r.getChildren().add(t);
					sB.setContent(r);
					filePrompt.close();
				} catch(UserInputException err) {
					errorDisplay(err);
				}
			});
			
			fileName.getChildren().addAll(f, fileNme, fileButton);
			filePrompt.setScene(new Scene(fileName, 400, 400));
			filePrompt.show();
		});
		file.getItems().addAll(load1, load2);
		
		final Menu help = new Menu("Help"); 
		mainMenu.getMenus().addAll(file, help);
		
		// Create the toolbar
		ToolBar topBar = new ToolBar();
		// Create the toolbar buttons
		Label exp = new Label("# of Function Inputs");
		TextField functionInputs = new TextField();
		functionInputs.setPromptText("#");
		functionInputs.setPrefWidth(40);
		Button tTableGen = new Button("Draw Truth Table");
		tTableGen.setOnAction(e -> {
			String tmp = functionInputs.getText();
			try {
				int tmp2 = Integer.parseInt(tmp);
				if(tmp2 > 26 || tmp2 < 1) {
					errorDisplay(new UserInputException("Program only accepts values in the " +
					"range of [1, 26] for # of function arguments"));
				} else {
					inputs = tmp2;
					userInput = userInputFieldsSimple(inputs);
					truthTable = truthTableSimple(TTABLEWIDTH, TTABLEHEIGHT, inputs, userInput);
					sL.setContent(truthTable);
				}
			} catch(NumberFormatException err) {
				errorDisplay(new UserInputException("Value entered for # of function arguments " +
													"is not a number"));
			}
		});
		Separator s = new Separator();
		// Create the generate circuit button
		Button generate = new Button("Generate Circuit");
		generate.setOnAction(e -> {
			try {
				int[][] input = null;
				input = processUserInputSimple(userInput);
				
				if(this.algorithmFlag == 0) {
					input = TransformationAlgorithms.outputAlgorithm(input);
				} else if(this.algorithmFlag == 1) {
					input = TransformationAlgorithms.inputAlgorithm(input);
				} else {
					input = TransformationAlgorithms.bidirectionalAlgorithm(input);
				}
				
				this.t = null;
				this.r.getChildren().clear();
				this.t = CircuitDisplay(WIDTH, HEIGHT, input);
				this.r.getChildren().add(t);
				sB.setContent(r);
			} catch(UserInputException err) {
				errorDisplay(err);
			}
		});
		// Create the clear all inputs button
		Button clearInput = new Button("Clear all Inputs");
		clearInput.setOnAction(e -> {
				clearUserInputFieldsSimple(userInput);
			}
		);
		// Create the toggles for the algorithm options
		ToggleGroup g = new ToggleGroup();
		RadioButton outAlg = new RadioButton("Output Algorithm");
		outAlg.setOnAction(e -> {
			this.algorithmFlag = 0;
		});
		RadioButton inAlg = new RadioButton("Input Algorithm");
		inAlg.setOnAction(e -> {
			this.algorithmFlag = 1;
		});
		RadioButton biAlg = new RadioButton("Bidirectional Algorithm");
		biAlg.setOnAction(e -> {
			this.algorithmFlag = 2;
		});
		outAlg.setSelected(true);
		outAlg.setToggleGroup(g);
		inAlg.setToggleGroup(g);
		biAlg.setToggleGroup(g);
		topBar.getItems().addAll(exp, functionInputs, tTableGen, s, generate, outAlg, inAlg, biAlg, clearInput);
		
		// Create the layout for the stage
		GridPane mainLayout = new GridPane();
		mainLayout.setHgap(0);
		mainLayout.setVgap(0);
		ColumnConstraints c1 = new ColumnConstraints();
		ColumnConstraints c2 = new ColumnConstraints();
		c1.setHgrow(Priority.ALWAYS);
		RowConstraints r1 = new RowConstraints();
		r1.setVgrow(Priority.NEVER);
		RowConstraints r2 = new RowConstraints();
		r2.setVgrow(Priority.NEVER);
		RowConstraints r3 = new RowConstraints();
		r3.setVgrow(Priority.ALWAYS);
		mainLayout.getColumnConstraints().addAll(c1, c2);
		mainLayout.getRowConstraints().addAll(r1, r2, r3);
		mainLayout.add(mainMenu, 0, 0, 2, 1);
		mainLayout.add(topBar, 0, 1, 2, 1);
		mainLayout.add(sL, 0, 2);
		mainLayout.add(sB, 1, 2);//, 1, 2);
		
		primaryStage.setScene(new Scene(mainLayout, WIDTH, HEIGHT));
		primaryStage.show();
	}
	
	/** Creates a grahpical display of a circuit
	 * @param width The width the default display will have
	 * @param height The height the default circuit will have
	 * @param gates The array representation of the gates to be displayed
	 * @return The Canvas that displays the circuit
	 */
	private Canvas CircuitDisplay(double width, double height, int[][] gates) {
		double gateSeparation = 50;
		double verticalPadding = 30.0;
		double horizontalPadding = 50.0;
		double lineSeparation = 30.0;
		double lineLength;
		int numberOfGates;
		int numberOfInputs;
		
		if(gates == null) {
			numberOfGates = 0;
			numberOfInputs = 3;
			lineLength = width - (verticalPadding * 3);
		} else {
			numberOfGates = gates.length;
			numberOfInputs = gates[0].length;
			lineLength = (numberOfGates + 1) * gateSeparation;
			width = (lineLength + 100.0);
			height = (numberOfInputs * lineSeparation) + 50.0;
		}
		
		Canvas circuitDisplay = new Canvas(width, height);
		GraphicsContext g = circuitDisplay.getGraphicsContext2D();
		
		double hLeftPos = horizontalPadding;
		double vLeftPos = verticalPadding;
		
		for(int i = 0; i < numberOfInputs; i++) {
			g.strokeLine(hLeftPos, vLeftPos, hLeftPos + lineLength, vLeftPos);
			g.setLineWidth(0.75);
			g.strokeText(this.labels[i % this.labels.length], hLeftPos - 15.0, vLeftPos);
			g.strokeText(this.labels[i % this.labels.length] + "\u2070", hLeftPos + lineLength + 15.0, vLeftPos);
			g.setLineWidth(1.0);
			vLeftPos += lineSeparation;
		}
		
		hLeftPos = horizontalPadding + gateSeparation;
		vLeftPos = verticalPadding;
		for(int i = 0; i < numberOfGates; i++) {
			for(int j = 0; j < numberOfInputs; j++) {
				if(gates[i][j] == 2) {
					createTarget(g, hLeftPos, vLeftPos);
				} else if(gates[i][j] == 1) {
					createControl(g, hLeftPos, vLeftPos);
				} else {
					// Skip
				}
				
				vLeftPos += lineSeparation;
			}
			vLeftPos = verticalPadding;
			hLeftPos += gateSeparation;
		}
		
		hLeftPos = horizontalPadding + gateSeparation;
		vLeftPos = verticalPadding;
		for(int i = 0; i < numberOfGates; i++) {
			boolean rowOfTargets = false;
			boolean rowOfTargets2 = false;
			int start = 0;
			int end = 0;
			int j;
			for(j = 0; j < numberOfInputs; j++) {
				if(gates[i][j] > 0) {
					start = j;
					
					if(gates[i][j] == 2) {
						rowOfTargets = true;
					}
					
					break;
				}
			}

			for(int k = j; k < numberOfInputs; k++) {
				if(gates[i][k] > 0) {
					end = k;
					if(gates[i][k] == 2) {
						rowOfTargets2 = true;
					} else {
						rowOfTargets2 = false;
					}
				}
			}
			
			// Draw the line
			if(start < end && (rowOfTargets == false || rowOfTargets2 == false)) {
				g.strokeLine(hLeftPos, (vLeftPos + (lineSeparation * start)), 
							hLeftPos, (vLeftPos + (lineSeparation * end)));
			}
			vLeftPos = verticalPadding;
			hLeftPos += gateSeparation;
		}
		return circuitDisplay;
	}
	
	/** Draws a target symbol at the location specified
	 * @param g The GraphicsContext to draw with
	 * @param xCenter The x-coordinate to draw at
	 * @param yCenter Thy y-coordinate to draw at
	 */
	private void createTarget(GraphicsContext g, double xCenter, double yCenter) {
		double offset = 10.0;
		double x = xCenter - offset;
		double y = yCenter - offset;
		double width = 2 * offset;
		g.fillOval(x, y, width, width);
		
		g.setFill(Color.WHITE);
		offset = 8;
		x = xCenter - offset;
		y = yCenter - offset;
		width = 2 * offset;
		g.fillOval(x, y, width, width);
		
		g.setFill(Color.BLACK);
		g.strokeLine(xCenter - offset, yCenter, xCenter + offset, yCenter);
		g.strokeLine(xCenter, yCenter - offset, xCenter, yCenter + offset);
	}

	/** Draws a control symbol at the location specified
	 * @param g The GraphicsContext to draw with
	 * @param xCenter The x-coordinate to draw at
	 * @param yCenter Thy y-coordinate to draw at
	 */
	private void createControl(GraphicsContext g, double xCenter, double yCenter) {
		double offset = 5.0;
		double x = xCenter - offset;
		double y = yCenter - offset;
		double width = 2 * offset;
		g.fillOval(x, y, width, width);
	}
	
	/** Creates a truth table like collection of TextFields
	 * @param width The width of the GridPane
	 * @param height The height of the GridPane
	 * @param inputs The number of inputs to the function
	 * @param userInput The TextFields to use
	 */
	private GridPane truthTableSimple(double width, double height, int inputs, TextField[] userInput) {
		int inset = 10;
		int rows = (int) Math.pow(2, inputs);
		
		// Create the GridPane that will hold the TruthTable
		GridPane truthTable = new GridPane();
		truthTable.setHgap(2);
		truthTable.setVgap(2);
		truthTable.setPadding(new Insets(inset, inset, inset, inset));
		
		// Add instructions
		truthTable.add(new Text("Input Function Truth Table:"), 0, 0, 20, 1);
		
		String tmp = "";
		String tmp2 = "  ";
		for(int i = 0; i < inputs; i++) {
			tmp += this.labels[(inputs - 1 - i) % this.labels.length];
			tmp2 +=  this.labels[(inputs - 1 - i) % this.labels.length] + "\u2070";
		}
		truthTable.add(new Text(tmp), 0, 1);
		truthTable.add(new Text(tmp2), 1, 1);
		
		// Add the Textfields
		for(int i = 0; i < rows; i++) {
			truthTable.add(new Text(paddedBinaryRep(i, inputs)), 0, (i + 2));
			// Set the prefered width of the table
			userInput[i] = new TextField();
			userInput[i].setPrefWidth(80);
			truthTable.add(userInput[i], 1, (i + 2));
		}
		return truthTable;
	}
	
	/** Creates a collection of TextFields
	 * @param inputs The number of inputs to the Boolean function
	 */
	private TextField[] userInputFieldsSimple(int inputs) {
		int rows = (int) Math.pow(2, inputs);
		// Create an array to hold the TextFields
		TextField[] userInput = new TextField[rows];
		return userInput;
	}
	
	/** Pads the binary rep of a number with an appropriate number of zeros
	 */
	private String paddedBinaryRep(int i, int inputs) {
		String num = Integer.toBinaryString(i);
		int len = num.length();
		int padding = this.inputs - len;
		if(padding > 0) {
			String tmp = "";
			for(i = 0; i < padding; i++) {
				tmp += '0';
			}
			tmp += num;
			return tmp;
		} else {
			return num;
		}
	}
	
	/** Processes user input contained in the TextFields
	 * @param The TextFields containing user input
	 * @return The int[][] representation of the user input
	 */
	private int[][] processUserInputSimple(TextField[] userInput) throws UserInputException{
		int rows = userInput.length;
		int columns = expOfTwo(rows);
		int[][] input = new int[rows][columns];
		char tmp;
		String tmp2;
		
		for(int i = 0; i < rows; i++) {
			tmp2 = userInput[i].getText();
			
			for(int j = 0; j < columns; j++) {
				
				if(tmp2.length() != columns) {
					throw new UserInputException("Excessive or Insufficient input in row " + i + ".");
				}
				
				tmp = tmp2.charAt(j);
				if(tmp == '1' || tmp == '0') {
					input[i][j] = Character.getNumericValue(tmp);
				} else {
					throw new UserInputException("Invalid character in row " + i + ", at column " + j + ".");
				}
			}
		}
		
		return input;
	}
	

	/** Calculates the exponent of two it would take for a power, small helper method
	 */
	private static int expOfTwo(int Pow) {
		int exp = 0;
		while(Pow >= 2) {
			Pow /= 2;
			exp++;
		}
		return exp;
	}
	
	/** Clears the user input in a collection of TextFields
	 * @param userInput The array of TextFields to be cleared
	 */
	private void clearUserInputFieldsSimple(TextField[] userInput) {
		int rows = userInput.length;
		int columns = expOfTwo(rows);
		// Traverse the rows
		for(int i = 0; i < rows; i++) {
			userInput[i].clear();
		}
	}
	
	/** Creates a graphical display for a passed error
	 * @param err The error to be displayed
	 */
	private void errorDisplay(Exception err) {
		// Create the Error Message Stage
		Stage errorMessage = new Stage();
		errorMessage.initModality(Modality.WINDOW_MODAL);
		VBox error = new VBox(10.0);
		error.setAlignment(Pos.CENTER);
		Button button = new Button("Close");
		button.setOnAction(e -> {
				errorMessage.close();
			}
		);
		Label errMsg = new Label(err.getMessage());
		error.getChildren().addAll(errMsg, button);
		errorMessage.setScene(new Scene(error, 300.0, 300.0));
		errorMessage.show();
	}
	
	/** Reads a file into a double array of ints
	 * @param fileName The name of the file to be read
	 */
	private static int[][] readSpecificationFile(String fileName) throws UserInputException {
		int numberOfInputs = 0;
		
		try {
			Scanner s = new Scanner(new File(fileName));
			
			// Read in the number of inputs for the specification
			if(s.hasNextInt()) {
				numberOfInputs = s.nextInt();
				if(numberOfInputs < 1) {
					throw new UserInputException("FIle contains an input value to low, must be greater than 0.");
				}
			}
			
			int rows = (int) Math.pow(2, numberOfInputs);
			int[][] fileInput = new int[rows][numberOfInputs];
			int added = 0;
			int expectedAdded = (rows * numberOfInputs);
			
			for(int i = 0; i < rows; i++) {
				for(int j = 0; j < numberOfInputs; j++) {
					int tmp = s.nextInt();
					if(tmp == 0 || tmp == 1) {
						added++;
						fileInput[i][j] = tmp;
					} else {
						throw new UserInputException("File contains a value that is not a 1 or a 0.");
					}
				}
			}
			
			if(added != expectedAdded) {
				throw new UserInputException("File contains an insufficient number of values.");
			}
			return fileInput;
			
		} catch(FileNotFoundException e) {
			throw new UserInputException("File name entered was not found in directory.");
		} catch(InputMismatchException e) {
			throw new UserInputException("File contains a value that is not an integer.");
		} catch (NoSuchElementException e) {
			throw new UserInputException("File contains an insufficient number of values.");
		}
	}
	 
	/** Reads a file into a double string array
	 * @param fileName The name of the file to be read including extensions
	 */ 
	private static String[][] readSpecificationFileToString(String fileName) throws UserInputException {
		int numberOfInputs = 0;
		
		try {
			Scanner s = new Scanner(new File(fileName));
			
			// Read in the number of inputs for the specification
			if(s.hasNextInt()) {
				numberOfInputs = s.nextInt();
				if(numberOfInputs < 1) {
					throw new UserInputException("FIle contains an input value to low, must be greater than 0.");
				}
			}
			
			int rows = (int) Math.pow(2, numberOfInputs);
			String[][] fileInput = new String[rows][numberOfInputs];
			int added = 0;
			int expectedAdded = (rows * numberOfInputs);
			
			for(int i = 0; i < rows; i++) {
				for(int j = 0; j < numberOfInputs; j++) {
					int tmp = s.nextInt();
					if(tmp == 0 || tmp == 1) {
						added++;
						fileInput[i][j] = Integer.toString(tmp);
					} else {
						throw new UserInputException("File contains a value that is not a 1 or a 0.");
					}
				}
			}
			
			if(added != expectedAdded) {
				throw new UserInputException("File contains an insufficient number of values.");
			}
			return fileInput;
			
		} catch(FileNotFoundException e) {
			throw new UserInputException("File name entered was not found in directory.");
		} catch(InputMismatchException e) {
			throw new UserInputException("File contains a value that is not an integer.");
		} catch (NoSuchElementException e) {
			throw new UserInputException("File contains an insufficient number of values.");
		}
	}
}