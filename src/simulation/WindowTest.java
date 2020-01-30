/*
 * The MIT License
 *
 * Copyright 2020 croth.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package simulation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Simple simulation of coincidence din
 *
 * @author croth
 */
public class WindowTest {

    /* detection events at A and B */
    boolean[] deta1a; // we record two streams each, since in reality, we cannot reuse any existing measurements
    boolean[] deta1b;
    boolean[] detb1a;
    boolean[] detb1b;
    boolean[] deta2a;
    boolean[] detb2a;
    boolean[] deta2b;
    boolean[] detb2b;

    static Random rnd;

    int nrtrials;
    double efficiency;
    int uncertainty;
    boolean acceptDoubleCounts;

    public WindowTest() {
        rnd = new Random();
        // Default random seed, can be overwritten via arguments in main
        rnd.setSeed(1234);
        acceptDoubleCounts = true;
    }

    /* Simply count the nr of coincidences of A and B using FIXED windows */
    public int countCoincidences(int windowSize, int a, int b, boolean[] deta, boolean[] detb) {
        int count = 0;

        for (int startOfWindow = 0; startOfWindow + windowSize < nrtrials; startOfWindow += windowSize) {
            int position = startOfWindow;
            int aDetected = 0;
            int bDetected = 0;
            for (int i = 0; i < windowSize; i++) {
                if (deta[position + i]) {
                    aDetected++;
                    // stop count at 1
                    if (acceptDoubleCounts) {
                        aDetected = Math.min(1, aDetected);
                    }
                }
                if (detb[position + i]) {
                    bDetected++;
                    if (acceptDoubleCounts) {
                        bDetected = Math.min(1, bDetected);
                    }
                }
            }
            // we discard double counts - only if each window has one count it is considered valid
            if (aDetected == a && bDetected == b) {
                count++;
            }
        }
        return count;
    }

    /* Create a stream of detection events based on the detector angle detAngle and the efficiency. 
    Use a normal distribution to determine the probability to detect something */
    private boolean[] createDetectionStream(double detAngle) {
        boolean[] det = new boolean[nrtrials];

        for (int i = 0; i < nrtrials; i++) {
            double photonAngle = 0; // hidden variable
            double delta = (photonAngle + detAngle);
            double p = Math.cos(delta) * efficiency;

            if (p > 0) {
                double prob = p;
                if (uncertainty > 0) {
                    double expectedDistance = 1.0 / p; // mean distance between detection events
                    double relativeDistance = i % (int) expectedDistance; // current distance to next likely detection event
                    relativeDistance = Math.min(Math.abs(expectedDistance - relativeDistance), relativeDistance);;
                    prob = prob * pdf(relativeDistance, 0, uncertainty);
                } // normal distribution around that position
                det[i] = rnd.nextDouble() < prob;
            }
        }
        return det;
    }

    private void simpleWindowTest() {
        /* We chose probabilities that will lead to J < 0 */
//  0.25, 0.9000000000000002, 0.6, 0.1
        double pa1 = 0.9;
        double pa2 = 0.7;
        double pb1 = 0.4;
        double pb2 = 0.3;

        /* the joint probabilities that determine J */
        double p11 = pa1 * pb1;
        double p22 = pa2 * pb2;
        double p12 = pa1 * (1.0 - pb2);
        double p21 = (1.0 - pa2) * pb1;

        double jloc = p11 - p22 - p21 - p12;

        /* The detector angles */
        double a1 = Math.acos(pa1);
        double a2 = Math.acos(pa2);
        double b1 = Math.acos(pb1);
        double b2 = Math.acos(pb2);

        String out = "angles (degrees), a1, a2, b1, b2";
        out += ",,,efficiency, " + efficiency;
        out += "\n, " + round(Math.toDegrees(a1), 2) + "," + round(Math.toDegrees(a2), 2)
                + ", " + round(Math.toDegrees(b1), 2) + ", " + round(Math.toDegrees(b2), 2);
        out += ",,,nr trials, " + nrtrials;
        out += "\np(detection), " + pa1 + ", " + pa2 + ", " + pb1 + ", " + pb2;
        out += ",,,uncertainty, " + uncertainty;
        out += "\np(coinc), p11(PP), p12(P0), p21(0P), p22(PP)";
        out += "\n, " + round(p11, 4) + ", " + round(p12, 4) + ", " + round(pb1, 4) + ", " + round(p22, 4);
        out += "\n\np11 - p12 - p21 - p22 = " + round(jloc, 4);
        out += "\n" + round(p11, 4) + " - " + round(p12, 4) + " - " + round(p12, 4) + " - " + round(p22, 4) + " = " + round(jloc, 4);
        out += "\n\nwindow size, c11 (PP), c12 (P0), c21 (0P), c22 (PP), J, J/counts\n";
        p(out);

        deta1a = createDetectionStream(a1);
        detb1a = createDetectionStream(b1);
        deta2a = createDetectionStream(a2);
        detb2a = createDetectionStream(b2);
        deta1b = createDetectionStream(a1);
        detb1b = createDetectionStream(b1);
        deta2b = createDetectionStream(a2);
        detb2b = createDetectionStream(b2);

        int dw = 50;
        for (int window = 1; window <= 5500; window += dw) {

            int c11 = countCoincidences(window, 1, 1, deta1a, detb1a);
            int c12 = countCoincidences(window, 1, 0, deta1b, detb2a);
            int c21 = countCoincidences(window, 0, 1, deta2a, detb1b);
            int c22 = countCoincidences(window, 1, 1, deta2b, detb2b);

            // Compute J based on Counts
            int j = c11 - c12 - c21 - c22;

            double norm = (double) j / (double) (c11 + c12 + c22 + c21);
            String st = window + ", " + c11 + ", " + c12 + ", " + c21 + ", " + c22 + ", " + j + ", " + round(norm, 5);
            out += st + "\n";
            p(st);
        }
        writeStringToFile(new File("stream_u" + uncertainty + "_e" + efficiency + "_n" + nrtrials + "_a.csv"), out, false);
        p(out);
    }

   
    /* Read arguments */
    public static void main(String[] args) {
        WindowTest s = new WindowTest();
      
        long seed = 1234;
        int trials = 10000000;
        double efficiency = 0.1;
        int uncertainty = 3;
        if (args != null && args.length > 1) {
            for (int i = 0; i + 1 < args.length; i += 2) {
                String key = args[i].toUpperCase();
                String value = args[i + 1];
                if (key.startsWith("-")) {
                    key = key.substring(1);
                }
                if (key.startsWith("S")) {
                    try {
                        seed = Long.parseLong(value);
                    } catch (Exception ex) {
                        p("Could not convert " + value + " to long. Try something like 24252");
                    }
                } else if (key.startsWith("T")) {
                    try {
                        trials = Integer.parseInt(value);
                    } catch (Exception ex) {
                        p("Could not convert " + value + " to int. Try something like 10000");
                    }
                } else if (key.startsWith("E")) {
                    try {
                        efficiency = Double.parseDouble(value);
                    } catch (Exception ex) {
                        p("Could not convert " + value + " to int. Try something like 0.1");
                    }
                } else if (key.startsWith("U")) {
                    try {
                        uncertainty = Integer.parseInt(value);
                    } catch (Exception ex) {
                        p("Could not convert " + value + " to int. Try something like 2");
                    }
                }

            }

        }
        s.nrtrials = trials;
        s.efficiency = efficiency;
        s.uncertainty = uncertainty;
        rnd.setSeed(seed);
        s.simpleWindowTest();

    }

    private static void p(String s) {
        System.out.println(s);

    }

    /* Helper method to write the output */
    public static boolean writeStringToFile(File f, String content, boolean append) {
        PrintWriter fout = null;
        try {
            fout = new PrintWriter(new BufferedWriter(new FileWriter(f, append)));
            fout.print(content);
            fout.flush();
            fout.close();
            return true;
        } catch (FileNotFoundException e) {
            p("File " + f + " not found");
        } catch (IOException e) {
            p("IO Exception");
        } finally {
            if (fout != null) {
                fout.flush();
                fout.close();
            }
        }
        return false;
    }

    /* Helper method to round a double for better output */
    private static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    //  Helper method for Gaussian pdf with mean mu and stddev sigma
    private static double pdf(double x, double mu, double sigma) {
        return pdf((x - mu) / sigma) / sigma;
    }

    // Helper method for standard Gaussian pdf
    private static double pdf(double x) {
        return Math.exp(-x * x / 2) / Math.sqrt(2 * Math.PI);
    }

}
