/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wikiauswertung;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nico
 */
public class SmirnovEvaluation {

    static void evaluateHypotheses(ArrayList<CountWikiSession> sessionActionCountMap, boolean oneSided, TreeMap<String, LinkedList<String[]>> hypotheses) {

        try (PrintWriter writer = new PrintWriter("./eval/smirnov_eval_" + SessionConstruction.sessionLength + ".csv", "UTF-8")) {
            writer.println("action;hypothesis;smirnov distance;threshold_90;significant_90;threshold_95;significant_95");

            for (String action : SessionConstruction.relevantActions) {
                for (Entry<String, LinkedList<String[]>> e : hypotheses.entrySet()) {
                    String hypothesisName = e.getKey();
                    LinkedList<String[]> conditions = e.getValue();
                    String[] condA = conditions.get(0);
                    String[] condB = conditions.get(1);
                    TreeMap<Integer, Integer> distrA = createActionsDistribution(condA, action, sessionActionCountMap);
                    TreeMap<Integer, Integer> distrB = createActionsDistribution(condB, action, sessionActionCountMap);

                    double sampleSizeA = countSessions(distrA);
                    double sampleSizeB = countSessions(distrB);

                    // Threshold values were taken from literature:
                    // see W.J. Conover - Practical Nonparametric statistics
                    // 0.9 : 1.07  ; 0.95 : 1.22
                    // 0.9 : 1.22  ; 0.95 : 1,36
                    double threshold_90;
                    double threshold_95;

                    if (oneSided) {
                        threshold_90 = 1.07 * Math.sqrt((sampleSizeA + sampleSizeB) / (sampleSizeA * sampleSizeB));
                        threshold_95 = 1.22 * Math.sqrt((sampleSizeA + sampleSizeB) / (sampleSizeA * sampleSizeB));
                    } else {
                        threshold_90 = 1.22 * Math.sqrt((sampleSizeA + sampleSizeB) / (sampleSizeA * sampleSizeB));
                        threshold_95 = 1.36 * Math.sqrt((sampleSizeA + sampleSizeB) / (sampleSizeA * sampleSizeB));
                    }

                    double distance;

                    if (oneSided) {
                        distance = getSmirnovDistance_OneSided(distrA, distrB);
                    } else {
                        distance = getSmirnovDistance(distrA, distrB);
                    }

                    dumpDistributionToFile(distrA, "conditionA_distr_" + action + "_" + hypothesisName + ".csv");
                    dumpDistributionToFile(distrB, "conditionB_distr_" + action + "_" + hypothesisName + ".csv");

                    writer.println(action + ";" + hypothesisName + ";" + distance + ";" + threshold_90 + ";" + ((distance > threshold_90) ? "1" : "0") + ";" + threshold_95 + ";" + ((distance > threshold_95) ? "1" : "0"));

                }

            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(SessionConstruction.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    static void evaluateHypothesesWithoutPrinting(ArrayList<CountWikiSession> sessionActionCountMap, boolean oneSided, TreeMap<String, LinkedList<String[]>> hypotheses) {
        double h6View = 0;
        double h6Create = 0;
        double h6Save = 0;
        double h6Threshold_90 = 0;
        double h6Threshold_95 = 0;

        for (String action : SessionConstruction.relevantActions) {
            for (Entry<String, LinkedList<String[]>> e : hypotheses.entrySet()) {
                String hypothesisName = e.getKey();
                LinkedList<String[]> conditions = e.getValue();
                String[] condA = conditions.get(0);
                String[] condB = conditions.get(1);
                TreeMap<Integer, Integer> distrA = createActionsDistribution(condA, action, sessionActionCountMap);
                TreeMap<Integer, Integer> distrB = createActionsDistribution(condB, action, sessionActionCountMap);

                double sampleSizeA = countSessions(distrA);
                double sampleSizeB = countSessions(distrB);

                // see W.J. Conover - Practical Nonparametric statistics
                // 0.9 : 1.07  ; 0.95 : 1.22
                // 0.9 : 1.22  ; 0.95 : 1,36
                double threshold_90;
                double threshold_95;

                if (oneSided) {
                    threshold_90 = 1.07 * Math.sqrt((sampleSizeA + sampleSizeB) / (sampleSizeA * sampleSizeB));
                    threshold_95 = 1.22 * Math.sqrt((sampleSizeA + sampleSizeB) / (sampleSizeA * sampleSizeB));
                } else {
                    threshold_90 = 1.22 * Math.sqrt((sampleSizeA + sampleSizeB) / (sampleSizeA * sampleSizeB));
                    threshold_95 = 1.36 * Math.sqrt((sampleSizeA + sampleSizeB) / (sampleSizeA * sampleSizeB));
                }

                double distance;

                if (oneSided) {
                    distance = getSmirnovDistance_OneSided(distrA, distrB);
                } else {
                    distance = getSmirnovDistance(distrA, distrB);
                }

                if (hypothesisName.equals("h2")) {
                    h6Threshold_90 = threshold_90;
                    h6Threshold_95 = threshold_95;
                }

                if (hypothesisName.equals("h2") && action.equals("View")) {
                    h6View = distance;
                } else if (hypothesisName.equals("h2") && action.equals("CREATE")) {
                    h6Create = distance;
                } else if (hypothesisName.equals("h2") && action.equals("SAVE: Complete")) {
                    h6Save = distance;
                }
            }
        }
        System.out.println(h6View + ";" + h6Create + ";" + h6Save + ";" + h6Threshold_90 + ";" + h6Threshold_95);
    }

    public static void dumpDistributionToFile(Map<Integer, Integer> distribution, String fileName) {
        Map<Integer, Integer> fullDistribution = fillMissingValuesWithZero(distribution);
        try (PrintWriter writer = new PrintWriter("./distributions/" + fileName, "UTF-8")) {
            for (Entry e : fullDistribution.entrySet()) {
                writer.println(String.valueOf(e.getKey()) + "," + String.valueOf(e.getValue()));
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(SessionConstruction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static TreeMap<Integer, Integer> createActionsDistribution(String[] cond, String relevantAction, ArrayList<CountWikiSession> sessionActionCountList) {
        TreeMap<String, Double> actionCountMap;
        TreeMap<Integer, Integer> actionDistr = new TreeMap<>();
        actionDistr.put(0, 0);
        for (CountWikiSession countwikiSession : sessionActionCountList) {
            String dashBoard;
            if (cond[0].contains("G")) {      // if G is in the dashboard name then treat whole timegroup as a separate dashboard
                dashBoard = countwikiSession.getTimeGroup();
            } else {
                dashBoard = countwikiSession.getDashBoard().split("GesisDashboard")[1];
            }
            if (Arrays.asList(cond).contains(dashBoard)) {
                actionCountMap = countwikiSession.getSessionActionsDistribution();

                if (!actionCountMap.containsKey(relevantAction)) {
                    actionDistr.put(0, actionDistr.get(0) + 1);
                } else {
                    Integer actionFreq = actionCountMap.get(relevantAction).intValue();
                    if (!actionDistr.containsKey(actionFreq)) {
                        actionDistr.put(actionFreq, 1);
                    } else {
                        actionDistr.put(actionFreq, actionDistr.get(actionFreq) + 1);
                    }

                }
            }
        }
        return actionDistr;
    }

    public static Map<Integer, Double> createCumulativeDistribution(Map<Integer, Integer> distribution) {
        Map<Integer, Integer> distributionMap = fillMissingValuesWithZero(distribution);
        Object[] keyArray = distributionMap.keySet().toArray();
        Map<Integer, Double> cumulativeMap = new TreeMap<>();
        Double totalsum = 0.0;
        for (Integer i : distributionMap.values()) {
            totalsum += i;
        }
        for (int i = 0; i < keyArray.length; i++) {
            Double sum = 0.0;
            for (int j = 0; j <= i; j++) {
                sum += distributionMap.get((Integer) keyArray[j]);
            }
            cumulativeMap.put((Integer) keyArray[i], sum / totalsum);
        }
        return cumulativeMap;
    }

    public static Map<Integer, Integer> fillMissingValuesWithZero(Map<Integer, Integer> distribution) {
        Map<Integer, Integer> distributionMap = new TreeMap<>(distribution);
        int maxThreadSize = Collections.max(distributionMap.keySet());
        for (int i = 1; i < maxThreadSize; i++) {
            if (!distributionMap.containsKey(i)) {
                distributionMap.put(i, 0);
            }
        }
        return distributionMap;
    }

    static double getSmirnovDistance(Map<Integer, Integer> frequencyTable1, Map<Integer, Integer> frequencyTable2) {
        Map<Integer, Double> cumulMap1 = createCumulativeDistribution(frequencyTable1);
        Object[] keyArray1 = cumulMap1.keySet().toArray();
        Map<Integer, Double> cumulMap2 = createCumulativeDistribution(frequencyTable2);
        Object[] keyArray2 = cumulMap2.keySet().toArray();

        ArrayList<Double> diffList = new ArrayList<>();
        int minSize = Math.min(keyArray1.length, keyArray2.length);
        for (int i = 0; i < minSize; i++) {
            diffList.add(Math.abs(cumulMap1.get((Integer) keyArray1[i]) - cumulMap2.get((Integer) keyArray2[i])));
        }
        if (keyArray1.length > minSize) {
            for (int i = minSize; i < keyArray1.length; i++) {
                diffList.add(1.0 - cumulMap1.get((Integer) keyArray1[i]));
            }

        } else if (keyArray2.length > minSize) {
            for (int i = minSize; i < keyArray2.length; i++) {
                diffList.add(1.0 - cumulMap2.get((Integer) keyArray2[i]));
            }
        }

        return Collections.max(diffList);
    }

    static double getSmirnovDistance_OneSided(Map<Integer, Integer> frequencyTable1, Map<Integer, Integer> frequencyTable2) {
        Map<Integer, Double> cumulMap1 = createCumulativeDistribution(frequencyTable1);
        Object[] keyArray1 = cumulMap1.keySet().toArray();
        Map<Integer, Double> cumulMap2 = createCumulativeDistribution(frequencyTable2);
        Object[] keyArray2 = cumulMap2.keySet().toArray();

        ArrayList<Double> diffList = new ArrayList<>();
        int minSize = Math.min(keyArray1.length, keyArray2.length);
        for (int i = 0; i < minSize; i++) {
            diffList.add(cumulMap1.get((Integer) keyArray1[i]) - cumulMap2.get((Integer) keyArray2[i]));
        }
        if (keyArray1.length > minSize) {
            for (int i = minSize; i < keyArray1.length; i++) {
                diffList.add(cumulMap1.get((Integer) keyArray1[i]) - 1.0);
            }

        } else if (keyArray2.length > minSize) {
            for (int i = minSize; i < keyArray2.length; i++) {
                diffList.add(1.0 - cumulMap2.get((Integer) keyArray2[i]));
            }
        }

        return Collections.max(diffList);
    }

    private static double countSessions(TreeMap<Integer, Integer> distr) {
        double sum = 0;
        for (Entry<Integer, Integer> e : distr.entrySet()) {
            sum += (double) e.getValue();
        }
        return sum;
    }

}
