import java.util.Arrays;
import java.awt.Color;
import java.util.Arrays;

public class PlayerSkeleton {

    // *** Paste weights here
     //double[] weights = {-0.08075476343626997,0.06512710745251571,0.026528301544375688,0.9841333927818585,-0.529638056344585,-0.1136445616744991,-0.27466748067531854};
    double[] weights = { -1, 1, 1, 1, -1, -1, -1 };
    static int numOfGames = 100;

    // implement this function to have a working system
    public int[] pickMove(State s, int[][] legalMoves) {
	int[] move = { 0, 0 };
	double max = -Double.MAX_VALUE;
	for (int[] x : legalMoves) {
	    DummyState dummyState = new DummyState(s);
	    dummyState.makeMove(x);

	    // *** Clean version
	    //double sum = Heuristics.evaluate(dummyState, weights);

	    // *** Expectimax version
	    // double sum = expectimaxAlgo(dummyState);

	    // *** Minimax version
	    double sum = minimaxAlgo(dummyState ,max);

	    if (sum > max) {
		max = sum;
		move[0] = x[0];
		move[1] = x[1];
	    }
	}
	return move;
    }

    public double expectimaxAlgo(DummyState s) {
	double sum = 0;
	for (int i = 0; i < 7; i++) {
	    DummyState dummyState = new DummyState(s);
	    dummyState.nextPiece = i;
	    double max = -Double.MAX_VALUE;
	    
	    for (int[] possibleMove : dummyState.legalMoves()) {
		DummyState secondDummyState = new DummyState(dummyState);
		secondDummyState.makeMove(possibleMove);
		double value = Heuristics.evaluate(secondDummyState, weights);
		max = Math.max(max, value);
	    }
	    sum += max;
	}
	return sum;
    }

    public double minimaxAlgo(DummyState s, double beta) {
	double alpha = Double.MAX_VALUE;
	double score = 0;
	for (int i = 0; i < 7; i++) {
	    double largest = -Double.MAX_VALUE;
	    boolean broken = false;
	    for (int[] possibleMove : s.legalMoves()) {
		DummyState dummyState = new DummyState(s);
		dummyState.makeMove(possibleMove);
		score = Heuristics.evaluate(dummyState, weights);
		if (score > alpha) {
		    broken = true;
		    break; // prune
		}
		if (score > largest) {
		    largest = score;
		}
	    }
	    if (alpha > largest && !broken) {
		alpha = largest;
	    }
	    if (beta > alpha) {
		return -Double.MAX_VALUE; // prune //prune
	    }
	}
	return alpha;
    }

    public static void main(String[] args) {

	for (int i = 0; i < numOfGames; i++) {

	    State s = new State();
	    // new TFrame(s);
	    PlayerSkeleton p = new PlayerSkeleton();
	    while (!s.hasLost()) {
		s.makeMove(p.pickMove(s, s.legalMoves()));
		/*
		 * s.draw(); s.drawNext(0, 0); try { Thread.sleep(300); } catch
		 * (InterruptedException e) { e.printStackTrace(); }
		 */
	    }
	    System.out.print(s.getRowsCleared() + ", ");
	}
    }
}

class DummyState {

    public static final int COLS = 10;
    public static final int ROWS = 21;
    public static final int N_PIECES = 7;
    public boolean lost = false;
    public TLabel label;

    // current turn
    private int turn = 0;
    private int cleared = 0;

    // each square in the grid - int means empty - other values mean the turn it
    // was placed
    private int[][] field = new int[ROWS][COLS];
    // top row+1 of each column
    // 0 means empty
    private int[] top = new int[COLS];

    // number of next piece
    protected int nextPiece;

    // all legal moves - first index is piece type - then a list of 2-length
    // arrays
    protected static int[][][] legalMoves = new int[N_PIECES][][];

    // indices for legalMoves
    public static final int ORIENT = 0;
    public static final int SLOT = 1;

    // possible orientations for a given piece type
    protected static int[] pOrients = { 1, 2, 4, 4, 4, 2, 2 };

    // the next several arrays define the piece vocabulary in detail
    // width of the pieces [piece ID][orientation]
    protected static int[][] pWidth = { { 2 }, { 1, 4 }, { 2, 3, 2, 3 }, { 2, 3, 2, 3 }, { 2, 3, 2, 3 }, { 3, 2 },
	    { 3, 2 } };
    // height of the pieces [piece ID][orientation]
    private static int[][] pHeight = { { 2 }, { 4, 1 }, { 3, 2, 3, 2 }, { 3, 2, 3, 2 }, { 3, 2, 3, 2 }, { 2, 3 },
	    { 2, 3 } };
    private static int[][][] pBottom = { { { 0, 0 } }, { { 0 }, { 0, 0, 0, 0 } },
	    { { 0, 0 }, { 0, 1, 1 }, { 2, 0 }, { 0, 0, 0 } }, { { 0, 0 }, { 0, 0, 0 }, { 0, 2 }, { 1, 1, 0 } },
	    { { 0, 1 }, { 1, 0, 1 }, { 1, 0 }, { 0, 0, 0 } }, { { 0, 0, 1 }, { 1, 0 } }, { { 1, 0, 0 }, { 0, 1 } } };
    private static int[][][] pTop = { { { 2, 2 } }, { { 4 }, { 1, 1, 1, 1 } },
	    { { 3, 1 }, { 2, 2, 2 }, { 3, 3 }, { 1, 1, 2 } }, { { 1, 3 }, { 2, 1, 1 }, { 3, 3 }, { 2, 2, 2 } },
	    { { 3, 2 }, { 2, 2, 2 }, { 2, 3 }, { 1, 2, 1 } }, { { 1, 2, 2 }, { 3, 2 } }, { { 2, 2, 1 }, { 2, 3 } } };

    // initialize legalMoves
    {
	// for each piece type
	for (int i = 0; i < N_PIECES; i++) {
	    // figure number of legal moves
	    int n = 0;
	    for (int j = 0; j < pOrients[i]; j++) {
		// number of locations in this orientation
		n += COLS + 1 - pWidth[i][j];
	    }
	    // allocate space
	    legalMoves[i] = new int[n][2];
	    // for each orientation
	    n = 0;
	    for (int j = 0; j < pOrients[i]; j++) {
		// for each slot
		for (int k = 0; k < COLS + 1 - pWidth[i][j]; k++) {
		    legalMoves[i][n][ORIENT] = j;
		    legalMoves[i][n][SLOT] = k;
		    n++;
		}
	    }
	}
    }

    public int[][] getField() {
	return field;
    }

    public int[] getTop() {
	return top;
    }

    public static int[] getpOrients() {
	return pOrients;
    }

    public static int[][] getpWidth() {
	return pWidth;
    }

    public static int[][] getpHeight() {
	return pHeight;
    }

    public static int[][][] getpBottom() {
	return pBottom;
    }

    public static int[][][] getpTop() {
	return pTop;
    }

    public int getNextPiece() {
	return nextPiece;
    }

    public boolean hasLost() {
	return lost;
    }

    public int getRowsCleared() {
	return cleared;
    }

    public int getTurnNumber() {
	return turn;
    }

    // constructor
    public DummyState() {
	nextPiece = randomPiece();
    }

    public DummyState(State s) {
	this.lost = s.hasLost();
	this.turn = s.getTurnNumber();
	this.cleared = s.getRowsCleared();
	this.field = copyField(s.getField());
	this.top = Arrays.copyOf(s.getTop(), s.getTop().length);
	this.nextPiece = s.getNextPiece();
    }

    public DummyState(DummyState s) {
	this.lost = s.hasLost();
	this.turn = s.getTurnNumber();
	this.cleared = s.getRowsCleared();
	this.field = copyField(s.getField());
	this.top = Arrays.copyOf(s.getTop(), s.getTop().length);
	this.nextPiece = s.getNextPiece();
    }

    private static int[][] copyField(int[][] srcField) {
	int[][] copy = new int[ROWS][COLS];

	for (int i = 0; i < ROWS; ++i) {
	    for (int j = 0; j < COLS; ++j) {
		copy[i][j] = srcField[i][j];
	    }
	}

	return copy;
    }

    // random integer, returns 0-6
    private int randomPiece() {
	return (int) (Math.random() * N_PIECES);
    }

    // gives legal moves for
    public int[][] legalMoves() {
	return legalMoves[nextPiece];
    }

    // make a move based on the move index - its order in the legalMoves list
    public void makeMove(int move) {
	makeMove(legalMoves[nextPiece][move]);
    }

    // make a move based on an array of orient and slot
    public void makeMove(int[] move) {
	makeMove(move[ORIENT], move[SLOT]);
    }

    // returns false if you lose - true otherwise
    public boolean makeMove(int orient, int slot) {
	turn++;
	// height if the first column makes contact
	int height = top[slot] - pBottom[nextPiece][orient][0];
	// for each column beyond the first in the piece
	for (int c = 1; c < pWidth[nextPiece][orient]; c++) {
	    height = Math.max(height, top[slot + c] - pBottom[nextPiece][orient][c]);
	}

	// check if game ended
	if (height + pHeight[nextPiece][orient] >= ROWS) {
	    lost = true;
	    return false;
	}

	// for each column in the piece - fill in the appropriate blocks
	for (int i = 0; i < pWidth[nextPiece][orient]; i++) {

	    // from bottom to top of brick
	    for (int h = height + pBottom[nextPiece][orient][i]; h < height + pTop[nextPiece][orient][i]; h++) {
		field[h][i + slot] = turn;
	    }
	}

	// adjust top
	for (int c = 0; c < pWidth[nextPiece][orient]; c++) {
	    top[slot + c] = height + pTop[nextPiece][orient][c];
	}

	int rowsCleared = 0;

	// check for full rows - starting at the top
	for (int r = height + pHeight[nextPiece][orient] - 1; r >= height; r--) {
	    // check all columns in the row
	    boolean full = true;
	    for (int c = 0; c < COLS; c++) {
		if (field[r][c] == 0) {
		    full = false;
		    break;
		}
	    }
	    // if the row was full - remove it and slide above stuff down
	    if (full) {
		rowsCleared++;
		cleared++;
		// for each column
		for (int c = 0; c < COLS; c++) {

		    // slide down all bricks
		    for (int i = r; i < top[c]; i++) {
			field[i][c] = field[i + 1][c];
		    }
		    // lower the top
		    top[c]--;
		    while (top[c] >= 1 && field[top[c] - 1][c] == 0) {
			top[c]--;
		    }
		}
	    }
	}

	// pick a new piece
	nextPiece = randomPiece();

	return true;
    }

    public void draw() {
	label.clear();
	label.setPenRadius();
	// outline board
	label.line(0, 0, 0, ROWS + 5);
	label.line(COLS, 0, COLS, ROWS + 5);
	label.line(0, 0, COLS, 0);
	label.line(0, ROWS - 1, COLS, ROWS - 1);

	// show bricks
	for (int c = 0; c < COLS; c++) {
	    for (int r = 0; r < top[c]; r++) {
		if (field[r][c] != 0) {
		    drawBrick(c, r);
		}
	    }
	}

	for (int i = 0; i < COLS; i++) {
	    label.setPenColor(Color.red);
	    label.line(i, top[i], i + 1, top[i]);
	    label.setPenColor();
	}

	label.show();

    }

    public static final Color brickCol = Color.gray;

    private void drawBrick(int c, int r) {
	label.filledRectangleLL(c, r, 1, 1, brickCol);
	label.rectangleLL(c, r, 1, 1);
    }

    public void drawNext(int slot, int orient) {
	for (int i = 0; i < pWidth[nextPiece][orient]; i++) {
	    for (int j = pBottom[nextPiece][orient][i]; j < pTop[nextPiece][orient][i]; j++) {
		drawBrick(i + slot, j + ROWS + 1);
	    }
	}
	label.show();
    }

    // visualization
    // clears the area where the next piece is shown (top)
    public void clearNext() {
	label.filledRectangleLL(0, ROWS + .9, COLS, 4.2, TLabel.DEFAULT_CLEAR_COLOR);
	label.line(0, 0, 0, ROWS + 5);
	label.line(COLS, 0, COLS, ROWS + 5);
    }
}

class Heuristics {

    public static double evaluate(DummyState s, double[] weights) {

	int length = weights.length;
	double[] features = new double[length];

	// 0 height difference between all pairs
	int[] top = s.getTop();
	int heightDiff = 0;
	for (int i = 0; i < top.length - 1; ++i) {
	    heightDiff += Math.abs(top[i] - top[i + 1]);
	}
	features[0] = heightDiff;

	// 1 max column height
	int maxHeight = Integer.MIN_VALUE;
	for (int column = 0; column < top.length; column++) {
	    int height = top[column];
	    if (height > maxHeight) {
		maxHeight = height;
	    }
	}

	features[1] = maxHeight;

	// 2 number of rows cleared
	features[2] = s.getRowsCleared();

	// 3 whether game has been lost
	features[3] = s.hasLost() ? -1 : 1;

	// 4 number of holes
	int[][] field = s.getField();
	int numHoles = 0;

	for (int col = 0; col < Constants.COLS; col++) {
	    for (int row = top[col] - 1; row >= 0; row--) {
		if (field[row][col] == 0) {
		    numHoles++;
		}
	    }
	}
	features[4] = numHoles;

	// 5 pit depths
	int sumOfPitDepths = 0;

	int heightOfCol;
	int heightOfLeftCol;
	int heightOfRightCol;

	// pit depth of first column
	heightOfCol = top[0];
	heightOfRightCol = top[1];
	int heightDifference = heightOfRightCol - heightOfCol;
	if (heightDifference > 2) {
	    sumOfPitDepths += heightDifference;
	}

	for (int col = 0; col < Constants.COLS - 2; col++) {
	    heightOfLeftCol = top[col];
	    heightOfCol = top[col + 1];
	    heightOfRightCol = top[col + 2];

	    int leftHeightDifference = heightOfLeftCol - heightOfCol;
	    int rightHeightDifference = heightOfRightCol - heightOfCol;
	    int minDiff = Math.min(leftHeightDifference, rightHeightDifference);

	    if (minDiff > 2) {
		sumOfPitDepths += minDiff;
	    }
	}

	// pit depth of last column
	heightOfCol = top[Constants.COLS - 1];
	heightOfLeftCol = top[Constants.COLS - 2];
	heightDifference = heightOfLeftCol - heightOfCol;
	if (heightDifference > 2) {
	    sumOfPitDepths += heightDifference;
	}

	features[5] = sumOfPitDepths;

	// 6 weighted height difference
	int totalHeight = 0;
	for (int height : top) {
	    totalHeight += height;
	}

	double meanHeight = (double) totalHeight / top.length;

	double avgDiff = 0;

	for (int height : top) {
	    avgDiff += Math.abs(meanHeight - height);
	}

	features[6] = avgDiff / top.length;

	double value = 0;
	for (int i = 0; i < length; i++) {
	    value += weights[i] * features[i];
	}
	return value;
    }

    public int colHeight(int[][] field, int col) {
	for (int x = Constants.ROWS - 2; x >= 0; x--) {
	    if (field[x][col] != 0) {
		return x + 1;
	    }
	}
	return 0;
    }
}

class Constants {

    public static final int COLS = 10;
    public static final int ROWS = 21;
}
