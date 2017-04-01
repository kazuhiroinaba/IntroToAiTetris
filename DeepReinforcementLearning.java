package Tetris;

import java.text.DecimalFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author JunKiat
 */
public class DeepReinforcementLearning {

    public static final int ROWS = 21;
    public static final int COLS = 10;
    public static final int NODES = ROWS * COLS;

    public double[][][] w1_;
    public double[] w2_;
    public double[][][] bias_;
    //Bias is of size 4 * 10 based on the selected piece and orientation

    DecimalFormat df = new DecimalFormat("#.####");
    Weights w;
    IO io;
    public int maxRowsCleared = 1;

    public DeepReinforcementLearning(boolean reset) {
        io = new IO();
        if (reset) {
            w = new Weights();
            this.w1_ = new double[ROWS][COLS][NODES];
            this.w2_ = new double[NODES];
            this.bias_ = new double[4][COLS][NODES];

            //Random Initialisation
            for (int i = 0; i < NODES; i++) {
                for (int j = 0; j < ROWS; j++) {
                    for (int k = 0; k < COLS; k++) {
                        w1_[j][k][i] = ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
                    }
                }
                w2_[i] = ThreadLocalRandom.current().nextDouble(-1, 1);
                for (int j = 0; j < 4; j++) {
                    for (int k = 0; k < COLS; k++) {
                        bias_[j][k][i] = ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
                    }
                }
            }
        } else {
            w = io.importWeights();
            this.w1_ = w.w1_;
            this.w2_ = w.w2_;
            this.bias_ = w.bias_;
        }
    }

    public int[] pickMove(State s) {
        int counter = s.legalMoves().length;
        double[] values = new double[counter];
        for (int i = 0; i < counter; i++) {
            double value = neural(s.getField(), convert(s.legalMoves()[i], s.getNextPiece()));
            System.out.println("value: " + df.format(value));

            //If there is a larger than 50% chance of dropping, we return the move
            //else switch move
            if (value >= 0.5) {
                return s.legalMoves()[i];
            } else {
                values[i] = value;
            }
        }

        //In the case where it is always switching move,
        //We will choose the one with highest probability of dropping
        double max = -1;
        int move = 0;
        for (int i = 0; i < counter; i++) {
            if (values[i] > max) {
                max = values[i];
                move = i;
            }
        }
        System.err.println("Full Clear");
        return s.legalMoves()[move];
    }

    //Neural network that returns the probability of dropping in given state
    //as compared to changing state
    public double neural(int[][] field, int[][] move) {
        //First layer
        double[] layer = new double[NODES];

        for (int i = 0; i < NODES; i++) {
            for (int j = 0; j < ROWS; j++) {
                for (int k = 0; k < COLS; k++) {
                    layer[i] += field[j][k] * w1_[j][k][i];
                }
            }
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < COLS; k++) {
                    layer[i] += bias_[j][k][i] * move[j][k];
                }
            }

            //ReLU
            if (layer[i] < 0) {
                layer[i] = 0;
            }
        }

        //Second layer
        double value = 0;

        for (int i = 0; i < NODES; i++) {
            value += layer[i] * w2_[i];
        }

        //Sigmoid
        return sigmoid(value);
    }

    public void updateWeights() {
        backwardPropagation();
        w.w1_ = this.w1_;
        w.w2_ = this.w2_;
        w.bias_ = this.bias_;
        io.exportWeights(w);
    }
    
    /* Updates the set of weights w1,bias,w2 to a new better set of weights after carrying out backward propagation through every move.
     * 
     * n is the number of moves that were played in this game
     * 
     * Inputs is the data structure containing all 250 inputs to the input nodes, the 210 inputs to the hidden layer nodes 
     * and the final input to the output node, for every move. 
     * Example: in.getInputLayer(n,move) gives value for input node n for a certain move i. in.getHiddenLayer(n,move) works in the same way.
     * 			in.getFinalInput(move) gives the value for the input to the last output node for move i. (move starts from 1 to last move)
     * 
     * Outputs is the object containing all 210 outputs from the hidden layer nodes and the output of the final node for every move.
     * Example: out.getHiddenLayer(n,move) gives output of hidden layer node n for a certain move i. 
     * 			out.getFinalOutput(move) gives the output of the final node for a certain move i. (move starts from 1 to last move)
     * 
     * payoff is the payoff achieved at the end of this game
     */
    public void backwardPropagation(int n, Inputs in, Outputs out, double payoff, double[][][] w1_, double[] w2_, double[][][] bias_) {
        double[][][] current_w1 = w1_;
        double[][][] current_bias = bias_;
        double[] current_w2 = w2_;
            
        for(int i=n;i>0;i--) {
        	//Update current weights after doing calculation for this move
            
        	//update outer layer weights
        	for(int j=0;j<210;j++) {
            	double d1 = -1*payoff;
            	double d2 = out.getFinalOutput(n)*(1-out.getFinalOutput(n));
            	double d3 = out.getHiddenLayer(j,n);
            	
            	current_w2[j] = current_w2[j] - (d1*d2*d3);
            }
        	
        	//update inner layer weights
        }
        
        this.w1_ = current_w1;
        this.bias_ = current_bias;
        this.w2_ = current_bias;
    }
    
  

    public int error(int rowsCleared) {
        return rowsCleared - maxRowsCleared;
    }

    public static double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    public int[][] convert(int[] move, int nextPiece) {
        int[][] bias = new int[4][COLS];
        int width = pWidth[nextPiece][move[0]];
        for (int i = 0; i < width; i++) {
            for (int j = pBottom[nextPiece][move[0]][i]; j < pTop[nextPiece][move[0]][i]; j++) {
                bias[j][i + move[1]] = 1;
            }
        }
        return bias;
    }

    //possible orientations for a given piece type
    protected static int[] pOrients = {1, 2, 4, 4, 4, 2, 2};

    //the next several arrays define the piece vocabulary in detail
    //width of the pieces [piece ID][orientation]
    protected static int[][] pWidth = {
        {2},
        {1, 4},
        {2, 3, 2, 3},
        {2, 3, 2, 3},
        {2, 3, 2, 3},
        {3, 2},
        {3, 2}
    };
    //height of the pieces [piece ID][orientation]
    private static int[][] pHeight = {
        {2},
        {4, 1},
        {3, 2, 3, 2},
        {3, 2, 3, 2},
        {3, 2, 3, 2},
        {2, 3},
        {2, 3}
    };
    private static int[][][] pBottom = {
        {{0, 0}},
        {{0}, {0, 0, 0, 0}},
        {{0, 0}, {0, 1, 1}, {2, 0}, {0, 0, 0}},
        {{0, 0}, {0, 0, 0}, {0, 2}, {1, 1, 0}},
        {{0, 1}, {1, 0, 1}, {1, 0}, {0, 0, 0}},
        {{0, 0, 1}, {1, 0}},
        {{1, 0, 0}, {0, 1}}
    };
    private static int[][][] pTop = {
        {{2, 2}},
        {{4}, {1, 1, 1, 1}},
        {{3, 1}, {2, 2, 2}, {3, 3}, {1, 1, 2}},
        {{1, 3}, {2, 1, 1}, {3, 3}, {2, 2, 2}},
        {{3, 2}, {2, 2, 2}, {2, 3}, {1, 2, 1}},
        {{1, 2, 2}, {3, 2}},
        {{2, 2, 1}, {2, 3}}
    };
}
