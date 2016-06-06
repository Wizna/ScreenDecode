package com.example.testdecode.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.example.testdecode.RSDecoder;
import com.example.testdecode.entity.Point;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.PerspectiveTransform;
import com.google.zxing.common.reedsolomon.ReedSolomonException;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Ruiming Huang on 2016/5/31.
 */
public class DecodeUtils {

    private static final String TAG = "DecodeProcess";
    private static final int[][] changePixelCenter = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    //decode function for 7*7, with 0~3 coding
    public static String decodeBitMap(BinaryBitmap bitmap, Bitmap grayMap)
            throws NotFoundException, ReedSolomonException {
        long[] times = new long[10];
        times[0] = System.currentTimeMillis();

        boolean[][] whetherChecked = new boolean[bitmap.getHeight()][bitmap.getWidth()];
        float[][][] points = new float[60][60][2];

        ArrayList<Point> pointArrayList = new ArrayList<>();

        BitMatrix binaryMatrix = bitmap.getBlackMatrix();
        int bitMapWidth = binaryMatrix.getWidth();
        int bitMapHeight = binaryMatrix.getHeight();

        int[] pixels = new int[bitMapWidth * bitMapHeight];

        setGreyMapByBinaryBitmap(binaryMatrix, grayMap, bitMapWidth, bitMapHeight, pixels);

        times[1] = System.currentTimeMillis();

        getPointsArray(whetherChecked, pointArrayList, pixels, bitMapWidth, bitMapHeight);

        if (pointArrayList.size() < 81)
            return "not enough points";

        times[2] = System.currentTimeMillis();

        //find theta
        Double foundTheta = findRotateTheta(pointArrayList);

        if (foundTheta == 500)
            return "rotate theta not found";

        times[3] = System.currentTimeMillis();

        grayMap = rotateBitmap(foundTheta, grayMap);

        times[4] = System.currentTimeMillis();

        bitMapWidth = grayMap.getWidth();
        bitMapHeight = grayMap.getHeight();

        whetherChecked = new boolean[bitMapHeight][bitMapWidth];
        pixels = new int[bitMapWidth * bitMapHeight];
        grayMap.getPixels(pixels, 0, bitMapWidth, 0, 0, bitMapWidth, bitMapHeight);
        times[5] = System.currentTimeMillis();

        //reform now here
        float[] xArrayValues = new float[2000];
        float[] yArrayValues = new float[2000];
        getStandardPointAndDataPoint(xArrayValues, yArrayValues
                , points, bitMapWidth, bitMapHeight, whetherChecked, pixels);

        times[6] = System.currentTimeMillis();
        float[] xValues = new float[120];
        float[] yValues = new float[120];

        int foundStartPoint = findStartPoint(points);

        Log.w("start point:", "" + foundStartPoint);
        if (foundStartPoint == 60 - 8)
            return "no proper start point";

        if (!checkSecondStandardLine(points, foundStartPoint))
            return "second standard line not verified";

        performPerspectiveTransform(points, foundStartPoint,
                xArrayValues, yArrayValues, xValues, yValues);

        arrangePoints(xArrayValues, yArrayValues, points, foundStartPoint);

        String retString = getRSDecodeResult(getDecodeResult(points, foundStartPoint));

        times[7] = System.currentTimeMillis();
        for (int i = 0; i < 8; i++) {
            Log.w(TAG, "time:" + times[i]);
        }
        return retString;
    }

    public static int findLinePoint(Point[] possiblePoints, ArrayList<Point> pointsList) {
        if (possiblePoints[0] != null) {
            for (int j = 0; j < pointsList.size(); j++) {
                pointsList.get(j).setDistanceTo(possiblePoints[0]);
            }
            Collections.sort(pointsList);
            if (pointsList.get(0).getDistance() < 2) {
                for (int j = 0; j < pointsList.size(); j++) {
                    pointsList.get(j).setDistanceTo(possiblePoints[1]);
                }
                Collections.sort(pointsList);
                if (pointsList.get(0).getDistance() < 2)
                    return 0;
            }
        }
        return -1;
    }

    private static void getPointsArray(boolean[][] whetherChecked, ArrayList<Point> pointArrayList,
                                       int[] pixels, int bitMapWidth, int bitMapHeight) {
        for (int j = 0; j < bitMapWidth; j++) {
            for (int i = 0; i < bitMapHeight; i++) {
                if (!whetherChecked[i][j] && (0xF & pixels[(bitMapHeight - 1 - i) * bitMapWidth + j]) != 0) {
                    int lastMove = 1;

                    int originX, changeX, xs, xe;
                    int originY, changeY, ys, ye;
                    originX = changeX = xs = xe = j;
                    originY = changeY = ys = ye = i;

                    while (true) {
                        for (int k = 0; k < 4; k++) {
                            int tempMoveDirection = (lastMove + k + 3) % 4;
                            int tempX = changeX + changePixelCenter[tempMoveDirection][0];
                            int tempY = changeY + changePixelCenter[tempMoveDirection][1];

                            if (tempX > -1 && tempY > -1 && tempX < bitMapWidth
                                    && tempY < bitMapHeight && !whetherChecked[tempY][tempX]
                                    && (0xF & pixels[(bitMapHeight - 1 - tempY) * bitMapWidth + tempX]) != 0) {
                                changeX = tempX;
                                changeY = tempY;
                                lastMove = tempMoveDirection;
                                break;
                            }
                        }

                        if (changeX > xe)
                            xe = changeX;

                        if (changeX < xs)
                            xs = changeX;

                        if (changeY > ye)
                            ye = changeY;

                        if (changeY < ys)
                            ys = changeY;

                        if (changeX == originX && changeY == originY)
                            break;
                    }

                    for (int setI = ys; setI <= ye; setI++) {
                        for (int setJ = xs; setJ <= xe; setJ++) {
                            whetherChecked[setI][setJ] = true;
                        }
                    }

                    float centX = ((float) xs + xe) / 2;
                    float centY = ((float) ys + ye) / 2;

                    pointArrayList.add(new Point(centX, centY));
                }
            }
        }
    }

    private static void setGreyMapByBinaryBitmap(BitMatrix binaryMatrix, Bitmap grayMap,
                                                 int bitMapWidth, int bitMapHeight, int[] pixels) {
        final int WHITE = 0xFFFFFFFF;
        final int BLACK = 0xFF000000;

        for (int y = 0; y < bitMapHeight; y++) {
            int offset = y * bitMapWidth;
            for (int x = 0; x < bitMapWidth; x++) {
                pixels[offset + x] = binaryMatrix.get(x, y) ? WHITE : BLACK;
            }
        }
        grayMap.setPixels(pixels, 0, binaryMatrix.getWidth(), 0, 0, bitMapWidth, bitMapHeight);

    }

    public static void swapPoints(int i1, int j1, int i2, int j2, float[][][] points) {
        float tempX = points[i1][j1][0];
        float tempY = points[i1][j1][1];
        points[i1][j1][0] = points[i2][j2][0];
        points[i1][j1][1] = points[i2][j2][1];
        points[i2][j2][0] = tempX;
        points[i2][j2][1] = tempY;
    }

    public static void sortPoints(float[][][] points) {
        for (int i = 0; i < points.length; i++) {
            for (int j = 0; j < points.length; j++) {
                if (points[i][j][1] == 0f && points[i][j][0] == 0f)
                    break;
                for (int k = j + 1; k < points.length; k++) {
                    if (points[i][k][1] == 0f && points[i][k][0] == 0f)
                        break;
                    if (points[i][j][1] > points[i][k][1])
                        swapPoints(i, j, i, k, points);
                }
            }
        }
    }

    private static double findRotateTheta(ArrayList<Point> pointArrayList) {
        final int[][] config = {{0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3}};
        double foundTheta = 500;
        boolean whetherFound = false;
        int countSelectingPoint = 0;
        while (!whetherFound && countSelectingPoint < pointArrayList.size()) {

            Point midPoint = pointArrayList.get(countSelectingPoint);
            countSelectingPoint++;

            for (int i = 0; i < pointArrayList.size(); i++) {
                pointArrayList.get(i).setDistanceTo(midPoint);
            }
            Collections.sort(pointArrayList);

            double[][] fourPoints = new double[4][2];
            for (int i = 1; i < 5; i++) {
                fourPoints[i - 1][0] = pointArrayList.get(i).getCentX();
                fourPoints[i - 1][1] = pointArrayList.get(i).getCentY();
            }

            int m = 0, n = 0;
            for (int i = 0; i < 6; i++) {
                m = config[i][0];
                n = config[i][1];

                double disPair = pointArrayList.get(m + 1).getDistance() + pointArrayList.get(n + 1).getDistance();
                double ratePair = (pointArrayList.get(m + 1).getDistance() / pointArrayList.get(n + 1).getDistance());
                Point[] possiblePoints = new Point[2];
                if (1.05263 > ratePair && ratePair > 0.95 &&
                        Math.hypot((fourPoints[m][1] - fourPoints[n][1]), (fourPoints[m][0] - fourPoints[n][0])) / disPair > 0.95) {
                    possiblePoints[0] = new Point((2 * pointArrayList.get(m + 1).getCentX() - midPoint.getCentX()),
                            (2 * pointArrayList.get(m + 1).getCentY() - midPoint.getCentY()));
                    possiblePoints[1] = new Point((2 * pointArrayList.get(n + 1).getCentX() - midPoint.getCentX()),
                            (2 * pointArrayList.get(n + 1).getCentY() - midPoint.getCentY()));

                    int findResult = findLinePoint(possiblePoints, pointArrayList);
                    if (findResult != -1) {
                        whetherFound = true;

                        foundTheta = Math.atan((fourPoints[m][1] - fourPoints[n][1]) /
                                (fourPoints[m][0] - fourPoints[n][0])) / Math.PI * 180;
                        break;
                    }
                }
            }
        }

        return foundTheta;
    }

    private static Bitmap rotateBitmap(double foundTheta, Bitmap grayMap) {
        Matrix tempTransMatrix = new Matrix();
        tempTransMatrix.postRotate((float) (foundTheta - 90));
        return Bitmap.createBitmap(grayMap, 0, 0, grayMap.getWidth(), grayMap.getHeight(), tempTransMatrix, true);
    }

    private static void getStandardPointAndDataPoint(float[] xArrayValues, float[] yArrayValues,
                                                     float[][][] points, int bitMapWidth, int bitMapHeight,
                                                     boolean[][] whetherChecked, int[] pixels) {
        boolean alreadyOne = false;
        boolean lineChange = false;
        int countXYValues = 0;
        boolean firstRound = true;
        double interval = 0;
        int indexI = 0;
        int indexJ = 0;

        for (int j = 0; j < bitMapWidth; j++) {
            for (int i = 0; i < bitMapHeight; i++) {
                if (whetherChecked[i][j]) {
                    lineChange = true;
                    alreadyOne = true;
                } else if ((0xF & pixels[(bitMapHeight - 1 - i) * bitMapWidth + j]) != 0) {

                    int lastMove = 1;

                    int originX, changeX, xs, xe;
                    int originY, changeY, ys, ye;
                    originX = changeX = xs = xe = j;
                    originY = changeY = ys = ye = i;

                    while (true) {
                        for (int k = 0; k < 4; k++) {
                            int tempMoveDirection = (lastMove + k + 3) % 4;
                            int tempX = changeX + changePixelCenter[tempMoveDirection][0];
                            int tempY = changeY + changePixelCenter[tempMoveDirection][1];

                            if (tempX > -1 && tempY > -1 && tempX < bitMapWidth && tempY < bitMapHeight &&
                                    !whetherChecked[tempY][tempX] &&
                                    (0xF & pixels[(bitMapHeight - 1 - tempY) * bitMapWidth + tempX]) != 0) {
                                changeX = tempX;
                                changeY = tempY;
                                lastMove = tempMoveDirection;
                                break;
                            }
                        }

                        if (changeX > xe)
                            xe = changeX;

                        if (changeX < xs)
                            xs = changeX;

                        if (changeY > ye)
                            ye = changeY;

                        if (changeY < ys)
                            ys = changeY;

                        if (changeX == originX && changeY == originY)
                            break;
                    }


                    for (int setI = ys; setI <= ye; setI++) {
                        for (int setJ = xs; setJ <= xe; setJ++) {
                            whetherChecked[setI][setJ] = true;
                        }
                    }

                    float centX = ((float) xs + xe) / 2;
                    float centY = ((float) ys + ye) / 2;

                    //remove small spots
                    if (Math.abs(xe - xs) < 2 && Math.abs(ye - ys) < 2) {
//                        Log.w("removed:", " too small:" + centX + "|" + centY);
                        continue;
                    }
                    //the second half check is to make segment line into one
                    if (firstRound || (centX - points[0][0][0] < 0.1 * interval)) {
                        points[indexI][indexJ][0] = centX;
                        points[indexI][indexJ][1] = centY;

                        if (indexJ < 59)
                            indexJ++;
                    } else
                        for (int m = 0; m < (60 - 1); m++) {
                            if (points[0][m + 1][1] == points[59][59][1] &&
                                    (Math.abs(centY - points[0][m][1]) > 0.5 * interval)) {
                                break;
                            }
                            if (Math.abs(centY - points[0][m][1]) < Math.abs(centY - points[0][m + 1][1]) &&
                                    (Math.abs(centY - points[0][m][1]) < 0.5 * interval)) {

                                double tempIndexI = (centX - points[0][m][0]) / interval;
                                double floorIndexI = Math.floor(tempIndexI);
                                if (tempIndexI - floorIndexI > 0.5)
                                    indexI = (int) floorIndexI + 1;
                                else
                                    indexI = (int) floorIndexI;

                                if (indexI < 60 && ((points[indexI][m][0] == points[59][59][0] &&
                                        points[indexI][m][1] == points[59][59][1])
                                        || (m == 0 && centY > points[indexI][m][1])) && indexI == 8) {
                                    points[indexI][m][0] = centX;
                                    points[indexI][m][1] = centY;
                                } else {
                                    xArrayValues[countXYValues] = centX;
                                    yArrayValues[countXYValues] = centY;
                                    countXYValues++;
                                }

                                break;
                            }
                        }

                    lineChange = true;
                    alreadyOne = true;
                }
            }
            if (!lineChange && alreadyOne && firstRound) {
                firstRound = false;
                sortPoints(points);
                for (int i = 0; i < 60; i++) {
                    if (points[0][i][0] == points[59][59][0] && points[0][i][1] == points[59][59][1] && i > 0) {
                        interval = (points[0][i - 1][1] - points[0][0][1]) / (double) (i - 1);
                        if (i < 9) {
                            indexI = 0;
                            indexJ = 0;
                            firstRound = true;
                            fillArrayZero(points);
                            break;
                        }
                        for (int k = 1; k < i - 1; k++) {
                            if (Math.abs(2 * points[0][k][1] - points[0][k - 1][1] - points[0][k + 1][1]) > 3 ||
                                    Math.hypot(Math.abs(points[0][k + 1][1] - points[0][k - 1][1]),
                                            Math.abs(points[0][k + 1][0] - points[0][k - 1][0])) /
                                            (Math.hypot(Math.abs(points[0][k + 1][1] - points[0][k][1]),
                                                    Math.abs(points[0][k + 1][0] - points[0][k][0])) +
                                                    Math.hypot(Math.abs(points[0][k][1] - points[0][k - 1][1]),
                                                            Math.abs(points[0][k][0] - points[0][k - 1][0]))) < 0.95) {
                                indexI = 0;
                                indexJ = 0;
                                firstRound = true;
                                fillArrayZero(points);
                                break;
                            }
                        }
                        break;
                    }
                }

                alreadyOne = false;
            }

            lineChange = false;

        }
    }

    //this function is created because it can not assign new array to original array
    private static void fillArrayZero(float[][][] points) {
        for (int i = 0; i < points[0].length; i++) {
            points[0][i][0] = 0;
            points[0][i][1] = 0;
        }
    }

    private static int findStartPoint(float[][][] points) {
        int foundStartPoint = 0;
        for (int i = 0; i < 60 - 8; i++) {
            if ((points[8][i][0] != 0.0 || points[8][i][1] != 0.0) &&
                    (points[8][i + 8][0] != 0.0 || points[8][i + 8][1] != 0.0))
                break;
            else
                foundStartPoint++;
        }
        return foundStartPoint;
    }

    private static boolean checkSecondStandardLine(float[][][] points, int foundStartPoint) {
        for (int i = 0; i < 7; i++) {
            double disFtoS = Math.hypot((points[8][foundStartPoint + i][0] - points[8][foundStartPoint + i + 1][0]),
                    (points[8][foundStartPoint + i][1] - points[8][foundStartPoint + i + 1][1]));
            double disStoT = Math.hypot((points[8][foundStartPoint + i + 1][0] - points[8][foundStartPoint + i + 2][0]),
                    (points[8][foundStartPoint + i + 1][1] - points[8][foundStartPoint + i + 2][1]));
            double disFtoT = Math.hypot((points[8][foundStartPoint + i][0] - points[8][foundStartPoint + i + 2][0]),
                    (points[8][foundStartPoint + i][1] - points[8][foundStartPoint + i + 2][1]));
            double tempRate = disFtoS / disStoT;
            if (1.05263 < tempRate || tempRate < 0.95 || (disFtoT / (disFtoS + disStoT)) < 0.95)
                return false;

        }
        return true;
    }

    private static void performPerspectiveTransform(float[][][] points, int foundStartPoint, float[] xArrayValues,
                                                    float[] yArrayValues, float[] xValues, float[] yValues) {
        PerspectiveTransform perspectiveTransform = PerspectiveTransform.quadrilateralToQuadrilateral(
                points[0][foundStartPoint][0], points[0][foundStartPoint][1],
                points[0][foundStartPoint + 8][0], points[0][foundStartPoint + 8][1],
                points[8][foundStartPoint][0], points[8][foundStartPoint][1],
                points[8][foundStartPoint + 8][0], points[8][foundStartPoint + 8][1],
                50, 50,
                50, 330,
                330, 50,
                330, 330);

        for (int j = 0; j < points.length; j++) {
            xValues[j] = points[0][j][0];
            yValues[j] = points[0][j][1];
            xValues[60 + j] = points[8][j][0];
            yValues[60 + j] = points[8][j][1];
        }

        perspectiveTransform.transformPoints(xValues, yValues);
        perspectiveTransform.transformPoints(xArrayValues, yArrayValues);

        for (int j = 0; j < points.length; j++) {
            points[0][j][0] = xValues[j];
            points[0][j][1] = yValues[j];
            points[8][j][0] = xValues[60 + j];
            points[8][j][1] = yValues[60 + j];
        }
    }

    private static void arrangePoints(float[] xArrayValues, float[] yArrayValues, float[][][] points,
                                      int foundStartPoint) {
        double interval = 35.0;
        int indexI = 0;
        for (int i = 0; i < 2000; i++) {
            if (xArrayValues[i] == 0f && yArrayValues[i] == 0f)
                break;
            for (int m = foundStartPoint; m < (60 - 1); m++) {
                if (points[0][m + 1][1] == points[59][59][1] &&
                        (Math.abs(yArrayValues[i] - points[0][m][1]) > 0.5 * interval)) {
                    break;
                }
                if (Math.abs(yArrayValues[i] - points[0][m][1]) < Math.abs(yArrayValues[i] - points[0][m + 1][1]) &&
                        (Math.abs(yArrayValues[i] - points[0][m][1]) < 0.5 * interval)) {

                    double tempIndexI = (xArrayValues[i] - points[0][m][0]) / interval;
                    double floorIndexI = Math.floor(tempIndexI);
                    if (tempIndexI - floorIndexI > 0.5)
                        indexI = (int) floorIndexI + 1;
                    else
                        indexI = (int) floorIndexI;

                    if (indexI <= 0) {
                        Log.w(TAG, " not proper points that has bad indexI, " + indexI +
                                "|" + xArrayValues[i] + "|" + points[0][m][0]);

                    } else if (indexI < 60 && ((points[indexI][m][0] == points[59][59][0] &&
                            points[indexI][m][1] == points[59][59][1])
                            || (m == 0 && yArrayValues[i] > points[indexI][m][1]))) {
                        points[indexI][m][0] = xArrayValues[i];
                        points[indexI][m][1] = yArrayValues[i];
                    } else {
                        Log.w("not proper points:", "" + xArrayValues[i] + "|" + yArrayValues[i] + "|" +
                                tempIndexI + "|" + points[indexI][m][0] + "|" + points[indexI][m][1]);
                    }

                    break;
                }
            }
        }

    }

    private static String getDecodeResult(float[][][] points, int foundStartPoint) {
        String retString = "";
        for (int i = 0; i < 8; i++) {
            for (int j = foundStartPoint; j < foundStartPoint + 8; j++) {
                float expectX = (points[8][j][0] - points[0][j][0]) * i / 8 + points[0][j][0];
                float expectY = (points[8][j][1] - points[0][j][1]) * i / 8 + points[0][j][1];
                if ((points[i][j][0] == points[59][59][0] && points[i][j][1] == points[59][59][1]) ||
                        (points[8][j][0] == points[59][59][0] && points[8][j][1] == points[59][59][1])) {
                    retString += "*";
                    continue;
                }

                //from experience
                double shreshhold = 3.88;

                if (points[i][j][0] < expectX - shreshhold / 2) {
                    if (points[i][j][1] < expectY) {
                        retString += "0";
                    } else {
                        retString += "1";
                    }
                } else if (points[i][j][0] > expectX + shreshhold / 2) {
                    if (points[i][j][1] < expectY) {
                        retString += "2";
                    } else {
                        retString += "3";
                    }
                } else {
                    if (points[i][j][1] == expectY) {
                        retString += "M";
                    } else if (points[i][j][1] < expectY) {
                        retString += "a";
                    } else {
                        retString += "b";
                    }
                }

            }
            retString += "\n";
        }

        return retString;
    }

    private static String getRSDecodeResult(String retString) {
        String findTwoOnePattern = "";
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                findTwoOnePattern += retString.charAt(j * 9 + i);
            }
        }
        RSDecoder rs = new RSDecoder();
        if (findTwoOnePattern.contains("Mb") || findTwoOnePattern.contains("bM")) {
            findTwoOnePattern = findTwoOnePattern.replace("1", "q").replace("a", "e").replace("0", "w")
                    .replace("2", "1").replace("b", "a").replace("3", "0")
                    .replace("q", "2").replace("e", "b").replace("w", "3");
            findTwoOnePattern = new StringBuilder(findTwoOnePattern).reverse().toString();
        }

        int findMA = findTwoOnePattern.indexOf("Ma");
        if (findMA < 0)
            return "findMA:" + findMA;

        findTwoOnePattern = findTwoOnePattern.substring(findMA) + findTwoOnePattern.substring(0, findMA);
        findTwoOnePattern = findTwoOnePattern.replace("abababa", "").replace("a", "3").replace("b", "2")
                .replace("M", "").replace("*", "1");
        String tempFindTwoOne = "";
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                tempFindTwoOne += findTwoOnePattern.charAt(j * 7 + i);
            }
        }
        findTwoOnePattern = tempFindTwoOne;
        int[] input = new int[49];
        for (int i = 0; i < 49; i++) {
            input[i] = Character.getNumericValue(findTwoOnePattern.charAt(i));
        }

        int[] result = new int[16];
        int type = rs.decodeRS(input, result);
        if (type != 0)
            return -3 + "";
        String rscode = "";
        for (int i = 0; i < 16; i++) {
            rscode += result[i] + " ";
        }
        return rscode;
    }
}
