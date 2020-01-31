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
public class WindowMixedTest {

    static Random rnd;

    int nrtrials;
    double efficiency;
    int uncertainty;
    boolean acceptDoubleCounts;

    public WindowMixedTest() {
        rnd = new Random();
        // Default random seed, can be overwritten via arguments in main
        rnd.setSeed(1234);
        acceptDoubleCounts = true;
    }

    /* Simply count the nr of coincidences of A and B using FIXED windows */
    public int countCoincidences(int windowSize, int a, int b, boolean[] deta, boolean[] detb) {
        int count = 0;

        if (nrtrials < 500) {
            showData(deta, detb, windowSize, a, b);
        }

        for (int startOfWindow = 0; startOfWindow + windowSize <= deta.length; startOfWindow += windowSize) {
            int position = startOfWindow;
            int aDetected = 0;
            int bDetected = 0;
            for (int i = 0; i < windowSize; i++) {
                if (deta[position + i]) {
                    aDetected++;
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
        if (nrtrials < 500) {
            p("Counts: " + count);
        }
        return count;
    }

    /* Create a stream of detection events based on the detector angle detAngle and the efficiency. 
    Use a normal distribution to determine the probability to detect something */
    private boolean[] createDetectionStream(double detAngle, int nr, long startTime) {
        boolean[] det = new boolean[nr];

        for (int i = 0; i < nr; i++) {
            double photonAngle = 0; // hidden variable
            double delta = (photonAngle + detAngle);
            double p = Math.cos(delta) * efficiency;

            if (p > 0) {
                double expectedDistance = 1.0 / p; // mean distance between detection events
                double relativeDistance = (startTime + i) % (int) expectedDistance; // current distance to next likely detection event
                relativeDistance = Math.min(Math.abs(expectedDistance - relativeDistance), relativeDistance);
                // boolean detected = rnd.nextDouble() < p;
                boolean detected = relativeDistance < 1;

                if (uncertainty > 0 && detected) {

                    //prob = prob * pdf(relativeDistance, 0, uncertainty);
                    int dpos = (int) (rnd.nextGaussian() * uncertainty);
                    int pos = i + dpos;
                    if (pos >= 0 && pos < nr) {
                        det[pos] = detected;
                    }
                } // normal distribution around that position
                else {
                    det[i] = detected;
                }
            }
        }
        return det;
    }

    private void simpleWindowTest() {
        /* We chose probabilities that will lead to J < 0 */

        double pa1 = 0.85;
        double pa2 = 0.4;
        double pb1 = 0.9;
        double pb2 = 0.1;

        /* the joint probabilities that determine J */
        double p11 = pa1 * pb1;
        double p12 = pa1 * (1.0 - pb2);
        double p21 = (1.0 - pa2) * pb1;
        double p22 = pa2 * pb2;

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
        out += "\nprobability, p11(PP), p12(P0), p21(0P), p22(PP)";
        out += "\n, " + round(p11, 6) + ", " + round(p12, 6) + ", " + round(p21, 6) + ", " + round(p22, 6);
        out += "\n\np11 - p12 - p21 - p22 = " + round(jloc, 6);
        out += "\n" + round(p11, 6) + " - " + round(p12, 6) + " - " + round(p21, 6) + " - " + round(p22, 6) + " = " + round(jloc, 6);
        out += "\n\nwindow size, c11 (PP), c12 (P0), c21 (0P), c22 (PP), J,,  Total counts, c11 (PP) /counts, c12 (P0)/counts, c21 (0P) /counts, c22 (PP)/counts, J/counts, tot a1, tot a2, tot b2, tot b2\n";
        p(out);

        /* We create multiple parts, because we cannot reuse a measurement in practice!
         */
        int dw = 1;
        for (int window = 1; window <= 100; window += dw) {

            if (nrtrials < 200) {
                p("______________________ WINDOW " + window + " ____________________");
            }
            int c11 = 0;
            int c12 = 0;
            int c21 = 0;
            int c22 = 0;
            
            int tota1= 0;
            int tota2= 0;
            int totb1= 0;
            int totb2= 0; 
            
            int tot11=0;
            int tot12 =0;
            int tot21 = 0;
            int tot22 = 0;
            
            for (int trial = 0; trial < nrtrials; trial++) {
                /* We create multiple parts, because we cannot reuse a measurement in practice! */
                boolean choiceA1 = rnd.nextDouble() < 0.5;
                boolean[] detA = createDetectionStream(choiceA1 ? a1 : a2, window, trial);
                boolean choiceB1 = rnd.nextDouble() < 0.5;
                boolean[] detB = createDetectionStream(choiceB1 ? b1 : b2, window, trial);

                int pp = countCoincidences(window, 1, 1, detA, detB);
                int pz = countCoincidences(window, 1, 0, detA, detB);
                int zp = countCoincidences(window, 0, 1, detA, detB);
                int zz = countCoincidences(window, 0, 0, detA, detB);
                // test: sum shold be 1
                int sanity = pp + pz + zp + zz;
                if (sanity !=1) {
                    p("Sanity test failed, total should be 1");
                }
                if (choiceA1) tota1++;
                else tota2++;
                 if (choiceB1) totb1++;
                else totb2++;
                 
                if (choiceA1 && choiceB1) {
                    c11 += pp;
                    tot11++;
                } else if (choiceA1 && !choiceB1) {
                    c12 += pz;
                    tot12++;
                } else if (!choiceA1 && choiceB1) {
                    c21 += zp;
                    tot21++;
                } else if (!choiceA1 && !choiceB1) {
                    c22 += pp;
                    tot22++;
                }

            }
            // Compute J based on Counts
            int j = c11 - c12 - c21 - c22;

            long counts = c11 + c12 + c22 + c21;
            String st = window + ", " + c11 + ", " + c12 + ", " + c21 + ", " + c22 + ", " + j + ",, " + counts;
            st += ", " + format(c11, tot11) + ", " + format(c12, tot12) + ", " + format(c21, tot21) + ", " + format(c22, tot22) + ", " + format(j, counts);
            st += ", "+tota1+", "+tota2+", "+totb1+", "+totb2;
            out += st + "\n";
            p(st);
        }
        writeStringToFile(new File("stream_u" + uncertainty + "_e" + efficiency + "_n" + nrtrials + "_double_" + acceptDoubleCounts + ".csv"), out, false);
        p(out);
    }

    private double format(long c, long t) {
        return round((double) c / (double) t, 6);
    }

    /* Read arguments */
    public static void main(String[] args) {
        WindowMixedTest s = new WindowMixedTest();

        long seed = 1234;
        int trials = 10000;
        double efficiency = 0.1;
        int uncertainty = 2;
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

    private void showData(boolean[] deta, boolean[] detb, int windowSize, int a, int b) {
        String sa = "\nA: ";
        String sb = "\nB: ";
        String sw = "\nW: ";
        for (int i = 0; i < nrtrials; i++) {
            if (deta[i]) {
                sa += "1";
            } else {
                sa += "_";
            }
            if (detb[i]) {
                sb += "1";
            } else {
                sb += "_";
            }
            if (i % windowSize == 0) {
                sw += "|";
            } else {
                sw += " ";
            }
        }
        String s = "\nWindowSize: " + windowSize + ", Checking for det a: " + a + " det b: " + b;
        p(s + sa + sb + sw);
    }

}
