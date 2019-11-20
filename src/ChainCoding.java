import java.io.*;
import java.util.Scanner;

public class ChainCoding {

    Point startP, currentP, nextP;
    // This array store the x-y coordinates of currentP's eight neighbors.
    // Index of the array are the chain code directions from currentP.
    // i.e., neighborCoord [2] has the x-y coordinate of currentP’s
    // neighbor at chain-code direction 2, which is (i-1, j)
    // This array is very useful for finding the next non-zero neighbor of
    // currentP.
    Point [] neighborCoord;
    // the last zero before getting to currentP
    int lastZero;
    // A look-up table to get the next scan direction.
    // You may *hard code* this table as given in class
    int [] dirTable = {6,6,0,0,2,2,4,4};
    // chain code direction from currentP to nextP
    int chainDir;
    Image image;
    PrintWriter chainCodeFile, deCompressFile;

    public ChainCoding (Image image, PrintWriter chainCodeFile, PrintWriter deCompressFile) {
        this.image = image;
        this.chainCodeFile = chainCodeFile;
        this.deCompressFile = deCompressFile;
        neighborCoord = new Point[8];
    }

    public void getChainCode () {
        chainCodeFile.println(image.numRows + " " + image.numCols + " " +
                image.minVal + " " + image.maxVal);
        for (int i = 1; i <= image.numRows; i++) {
            for (int j = 1; j <= image.numCols; j++) {
                int current = image.zeroFramedAry[i][j];
                if (current > 0) {
                    startP = new Point(i,j);
                    chainCodeFile.print(startP.row + " " + startP.col + " " + current + " ");
                    lastZero = 4;
                    image.label = current;
                    currentP = new Point(i,j);
                    break;
                }
            }
            if (startP != null) break;
        }
        lastZero = (lastZero + 1) % 8;
        do {
            findNextP();
            chainCodeFile.print(chainDir + " ");
            nextP = neighborCoord[chainDir];
            currentP = nextP;
            lastZero = dirTable[chainDir];
        } while (!currentP.equals(startP));
        chainCodeFile.println();

    }

    private void findNextP () {
        loadNeighborsCoord();
        int index = lastZero;
        boolean found = false;
        while (!found) {
            int iRow = neighborCoord[index].row;
            int iCol = neighborCoord[index].col;
            if (image.zeroFramedAry[iRow][iCol] == image.label) {
                chainDir = index;
                found = true;
            }
            index = (index + 1) % 8;
        }
    }

    void loadNeighborsCoord () {
        int row = currentP.row;
        int col = currentP.col;
        neighborCoord[0] = new Point(row, col+1);
        neighborCoord[1] = new Point(row-1, col+1);
        neighborCoord[2] = new Point(row-1, col);
        neighborCoord[3] = new Point(row-1, col-1);
        neighborCoord[4] = new Point(row, col-1);
        neighborCoord[5] = new Point(row+1, col-1);
        neighborCoord[6] = new Point(row+1, col);
        neighborCoord[7] = new Point(row+1, col+1);
    }

    void reconstructObject (Scanner ccFile) {
        int numRows = ccFile.nextInt();
        int numCols = ccFile.nextInt();
        int minVal = ccFile.nextInt();
        int maxVal = ccFile.nextInt();
        int startRow = ccFile.nextInt();
        int startCol = ccFile.nextInt();
        int boundaryLabel = ccFile.nextInt() + 2;
        constructBoundary (ccFile, startRow, startCol, boundaryLabel);
        deCompressFile.println(numRows + " " + numCols + " " +
                minVal + " " + maxVal);
        printReconstructedImage();
    }

    void constructBoundary (Scanner ccFile, int startRow, int startCol, int boundaryLabel) {
        int [] [] imgAry = image.imgAry;
        int direction;
        imgAry[startRow-1][startCol-1] = boundaryLabel;
        currentP = new Point (startRow, startCol);
        while (ccFile.hasNext()) {
            loadNeighborsCoord();
            direction = ccFile.nextInt();
            startRow = neighborCoord[direction].row-1;
            startCol = neighborCoord[direction].col-1;
            imgAry[startRow][startCol] = boundaryLabel;
            currentP = neighborCoord[direction];
        }
    }

    void printReconstructedImage () {
        for (int i = 0; i < image.imgAry.length; i++) {
            for (int j = 0; j < image.imgAry[0].length; j++) {
                deCompressFile.print(image.imgAry[i][j] + " ");
            }
            deCompressFile.println();
        }
    }
    public static void main (String [] args) {

        if (args.length != 1) {
            System.out.println("ERROR: Illegal arguments");
            System.exit(1);
        }
        try {
            Scanner inFile1 = new Scanner(new FileReader(args[0]));
            Image image = new Image (inFile1);
            String chainCodeFileName, deCompressFileName;
            if (args[0].indexOf('.') > 0) {
                chainCodeFileName = args[0].substring(0, args[0].lastIndexOf('.'))
                        + "_chainCode.txt";
                deCompressFileName = args[0].substring(0, args[0].lastIndexOf('.'))
                        + "_chainCodeDecompressed.txt";
            } else {
                chainCodeFileName = args[0] + "_chainCode.txt";
                deCompressFileName = args[0] + "_chainCodeDecompressed.txt";
            }
            PrintWriter chainCodeFile = new PrintWriter(new BufferedWriter(new FileWriter(chainCodeFileName)), true);
            PrintWriter deCompressFile = new PrintWriter(new BufferedWriter(new FileWriter(deCompressFileName)), true);
            ChainCoding chainCode = new ChainCoding(image, chainCodeFile, deCompressFile);
            chainCode.getChainCode();
            chainCodeFile.close();
            Scanner chainCodeIn = new Scanner(new FileReader(chainCodeFileName));
            chainCode.reconstructObject(chainCodeIn);
            inFile1.close();

        } catch (FileNotFoundException e) {
            System.out.println("One or more input files not found.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class Image {

    int numRows, numCols, minVal, maxVal, label, numBoundaryPts;
    int [] [] zeroFramedAry;
    int [] [] imgAry;
    Scanner inFile;

    public Image (Scanner in) {
        inFile = in;
        numRows = inFile.nextInt();
        numCols = inFile.nextInt();
        minVal = inFile.nextInt();
        maxVal = inFile.nextInt();
        zeroFramedAry = new int [numRows+2][numCols+2];
        imgAry = new int [numRows][numCols];
        loadImage(inFile);
    }

    public void loadImage (Scanner in) {
        for (int i = 0; i < numRows; i++) // Frame array
            for (int j = 0; j < numCols; j++) {
                int current = in.nextInt();
                zeroFramedAry[i + 1][j + 1] = current;
                imgAry[i][j] = current;
            }
    }

}

class Point {

    int row, col;

    public Point (int r, int c) {
        row = r;
        col = c;
    }

    @Override
    public boolean equals (Object other) {
        Point otherP = (Point) other;
        return otherP.row == this.row && otherP.col == this.col;
    }

}