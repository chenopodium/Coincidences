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
    boolean[] deta;
    boolean[] detb;

    static Random rnd;

    int nrtrials;
    double efficiency;

    public WindowTest() {
        rnd = new Random();
        // Default random seed, can be overwritten via arguments
        rnd.setSeed(1234);
    }

    /* Simply count the nr of coincidences of A and B using FIXED windows */
    public int countCoincidences(int window) {
        int count = 0;

        for (int startOfWindow = 0; startOfWindow + window < nrtrials; startOfWindow += window) {
            int position = startOfWindow;
            boolean aDetected = false;
            boolean bDetected = false;
            for (int i = 0; i < window; i++) {
                if (deta[position + i]) {
                    aDetected = true;
                }
                if (detb[position + i]) {
                    bDetected = true;
                }
                if (aDetected && bDetected) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    /* Create a stream of detection events based on the probability p and the efficiency. 
    Use a normal distribution to determine the probability to detect something */
    private boolean[] createDetectionStream(double p) {
        boolean[] det = new boolean[nrtrials];
        double every = 1.0 / p;
        for (int i = 0; i < nrtrials; i++) {
            double dist = i % (int) every;
            double prob = p * pdf(dist, 0, 2);
            det[i] = rnd.nextDouble() < prob * efficiency;
        }
        return det;
    }

    private void simpleWindowTest() {
        /* We chose probabilities that will lead to J < 0 */
        double pa1 = 0.8;
        double pa2 = 0.2;
        double pb1 = 0.8;
        double pb2 = 0.5;

        /* the joint probabilities that determine J */
        double p11 = pa1 * pb1;
        double p22 = pa2 * pb2;
        double p12 = pa1 * pb2;
        double p21 = pa2 * pb1;

        double jloc = p11 - p22 - p21 - p12;
        
        String out = ("angles (degrees), a1, a2, b1, b2");
        out += ("\n, " + round(Math.toDegrees(Math.acos(pa1)), 2) + "," + round(Math.toDegrees(Math.acos(pa2)), 2)
                + "angles (degrees), " + round(Math.toDegrees(Math.acos(pb1)), 2) + ", " + round(Math.toDegrees(Math.acos(pb2)), 2));
        out += ("\n\np(detection), pa1, pa2, pb1, pb2");
        out += ("\np(detection), " + pa1 + ", " + pa2 + ", " + pb1 + ", " + pb2);

        out += ("\n\np(coinc), p11, p12, p21, p22");
        out += ("\n, " + round(p11, 4) + ", " + round(p12, 4) + ", " + round(pb1, 4) + ", " + round(p22, 4));

        out += ("\n\np11 - p12 - p21 - p22 = " + round(jloc, 4));
        out += ("\n" + round(p11, 4) + " - " + round(p12, 4) + " - " + round(p12, 4) + " - " + round(p22, 4) + " = " + round(jloc, 4));
        out += "\nefficiency, " + efficiency;
        out += "\nnr trials, " + nrtrials;

        out += "\n\nwindow size, c11, c12, c21, c22, j\n";

        p(out);
        for (int window = 1; window < 200; window += 1) {

            // Detection stream and counts for A1 B1
            deta = createDetectionStream(pa1);
            detb = createDetectionStream(pb1);
            int c11 = countCoincidences(window);

            // Detection stream and counts for A2 B2
            deta = createDetectionStream(pa2);
            detb = createDetectionStream(pb2);
            int c22 = countCoincidences(window);

            // Detection stream and counts for A1 B2
            deta = createDetectionStream(pa1);
            detb = createDetectionStream(pb2);
            int c12 = countCoincidences(window);

            // Detection stream and counts for A2 B1
            deta = createDetectionStream(pa2);
            detb = createDetectionStream(pb1);
            int c21 = countCoincidences(window);

            // Compute J based on Counts
            int j = c11 - c12 - c21 - c22;
            String st = window + ", " + c11 + ", " + c12 + ", " + c21 + ", " + c22 + ", " + j;
            out += st + "\n";

            // Note: import the file in Excel to plot
            p(st);
        }
        writeStringToFile(new File("stream.csv"), out, false);
        p(out);
    }

    /* Read arguments */
    public static void main(String[] args) {
        WindowTest s = new WindowTest();
        long seed = 1234;
        int trials = 10000000;
        double efficiency = 0.1;
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
                }

            }

        }
        s.nrtrials = trials;
        s.efficiency = efficiency;
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