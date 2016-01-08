/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wikiauswertung;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.LocalDate;

/**
 *
 * @author nico
 */
public class SessionConstruction {

    public static long idleTime = 260;
    public static long sessionLength = 94;
    public static String[] relevantActions = {"View", "CREATE", "SAVE: Complete"};
//  public static String[] relevantActions = {"View_Unique", "CREATE_Unique", "SAVE: Complete_Unique"};

    public static DateFormat df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
    public static Date g0_endDate;
    public static Date g1_endDate;

    public static void main(String[] args) throws ParseException {

//      G0 Control Group        Alle Aktivitäten bis 09.02.2014
//      G1 Activated with Mail  Alle Aktivitäten ab 10.02.2014 bis 20.03.2014
//      G2 Activated with Email Alle Aktivätäten ab 21.02.2014
        g0_endDate = df.parse("2014-02-10 00:00:00");
        g1_endDate = df.parse("2014-03-21 00:00:00");

//        for (sessionLength = 1; sessionLength <= 500; sessionLength++) {
        TreeMap<String, ArrayList<Action>> sessions = createSessions(true);
//        countConcurrentActions(sessions);
        ArrayList<WikiSession> dashboardSessions = assignDashboardAndTimeGroupToSessions(sessions);
        filterSessionWithoutLogin(dashboardSessions);

//        filterByTimeGroup(dashboardSessions, "G2");
//        countActionsPerDay(dashboardSessions);
//        countConcurrentActions(dashboardSessions);
//        TreeMap<String, Integer> sessionSizeMap = countSessionsPerDashboard(dashboardSessions);
        //      writeSessionsToFile(dashboardSessions);
        // count actions per session
        ArrayList<CountWikiSession> sessionActionCountMap = countActionsPerSession(dashboardSessions);
        // count actions per dashboard
//        TreeMap<String, TreeMap<String, Double>> dashBoardActionCountMap = countActionsPerDashboard(sessionActionCountMap);
//        TreeMap<String, TreeMap<String, Double>> normalizedDashBoardActionCountMap = normalizeDashboardCounts(dashBoardActionCountMap, sessionSizeMap);
//        writeResultsToFile(normalizedDashBoardActionCountMap);
//        evaluateMeans(normalizedDashBoardActionCountMap, sessionSizeMap);

        // evaluate hypotheses one sided
        SmirnovEvaluation.evaluateHypotheses(sessionActionCountMap, true, createHypotheses());
        // evaluate brute force hypotheses two sided
//        SmirnovEvaluation.evaluateHypothesesWithoutPrinting(sessionActionCountMap, true, createHypotheses());
//        SessionActivity.createActivityDistribution(dashboardSessions, createHypotheses().get("h6"), 1);
//        }

    }

    static HashMap<String, HashMap<String, ArrayList<Double>>> createRandomizationTestInput(ArrayList<CountWikiSession> sessionActionCountList, String hypothesisName, TreeMap<String, LinkedList<String[]>> hypotheses) {
        HashMap<String, HashMap<String, ArrayList<Double>>> randomizationTestInput = new HashMap<>();
        LinkedList<String[]> hyp = hypotheses.get(hypothesisName);

        for (String[] cond : hyp) {
            String conditionName = "";
            for (String s : cond) {
                conditionName += "_" + s;
            }
            randomizationTestInput.put(conditionName, createSampleForCondition(sessionActionCountList, cond));
        }

        return randomizationTestInput;
    }

    private static HashMap<String, ArrayList<Double>> createSampleForCondition(ArrayList<CountWikiSession> sessionActionCountList, String[] cond) {
        HashMap<String, ArrayList<Double>> actionSamples = new HashMap<>();

        for (String action : SessionConstruction.relevantActions) {
            actionSamples.put(action, new ArrayList<Double>());

            for (CountWikiSession countwikiSession : sessionActionCountList) {
                TreeMap<String, Double> actionCountMap;
                String dashBoard = countwikiSession.getDashBoard().split("GesisDashboard")[1];

                if (Arrays.asList(cond).contains(dashBoard)) {
                    actionCountMap = countwikiSession.getSessionActionsDistribution();

                    if (!actionCountMap.containsKey(action)) {
                        actionSamples.get(action).add(0d);
                    } else {
                        actionSamples.get(action).add(actionCountMap.get(action));
                    }
                }
            }
        }
        return actionSamples;
    }

    public static void evaluateMeans(TreeMap<String, TreeMap<String, Double>> normalizedDashBoardActionCountMap, TreeMap<String, Integer> sessionCounts) {
        TreeMap<String, LinkedList<String[]>> hypothesis = createHypotheses();

        try (PrintWriter writer = new PrintWriter("./eval/hypothesis_test_" + idleTime + ".csv", "UTF-8")) {
            writer.println("hypothesis;conditionA;conditionB;sessionsA;sessionsB;action");
            for (String action : relevantActions) {
                for (Entry<String, LinkedList<String[]>> h : hypothesis.entrySet()) {
                    String hyp = h.getKey();
                    String[] condA = h.getValue().get(0);
                    String[] condB = h.getValue().get(1);
                    double meanA = 0;
                    double meanB = 0;
                    int sessionCountA = 0;
                    int sessionCountB = 0;
                    for (String dashBoard : condA) {
                        sessionCountA += sessionCounts.get(dashBoard);
                        if (normalizedDashBoardActionCountMap.get(dashBoard).containsKey(action)) {
                            meanA += normalizedDashBoardActionCountMap.get(dashBoard).get(action);
                        }
                    }
                    meanA = meanA / condA.length;
                    for (String dashBoard : condB) {
                        sessionCountB += sessionCounts.get(dashBoard);
                        if (normalizedDashBoardActionCountMap.get(dashBoard).containsKey(action)) {
                            meanB += normalizedDashBoardActionCountMap.get(dashBoard).get(action);
                        }
                    }
                    meanB = meanB / condB.length;
                    writer.println(hyp + ";" + meanA + ";" + meanB + ";" + sessionCountA + ";" + sessionCountB + ";" + action);
                }
                writer.println();
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(SessionConstruction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
//      A       E       F
//2 	1 	1 	1 		/sofiswiki/Spezial:GesisDashboardA1F0
//3 	1 	2 	1 		/sofiswiki/Spezial:GesisDashboardA1F1
//4 	1 	1 	2 		/sofiswiki/Spezial:GesisDashboardA1F2
//5 	1 	2 	2 		/sofiswiki/Spezial:GesisDashboardA1F3
//6 	2 	1 	1 		/sofiswiki/Spezial:GesisDashboardA2F0
//7 	2 	2 	1 		/sofiswiki/Spezial:GesisDashboardA2F1
//8 	2 	1 	2 		/sofiswiki/Spezial:GesisDashboardA2F2
//9 	2 	2 	2 		/sofiswiki/Spezial:GesisDashboardA2F3
//10 	3 	1 	1 		/sofiswiki/Spezial:GesisDashboardA3F0
//11 	3 	2 	1 		/sofiswiki/Spezial:GesisDashboardA3F1
//12 	3 	1 	2 		/sofiswiki/Spezial:GesisDashboardA3F2
//13 	3 	2 	2 		/sofiswiki/Spezial:GesisDashboardA3F3
//14 	5 	1 	1 		/sofiswiki/Spezial:GesisDashboardA5F0
//15 	5 	2 	1 		/sofiswiki/Spezial:GesisDashboardA5F1
//16 	5 	1 	2 		/sofiswiki/Spezial:GesisDashboardA5F2
//17 	5 	2 	2 		/sofiswiki/Spezial:GesisDashboardA5F3

    public static TreeMap<String, LinkedList<String[]>> createConditionHypotheses() {
        TreeMap<String, LinkedList<String[]>> hypothesis = new TreeMap<>();             
        
        String[] a1 = {"A1F0", "A1F2", "A1F2", "A1F3"};
        String[] a2 = {"A2F0", "A2F2", "A2F2", "A2F3"};
        String[] a3 = {"A3F0", "A3F2", "A3F2", "A3F3"};
        String[] a5 = {"A5F0", "A5F2", "A5F2", "A5F3"};
        String[] none = {"NONE"};

        LinkedList<String[]> a1a2 = new LinkedList<>();
        a1a2.add(a1);
        a1a2.add(a2);
        hypothesis.put("A1-A2", a1a2);
        LinkedList<String[]> a1a3 = new LinkedList<>();
        a1a3.add(a1);
        a1a3.add(a3);
        hypothesis.put("A1-A3", a1a3);
        LinkedList<String[]> a1a5 = new LinkedList<>();
        a1a5.add(a1);
        a1a5.add(a5);
        hypothesis.put("A1-A5", a1a5);
        LinkedList<String[]> a2a3 = new LinkedList<>();
        a2a3.add(a2);
        a2a3.add(a3);
        hypothesis.put("A2-A3", a2a3);
        LinkedList<String[]> a2a5 = new LinkedList<>();
        a2a5.add(a2);
        a2a5.add(a5);
        hypothesis.put("A2-A5", a2a5);
        LinkedList<String[]> a3a5 = new LinkedList<>();
        a3a5.add(a3);
        a3a5.add(a5);
        hypothesis.put("A3-A5", a3a5);
        LinkedList<String[]> a1none = new LinkedList<>();
        a1none.add(a1);
        a1none.add(none);
        hypothesis.put("A1-NONE", a1none);
        LinkedList<String[]> a2none = new LinkedList<>();
        a2none.add(a2);
        a2none.add(none);
        hypothesis.put("A2-NONE", a2none);
        LinkedList<String[]> a3none = new LinkedList<>();
        a3none.add(a3);
        a3none.add(none);
        hypothesis.put("A3-NONE", a3none);
        LinkedList<String[]> a5none = new LinkedList<>();
        a5none.add(a5);
        a5none.add(none);
        hypothesis.put("A5-NONE", a5none);

        String[] e1 = {"A1F0", "A1F2", "A2F0", "A2F2", "A3F0", "A3F2", "A5F0", "A5F2"};
        String[] e2 = {"A1F1", "A1F3", "A2F1", "A2F3", "A3F1", "A3F3", "A5F1", "A5F3"};

        LinkedList<String[]> e1e2 = new LinkedList<>();
        e1e2.add(e1);
        e1e2.add(e2);
        hypothesis.put("E1-E2", e1e2);

        LinkedList<String[]> e1none = new LinkedList<>();
        e1none.add(e1);
        e1none.add(none);
        hypothesis.put("E1-NONE", e1none);

        LinkedList<String[]> e2none = new LinkedList<>();
        e2none.add(e2);
        e2none.add(none);
        hypothesis.put("E2-NONE", e2none);

        String[] f1 = {"A1F0", "A1F1", "A2F0", "A2F1", "A3F0", "A3F1", "A5F0", "A5F1"};
        String[] f2 = {"A1F2", "A1F3", "A2F2", "A2F3", "A3F2", "A3F3", "A5F2", "A5F3"};

        LinkedList<String[]> f1f2 = new LinkedList<>();
        f1f2.add(f1);
        f1f2.add(f2);
        hypothesis.put("F1-F2", f1f2);

        LinkedList<String[]> f1none = new LinkedList<>();
        f1none.add(f1);
        f1none.add(none);
        hypothesis.put("F1-NONE", f1none);

        LinkedList<String[]> f2none = new LinkedList<>();
        f2none.add(f2);
        f2none.add(none);
        hypothesis.put("F2-NONE", f2none);

//        String[] g0 = {"G0"};
//        String[] g1 = {"G1"};
//        String[] g2 = {"G2"};
//
//        LinkedList<String[]> g1g2 = new LinkedList<>();
//        g1g2.add(g1);
//        g1g2.add(g2);
//        hypothesis.put("G1-G2", g1g2);
//
//        LinkedList<String[]> g1g0 = new LinkedList<>();
//        g1g0.add(g1);
//        g1g0.add(g0);
//        hypothesis.put("G1-G0", g1g0);
//
//        LinkedList<String[]> g2g0 = new LinkedList<>();
//        g2g0.add(g2);
//        g2g0.add(g0);
//        hypothesis.put("G2-G0", g2g0);
        return hypothesis;
    }

    public static TreeMap<String, LinkedList<String[]>> createHypotheses() {
    
        // A1-A5 = different "actions"
        // F0+F1 = non-motivational text
        // F2+F3 = motivational text
                
        LinkedList<String[]> h1 = new LinkedList<>();
        String[] h1a = {"A1F0", "A1F2", "A2F0", "A2F2", "A3F0", "A3F2", "A5F0", "A5F2"};
        String[] h1b = {"A1F1", "A1F3", "A2F1", "A2F3", "A3F1", "A3F3", "A5F1", "A5F3"};
        h1.add(h1a);
        h1.add(h1b);
        LinkedList<String[]> h2 = new LinkedList<>();
        String[] h2a = {"NONE"};
        String[] h2b = {"A1F0", "A1F1", "A1F2", "A1F3", "A2F0", "A2F1", "A2F2", "A2F3"};
        h2.add(h2a);
        h2.add(h2b);
        LinkedList<String[]> h3 = new LinkedList<>();
        String[] h3a = {"NONE"};
        String[] h3b = {"A3F0", "A3F1", "A3F2", "A3F3", "A5F0", "A5F1", "A5F2", "A5F3"};
        h3.add(h3a);
        h3.add(h3b);
        LinkedList<String[]> h4 = new LinkedList<>();
        String[] h4a = {"A3F0", "A3F2", "A5F0", "A5F2"};
        String[] h4b = {"A3F1", "A3F3", "A5F1", "A5F3"};
        h4.add(h4a);
        h4.add(h4b);
        LinkedList<String[]> h5 = new LinkedList<>();
        String[] h5a = {"A1F0", "A1F1", "A2F0", "A2F1", "A3F0", "A3F1", "A5F0", "A5F1"};
        String[] h5b = {"A1F2", "A1F3", "A2F2", "A2F3", "A3F2", "A3F3", "A5F2", "A5F3"};
        h5.add(h5a);
        h5.add(h5b);
        LinkedList<String[]> h6 = new LinkedList<>();
        String[] h6a = {"NONE"};
        String[] h6b = {"A1F0", "A1F1", "A1F2", "A1F3", "A2F0", "A2F1", "A2F2", "A2F3", "A3F0", "A3F1", "A3F2", "A3F3", "A5F0", "A5F1", "A5F2", "A5F3"};
        h6.add(h6a);
        h6.add(h6b);
        
        LinkedList<String[]> h7 = new LinkedList<>();
        String[] h7a = {"A1F0", "A1F1", "A2F0", "A2F1", "A3F0", "A3F1", "A5F0", "A5F1"};
        String[] h7b = {"A1F2", "A1F3", "A2F2", "A2F3", "A3F2", "A3F3", "A5F2", "A5F3"};        
        h7.add(h7a);
        h7.add(h7b);
        
        TreeMap<String, LinkedList<String[]>> hypothesis = new TreeMap<>();
        hypothesis.put("h1", h1);
        hypothesis.put("h2", h2);
        hypothesis.put("h3", h3);
        hypothesis.put("h4", h4);
        hypothesis.put("h5", h5);
        hypothesis.put("h6", h6);
        hypothesis.put("h7", h7);
        return hypothesis;
    }

    public static void writeResultsToFile(TreeMap<String, TreeMap<String, Double>> normalizedDashBoardActionCountMap) {

        try (PrintWriter writer = new PrintWriter("dashboard_results.csv", "UTF-8")) {
            String headerline = "dashboard";
            for (String action : relevantActions) {
                headerline += ";" + action;
            }
            writer.println(headerline);
            for (Entry e : normalizedDashBoardActionCountMap.entrySet()) {
                String line = (String) e.getKey();
                TreeMap<String, Double> actionsCountMap = (TreeMap<String, Double>) e.getValue();
                for (String action : relevantActions) {
                    line += ";" + actionsCountMap.get(action);
                }
                writer.println(line);
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(SessionConstruction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static TreeMap<String, TreeMap<String, Double>> countActionsPerDashboard(TreeMap<String, TreeMap<String, Double>> sessionActionCountMap) {
        TreeMap<String, TreeMap<String, Double>> dashboardActionCountMap = new TreeMap<>();
        for (Entry e : sessionActionCountMap.entrySet()) {
            String dashBoard = ((String) e.getKey()).split("GesisDashboard")[1];
            TreeMap<String, Double> sessionActions = (TreeMap<String, Double>) e.getValue();
            if (!dashboardActionCountMap.containsKey(dashBoard)) {
                dashboardActionCountMap.put(dashBoard, (TreeMap<String, Double>) sessionActions.clone());
            } else {
                for (Entry actionEntry : sessionActions.entrySet()) {
                    String action = (String) actionEntry.getKey();
                    Double freq = (Double) actionEntry.getValue();
                    if (!dashboardActionCountMap.get(dashBoard).containsKey(action)) {
                        dashboardActionCountMap.get(dashBoard).put(action, freq);
                    } else {
                        dashboardActionCountMap.get(dashBoard).put(action, dashboardActionCountMap.get(dashBoard).get(action) + freq);
                    }
                }
            }

        }
        return dashboardActionCountMap;
    }

    public static void writeSessionsToFile(ArrayList<WikiSession> filteredSessions) {
        try (PrintWriter writer = new PrintWriter("filteredsessions.csv", "UTF-8")) {
            for (WikiSession session : filteredSessions) {
                writer.println(session.toString());
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(SessionConstruction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static ArrayList<CountWikiSession> countActionsPerSession(ArrayList<WikiSession> filteredSessions) {
        ArrayList<CountWikiSession> actionCountList = new ArrayList<>();
        TreeMap<UniqueAction, Double> uniqueActionCountMap;

        for (WikiSession session : filteredSessions) {
            CountWikiSession countWikiSession = new CountWikiSession(session.getSessionKey(), session.getDashBoard(), new TreeMap<String, Double>(), session.getTimeGroup());
            ArrayList<Action> actions = session.getActions();
            uniqueActionCountMap = new TreeMap<>();

            for (Action a : actions) {
                String action = a.getAction();

                // count normal actions
                if (!countWikiSession.getSessionActionsDistribution().containsKey(action)) {
                    countWikiSession.getSessionActionsDistribution().put(action, 1.0);
                } else {
                    countWikiSession.getSessionActionsDistribution().put(action, countWikiSession.getSessionActionsDistribution().get(action) + 1.0);
                }

                // count unique actions
                String uniqueActionName = action + "_Unique";
                String url = a.getDashboard().replace("?gesis=dashboard", "");
                UniqueAction uniqueAction = new UniqueAction(uniqueActionName, url);

                if (!uniqueActionCountMap.containsKey(uniqueAction)) {
                    uniqueActionCountMap.put(uniqueAction, 1.0);
                } else {
                    uniqueActionCountMap.put(uniqueAction, uniqueActionCountMap.get(uniqueAction) + 1.0);
                }
            }

            // copy unique Actions into normal action count map
            for (Entry<UniqueAction, Double> e : uniqueActionCountMap.entrySet()) {
                countWikiSession.getSessionActionsDistribution().put(e.getKey().getAction(), e.getValue());
            }

            actionCountList.add(countWikiSession);

        }
        return actionCountList;
    }

    public static ArrayList<WikiSession> assignDashboardAndTimeGroupToSessions(TreeMap<String, ArrayList<Action>> sessions) {
        ArrayList<WikiSession> filteredSessions = new ArrayList<>();
        Iterator<Entry<String, ArrayList<Action>>> eIt = sessions.entrySet().iterator();
        while (eIt.hasNext()) {
            Entry<String, ArrayList<Action>> e = eIt.next();
            ArrayList<Action> sessionActions = (ArrayList<Action>) e.getValue();
            String timeGroup;
            Date firstActionDate = sessionActions.get(0).getTimestamp();
            if (firstActionDate.getTime() < g0_endDate.getTime()) {
                timeGroup = "G0";
            } else if (firstActionDate.getTime() < g1_endDate.getTime()) {
                timeGroup = "G1";
            } else {
                timeGroup = "G2";
            }
            boolean found = false;
            String sessionKey = (String) e.getKey();
            for (Action a : sessionActions) {
                if (!found && a.getDashboard().contains("/sofiswiki/Spezial:GesisDashboard")) {
                    found = true;
                    WikiSession wikisession = new WikiSession(sessionKey, a.getDashboard(), sessionActions, timeGroup);
                    filteredSessions.add(wikisession);
                    //break;
                }
            }
            if (!found) {
                WikiSession wikisession = new WikiSession(sessionKey, "/sofiswiki/Spezial:GesisDashboardNONE", sessionActions, timeGroup);
                filteredSessions.add(wikisession);
            }
        }
        return filteredSessions;
    }

    public static TreeMap<String, ArrayList<Action>> createSessions(boolean useSessions) throws ParseException {
        TreeMap<String, ArrayList<Action>> sessionMap = null;
        try (BufferedReader br = new BufferedReader(new FileReader("./User_action_detail_FULL.csv"))) {
            String line;
            String user;
            String[] lineArray;
            String action;
            String user_id;
            Date time;
            String dashboard;

            int headerlines = 8;
            for (int i = 1; i <= headerlines; i++) {
                br.readLine(); // skip header lines
            }

            TreeMap<String, ArrayList<Action>> userMap = new TreeMap<>();

            while ((line = br.readLine()) != null) {
                // USER;user_id;action;dashboard;url;TIME
                lineArray = line.split(";");
                if (lineArray.length == 6) {
                    user = lineArray[0];
                    if (!user.toLowerCase().contains("gesis") && !user.equals("WikiWorker")) {

                        user_id = lineArray[1];
                        action = lineArray[2];
                        dashboard = lineArray[4];
                        time = df.parse(lineArray[5]);

                        if (!userMap.containsKey(user_id)) {
                            ArrayList<Action> actionsList = new ArrayList<>();
                            userMap.put(user_id, actionsList);
                        }
                        userMap.get(user_id).add(new Action(action, time, dashboard, user_id));
                    }
                } else {
                    // System.err.println("A line could not be parsed");
                }
            }
            filterErrorneousActions(userMap, 2000);
            filterAutomaticActions(userMap);

            if (useSessions) {
                sessionMap = splitIntoLengthNSessions(userMap);
//                sessionMap = splitIntoSessions(userMap);
            } else {
                sessionMap = userMap;
            }

        } catch (IOException ex) {
            System.err.println(ex);
        }
        return sessionMap;
    }

    public static TreeMap<String, ArrayList<Action>> splitIntoSessions(TreeMap<String, ArrayList<Action>> userMap) {
        TreeMap<String, ArrayList<Action>> sessionMap = new TreeMap<>();
        for (Entry e : userMap.entrySet()) {
            int sessionid = 0;
            String user = (String) e.getKey();
            ArrayList<Action> sessionActions = new ArrayList<>();
            ArrayList<Action> actions = (ArrayList) e.getValue();
            Collections.sort(actions);
            sessionActions.add(actions.get(0));
            sessionMap.put(user + "_" + sessionid, sessionActions);
            Date previousTimeStamp = actions.get(0).getTimestamp();

            for (int i = 1; i < actions.size(); i++) {
                if (TimeUnit.MILLISECONDS.toMinutes(actions.get(i).getTimestamp().getTime() - previousTimeStamp.getTime()) >= idleTime) {
                    // new session started
                    sessionid++;
                    sessionActions = new ArrayList<>();
                    sessionActions.add(actions.get(i));
                    sessionMap.put(user + "_" + sessionid, sessionActions);
                } else {
                    sessionMap.get(user + "_" + sessionid).add(actions.get(i));
                }
                previousTimeStamp = actions.get(i).getTimestamp();
            }
        }
        return sessionMap;
    }

    public static TreeMap<String, ArrayList<Action>> splitIntoLengthNSessions(TreeMap<String, ArrayList<Action>> userMap) {
        TreeMap<String, ArrayList<Action>> sessionMap = new TreeMap<>();

        for (Entry e : userMap.entrySet()) {
            int sessionid = 0;
            String user = (String) e.getKey();
            ArrayList<Action> sessionActions = new ArrayList<>();
            ArrayList<Action> actions = (ArrayList) e.getValue();
            Collections.sort(actions);
            sessionMap.put(user + "_" + sessionid, sessionActions);
            boolean loginFound = false;
            Date loginTimeStamp = null;

            for (Action a : actions) {
                if (!loginFound && a.getAction().equals("LOGIN")) {
                    loginFound = true;
                    sessionActions.add(a);
                    loginTimeStamp = a.getTimestamp();
                } //                else if (loginFound && a.getAction().equals("LOGIN") && TimeUnit.MILLISECONDS.toMinutes(a.getTimestamp().getTime() - loginTimeStamp.getTime()) <= sessionLength) {
                //                    loginTimeStamp = a.getTimestamp();
                //                    sessionid++;
                //                    sessionActions = new ArrayList<>();
                //                    sessionActions.add(a);
                //                    sessionMap.put(user + "_" + sessionid, sessionActions);
                //                } 
                else if (loginFound && TimeUnit.MILLISECONDS.toMinutes(a.getTimestamp().getTime() - loginTimeStamp.getTime()) <= sessionLength) {
                    sessionActions.add(a);
                } else if (loginFound && TimeUnit.MILLISECONDS.toMinutes(a.getTimestamp().getTime() - loginTimeStamp.getTime()) > sessionLength && a.getAction().equals("LOGIN")) {
                    loginTimeStamp = a.getTimestamp();
                    sessionid++;
                    sessionActions = new ArrayList<>();
                    sessionActions.add(a);
                    sessionMap.put(user + "_" + sessionid, sessionActions);
                }
            }
        }
        return sessionMap;
    }

    private static void filterErrorneousActions(TreeMap<String, ArrayList<Action>> userActions, int threshold) {
        for (Entry<String, ArrayList<Action>> e : userActions.entrySet()) {
            ArrayList<Action> actions = e.getValue();
            Collections.sort(actions);
            Iterator<Action> aIt = actions.iterator();
            Action previousAction = aIt.next();

            // only applies if the first action is a search 
            if (previousAction.getAction().contains("SEARCH")) {
                previousAction.setAction("SEARCH");
            }

            while (aIt.hasNext()) {
                Action currentAction = aIt.next();

                if (currentAction.getAction().contains("SEARCH")) {
                    currentAction.setAction("SEARCH");
                }
                if (currentAction.getAction().equals(previousAction.getAction()) && currentAction.getTimestamp().getTime() - previousAction.getTimestamp().getTime() <= threshold && currentAction.getDashboard().equals(previousAction.getDashboard())) {
                    aIt.remove();
                } else {
                    previousAction = currentAction;
                }
            }
        }
    }

    private static void filterAutomaticActions(TreeMap<String, ArrayList<Action>> userActions) {
        for (Entry<String, ArrayList<Action>> e : userActions.entrySet()) {
            ArrayList<Action> actions = e.getValue();
            Collections.sort(actions);
            Iterator<Action> aIt = actions.iterator();
            Action previousAction = aIt.next();
            while (aIt.hasNext()) {
                Action currentAction = aIt.next();
                renameDashboardViewsIfNecessary(previousAction, currentAction);
                if (isAutomaticAction(previousAction.getAction(), currentAction.getAction())) {
                    aIt.remove();
                    if (aIt.hasNext()) {
                        // do not delete more than a pair
                        previousAction = aIt.next();
                    }
                } else {
                    previousAction = currentAction;
                }
            }
        }
    }

    private static void renameDashboardViewsIfNecessary(Action previousAction, Action currentAction) {
        if (previousAction.getAction().equals("LOGIN") && currentAction.getAction().equals("View") && currentAction.getDashboard().contains("/sofiswiki/Spezial:GesisDashboard")) {
            currentAction.setAction("Dashboard View");
        }
    }

    private static boolean isAutomaticAction(String firstAction, String secondAction) {
        if (firstAction.equals("CREATE") && secondAction.equals("View")) {
            return true;
        } else if (firstAction.equals("EDIT: FORM") && secondAction.equals("View")) {
            return true;
        } else if (firstAction.equals("EDIT: SOURCE") && secondAction.equals("View")) {
            return true;
        } else if (firstAction.equals("LOGOUT") && secondAction.equals("View")) {
            return true;
        } else if (firstAction.equals("WATCH") && secondAction.equals("View")) {
            return true;
        } else if (firstAction.equals("VIEW: Watchlist Changes") && secondAction.equals("View")) {
            return true;
        } else if (firstAction.equals("CATEGORY: View") && secondAction.equals("View")) {
            return true;
        } else {
            return false;
        }
    }

    private static TreeMap<String, Integer> countSessionsPerDashboard(ArrayList<WikiSession> filteredSessions) {
        TreeMap<String, Integer> sessionCounts = new TreeMap<>();
        for (WikiSession w : filteredSessions) {
            String dashBoard = w.getDashBoard().split("GesisDashboard")[1];
            if (!sessionCounts.containsKey(dashBoard)) {
                sessionCounts.put(dashBoard, 1);
            } else {
                sessionCounts.put(dashBoard, sessionCounts.get(dashBoard) + 1);
            }
        }
        return sessionCounts;
    }

    private static TreeMap<String, TreeMap<String, Double>> normalizeDashboardCounts(TreeMap<String, TreeMap<String, Double>> dashBoardActionCountMap, TreeMap<String, Integer> sessionCounts) {
        for (Entry dashBoardEntry : dashBoardActionCountMap.entrySet()) {
            String dashBoard = (String) dashBoardEntry.getKey();
            TreeMap<String, Double> actionCounts = (TreeMap<String, Double>) dashBoardEntry.getValue();
            for (Entry actionEntry : actionCounts.entrySet()) {
                String action = (String) actionEntry.getKey();
                actionCounts.put(action, actionCounts.get(action) / sessionCounts.get(dashBoard));
            }
        }
        return dashBoardActionCountMap;
    }

//    private static void countConcurrentActions(TreeMap<String, ArrayList<Action>> sessions) {
//        String[] allActions = {"CATEGORY: View", "CREATE", "EDIT: FORM", "EDIT: SOURCE", "LOGIN", "LOGOUT", "SAVE: Attempt", "SAVE: Complete", "SEARCH", "UNWATCH", "VIEW: Watchlist Changes", "View", "WATCH"};
//        TreeMap<String, Integer> concurrenActionCountMap = new TreeMap<>();
//        TreeMap<String, Integer> TotalActionCountMap = new TreeMap<>();
//
//        for (String action1 : allActions) {
//            for (String action2 : allActions) {
//                concurrenActionCountMap.put(action1 + "_" + action2, 0);
//            }
//        }
//        for (Entry<String, ArrayList<Action>> e : sessions.entrySet()) {
//            String id = e.getKey();
//            ArrayList<Action> actions = e.getValue();
//            if (actions.size() >= 2) {
//                String previousAction = actions.get(0).getAction();
//
//                if (previousAction.contains("SEARCH")) {
//                    previousAction = "SEARCH";
//                }
//
//                for (int i = 1; i < actions.size(); i++) {
//                    if (!TotalActionCountMap.containsKey(previousAction)) {
//                        TotalActionCountMap.put(previousAction, 1);
//                    } else {
//                        TotalActionCountMap.put(previousAction, TotalActionCountMap.get(previousAction) + 1);
//                    }
//                    String currentAction = actions.get(i).getAction();
//                    if (currentAction.contains("SEARCH")) {
//                        currentAction = "SEARCH";
//                    }
//                    concurrenActionCountMap.put(previousAction + "_" + currentAction, concurrenActionCountMap.get(previousAction + "_" + currentAction) + 1);
//                    previousAction = currentAction;
//                }
//            }
//        }
//        for (Entry<String, Integer> e : concurrenActionCountMap.entrySet()) {
//            String firstAction = e.getKey().split("_")[0];
//            String secondAction = e.getKey().split("_")[1];
//            System.out.println(firstAction + ";" + secondAction + ";"+ e.getValue() / (double) TotalActionCountMap.get(firstAction));
//        }
//
//    }
    private static void countConcurrentActions(ArrayList<WikiSession> sessions) {
        String[] allActions = {"CATEGORY: View", "CREATE", "EDIT: FORM", "EDIT: SOURCE", "LOGIN", "LOGOUT", "SAVE: Attempt", "SAVE: Complete", "SEARCH", "UNWATCH", "VIEW: Watchlist Changes", "View", "WATCH"};
        TreeMap<String, Integer> concurrenActionCountMap = new TreeMap<>();
        TreeMap<String, Integer> TotalActionCountMap = new TreeMap<>();

        for (String action1 : allActions) {
            for (String action2 : allActions) {
                concurrenActionCountMap.put(action1 + "_" + action2, 0);
            }
        }
        for (WikiSession w : sessions) {
            String id = w.getSessionKey();
            ArrayList<Action> actions = w.getActions();
            if (actions.size() >= 2) {
                String previousAction = actions.get(0).getAction();

                if (previousAction.contains("SEARCH")) {
                    previousAction = "SEARCH";
                }

                for (int i = 1; i < actions.size(); i++) {
                    if (!TotalActionCountMap.containsKey(previousAction)) {
                        TotalActionCountMap.put(previousAction, 1);
                    } else {
                        TotalActionCountMap.put(previousAction, TotalActionCountMap.get(previousAction) + 1);
                    }
                    String currentAction = actions.get(i).getAction();
                    if (currentAction.contains("SEARCH")) {
                        currentAction = "SEARCH";
                    }
                    concurrenActionCountMap.put(previousAction + "_" + currentAction, concurrenActionCountMap.get(previousAction + "_" + currentAction) + 1);
                    previousAction = currentAction;
                }
            }
        }
        for (Entry<String, Integer> e : concurrenActionCountMap.entrySet()) {
            String firstAction = e.getKey().split("_")[0];
            String secondAction = e.getKey().split("_")[1];
            System.out.println(firstAction + ";" + secondAction + ";" + e.getValue() / (double) TotalActionCountMap.get(firstAction));
        }

    }

    private static TreeMap<String, LinkedList<String[]>> createBruteForceHypotheses() {

        TreeMap<String, LinkedList<String[]>> hypothesis = new TreeMap<>();

        String[] dashboards = {"NONE", "A1F0", "A1F2", "A2F0", "A2F2", "A3F0", "A3F2", "A5F0", "A5F2", "A1F1", "A1F3", "A2F1", "A2F3", "A3F1", "A3F3", "A5F1", "A5F3"};
        List<String> dbList = new LinkedList<>(Arrays.asList(dashboards));

        Iterator<String> it1 = dbList.iterator();

        while (it1.hasNext()) {
            String db1 = it1.next();
            Iterator<String> it2 = dbList.iterator();
            while (it2.hasNext()) {
                String db2 = it2.next();
                if (!db1.equals(db2)) {
                    String[] condA = new String[1];
                    condA[0] = db1;
                    String[] condB = new String[1];
                    condB[0] = db2;
                    LinkedList<String[]> h = new LinkedList<>();
                    h.add(condA);
                    h.add(condB);
                    hypothesis.put(db1 + "-" + db2, h);
                }
            }
            it1.remove();
        }
        return hypothesis;
    }

    private static void countActionsPerDay(ArrayList<WikiSession> dashboardSessions) {
        ArrayList<Action> actions = new ArrayList<>();
        TreeMap<String, TreeMap<String, Integer>> actionsPerDayCountMap = new TreeMap<>();

        for (WikiSession w : dashboardSessions) {
            actions.addAll(w.getActions());
        }

        String actionString;
        LocalDate date;
        TreeMap<String, Integer> actionsOnThisDay;

        for (Action a : actions) {
            actionString = a.getAction();
            if (Arrays.asList(relevantActions).contains(actionString)) {
                date = LocalDate.fromDateFields(a.getTimestamp());
                if (!actionsPerDayCountMap.containsKey(date.toString())) {
                    actionsPerDayCountMap.put(date.toString(), new TreeMap<String, Integer>());
                }

                actionsOnThisDay = actionsPerDayCountMap.get(date.toString());

                if (!actionsOnThisDay.containsKey(actionString)) {
                    actionsOnThisDay.put(actionString, 1);
                } else {
                    actionsOnThisDay.put(actionString, actionsOnThisDay.get(actionString) + 1);
                }
            }

        }

        LocalDate startDate = LocalDate.fromDateFields(Collections.min(actions).getTimestamp());
        LocalDate endDate = LocalDate.fromDateFields(Collections.max(actions).getTimestamp());

        System.out.print("day");
        for (String relAction : relevantActions) {
            System.out.print(";" + relAction);
        }
        System.out.println();

        for (LocalDate day = startDate; day.isBefore(endDate); day = day.plusDays(1)) {
            String line = day.toString();
            TreeMap<String, Integer> dayActions = actionsPerDayCountMap.get(day.toString());
            if (dayActions == null) {
                for (String relAction : relevantActions) {
                    line += ";0";
                }
            } else {
                for (String relAction : relevantActions) {
                    line += ";" + ((dayActions.get(relAction) == null) ? "0" : dayActions.get(relAction));
                }
            }
            System.out.println(line);
        }

    }

    private static void filterSessionWithoutLogin(ArrayList<WikiSession> dashboardSessions) {
        Iterator<WikiSession> wIt = dashboardSessions.iterator();
        while (wIt.hasNext()) {
            WikiSession wikiSession = wIt.next();
            if (wikiSession.getDashBoard().equals("/sofiswiki/Spezial:GesisDashboardNONE")) {
                boolean found = false;
                for (Action a : wikiSession.getActions()) {
                    if (a.getAction().equals("LOGIN")) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    wIt.remove();
                }
            }
        }
    }

    private static void filterByTimeGroup(ArrayList<WikiSession> dashboardSessions, String filteredTimeGroup) {
        Iterator<WikiSession> wIt = dashboardSessions.iterator();
        while (wIt.hasNext()) {
            WikiSession wikiSession = wIt.next();
            if (wikiSession.getTimeGroup().equals(filteredTimeGroup)) {
                wIt.remove();
            }
        }
    }

}
